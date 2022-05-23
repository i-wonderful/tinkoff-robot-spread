package com.byby.trobot.controller.dto;

import java.math.BigDecimal;

public class MoneyDto {
    private BigDecimal value;
    private String currency;

    public MoneyDto() {
    }

    public MoneyDto(BigDecimal value, String currency) {
        this.value = value;
        this.currency = currency;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
