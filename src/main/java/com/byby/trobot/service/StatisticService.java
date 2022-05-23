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
     *
     * @return
     */
    Uni<Void> start();

    /**
     *
     * @return
     */
    Uni<Void> stop();

    /**
     *
     * @return
     */
    Multi<RobotSessionDto> getAll();

    /**
     *
     * @param orderTrades
     * @return
     */
    Uni<Void> save(OrderTrades orderTrades);


}
