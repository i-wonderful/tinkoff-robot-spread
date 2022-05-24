package com.byby.trobot.service.impl;


import com.byby.trobot.cache.AppCache;
import com.byby.trobot.controller.exception.CriticalException;
import com.byby.trobot.service.OrderbookService;
import io.smallrye.mutiny.Uni;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.MarketDataService;
import ru.tinkoff.piapi.core.stream.MarketDataStreamService;
import ru.tinkoff.piapi.core.stream.MarketDataSubscriptionService;
import ru.tinkoff.piapi.core.stream.OrdersStreamService;
import ru.tinkoff.piapi.core.stream.StreamProcessor;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.function.Consumer;

@ApplicationScoped
public class OrderbookServiceImpl implements OrderbookService {
    private static final Logger log = LoggerFactory.getLogger(OrderbookServiceImpl.class);
    private static final String ORDERBOOK_STREAM_NAME = "my_orderbook";
    private static final int ORDERBOOK_DEPTH = 1;

    private MarketDataStreamService marketDataStreamService;
    private MarketDataService marketDataService;
    private OrdersStreamService ordersStreamService;
    private AppCache appCache;

    public OrderbookServiceImpl(InvestApi api, AppCache appCache) {
        this.marketDataStreamService = api.getMarketDataStreamService();
        this.marketDataService = api.getMarketDataService();
        this.ordersStreamService = api.getOrdersStreamService();
        this.appCache = appCache;
    }

    /**
     * Подписаться на изменения стакана
     *
     * @param figi
     * @param listener
     */
    public void subscribeOrderBook(List<String> figi, Consumer<OrderBook> listener) {
        StreamProcessor<MarketDataResponse> processor = response -> {
            if (response.hasTradingStatus()) {
                log.info("Новые данные по статусам: {}", response);
            } else if (response.hasPing()) {
                log.info("пинг сообщение, figi {}", figi);
            } else if (response.hasOrderbook()) {
                listener.accept(response.getOrderbook());
            } else if (response.hasSubscribeOrderBookResponse()) {
                var successCount = response.getSubscribeOrderBookResponse().getOrderBookSubscriptionsList().stream().filter(el -> el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
                var errorCount = response.getSubscribeTradesResponse().getTradeSubscriptionsList().stream().filter(el -> !el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
                log.info("удачных подписок на стакан: {}", successCount);
                log.info("неудачных подписок на стакан: {}", errorCount);
            }
        };

        Consumer<Throwable> onErrorCallback = error -> log.error(error.toString());

        marketDataStreamService
                .newStream(ORDERBOOK_STREAM_NAME, processor, onErrorCallback)
                .subscribeOrderbook(figi, ORDERBOOK_DEPTH);
    }

    /**
     * Отменить подписки на стаканы акций.
     *
     * @param figi список акций
     */
    public void unsucscribeOrderbook(List<String> figi) {
        log.info(">>> Unsucscribe1");
        MarketDataSubscriptionService stream = marketDataStreamService
                .getStreamById(ORDERBOOK_STREAM_NAME);
        if (stream != null) {
            log.info(">>> Unsucscribe2");
            stream.unsubscribeOrderbook(figi, ORDERBOOK_DEPTH);
            log.info(">>> Unsucscribe3");
        }
    }

    public int streamCount() {
        return marketDataStreamService.streamCount();
    }

    public Uni<GetOrderBookResponse> getOrderbook(String figi) {
        return getOrderbook(figi, 1);
    }

    public Uni<GetOrderBookResponse> getOrderbook(String figi, int depth) {
        return Uni.createFrom().completionStage(marketDataService.getOrderBook(figi, depth));
    }

}
