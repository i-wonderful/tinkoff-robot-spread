package com.byby.trobot.controller;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

@Path("/vertx")
public class VertxController {

    @Inject
    EventBus bus;

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
