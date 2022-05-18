package com.byby.trobot.strategy.impl;

import com.byby.trobot.strategy.impl.model.OrderPair;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.utils.MapperUtils;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public class Helper {
    /**
     * Найти пару заявкок на покупку и продажу с оптимальными ценами:
     * максимальная цена покупки и минимальная цена продажию
     *
     * @param orders список моих заявок по одной акции
     * @return пара заявок на продажу и покупку
     */
    protected static OrderPair getOrderPair(List<OrderState> orders) {
        OrderPair orderPair = new OrderPair();
        if (orders == null || orders.isEmpty()) {
            return orderPair;
        }

        orders.stream()
                .filter(orderState -> OrderDirection.ORDER_DIRECTION_BUY.equals(orderState.getDirection()))
                .sorted(Comparator.comparing(Helper::getPriceBigDecimal))
                .findFirst()
                .ifPresent(orderSate -> {
                    orderPair.setFigi(orderSate.getFigi());
                    orderPair.setBuy(orderSate);
                });

        orders.stream()
                .filter(orderState -> OrderDirection.ORDER_DIRECTION_SELL.equals(orderState.getDirection()))
                .sorted(Comparator.comparing(Helper::getPriceBigDecimal).reversed())
                .findFirst()
                .ifPresent(orderState -> {
                            orderPair.setFigi(orderState.getFigi());
                            orderPair.setSell(orderState);
                        }
                );

        return orderPair;
    }

    protected static Order getFirstBid(GetOrderBookResponse orderBook) {
        return orderBook.getBidsCount() > 0 ?
                orderBook.getBids(0) :
                null;
    }

    protected static Order getFirstAsk(GetOrderBookResponse orderBook) {
        return orderBook.getAsksCount() > 0 ?
                orderBook.getAsks(0) :
                null;
    }


    protected static Order getFirstBid(OrderBook orderBook) {
        return orderBook.getBidsCount() > 0 ?
                orderBook.getBids(0) :
                null;
    }

    protected static Order getFirstAsk(OrderBook orderBook) {
        return orderBook.getAsksCount() > 0 ?
                orderBook.getAsks(0) :
                null;
    }

    private static BigDecimal getPriceBigDecimal(OrderState orderState) {
        return MapperUtils.moneyValueToBigDecimal(orderState.getInitialSecurityPrice());
    }
}
