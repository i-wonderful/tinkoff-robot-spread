package com.byby.trobot.strategy.impl;

import com.byby.trobot.config.StrategySharesProperties;
import com.byby.trobot.strategy.impl.model.Spread;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Сервис принятия решения о подходящих акциях.
 */
@ApplicationScoped
public class SpreadDecision {

    @Inject
    StrategySharesProperties properties;

    /**
     * Принять решение: подходящий ли спред.
     *
     * @param spread
     * @return
     */
    public boolean isAppropriate(Spread spread) {
        boolean isPriceOk = true;
        if ("usd".equalsIgnoreCase(spread.getCurrency())) {
            isPriceOk = priceOk(properties.priceMaxUsd(), spread.getNextAskPrice() );
        } else if ("rub".equalsIgnoreCase(spread.getCurrency())) {
            isPriceOk = priceOk(properties.priceMaxRub(), spread.getNextAskPrice() );
        }
        boolean isSpreadOk = properties.spreadPercent() <= spread.getPercent();

        return isPriceOk && isSpreadOk;
    }

    private boolean priceOk(Optional<BigDecimal> maxPrice, BigDecimal price) {
        if (maxPrice.isPresent()) {
            return price.compareTo(maxPrice.get()) <= 0;
        }
        return true;
    }
}
