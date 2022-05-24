package com.byby.trobot.service.impl;

import com.byby.trobot.cache.AppCache;
import com.byby.trobot.controller.exception.CriticalException;
import com.byby.trobot.service.OrdersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.OrderTrades;
import ru.tinkoff.piapi.contract.v1.TradesStreamResponse;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.stream.MarketDataSubscriptionService;
import ru.tinkoff.piapi.core.stream.OrdersStreamService;
import ru.tinkoff.piapi.core.stream.StreamProcessor;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.function.Consumer;

@ApplicationScoped
public class OrdersServiceImpl implements OrdersService {
    private static final Logger log = LoggerFactory.getLogger(OrdersServiceImpl.class);

    private OrdersStreamService ordersStreamService;
    private AppCache appCache;

    public OrdersServiceImpl(InvestApi investApi, AppCache appCache) {
        this.ordersStreamService = investApi.getOrdersStreamService();
        this.appCache = appCache;
    }

    /**
     * Подписка стрим сделок.
     *
     * @param listener обработчик
     */
    public void subscribeTradesStream(Consumer<OrderTrades> listener) {
        StreamProcessor<TradesStreamResponse> consumer = response -> {
            if (response.hasPing()) {
                log.info("TradesStream пинг сообщение");
            } else if (response.hasOrderTrades()) {
                OrderTrades ot = response.getOrderTrades();
                log.info(">>> TradesStream Новые данные по сделкам: {}", ot.getOrderId());
                listener.accept(ot);
            }
        };

        Consumer<Throwable> onErrorCallback = error -> {
            throw new CriticalException(error, "Ошибка подписки на OrdersStreamService.subscribeTrades");
        };
        // List<String> accounts = List.of(appCache.getAccountId()); todo make non blocking
        ordersStreamService.subscribeTrades(consumer, onErrorCallback);
    }
}
