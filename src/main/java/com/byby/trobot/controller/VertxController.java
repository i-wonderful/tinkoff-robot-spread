package com.byby.trobot.controller;

import com.byby.trobot.config.ApplicationProperties;
import com.byby.trobot.dto.ExchangeOpenDto;
import com.byby.trobot.service.impl.ExchangeService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/vertx")
public class VertxController {

    @Inject
    EventBus bus;

    @Inject
    ExchangeService exchangeService;

    @Inject
    ApplicationProperties properties;

    @GET
    @Path("/hello")
    public Uni<String> hello(@QueryParam("name") String name) {
        return bus.<String>request("greetings", name)
                .onItem()
                .transform(response -> response.body());
    }

    @GET
    @Path("/testbus")
    public Uni<Void> testEventBus(){
        bus.send("postBuyOrder", "123");
        return Uni.createFrom().voidItem();
    }


}
