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
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static ru.tinkoff.piapi.core.utils.MapperUtils.quotationToBigDecimal;

@ApplicationScoped
public class OrderbookService {
    private static final Logger log = LoggerFactory.getLogger(OrderbookService.class);
    private static final String MY_STREAM_NAME = "my_orderbook";

    @Inject
    InvestApi api;

    public void  subscribeOrderBook1(List<String> figi, Consumer<OrderBook> listener) {
        log.info(">>> Подписываемся на orderbook, figi: " + figi);
        StreamProcessor<MarketDataResponse> processor = response -> {
            if (response.hasTradingStatus()) {
                log.info("Новые данные по статусам: {}", response);
            } else if (response.hasPing()) {
                log.info("пинг сообщение");
                //} else if (response.hasCandle()) {
                //    log.info("Новые данные по свечам: {}", response);
            } else if (response.hasOrderbook()) {
                OrderBook orderBook = response.getOrderbook();
                listener.accept(orderBook);
                log.info("Новые данные по стакану: {}, figi {}", response, orderBook.getFigi());
            }

        };

        Consumer<Throwable> onErrorCallback = error -> log.error(error.toString());

        api.getMarketDataStreamService().newStream("orderbook_stream", processor, onErrorCallback).subscribeOrderbook(figi);

    }

    public void subscribeOrderBook(List<String> figi) {
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

    public void unsucscribe(List<String> figi) {
        api.getMarketDataStreamService().getStreamById("orderbook_stream").subscribeOrderbook(figi);
    }

    public List<Spread> getSpread(List<String> figi) {
        return figi.stream()
                .map(f -> getSpread(f))
                .filter(spread -> !BigDecimal.ZERO.equals(spread.getDiff()))
                .sorted(Comparator.comparingDouble(Spread::getPercent).reversed())
                .collect(Collectors.toList());
    }


    public List<Spread> getSpreadShapes(List<Share> share) {
        return getSpread(share.stream().map(Share::getFigi).collect(Collectors.toList()));
    }


    private Spread getSpread(String figi) {
        var orderBook = api.getMarketDataService().getOrderBookSync(figi, 1);
        return getSpread(orderBook);
    }

    private Spread getSpread(GetOrderBookResponse orderBook) {
        String figi = orderBook.getFigi();
        if (orderBook.getAsksCount() < 1 || orderBook.getBidsCount() < 1) {
            log.info(">>> Empty Order book, figi {} ", figi);
            return new Spread(figi, BigDecimal.ZERO, 0.0);
        }

        Order ask = orderBook.getAsks(0);
        Order bid = orderBook.getBids(0);

        return getSpread(ask, bid, figi);
    }

    public Spread getSpread(OrderBook orderBook) {
        String figi = orderBook.getFigi();
        if (orderBook.getAsksCount() < 1 || orderBook.getBidsCount() < 1) {
            log.info(">>> Empty Order book, figi {} ", figi);
            return new Spread(figi, BigDecimal.ZERO, 0.0);
        }

        Order ask = orderBook.getAsks(0);
        Order bid = orderBook.getBids(0);

        return getSpread(ask, bid, figi);
    }

    public double calcSpreadPercent(BigDecimal askPrice, BigDecimal bidPrice){
        BigDecimal diff = askPrice.remainder(bidPrice);
        return diff.multiply(BigDecimal.valueOf(100.0)).divide(askPrice, 9, RoundingMode.CEILING).doubleValue();
    }

    private Spread getSpread(Order ask, Order bid, String figi) {
        BigDecimal askPrice = quotationToBigDecimal(ask.getPrice());
        BigDecimal bidPrice = quotationToBigDecimal(bid.getPrice());

        BigDecimal diff = askPrice.remainder(bidPrice); // todo
        double percent = calcSpreadPercent(askPrice, bidPrice);

        log.info(">>> Figi: {}, Diff: {}, Percent: {}", figi, diff, percent);

        return new Spread(figi, diff, percent);
    }

}
