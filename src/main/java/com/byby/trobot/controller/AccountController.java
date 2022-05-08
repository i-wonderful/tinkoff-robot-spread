package com.byby.trobot.controller;

import com.byby.trobot.config.ApplicationProperties;
import com.byby.trobot.dto.PortfolioDto;
import com.byby.trobot.dto.SettingsRobotDto;
import com.byby.trobot.executor.Executor;
import io.smallrye.mutiny.Uni;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import static com.byby.trobot.dto.mapper.SettingsMapper.toDto;

@Path("/account")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class AccountController {
    @Inject
    Executor executor;

    @Inject
    ApplicationProperties properties;

    @GET
    @Path("/portfolio")
    public Uni<PortfolioDto> getPortfolio() {
        return Uni.createFrom().item(executor.getPortfolio());
    }

    @GET
    @Path("/settings")
    public Uni<SettingsRobotDto> getSettings(){
        return Uni.createFrom().item(toDto(properties));
    }
}
