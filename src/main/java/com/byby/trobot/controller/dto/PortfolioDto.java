package com.byby.trobot.controller.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PortfolioDto {
    private boolean isSandbox;
    private String accountId;
    private BigDecimal balanceRub;
    private BigDecimal balanceUsd;
    private BigDecimal totalAmountCurrencies;
    private MoneyDto totalAmountShares;
    private List<PortfolioPositionDto> positions;
    private BigDecimal expectedYeld;

    public PortfolioDto(boolean isSandbox, String accountId) {
        this.isSandbox = isSandbox;
        this.accountId = accountId;
    }

    public boolean isSandbox() {
        return isSandbox;
    }

    public void setSandbox(boolean sandbox) {
        isSandbox = sandbox;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getBalanceRub() {
        return balanceRub;
    }

    public void setBalanceRub(BigDecimal balance) {
        this.balanceRub = balance;
    }

    public MoneyDto getTotalAmountShares() {
        return totalAmountShares;
    }

    public void setTotalAmountShares(MoneyDto totalAmountShares) {
        this.totalAmountShares = totalAmountShares;
    }

    public List<PortfolioPositionDto> getPositions() {
        return positions;
    }

    public void setPositions(List<PortfolioPositionDto> positions) {
        this.positions = positions;
    }

    public void addPosition(PortfolioPositionDto positionDto) {
        if (this.positions == null) {
            this.positions = new ArrayList<>();
        }
        this.positions.add(positionDto);
    }

    public BigDecimal getBalanceUsd() {
        return balanceUsd;
    }

    public void setBalanceUsd(BigDecimal balanceUsd) {
        this.balanceUsd = balanceUsd;
    }

    public BigDecimal getExpectedYeld() {
        return expectedYeld;
    }

    public void setExpectedYeld(BigDecimal expectedYeld) {
        this.expectedYeld = expectedYeld;
    }

    public BigDecimal getTotalAmountCurrencies() {
        return totalAmountCurrencies;
    }

    public void setTotalAmountCurrencies(BigDecimal totalAmountCurrencies) {
        this.totalAmountCurrencies = totalAmountCurrencies;
    }
}
