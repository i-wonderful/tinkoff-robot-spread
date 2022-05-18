package com.byby.trobot.strategy.impl.model;

import java.math.BigDecimal;

public class Spread {
    private String figi;
    private String ticker;
    private BigDecimal diff;
    private double percent;
    private String currency;
    // bid покупка. максимально выгодная цена для покупки, на шаг выше существующих
    private BigDecimal nextBidPrice;
    // ask продажа. максимально выгодная цена продажи, на шаг ниже сужествующих
    private BigDecimal nextAskPrice;

    public Spread() {
    }

    public Spread(String figi) {
        this.figi = figi;
        this.diff = BigDecimal.ZERO;
        this.percent = 0.0;
        this.currency = "";
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

    public BigDecimal getDiff() {
        return diff;
    }

    public void setDiff(BigDecimal diff) {
        this.diff = diff;
    }

    public double getPercent() {
        return percent;
    }

    public void setPercent(double percent) {
        this.percent = percent;
    }

    public BigDecimal getNextBidPrice() {
        return nextBidPrice;
    }

    public void setNextBidPrice(BigDecimal nextBidPrice) {
        this.nextBidPrice = nextBidPrice;
    }

    public BigDecimal getNextAskPrice() {
        return nextAskPrice;
    }

    public void setNextAskPrice(BigDecimal nextAskPrice) {
        this.nextAskPrice = nextAskPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    @Override
    public String toString() {
        return "Spread{" +
                "figi='" + figi + '\'' +
                ", ticker='" + ticker + '\'' +
                ", diff=" + diff +
                ", percent=" + percent +
                '}';
    }
}
