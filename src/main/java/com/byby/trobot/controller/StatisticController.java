package com.byby.trobot.controller;

import com.byby.trobot.controller.dto.RobotSessionDto;
import com.byby.trobot.db.entity.RobotSession;
import com.byby.trobot.service.StatisticService;
import io.smallrye.mutiny.Multi;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/statistic")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class StatisticController {

    @Inject
    StatisticService statisticService;

    @GET
    @Path("/all")
    public Multi<RobotSessionDto> getAll() {
        return statisticService.getAll();
    }
}
