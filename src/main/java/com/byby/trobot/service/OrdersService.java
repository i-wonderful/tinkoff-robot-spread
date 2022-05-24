package com.byby.trobot.service;

import ru.tinkoff.piapi.contract.v1.OrderTrades;

import java.util.function.Consumer;

/**
 * Операции с заявками
 */
public interface OrdersService {

    /**
     * Подписаться на стрим моих заявок.
     *
     * @param listener
     */
    void subscribeTradesStream(Consumer<OrderTrades> listener);
}
