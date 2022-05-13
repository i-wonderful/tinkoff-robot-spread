package com.byby.trobot.strategy.impl;

import com.byby.trobot.common.EventLogger;
import com.byby.trobot.config.ApplicationProperties;
import com.byby.trobot.executor.Executor;
import com.byby.trobot.service.impl.ExchangeService;
import com.byby.trobot.service.impl.OrderbookService;
import com.byby.trobot.service.impl.SpreadService;
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
    SpreadService spreadService;

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
        eventLogger.log("Ищем акции...");

        List<String> exchanges = exchangeService.getExchangesOpenNow();
        List<Share> shares = sharesService.getShares(exchanges);
        eventLogger.log(String.format("Получено %d акций с бирж %s", shares.size(), exchanges));

        shares = shares.subList(0, 200);// todo
        eventLogger.log("Отбираем подходящие акции среди первых " + shares.size());

        List<Spread> spreadsAll = spreadService.getSpreads(shares)
                .stream()
                .sorted(Comparator.comparingDouble(Spread::getPercent).reversed())
                .collect(Collectors.toList());
        log.info(">>> All spreads: " + spreadsAll);
        List<Spread> spreads = spreadsAll
                .stream()
                .filter(s -> properties.getRobotSpreadPercent() <= s.getPercent())
                .collect(Collectors.toList());
        log.info(">>> " + spreads);
        return spreads.stream()
                .map(Spread::getFigi)
                .limit(properties.getSharesMaxCount())
                .collect(Collectors.toList());
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
            Spread spread = spreadService.getSpread(f);
            if (isBigSpread(spread.getPercent())) {
//                this.postBuyOptimalLimitOrder(f);
//                this.postSellOptimalLimitOrder(f);
            }
        });

        // подписываемся на стакан
        orderbookService.subscribeOrderBook(figis, (orderBook) -> {
            processOrderbook(orderBook);
        });
    }

    // todo это метод для тестирования
    public void processOrderbook(GetOrderBookResponse orderBook) {
        Spread spread = spreadService.calcSpread(orderBook);
        Order bidOrderbook = orderBook.getBidsCount() > 0 ?
                orderBook.getBids(0) :
                null;
        processOrderbook(spread, bidOrderbook);
    }

    // todo пока паблик для тестирования
    public void processOrderbook(OrderBook orderBook) {
        Spread spread = spreadService.calcSpread(orderBook);
        Order bidOrderbook = orderBook.getBidsCount() > 0 ?
                orderBook.getBids(0) :
                null;
        processOrderbook(spread, bidOrderbook);
    }

    public void processOrderbook(Spread spread, Order bidOrderbook) {
        String figi = spread.getFigi();
        eventLogger.log(String.format("Новые данные по стакану, spread=%f, %f%%", spread.getDiff().doubleValue(),spread.getPercent()), figi);

        // мои заявки на покупку/продажу
        OrderPair myOrderPair = getMyCurrentOpenOrders(figi);

        // спред слишком маленький
        if (!isBigSpread(spread.getPercent())) {
            // снимаем заявки на покупку, продажу если есть
            cancelOrders(myOrderPair);
            eventLogger.log("Спред меньше лимита. Убираем заявки если есть.", figi);
            return;
        }

        eventLogger.log("Спред подходящий, проверяем наличие заявок buy/sell.", figi);

        // проверяем есть ли заявки
        OrderState myBid = myOrderPair.getBuy();
        if (myBid == null) {
            eventLogger.log("Заявок на покупку еще нет. Выставляем.", figi);
            postBuyOptimalLimitOrder(spread);
        } else {
            if (checkMyBuyOrderOptimal(myBid, bidOrderbook)) {
                eventLogger.log("Оптимальная заявка на покупку уже есть.", figi);
            } else {
                // отменяем предыдущую
                bus.send(CANCEL_ORDER, myBid.getOrderId());
                eventLogger.log("Отменена предыдущая заявка buy. orderId=" + myBid.getOrderId(), figi);
                // Выставляем новую
                postBuyOptimalLimitOrder(spread);
            }
        }


        // todo заявка на продажу


    }

    // Проверяем что цена моей заявки равна цене лучшей заявки на покупку в стакане.
    private boolean checkMyBuyOrderOptimal(OrderState myOrderBuy, Order bidFromOrderbook) {
        return executor.get().isMyBuyOrderOptimal(myOrderBuy, bidFromOrderbook);
    }

    private void postBuyOptimalLimitOrder(Spread spread) {
        //bus.send(POST_BUY_ORDER, figi); todo?
        BigDecimal price = spread.getNextBidPrice();
        PostOrderResponse response = executor.get().postBuyLimitOrder(spread.getFigi(), price);
        eventLogger.logOrderBuyAdd(response.getOrderId(), price.doubleValue(), spread.getFigi());
    }

    private void postSellOptimalLimitOrder(String figi) {
        // todo
    }

    private void cancelOrders(OrderPair orderPair) {
        if (orderPair.getBuy() != null) {
            bus.send(CANCEL_ORDER, orderPair.getBuy().getOrderId());
        }
        if (orderPair.getSell() != null) {
            bus.send(CANCEL_ORDER, orderPair.getSell().getOrderId());
        }
    }

    /**
     * Открытые в данный момент пара заявков: на покупку и на продажу
     */
    private OrderPair getMyCurrentOpenOrders(String figi) {
        OrderPair orderPair = new OrderPair();
        List<OrderState> orders = getMyCurrentOpenOrderStates(figi);
        orders.forEach(orderState -> {
            if (OrderDirection.ORDER_DIRECTION_BUY.equals(orderState.getDirection())) {
                orderPair.setBuy(orderState);
            } else if (OrderDirection.ORDER_DIRECTION_SELL.equals(orderState.getDirection())) {
                orderPair.setSell(orderState);
            }
        });
        return orderPair;
    }

    /**
     * Мои открытые в данный момент лимитные заявки
     */
    private List<OrderState> getMyCurrentOpenOrderStates(String figi) {
        List<OrderState> orderStates = executor.get().getMyOrders().await().indefinitely();
        return orderStates.stream()
                .filter(os -> os.getOrderType().equals(OrderType.ORDER_TYPE_LIMIT))
                .filter(os -> os.getExecutionReportStatus().equals(OrderExecutionReportStatus.EXECUTION_REPORT_STATUS_NEW))
                .filter(os -> figi.equals(os.getFigi()))
                .collect(Collectors.toList());
    }


    private boolean isBigSpread(double spreadPercent) {
        return properties.getRobotSpreadPercent() <= spreadPercent;
    }

}
