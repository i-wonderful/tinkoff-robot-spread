package com.byby.trobot.strategy.impl;

import com.byby.trobot.common.EventLogger;
import com.byby.trobot.config.ApplicationProperties;
import com.byby.trobot.service.impl.ExchangeService;
import com.byby.trobot.service.impl.OrderbookService;
import com.byby.trobot.service.impl.ServiceUtil;
import com.byby.trobot.service.impl.SharesService;
import com.byby.trobot.strategy.Strategy;
import io.vertx.mutiny.core.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.Share;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.byby.trobot.common.GlobalBusAddress.*;

/**
 * Стратегия опеределяет что и когда купить и продать.
 * А сами покупки происходят в strategyManager.
 */
@ApplicationScoped
public class SpreadStrategy implements Strategy {
    private static final Logger log = LoggerFactory.getLogger(SpreadStrategy.class);

    @Inject
    EventBus bus;

    //todo заменить пропертиз на что-нибудь конфигурируемое
    @Inject
    ApplicationProperties properties;

    @Inject
    SharesService sharesService;

    @Inject
    OrderbookService orderbookService;

    @Inject
    ExchangeService exchangeService;

    @Inject
    EventLogger eventLogger;

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
            eventLogger.log(String.format("Будем торговать акциями из настроек tickers=%s", tickers));
            return sharesService.findByTicker(tickers).stream()
                    .map(Share::getFigi)
                    .collect(Collectors.toList());

        } else {
            eventLogger.log("Ищем акции...");

            List<String> exchanges = exchangeService.getExchangesOpenNow();
            List<Share> shares = sharesService.getShares(exchanges);
            eventLogger.log(String.format("Получено %d акций с бирж %s", shares.size(), exchanges));

            shares = shares.subList(0, 100);// todo
            eventLogger.log("Отбираем подходящие акции среди первых " + shares.size());

            return orderbookService.getSpreads(shares)
                    .stream()
                    .filter(s -> properties.getRobotSpreadPercent() <= s.getPercent())
                    .sorted(Comparator.comparingDouble(Spread::getPercent))
                    .map(Spread::getFigi)
                    .limit(properties.getSharesMaxCount())
                    .collect(Collectors.toList());
        }
    }

    @Override
    public void go(List<String> figis) {
        if (figis == null || figis.isEmpty()) {
            eventLogger.log("!!! Список акций в стратегии не указан");
        }

        // подписываемся на сделки
        if (!properties.isSandboxMode()) {
            orderbookService.subscribeTradesStream();
        }

        // подписываемся на стакан
        orderbookService.subscribeOrderBook(figis, (orderBook) -> {
            Spread spread = ServiceUtil.calcSpread(orderBook);
            String figi = orderBook.getFigi();

            eventLogger.log("Новые данные по стакану, spread: " + spread.getPercent() + "%", figi);
            if (makeDecisionBuySell(spread.getPercent())) {

                eventLogger.log("Спред подходящий, выставляем заявки buy/sell.", figi);
                bus.send(POST_BUY_ORDER.name(), figi);

                // todo заявка на продажу

            } else {
                // todo
                // снимаем заявки на покупку, продажу если есть
                eventLogger.log(" Спред меньше лимита. Убираем заявки если есть.", figi);
                bus.send(cancelBuyOrder, figi);
            }
        });
    }

    private boolean makeDecisionBuySell(double spreadPercent) {
        return properties.getRobotSpreadPercent() <= spreadPercent;
    }

//    private void log(String message) {
//        log.info(">>> " + message);
//        bus.publish(LOG.name(), message);
//    }

//    private void log(String message, String figi) {
//        String ticker = sharesService.findTickerByFigi(figi);
//        log.info(">>> [" + ticker + "] " + message);
//        bus.publish(LOG.name(), "[" + ticker + "] " + message);
//    }
}
