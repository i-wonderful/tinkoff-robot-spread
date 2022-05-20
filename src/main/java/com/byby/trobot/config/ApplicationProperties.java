package com.byby.trobot.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
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

    @ConfigProperty(name = "robot.exchange.names")
    List<String> robotExchangeNames;


    public String getFigiUsd() {
        return figiUsd;
    }

    public String getTokenSandbox() {
        return tokenSandbox;
    }

    public String getTokenReal() {
        return tokenReal;
    }

    public List<String> getRobotExchangeNames() {
        return robotExchangeNames;
    }

    public boolean isSandboxMode() {
        return sandboxMode;
    }

}
