package com.byby.trobot.strategy.impl.model;

import java.math.BigDecimal;

public class Spread {
    private String figi;
    private String ticker;
    private BigDecimal diff;
    private double percent;

    private BigDecimal minBuyPrice;
    private BigDecimal maxAskPrice;

    public Spread() {
    }

    //    public Spread(String figi, String ticker, BigDecimal diff, double percent) {
//        this.figi = figi;
//        this.ticker = ticker;
//        this.diff = diff;
//        this.percent = percent;
//    }
//
    public Spread(String figi, BigDecimal diff, double percent) {
        this.figi = figi;
        this.diff = diff;
        this.percent = percent;
    }

    public Spread(String figi) {
        this.figi = figi;
        this.diff = BigDecimal.ZERO;
        this.percent = 0.0;
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

    @Override
    public String toString() {
        return "Spread{" +
                "figi='" + figi + '\'' +
                ", ticker='" + ticker + '\'' +
                ", diff=" + diff +
                ", percent=" + percent +
                '}';
    }

    public BigDecimal getMinBuyPrice() {
        return minBuyPrice;
    }

    public void setMinBuyPrice(BigDecimal minBuyPrice) {
        this.minBuyPrice = minBuyPrice;
    }

    public BigDecimal getMaxAskPrice() {
        return maxAskPrice;
    }

    public void setMaxAskPrice(BigDecimal maxAskPrice) {
        this.maxAskPrice = maxAskPrice;
    }
}
