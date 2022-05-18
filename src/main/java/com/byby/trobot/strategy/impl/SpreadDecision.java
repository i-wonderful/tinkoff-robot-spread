package com.byby.trobot.strategy.impl;

import com.byby.trobot.config.ApplicationProperties;
import com.byby.trobot.strategy.impl.model.Spread;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class SpreadDecision {

    @Inject
    ApplicationProperties properties;

    /**
     * Принять решение: подходящий ли спред.
     *
     * @param spread
     * @return
     */
    public boolean isAppropriate(Spread spread) {
        boolean isPriceOk = true;
        if ("usd".equalsIgnoreCase(spread.getCurrency())) {
            isPriceOk = spread.getNextAskPrice().compareTo(properties.getShareMaxPriceUsd()) <= 0;
        } else if ("rub".equalsIgnoreCase(spread.getCurrency())) {
            isPriceOk = spread.getNextAskPrice().compareTo(properties.getShareMaxPriceRub()) <= 0;
        }
        boolean isSpreadOk = properties.getRobotSpreadPercent() <= spread.getPercent();

        return isPriceOk && isSpreadOk;
    }
}
