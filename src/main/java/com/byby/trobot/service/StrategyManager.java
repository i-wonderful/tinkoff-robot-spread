package com.byby.trobot.service;

public interface StrategyManager {
    void start();

    void stop();

    void cancelAllOrders();

    boolean isRun();
}
