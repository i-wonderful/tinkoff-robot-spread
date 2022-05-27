package com.byby.trobot.controller.dto;

import com.byby.trobot.db.entity.OrderDoneDirection;

import javax.persistence.Column;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

public class OrderDoneDto {

    private String orderId;

    private String figi;

    private String ticker;

    // Цена одного лота
    private BigDecimal price;

    // Количество лотов
    private long quantity;

    // полная стоимость
    private BigDecimal fullPrice;

    private ZonedDateTime dateTimeDone;

    private OrderDoneDirection direction;

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getFigi() {
        return figi;
    }

    public void setFigi(String figi) {
        this.figi = figi;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public long getQuantity() {
        return quantity;
    }

    public void setQuantity(long quantity) {
        this.quantity = quantity;
    }

    public ZonedDateTime getDateTimeDone() {
        return dateTimeDone;
    }

    public void setDateTimeDone(ZonedDateTime dateTimeDone) {
        this.dateTimeDone = dateTimeDone;
    }

    public OrderDoneDirection getDirection() {
        return direction;
    }

    public void setDirection(OrderDoneDirection direction) {
        this.direction = direction;
    }

    public BigDecimal getFullPrice() {
        return fullPrice;
    }

    public void setFullPrice(BigDecimal fullPrice) {
        this.fullPrice = fullPrice;
    }
}
