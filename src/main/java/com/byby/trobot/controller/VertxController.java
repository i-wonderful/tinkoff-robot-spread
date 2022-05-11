package com.byby.trobot.controller;

import com.byby.trobot.config.ApplicationProperties;
import com.byby.trobot.dto.ExchangeOpenDto;
import com.byby.trobot.executor.Executor;
import com.byby.trobot.service.impl.ExchangeService;
import com.byby.trobot.service.impl.SharesService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * For testing
 */
@Path("/vertx")
public class VertxController {

    @Inject
    EventBus bus;

    @Inject
    ExchangeService exchangeService;

    @Inject
    SharesService sharesService;

    @Inject
    ApplicationProperties properties;

    @Inject
    Instance<Executor> executor;

    @GET
    @Path("/hello")
    public Uni<String> hello(@QueryParam("name") String name) {
        return bus.<String>request("greetings", name)
                .onItem()
                .transform(response -> response.body());
    }

    //BBG004S68BR5
    @GET
    @Path("/testbus")
    public Uni<Void> testEventBus() {
        bus.send("postBuyOrder", "123");
        return Uni.createFrom().voidItem();
    }

    @GET
    @Path("/ticker")
    public String getTickerByFigi() {
        sharesService.findByTicker(List.of("NMTP"));
        return sharesService.findTickerByFigi("BBG004S68BR5");
    }

    @GET
    @Path("/cancelorder")
    public void cancelOrder(@QueryParam("orderId") String orderId) {
        executor.get().cancelOrder(orderId);
    }

}
