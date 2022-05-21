package com.byby.trobot.db.entity;

import com.google.protobuf.Timestamp;
import com.google.type.DateTime;
import io.quarkus.hibernate.reactive.panache.PanacheEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

/**
 * Завершенные заявки.
 *
 */
@Entity
@Table(name = "order_done")
//@NamedQuery(name = "Customers.findAll", query = "SELECT c FROM OrderDone c ORDER BY c.ticker")
public class OrderDone extends PanacheEntity {
    @Column(length = 100)
    String orderId;

    @Column(length = 20)
    String figi;

    @Column(length = 10)
    String ticker;

    BigDecimal price;

    ZonedDateTime dateTimeDone;

    OrderDoneDirection direction;

    public String getFigi() {
        return figi;
    }

    public void setFigi(String figi) {
        this.figi = figi;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }


    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
