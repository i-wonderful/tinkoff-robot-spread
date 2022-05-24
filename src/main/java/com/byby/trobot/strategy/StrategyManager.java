package com.byby.trobot.strategy;

import io.smallrye.mutiny.Uni;

/**
 * Менеджер стратегии.
 * Управляет запуском, остановкой, работа с кешем, статистикой.
 */
public interface StrategyManager {

    /**
     * Запуск робота.
     *
     * @return
     */
    Uni<Void> start();

    /**
     * Остановка робота.
     *
     * @return
     */
    Uni<Void> stop();

    /**
     * Запущен ли робот
     *
     * @return
     */
    boolean isRun();
}
