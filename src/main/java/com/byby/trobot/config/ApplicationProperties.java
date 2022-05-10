package com.byby.trobot.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import java.util.Collections;
import java.util.List;

// todo сделать через @ConfigMapping
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

    @ConfigProperty(name = "robot.strategy.find.buy.tickers")
    List<String> findBuyTickers;

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

    public List<String> getFindBuyTickers() {
        // todo ?
        return findBuyTickers.size() == 1 && findBuyTickers.get(0).equals("FIND_ALL") ?
                Collections.emptyList() :
                findBuyTickers;
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
