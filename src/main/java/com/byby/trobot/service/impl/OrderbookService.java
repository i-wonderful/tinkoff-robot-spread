package com.byby.trobot.service.impl;


import com.byby.trobot.strategy.impl.model.Spread;
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

        api.getMarketDataStreamService()
                .newStream("last_prices_stream", processor, onErrorCallback)
                .subscribeLastPrices(figi);
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
                .filter(spread -> !BigDecimal.ZERO.equals(spread.getDiff())) // todo убрать?
                .sorted(Comparator.comparingDouble(Spread::getPercent).reversed())
                .collect(Collectors.toList());
    }

    public Spread getSpread(String figi) {
        var orderBook = api.getMarketDataService().getOrderBookSync(figi, 1);
        return ServiceUtil.calcSpread(orderBook);
    }

}
