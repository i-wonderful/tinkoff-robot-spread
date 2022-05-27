package com.byby.trobot.strategy.impl;

import com.byby.trobot.common.EventLogger;
import com.byby.trobot.config.RobotSandboxProperties;
import com.byby.trobot.executor.Executor;
import com.byby.trobot.service.OrderbookService;
import com.byby.trobot.service.SpreadService;
import com.byby.trobot.service.StatisticService;
import com.byby.trobot.service.impl.SpreadServiceImpl;
import com.byby.trobot.strategy.Strategy;
import com.byby.trobot.strategy.impl.model.OrderPair;
import com.byby.trobot.strategy.impl.model.Spread;
import io.smallrye.mutiny.Uni;
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

import static com.byby.trobot.strategy.impl.Helper.*;

/**
 * Стратегия опеределяет что и когда купить и продать.
 */
@ApplicationScoped
public class SpreadStrategy implements Strategy {
    private static final Logger log = LoggerFactory.getLogger(SpreadStrategy.class);

    @Inject
    OrderbookService orderbookService;
    @Inject
    SpreadService spreadService;
    @Inject
    Instance<Executor> executor;
    @Inject
    EventLogger eventLogger;
    @Inject
    SpreadDecision spreadDecision;
    @Inject
    StatisticService statisticService;
    @Inject
    RobotSandboxProperties properties;

    boolean isSubscribedToTradesStream = false;

    @Override
    public void start(List<String> figi) {
        if (figi == null || figi.isEmpty()) {
            eventLogger.log("!!! Список акций в стратегии не указан. Запуск не осуществлен.");
            return;
        }

        eventLogger.log("Запуск стратегии. Отслеживаем акции.", figi);



        // выставляем начальные заявки
        // и подписываемся на стакан
        postFirstOrders(figi)
                .subscribe()
                .with((t) -> {
                    eventLogger.log("Подписываемся на стакан.", figi);
                    orderbookService.subscribeOrderBook(figi, (orderBook) -> {
                        processOrderbook(orderBook);
                    });
                });
    }

    /**
     * Stop: Отписываемся от стаканов.
     */
    @Override
    public Uni<Void> stopListening(List<String> figiUnsucscribe) {
        if (figiUnsucscribe != null && !figiUnsucscribe.isEmpty()) {
            orderbookService.unsucscribeOrderbook(figiUnsucscribe);
            eventLogger.log("Отписываемся от стаканов", figiUnsucscribe);
        }
        return Uni.createFrom().voidItem();
    }

    private Uni<Void> postFirstOrders(List<String> figi) {
        eventLogger.log("Выставляем первые заявки.", figi);
        return Uni.join().all(figi.stream()
                        .map(orderbookService::getOrderbook)
                        .map(orderBookResponseUni -> orderBookResponseUni
                                .onItem()
                                .transformToUni(orderBookResponse -> processOrderbook(orderBookResponse)))
                        .collect(Collectors.toList()))
                .andCollectFailures()
                .replaceWithVoid();
    }

    /**
     * @param orderBook
     */
    public Uni<Void> processOrderbook(GetOrderBookResponse orderBook) {
        return spreadService.calcSpread(orderBook)
                .call(spread -> {
                    Order bid = getFirstBid(orderBook);
                    Order ask = getFirstAsk(orderBook);
                    processOrderbook(spread, bid, ask);
                    return Uni.createFrom().voidItem();
                })
                .replaceWithVoid();
    }

    /**
     * @param orderBook
     */
    private void processOrderbook(OrderBook orderBook) {
        spreadService.calcSpread(orderBook)
                .subscribe()
                .with(s -> {
                    Order bid = getFirstBid(orderBook);
                    Order ask = getFirstAsk(orderBook);
                    processOrderbook(s, bid, ask);
                });
    }

    /**
     * Обработать новые данные по сткану:
     * выставить или снять заявки если необходимо.
     *
     * @param spread       спред
     * @param bidOrderbook первая заявка на покупку
     * @param askOrderbook первая заявка на продажу
     */
    private void processOrderbook(Spread spread, Order bidOrderbook, Order askOrderbook) {
        String figi = spread.getFigi();
        eventLogger.log(String.format("Новые данные по стакану, spread=%f, %f%%", spread.getDiff().doubleValue(), spread.getPercent()), figi);

        // мои заявки на покупку/продажу
        OrderPair myOrderPair = getMyCurrentOpenOrders(figi);

        if (!spreadDecision.isAppropriate(spread)) {
            // спред слишком маленький.
            // снимаем заявки на покупку, продажу если есть
            eventLogger.log("Спред меньше лимита. Убираем заявки если есть.", figi);
            cancelOrders(myOrderPair);
        } else {
            // спред подходящий.
            // проверяем есть ли заявки на покупку и выставляем оптимальные
            eventLogger.log("Спред подходящий. Проверяем наличие заявок buy/sell.", figi);
            processBuyOrder(myOrderPair.getBuy(), spread, bidOrderbook);
            processSellOrder(myOrderPair.getSell(), spread, askOrderbook);
        }
    }

    /**
     * @param myAsk
     * @param spread
     * @param askOrderbook
     */
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
                cancelOrder(myAsk);
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
                eventLogger.log(String.format("Оптимальная заявка на покупку уже есть. price=%f", MapperUtils.moneyValueToBigDecimal(myBid.getInitialSecurityPrice())), figi);
            } else {
                // отменяем предыдущую
                cancelOrder(myBid);
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
        executor.get().postBuyLimitOrder(figi, price)
                .subscribe()
                .with(response -> eventLogger.logPostOrder(response));
    }

    /**
     * Выставить лимитную заявку на продажу
     */
    private void postSellLimitOrder(String figi, BigDecimal price) {
        executor.get().postSellLimitOrder(figi, price)
                .subscribe()
                .with(response -> eventLogger.logPostOrder(response))
        ;
    }

    /**
     * Отменить заявку.
     *
     * @param order
     */
    private void cancelOrder(OrderState order) {
        if (order == null) {
            return;
        }
        String orderId = order.getOrderId();
        String figi = order.getFigi();
        OrderDirection orderDirection = order.getDirection();
        executor.get().cancelOrder(orderId)
                .subscribe()
                .with((t) -> eventLogger.logOrderCancel(orderId, figi, orderDirection));
    }

    /**
     * Отменить пару заявок в спреде.
     *
     * @param orderPair текущие заявки из спреда
     */
    private void cancelOrders(OrderPair orderPair) {
        cancelOrder(orderPair.getBuy());
        cancelOrder(orderPair.getSell());
    }

    /**
     * Открытые в данный момент пара заявков: на покупку и на продажу
     */
    private OrderPair getMyCurrentOpenOrders(String figi) {
        List<OrderState> orders = getMyCurrentOpenOrderStates(figi);
        return getOrderPair(orders);
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

}
