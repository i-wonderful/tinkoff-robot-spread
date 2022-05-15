package com.byby.trobot.strategy.impl;

import com.byby.trobot.common.EventLogger;
import com.byby.trobot.config.ApplicationProperties;
import com.byby.trobot.executor.Executor;
import com.byby.trobot.service.impl.OrderbookService;
import com.byby.trobot.service.impl.SpreadService;
import com.byby.trobot.service.impl.SharesService;
import com.byby.trobot.strategy.Strategy;
import com.byby.trobot.strategy.impl.model.OrderPair;
import com.byby.trobot.strategy.impl.model.Spread;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.utils.MapperUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static com.byby.trobot.common.GlobalBusAddress.*;

/**
 * Стратегия опеределяет что и когда купить и продать.
 */
@ApplicationScoped
public class SpreadStrategy implements Strategy {
    private static final Logger log = LoggerFactory.getLogger(SpreadStrategy.class);

    @Inject
    Vertx vertx;

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
    SpreadService spreadService;
    @Inject
    Instance<Executor> executor;
    @Inject
    EventLogger eventLogger;
    @Inject
    StrategyCacheManager cacheManager;

    @Override
    public void start(List<String> figis) {
        if (figis == null || figis.isEmpty()) {
            eventLogger.log("!!! Список акций в стратегии не указан. Поменяйте настройки.");
            return;
        }

        // подписываемся на сделки
        if (!properties.isSandboxMode()) {
            orderbookService.subscribeTradesStream((orderTrades) -> {
                // todo? оно надо?
                log.info(">>> Новые данные по заявке !!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            });
        }

        // выставляем начальные заявки

        // todo проверять наличие
        figis.forEach(f -> {
            Spread spread = spreadService.getSpreadSync(f);
            if (isBigSpread(spread.getPercent())) {
                postBuyLimitOrder(spread.getFigi(), spread.getNextBidPrice());
                postSellLimitOrder(spread.getFigi(), spread.getNextAskPrice());
            }
        });

        //
//        orderbookService.unsucscribeOrderbook(figi);

        // подписываемся на стакан
        orderbookService.subscribeOrderBook(figis, (orderBook) -> {
            processOrderbook(orderBook);
        });
    }

    @Override
    public Uni<Void> stop() {
        return cacheManager.getFigi()
                .invoke(orderbookService::unsucscribeOrderbook)
                .onItem()
                .transformToUni(f -> cacheManager.clear());
    }

    // todo это метод для тестирования
    public void processOrderbookTEST(GetOrderBookResponse orderBook) {
        Spread spread = spreadService.calcSpread(orderBook);
        Order bidOrderbook = orderBook.getBidsCount() > 0 ?
                orderBook.getBids(0) :
                null;
        Order askOrderbook = orderBook.getAsksCount() > 0 ?
                orderBook.getAsks(0) :
                null;
        processOrderbook(spread, bidOrderbook, askOrderbook);
    }

    private void processOrderbook(OrderBook orderBook) {
        Spread spread = spreadService.calcSpread(orderBook);
        Order bidOrderbook = orderBook.getBidsCount() > 0 ?
                orderBook.getBids(0) :
                null;
        Order askOrderbook = orderBook.getAsksCount() > 0 ?
                orderBook.getAsks(0) :
                null;
        processOrderbook(spread, bidOrderbook, askOrderbook);
    }

    /**
     * Обработать новые данные по сткану:
     * выставить или снять заявки если необходимо.
     *
     * @param spread       спред
     * @param bidOrderbook первая заявка на покупку
     * @param askOrderbook первая заявка на продажу
     */
    public void processOrderbook(Spread spread, Order bidOrderbook, Order askOrderbook) {
        String figi = spread.getFigi();
        eventLogger.log(String.format("Новые данные по стакану, spread=%f, %f%%", spread.getDiff().doubleValue(), spread.getPercent()), figi);

        // мои заявки на покупку/продажу
        OrderPair myOrderPair = getMyCurrentOpenOrders(figi);

        if (!isBigSpread(spread.getPercent())) {
            // спред слишком маленький.
            // снимаем заявки на покупку, продажу если есть
            eventLogger.log("Спред меньше лимита. Убираем заявки если есть.", figi);
            cancelOrders(myOrderPair);
        } else {
            // спред подходящий.
            // проверяем есть ли заявки на покупку и выставляем оптимальную
            eventLogger.log("Спред подходящий. Проверяем наличие заявок buy/sell.", figi);
            processBuyOrder(myOrderPair.getBuy(), spread, bidOrderbook);
            processSellOrder(myOrderPair.getSell(), spread, askOrderbook);
        }
    }

    private void processSellOrder(OrderState myAsk, Spread spread, Order askOrderbook) {
        String figi = spread.getFigi();
        if (myAsk == null) {
            eventLogger.log("Заявок на продажу еще нет. Выставляем.", figi);
            postSellLimitOrder(figi, spread.getNextAskPrice());
        } else {
            if (isMySellOrderOptimal(myAsk, askOrderbook)) {
                eventLogger.log("Оптимальная заявка на продажу уже есть.", figi);
            } else {
                // отменяем предыдущую
                //bus.send(CANCEL_ORDER, myAsk.getOrderId());
                executor.get().cancelOrder(myAsk.getOrderId());
                eventLogger.logOrderCancel(myAsk.getOrderId(), figi);
                // Выставляем новую
                postSellLimitOrder(figi, spread.getNextAskPrice());
            }
        }
    }

    private void processBuyOrder(OrderState myBid, Spread spread, Order bidOrderbook) {
        String figi = spread.getFigi();
        if (myBid == null) {
            eventLogger.log("Заявок на покупку еще нет. Выставляем.", figi);
            postBuyLimitOrder(figi, spread.getNextBidPrice());
        } else {
            if (isMyBuyOrderOptimal(myBid, bidOrderbook)) {
                eventLogger.log("Оптимальная заявка на покупку уже есть. price=" + MapperUtils.moneyValueToBigDecimal(myBid.getInitialSecurityPrice()), figi);
            } else {
                // отменяем предыдущую
                //bus.send(CANCEL_ORDER, myBid.getOrderId());
                executor.get().cancelOrder(CANCEL_ORDER);
                eventLogger.logOrderCancel(myBid.getOrderId(), figi);
                // Выставляем новую
                postBuyLimitOrder(figi, spread.getNextBidPrice());
            }
        }
    }

    // Проверяем что моя заявка на покупку на вершине стакана
    private boolean isMyBuyOrderOptimal(OrderState myOrderBuy, Order bidFromOrderbook) {
        return executor.get().isMyBuyOrderOptimal(myOrderBuy, bidFromOrderbook);
    }

    private boolean isMySellOrderOptimal(OrderState myOrderSell, Order askFromOrderbook) {
        return executor.get().isMySellOrderOptimal(myOrderSell, askFromOrderbook);
    }

    /**
     * Выставить лимитную заявку на покупку.
     */
    private void postBuyLimitOrder(String figi, BigDecimal price) {
        PostOrderResponse response = executor.get().postBuyLimitOrder(figi, price);
        eventLogger.logPostOrder(response);
    }

    /**
     * Выставить лимитную заявку на продажу
     */
    private void postSellLimitOrder(String figi, BigDecimal price) {
        PostOrderResponse response = executor.get().postSellLimitOrder(figi, price);
        eventLogger.logPostOrder(response);
    }

    /**
     * Отменить заявки.
     */
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


    /**
     * Достаточно ли большой спред чтобы выставить заявки.
     *
     * @param spreadPercent
     * @return
     */
    private boolean isBigSpread(double spreadPercent) {
        return properties.getRobotSpreadPercent() <= spreadPercent;
    }

}
