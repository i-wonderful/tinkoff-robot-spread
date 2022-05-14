package com.byby.trobot.config;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;

@ApplicationScoped
public class SandboxProperties {
    @ConfigProperty(name = "robot.sandbox.init.balance.usd")
    BigDecimal initBalanceUsd;
    @ConfigProperty(name = "robot.sandbox.init.balance.rub")
    BigDecimal initBalanceRub;

    public BigDecimal getInitBalanceUsd() {
        return initBalanceUsd;
    }

    public BigDecimal getInitBalanceRub() {
        return initBalanceRub;
    }


}
