package com.byby.trobot.strategy.impl;

import com.byby.trobot.config.ApplicationProperties;
import com.byby.trobot.executor.Executor;
import com.byby.trobot.service.impl.OrderbookService;
import com.byby.trobot.service.impl.ServiceUtil;
import com.byby.trobot.service.impl.SharesService;
import com.byby.trobot.strategy.Strategy;
import io.vertx.mutiny.core.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.Share;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

import static com.byby.trobot.config.GlobalBusAddress.LOG;

@ApplicationScoped
public class SpreadStrategy implements Strategy {
    private static final Logger log = LoggerFactory.getLogger(SpreadStrategy.class);

    @Inject
    EventBus bus;

    @Inject
    ApplicationProperties properties;

    @Inject
    SharesService sharesService;

    @Inject
    OrderbookService orderbookService;

    /**
     * Ищем кандидатов на покупку.
     * Если указан параметр robot.strategy.find.buy.tickers, то берем их.
     * Если не указан, то ищем спреды среди всех акций
     * и берем те что больше параметра robot.strategy.spread.percent.
     *
     * @return список figi
     */
    @Override
    public List<String> findFigi() {
        List<String> tickers = properties.getFindBuyTickers();


        if (tickers != null && !tickers.isEmpty()) {
            log(String.format("Будем торговать акциями из настроек tickers=%s", tickers));
            return sharesService.findByTicker(tickers).stream()
                    .map(Share::getFigi)
                    .collect(Collectors.toList());

        } else {
            log("Ищем акции...");

            List<String> exchanges = properties.getRobotExchangeNames();
            List<Share> shares = sharesService.getShares(exchanges);
            log(String.format("Получено %d акций с бирж %s", shares.size(), exchanges));

            shares = shares.subList(0, 100);// todo
            log("Отбираем подходящие акции среди первых " + shares.size());

            return orderbookService.getSpreads(shares)
                    .stream()
                    .filter(s -> properties.getRobotSpreadPercent() <= s.getPercent())
                    .map(Spread::getFigi)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public void go(List<String> figis) {

        orderbookService.subscribeOrderBook(figis, (orderBook) -> {
            Spread spread = ServiceUtil.calcSpread(orderBook);
            String figi = orderBook.getFigi();
            log("Новые данные по стакану " + figi + ", spread: " + spread.getPercent() + "%", figi);

            if (makeDecisionBuySell(spread.getPercent())) {
                // todo
                log("Спред подходящий, выставляем заявки. " + figi, figi);
                //executor.get().postBuyOrder(orderBook.getFigi());
                bus.send("postBuyOrder", figi);
                // todo может мессаджем
            } else {
                // todo
                // снимаем заявки на покупку, продажу если есть
                log.info(">>>off Buy/Sell " + figi);
                bus.publish(LOG.name(), figi + " Спред меньше лимита. Убираем заявки если есть.");
                bus.send("cancelBuyOrder", figi);
            }
        });
    }

    private boolean makeDecisionBuySell(double spreadPercent) {
        return properties.getRobotSpreadPercent() <= spreadPercent;
    }

    private void log(String message) {
        log.info(">>> " + message);
        bus.publish(LOG.name(), message);
    }

    private void log(String message, String figi) {
        String ticker = sharesService.findTickerByFigi(figi);
        log.info(">>> [" + ticker + "] " + message);
        bus.publish(LOG.name(), "[" + ticker + "] " + message);
    }
}
