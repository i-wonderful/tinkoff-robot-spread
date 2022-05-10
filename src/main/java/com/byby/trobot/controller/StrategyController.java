package com.byby.trobot.controller;

import com.byby.trobot.service.StrategyManager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Path;


@Path("/robot")
@ApplicationScoped
public class StrategyController {

    @Inject
    StrategyManager strategyManager;

    @Path("/start")
    public void go() {
        strategyManager.go();
    }
}
