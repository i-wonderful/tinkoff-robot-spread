package com.byby.trobot.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Настройки стратегии.
 */
@ConfigMapping(prefix = "robot.strategy.shares")
public interface StrategySharesProperties {

    @WithName("tickers.find")
    Optional<List<String>> tickersFind();

    @WithName("tickers.exclude")
    Optional<List<String>> tickersExclude();

    @WithName("count.one.minute")
    int countOneMinute();

    @WithName("max.count")
    int maxCount();

    @WithName("spread.percent")
    double spreadPercent();

    @WithName("price.max.usd")
    Optional<BigDecimal> priceMaxUsd();

    @WithName("price.max.rub")
    Optional<BigDecimal> priceMaxRub();

}
