package com.byby.trobot.config;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class ApplicationProperties {
    @ConfigProperty(name = "robot.sandbox.mode")
    boolean sandboxMode;

    @ConfigProperty(name = "tinkoff.figi.usd")
    String figiUsd;

    @ConfigProperty(name = "robot.token.sandbox")
    String tokenSandbox;

    @ConfigProperty(name = "robot.token.real")
    String tokenReal;

    @ConfigProperty(name = "robot.strategy.check.buy.tickers")
    List<String> checkBuyTickers;

    @ConfigProperty(name = "robot.exchange.names")
    List<String> robotExchangeNames;

    @ConfigProperty(name = "robot.strategy.spread.percent")
    double robotSpreadPercent;

    public String getFigiUsd() {
        return figiUsd;
    }

    public void setFigiUsd(String figiUsd) {
        this.figiUsd = figiUsd;
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

    public List<String> getCheckBuyTickers() {
        return checkBuyTickers;
    }

    public void setCheckBuyTickers(List<String> checkBuyTickers) {
        this.checkBuyTickers = checkBuyTickers;
    }

    public List<String> getRobotExchangeNames() {
        return robotExchangeNames;
    }

    public double getRobotSpreadPercent() {
        return robotSpreadPercent;
    }

    public boolean isSandboxMode() {
        return sandboxMode;
    }
}
