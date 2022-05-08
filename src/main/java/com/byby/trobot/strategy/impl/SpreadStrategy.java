package com.byby.trobot.strategy.impl;

import com.byby.trobot.config.ApplicationProperties;
import com.byby.trobot.executor.Executor;
import com.byby.trobot.service.impl.OrderbookService;
import com.byby.trobot.service.impl.SharesService;
import com.byby.trobot.strategy.Strategy;
import io.vertx.mutiny.core.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.OrderBook;
import ru.tinkoff.piapi.contract.v1.Share;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

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

    @Inject
    Executor executor;

    /**
     * Ищем кандидатов на покупку.
     * Если указан параметр robot.strategy.check.buy.tickers, то берем их.
     * Если не указан, то ищем спреды среди всех акций
     * и берем те что больше параметра robot.strategy.spread.percent.
     *
     * @return список figi
     */
    @Override
    public List<String> findFigi() {
        List<String> tickers = properties.getCheckBuyTickers();
        if (tickers != null && !tickers.isEmpty()) {
            return sharesService.findByTicker(tickers).stream()
                    .map(Share::getFigi)
                    .collect(Collectors.toList());

        } else {
            List<Share> shares = sharesService.getShares(properties.getRobotExchangeNames());
            return orderbookService.getSpreadShapes(shares)
                    .stream()
                    .filter(s -> properties.getRobotSpreadPercent() <= s.getPercent())
                    .map(Spread::getFigi)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public void go(List<String> figi) {
        // подписываемся на стакан
        bus.publish("log", "Подписываемся на стаканы " + figi);

        orderbookService.subscribeOrderBook1(figi, (orderBook) -> {
            Spread spread = orderbookService.getSpread(orderBook);
            log.info(">>> Listener Orderbook Spread: " + spread);
            if (properties.getRobotSpreadPercent() <= spread.getPercent() ) {
                // todo
                // выставляем заявки на покупку, продажу
                log.info(">>> Buy/Sell");
                // выставляем заявку на покупку
                executor.postBuyOrder(orderBook.getFigi());
            } else {
                // todo
                // снимаем заявки на покупку, продажу если есть
                log.info(">>>off Buy/Sell");
            }
        });
    }
}
