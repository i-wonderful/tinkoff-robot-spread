package com.byby.trobot.service;

import com.byby.trobot.db.entity.OrderDone;
import com.byby.trobot.db.entity.RobotSession;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import ru.tinkoff.piapi.contract.v1.OrderTrades;

/**
 *
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
    Multi<RobotSession> getAll();

    /**
     *
     * @param orderTrades
     * @return
     */
    Uni<Void> save(OrderTrades orderTrades);


}
