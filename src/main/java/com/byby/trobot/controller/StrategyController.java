package com.byby.trobot.controller;

import com.byby.trobot.strategy.StrategyManager;
import io.smallrye.mutiny.Uni;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * Действия робота: запуск, остановка и тд.
 */
@Path("/strategy")
@ApplicationScoped
public class StrategyController {

    @Inject
    StrategyManager strategyManager;

    /**
     * Запустить
     */
    @GET
    @Path("/start")
    public Uni start() {
        return strategyManager.start();
    }

    /**
     * Остановить отслеживание акций.
     */
    @GET
    @Path("/stop")
    public Uni<Void> stop() {
        return strategyManager.stop();
    }

    /**
     * Запущен ли робот
     */
    @GET
    @Path("/isrun")
    public Uni<Boolean> isRun() {
        return Uni.createFrom().item(strategyManager.isRun());
    }

}
