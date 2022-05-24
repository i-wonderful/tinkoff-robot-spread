package com.byby.trobot.service;

import io.smallrye.mutiny.Uni;
import ru.tinkoff.piapi.contract.v1.GetOrderBookResponse;
import ru.tinkoff.piapi.contract.v1.OrderBook;

import java.util.List;
import java.util.function.Consumer;

/**
 * Операции со стаканом.
 */
public interface OrderbookService {

    /**
     * Подписаться на ихменения в стакане.
     *
     * @param figi     акции
     * @param listener обработчик
     */
    void subscribeOrderBook(List<String> figi, Consumer<OrderBook> listener);

    /**
     * Отписаться
     *
     * @param figi акции
     */
    void unsucscribeOrderbook(List<String> figi);

    /**
     * Получать стакан по figi
     *
     * @param figi
     * @return
     */
    Uni<GetOrderBookResponse> getOrderbook(String figi);

    /**
     * Получить стакан заданной глубины по figi
     *
     * @param figi
     * @param depth
     * @return
     */
    Uni<GetOrderBookResponse> getOrderbook(String figi, int depth);

    /**
     * Количество стримов
     * @return
     */
    public int streamCount();
}
