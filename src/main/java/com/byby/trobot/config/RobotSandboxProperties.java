package com.byby.trobot.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

import java.math.BigDecimal;

/**
 * Настройки песочницы.
 */
@ConfigMapping(prefix = "robot.sandbox")
public interface RobotSandboxProperties {

    @WithName("mode")
    boolean isSandboxMode();

    @WithName("init.balance.usd")
    BigDecimal initBalanceUsd();

    @WithName("init.balance.rub")
    BigDecimal initBalanceRub();
}
