package com.byby.trobot.service.impl;


import com.byby.trobot.strategy.impl.model.Spread;
import io.quarkus.cache.CacheResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.stream.MarketDataSubscriptionService;
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

    @Inject
    InvestApi api;

    /**
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
                log.info("Новые данные по сделкам: {}", response);
            } else if (response.hasSubscribeOrderBookResponse()) {
                var successCount = response.getSubscribeOrderBookResponse().getOrderBookSubscriptionsList().stream().filter(el -> el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
                var errorCount = response.getSubscribeTradesResponse().getTradeSubscriptionsList().stream().filter(el -> !el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
                log.info("удачных подписок на стакан: {}", successCount);
                log.info("неудачных подписок на стакан: {}", errorCount);
            }
        };

        Consumer<Throwable> onErrorCallback = error -> log.error(error.toString());

        api.getMarketDataStreamService()
                .newStream(ORDERBOOK_STREAM_NAME, processor, onErrorCallback)
                .subscribeOrderbook(figi, 1);

//        api.getMarketDataStreamService()
//                .newStream("trades_stream", processor, onErrorCallback)
//                .subscribeTrades(figi);
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
                log.info("TradesStream Новые данные по сделкам: {}", response);
                listener.accept(ot);
            }
        };

        // todo
        Consumer<Throwable> onErrorCallback = error -> log.error(error.toString());

        //Подписка стрим сделок. Не блокирующий вызов
        //При необходимости обработки ошибок (реконнект по вине сервера или клиента), рекомендуется сделать onErrorCallback
        api.getOrdersStreamService().subscribeTrades(consumer, onErrorCallback);
    }

    public void unsucscribeOrderbook(List<String> figi) {
        MarketDataSubscriptionService stream = api.getMarketDataStreamService()
                .getStreamById(ORDERBOOK_STREAM_NAME);
        if (stream != null) {
            stream.unsubscribeOrderbook(figi, 1);
        }
    }

    public GetOrderBookResponse getOrderbook(String figi) {
        return getOrderbook(figi, 1);
    }

    public GetOrderBookResponse getOrderbook(String figi, int depth) {
        return api.getMarketDataService().getOrderBookSync(figi, depth);
    }

}
