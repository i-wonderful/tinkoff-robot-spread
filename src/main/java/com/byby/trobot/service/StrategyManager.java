package com.byby.trobot.service;

import io.smallrye.mutiny.Uni;

public interface StrategyManager {
    Uni<Void> start();

    Uni<Void> stop();

    Uni<Void> cancelAllOrders();

    boolean isRun();
}
