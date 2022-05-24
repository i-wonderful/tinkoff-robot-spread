package com.byby.trobot.strategy;

import io.smallrye.mutiny.Uni;

import java.util.List;

/**
 * Сервис поиска акций среди всех акций.
 */
public interface FindFigiService {

    /**
     * Найти акции-кандидаты для покупки.
     *
     * Запускает таймеры с интервалом в минуту.
     * Количество таймеров определяется исходя из общего количество доступных акций и количества обрабатываемых акций в один таймер.
     * Количество обрабатываемых в минуту акций задается параметром robot.strategy.shares.count.one.minute.
     * Поиск первой партии осуществляется без таймера.
     *
     * @return найденные figi акций иреди первых robot.strategy.shares.count.one.minute штук
     */
    Uni<List<String>> startFindFigi();

    /**
     * Остановать таймеры поиска.
     */
    void stopTimers();
}
