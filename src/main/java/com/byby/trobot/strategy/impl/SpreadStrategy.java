package com.byby.trobot.strategy.impl;

import com.byby.trobot.common.EventLogger;
import com.byby.trobot.config.ApplicationProperties;
import com.byby.trobot.executor.Executor;
import com.byby.trobot.service.impl.ExchangeService;
import com.byby.trobot.service.impl.OrderbookService;
import com.byby.trobot.service.impl.ServiceUtil;
import com.byby.trobot.service.impl.SharesService;
import com.byby.trobot.strategy.Strategy;
import com.byby.trobot.strategy.impl.model.OrderPair;
import com.byby.trobot.strategy.impl.model.Spread;
import io.vertx.mutiny.core.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.*;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static ru.tinkoff.piapi.core.utils.MapperUtils.*;
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
    Instance<Executor> executor;

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

            List<Spread> spreads = orderbookService.getSpreads(shares)
                    .stream()
                    .filter(s -> properties.getRobotSpreadPercent() <= s.getPercent())
                    .sorted(Comparator.comparingDouble(Spread::getPercent))
                    .collect(Collectors.toList());
            log.info(">>> " + spreads);
            return spreads.stream()
                    .map(Spread::getFigi)
                    .limit(properties.getSharesMaxCount())
                    .collect(Collectors.toList());
        }
    }

    @Override
    public void start(List<String> figis) {
        if (figis == null || figis.isEmpty()) {
            eventLogger.log("!!! Список акций в стратегии не указан");
        }

        // подписываемся на сделки
        if (!properties.isSandboxMode()) {
            orderbookService.subscribeTradesStream((orderTrades) -> {
                log.info(">>> Новые данные по заявке !!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            });
        }

        // выставляем начальные заявки
        figis.forEach(f -> {
            Spread spread = orderbookService.getSpread(f);
            if (makeDecisionBuySell(spread.getPercent())) {
                this.postBuyOptimalLimitOrder(f);
                this.postSellOptimalLimitOrder(f);
            }
        });

        // подписываемся на стакан
        orderbookService.subscribeOrderBook(figis, (orderBook) -> {
            Spread spread = ServiceUtil.calcSpread(orderBook);
            String figi = orderBook.getFigi();

            eventLogger.log("Новые данные по стакану, spread: " + spread.getPercent() + "%", figi);
            OrderPair orderPair = getOpenOrders(figi);
            if (makeDecisionBuySell(spread.getPercent())) {
                eventLogger.log("Спред подходящий, проверяем наличие заявок buy/sell.", figi);
                // проверяем есть ли заявки
                if (orderPair.getBuy() != null) {
                    OrderState buy = orderPair.getBuy();
                    if (checkOptimalBuyPrice(buy)) {
                        eventLogger.log("Оптимальная заявка на покупку уже есть.", figi);
                    } else {
                        // отменяем предыдущую
                        bus.send(BUY_ORDER, buy.getOrderId());
                        eventLogger.log("Отменена предыдущая заявка buy.", figi);
                        // Выставляем новую
                        postBuyOptimalLimitOrder(figi);
                    }
                } else {
                    // Выставляем
                    postBuyOptimalLimitOrder(figi);
                }


                // todo заявка на продажу

            } else {
                // снимаем заявки на покупку, продажу если есть
                cancelOrders(orderPair);
                eventLogger.log("Спред меньше лимита. Убираем заявки если есть.", figi);
            }
        });
    }

    private boolean checkOptimalBuyPrice(OrderState orderBuy) {
        MoneyValue initialPrice = orderBuy.getInitialSecurityPrice();
        Quotation minBuyPrice = sharesService.calcMinBuyPrice(orderBuy.getFigi());

        BigDecimal optimalPrice = quotationToBigDecimal(minBuyPrice);
        BigDecimal currentPrice = moneyValueToBigDecimal(initialPrice);

        log.info(">>> Optimal price: " + optimalPrice);
        log.info(">>> currentPrice: " + currentPrice);

        return optimalPrice.compareTo(currentPrice) == 0;
    }

    private void postBuyOptimalLimitOrder(String figi) {
        bus.send(POST_BUY_ORDER, figi);
        eventLogger.log("Выставлена заявка buy.", figi);
    }

    private void postSellOptimalLimitOrder(String figi) {
        // todo
    }

    private void cancelOrders(OrderPair orderPair) {
        if (orderPair.getBuy() != null) {
            bus.send(BUY_ORDER, orderPair.getBuy().getOrderId());
        }
        if (orderPair.getSell() != null) {
            bus.send(BUY_ORDER, orderPair.getSell().getOrderId());
        }
    }

    private OrderPair getOpenOrders(String figi) {
        OrderPair orderPair = new OrderPair();
        List<OrderState> orders = getCurrentOpenOrders(figi);
        orders.forEach(orderState -> {
            if (OrderDirection.ORDER_DIRECTION_BUY.equals(orderState.getDirection())) {
                orderPair.setBuy(orderState);
            } else if (OrderDirection.ORDER_DIRECTION_SELL.equals(orderState.getDirection())) {
                orderPair.setSell(orderState);
            }
        });
        return orderPair;
    }

    private List<OrderState> getCurrentOpenOrders(String figi) {
        List<OrderState> orderStates = executor.get().getOrders().await().indefinitely();
        return orderStates.stream()
                .filter(os -> os.getOrderType().equals(OrderType.ORDER_TYPE_LIMIT))
                .filter(os -> os.getExecutionReportStatus().equals(OrderExecutionReportStatus.EXECUTION_REPORT_STATUS_NEW))
                .filter(os -> figi.equals(os.getFigi()))
                .collect(Collectors.toList());
    }


    private boolean makeDecisionBuySell(double spreadPercent) {
        return properties.getRobotSpreadPercent() <= spreadPercent;
    }

}
