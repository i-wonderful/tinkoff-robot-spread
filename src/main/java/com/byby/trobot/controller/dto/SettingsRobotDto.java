package com.byby.trobot.controller.dto;

public class SettingsRobotDto {
    private boolean sandboxMode;
    private String exchangeNames;
    private double strategySpreadPercent;
    private String checkBuyTickers;
    private String tokenSandbox;
    private String tokenReal;

    public boolean isSandboxMode() {
        return sandboxMode;
    }

    public void setSandboxMode(boolean sandboxMode) {
        this.sandboxMode = sandboxMode;
    }

    public String getExchangeNames() {
        return exchangeNames;
    }

    public void setExchangeNames(String exchangeNames) {
        this.exchangeNames = exchangeNames;
    }

    public double getStrategySpreadPercent() {
        return strategySpreadPercent;
    }

    public void setStrategySpreadPercent(double strategySpreadPercent) {
        this.strategySpreadPercent = strategySpreadPercent;
    }

    public String getCheckBuyTickers() {
        return checkBuyTickers;
    }

    public void setCheckBuyTickers(String checkBuyTickers) {
        this.checkBuyTickers = checkBuyTickers;
    }

    public String getTokenSandbox() {
        return tokenSandbox;
    }

    public void setTokenSandbox(String tokenSandbox) {
        this.tokenSandbox = tokenSandbox;
    }

    public String getTokenReal() {
        return tokenReal;
    }

    public void setTokenReal(String tokenReal) {
        this.tokenReal = tokenReal;
    }
}
