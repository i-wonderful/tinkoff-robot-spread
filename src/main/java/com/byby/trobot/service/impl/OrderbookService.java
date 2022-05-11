package com.byby.trobot.service.impl;


import com.byby.trobot.strategy.impl.Spread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.stream.StreamProcessor;

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
                listener.accept(response.getOrderbook());
            } else if (response.hasTrade()) {
                log.info("Новые данные по сделкам: {}", response);
            }
        };

        Consumer<Throwable> onErrorCallback = error -> log.error(error.toString());

        api.getMarketDataStreamService()
                .newStream(ORDERBOOK_STREAM_NAME, processor, onErrorCallback)
                .subscribeOrderbook(figi);

        api.getMarketDataStreamService()
                .newStream("trades_stream", processor, onErrorCallback)
                .subscribeTrades(figi);
    }


    public void subscribeTradesStream() {
        StreamProcessor<TradesStreamResponse> consumer = response -> {
            if (response.hasPing()) {
                log.info("TradesStream пинг сообщение");
            } else if (response.hasOrderTrades()) {
                log.info("TradesStream Новые данные по сделкам: {}", response);
            }
        };

        // todo
        Consumer<Throwable> onErrorCallback = error -> log.error(error.toString());

        //Подписка стрим сделок. Не блокирующий вызов
        //При необходимости обработки ошибок (реконнект по вине сервера или клиента), рекомендуется сделать onErrorCallback
        api.getOrdersStreamService().subscribeTrades(consumer, onErrorCallback);
    }

    @Deprecated
    public void subscribeOrderBookExample(List<String> figi) {
        StreamProcessor<MarketDataResponse> processor = response -> {
            if (response.hasTradingStatus()) {
                log.info("Новые данные по статусам: {}", response);
            } else if (response.hasPing()) {
                log.info("пинг сообщение");
            } else if (response.hasCandle()) {
                log.info("Новые данные по свечам: {}", response);
            } else if (response.hasOrderbook()) {
                OrderBook orderBook = response.getOrderbook();
                log.info("Новые данные по стакану: {}, figi {}", response, orderBook.getFigi());
            } else if (response.hasTrade()) {
                log.info("Новые данные по сделкам: {}", response);
            } else if (response.hasSubscribeCandlesResponse()) {
                var successCount = response.getSubscribeCandlesResponse().getCandlesSubscriptionsList().stream().filter(el -> el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
                var errorCount = response.getSubscribeTradesResponse().getTradeSubscriptionsList().stream().filter(el -> !el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
                log.info("удачных подписок на свечи: {}", successCount);
                log.info("неудачных подписок на свечи: {}", errorCount);
            } else if (response.hasSubscribeInfoResponse()) {
                var successCount = response.getSubscribeInfoResponse().getInfoSubscriptionsList().stream().filter(el -> el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
                var errorCount = response.getSubscribeTradesResponse().getTradeSubscriptionsList().stream().filter(el -> !el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
                log.info("удачных подписок на статусы: {}", successCount);
                log.info("неудачных подписок на статусы: {}", errorCount);
            } else if (response.hasSubscribeOrderBookResponse()) {
                var successCount = response.getSubscribeOrderBookResponse().getOrderBookSubscriptionsList().stream().filter(el -> el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
                var errorCount = response.getSubscribeTradesResponse().getTradeSubscriptionsList().stream().filter(el -> !el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
                log.info("удачных подписок на стакан: {}", successCount);
                log.info("неудачных подписок на стакан: {}", errorCount);
            } else if (response.hasSubscribeTradesResponse()) {
                var successCount = response.getSubscribeTradesResponse().getTradeSubscriptionsList().stream().filter(el -> el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
                var errorCount = response.getSubscribeTradesResponse().getTradeSubscriptionsList().stream().filter(el -> !el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
                log.info("удачных подписок на сделки: {}", successCount);
                log.info("неудачных подписок на сделки: {}", errorCount);
            } else if (response.hasSubscribeLastPriceResponse()) {
                var successCount = response.getSubscribeLastPriceResponse().getLastPriceSubscriptionsList().stream().filter(el -> el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
                var errorCount = response.getSubscribeLastPriceResponse().getLastPriceSubscriptionsList().stream().filter(el -> !el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
                log.info("удачных подписок на последние цены: {}", successCount);
                log.info("неудачных подписок на последние цены: {}", errorCount);
            }
        };

        Consumer<Throwable> onErrorCallback = error -> log.error(error.toString());


        //Подписка на список инструментов. Не блокирующий вызов
        //При необходимости обработки ошибок (реконнект по вине сервера или клиента), рекомендуется сделать onErrorCallback
//        api.getMarketDataStreamService().newStream("trades_stream", processor, onErrorCallback).subscribeTrades(randomFigi);
//        api.getMarketDataStreamService().newStream("candles_stream", processor, onErrorCallback).subscribeCandles(randomFigi);
//        api.getMarketDataStreamService().newStream("info_stream", processor, onErrorCallback).subscribeInfo(randomFigi);
        api.getMarketDataStreamService().newStream("orderbook_stream", processor, onErrorCallback).subscribeOrderbook(figi);
        api.getMarketDataStreamService().newStream("last_prices_stream", processor, onErrorCallback).subscribeLastPrices(figi);


        //api.getMarketDataStreamService().getStreamById("trades_stream").subscribeOrderbook(randomFigi);
    }

    public void unsucscribeOrderbook(List<String> figi) {
        api.getMarketDataStreamService()
                .getStreamById(ORDERBOOK_STREAM_NAME)
                .subscribeOrderbook(figi);
    }

    public List<Spread> getSpreads(List<Share> share) {
        List<String> figi = share.stream().map(Share::getFigi).collect(Collectors.toList());
        return getSpread(figi);
    }

    public List<Spread> getSpread(List<String> figi) {
        return figi.stream()
                .map(f -> getSpread(f))
                .filter(spread -> !BigDecimal.ZERO.equals(spread.getDiff()))
                .sorted(Comparator.comparingDouble(Spread::getPercent).reversed())
                .collect(Collectors.toList());
    }

    private Spread getSpread(String figi) {
        var orderBook = api.getMarketDataService().getOrderBookSync(figi, 1);
        return ServiceUtil.calcSpread(orderBook);
    }

}
