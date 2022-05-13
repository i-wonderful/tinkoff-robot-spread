package com.byby.trobot.controller;

import com.byby.trobot.service.StrategyManager;
import io.smallrye.mutiny.Uni;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;


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
    public void start() {
        strategyManager.start();
    }

    @GET
    @Path("/stop")
    public void stop() {
        strategyManager.stop();
    }

    /**
     * Запущен ли робот
     */
    @GET
    @Path("/isrun")
    public Uni<Boolean> isRun() {
        return Uni.createFrom().item(strategyManager.isRun());
    }

    /**
     * Отменить все заявки
     */
    @GET
    @Path("/cancel-all-orders")
    public Uni cancelAllOrders() {
        strategyManager.cancelAllOrders();
        return Uni.createFrom().voidItem();
    }
}
