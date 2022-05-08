package com.byby.trobot.controller;

import com.byby.trobot.service.StrategyManager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Path;


@Path("/robot")
@ApplicationScoped
public class RobotController {

    @Inject
    StrategyManager strategyManager;

    @Path("/")
    public void go() {
        strategyManager.go();
    }
}
