package com.byby.trobot.controller.dto;

import java.math.BigDecimal;

public class PortfolioPositionDto {
    private String ticker;
    private String figi;
    private String name;
    private long quantity;
    private BigDecimal averagePrice;
    private String currency;
    private BigDecimal expectedYield;

    public String getFigi() {
        return figi;
    }

    public void setFigi(String figi) {
        this.figi = figi;
    }

    public long getQuantity() {
        return quantity;
    }

    public void setQuantity(long quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getExpectedYield() {
        return expectedYield;
    }

    public void setExpectedYield(BigDecimal expectedYield) {
        this.expectedYield = expectedYield;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public String getTicker() {
        return ticker;
    }

    public BigDecimal getAveragePrice() {
        return averagePrice;
    }

    public void setAveragePrice(BigDecimal averagePrice) {
        this.averagePrice = averagePrice;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
