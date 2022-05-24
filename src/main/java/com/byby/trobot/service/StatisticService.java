package com.byby.trobot.service;

import com.byby.trobot.controller.dto.RobotSessionDto;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import ru.tinkoff.piapi.contract.v1.OrderTrades;

/**
 * Сервис статистики
 */
public interface StatisticService {

    /**
     * Запуск робота.
     * Сохраняем в статистику начало сеанса.
     *
     * @return
     */
    Uni<Void> start();

    /**
     * Остановка робота.
     * Сохраняем в статистику время остановки.
     *
     * @return
     */
    Uni<Void> stop();

    /**
     * Получить все сеансы.
     *
     * @return
     */
    Multi<RobotSessionDto> getAll();

    /**
     * Сохранить выполненную заявку.
     *
     * @param orderTrades совершенная сделка.
     * @return
     */
    Uni<Void> save(OrderTrades orderTrades);


}
