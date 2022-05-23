package com.byby.trobot.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

import java.util.List;
import java.util.Optional;

/**
 * Общие настройки робота.
 */
@ConfigMapping(prefix = "robot")
public interface RobotProperties {

    @WithName("appname")
    String appname();

    @WithName("token.sandbox")
    Optional<String> tokenSandbox();

    @WithName("token.real")
    Optional<String> tokenReal();

    @WithName("exchange.names")
    List<String> exchangeNames();

    @WithName("margin.allow")
    boolean isMarginAllow();
}
