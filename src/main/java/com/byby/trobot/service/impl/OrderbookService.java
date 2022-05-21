package com.byby.trobot.service.impl;


import com.byby.trobot.controller.exception.ApiCallException;
import com.byby.trobot.strategy.impl.model.Spread;
import io.quarkus.cache.CacheResult;
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
import ru.tinkoff.piapi.core.utils.MapperUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@ApplicationScoped
public class OrderbookService {
    private static final Logger log = LoggerFactory.getLogger(OrderbookService.class);
    private static final String ORDERBOOK_STREAM_NAME = "my_orderbook";

    private MarketDataStreamService marketDataStreamService;
    private MarketDataService marketDataService;
    private OrdersStreamService ordersStreamService;

    public OrderbookService(InvestApi api) {
        this.marketDataStreamService = api.getMarketDataStreamService();
        this.marketDataService = api.getMarketDataService();
        this.ordersStreamService = api.getOrdersStreamService();
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
//                log.info("новый данные по стакану " + response.getOrderbook());
                listener.accept(response.getOrderbook());
            } else if (response.hasTrade()) {
                log.info("Новые данные по сделкам !!! из marketDataStreamService: {}", response);
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
                .subscribeOrderbook(figi, 1);

        marketDataStreamService // todo проверить!!!!
                .newStream("trades_stream", processor, onErrorCallback)
                .subscribeTrades(figi);
//
//        api.getMarketDataStreamService()
//                .newStream("last_prices_stream", processor, onErrorCallback)
//                .subscribeLastPrices(figi);
    }


    /**
     *
     */
    public void subscribeTradesStream(Consumer<OrderTrades> listener) {
        StreamProcessor<TradesStreamResponse> consumer = response -> {
            if (response.hasPing()) {
                log.info("TradesStream пинг сообщение");
            } else if (response.hasOrderTrades()) {
                OrderTrades ot = response.getOrderTrades();
                log.info(">>> TradesStream  Order complete: " + ot.getOrderId());
                log.info("TradesStream Новые данные по сделкам: {}", response);
                listener.accept(ot);
            }
        };

        // todo
        Consumer<Throwable> onErrorCallback = error -> {
            throw new ApiCallException(error.getMessage(), error, "ordersStreamService.subscribeTrades");
        };

        //Подписка стрим сделок. Не блокирующий вызов
        ordersStreamService.subscribeTrades(consumer, onErrorCallback);
    }

    /**
     * Отменить подписки на стаканы акций.
     *
     * @param figi список акций
     */
    public void unsucscribeOrderbook(List<String> figi) {
        MarketDataSubscriptionService stream = marketDataStreamService
                .getStreamById(ORDERBOOK_STREAM_NAME);
        if (stream != null) {
            stream.unsubscribeOrderbook(figi, 1);
        }
    }

    public Uni<GetOrderBookResponse> getOrderbook(String figi) {
        return getOrderbook(figi, 1);
    }

    public Uni<GetOrderBookResponse> getOrderbook(String figi, int depth) {
        return Uni.createFrom().completionStage(marketDataService.getOrderBook(figi, depth));
    }

}
