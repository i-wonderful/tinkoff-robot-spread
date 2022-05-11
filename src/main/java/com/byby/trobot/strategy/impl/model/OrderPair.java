package com.byby.trobot.strategy.impl.model;

import ru.tinkoff.piapi.contract.v1.OrderState;

public class OrderPair {
    private String figi;
    private OrderState buy;
    private OrderState sell;

    public OrderPair() {
    }

    public OrderPair(OrderState buy, OrderState sell) {
        this.buy = buy;
        this.sell = sell;
    }

    public OrderState getBuy() {
        return buy;
    }

    public void setBuy(OrderState buy) {
        this.buy = buy;
    }

    public OrderState getSell() {
        return sell;
    }

    public void setSell(OrderState sell) {
        this.sell = sell;
    }

    public String getFigi() {
        return figi;
    }

    public void setFigi(String figi) {
        this.figi = figi;
    }
}
