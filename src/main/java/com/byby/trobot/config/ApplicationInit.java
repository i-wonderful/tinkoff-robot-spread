package com.byby.trobot.config;

import static com.byby.trobot.common.GlobalBusAddress.*;

import com.byby.trobot.dto.codec.OrderStateDtoCodec;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.handler.sockjs.SockJSHandler;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

@ApplicationScoped
public class ApplicationInit {
    @Inject
    Vertx vertx;

    public void init(@Observes Router router) {
        System.out.println(">>> Init App");

        SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
        sockJSHandler.bridge(new SockJSBridgeOptions()
                .addOutboundPermitted(new PermittedOptions().setAddress(LOG))
                .addOutboundPermitted(new PermittedOptions().setAddress(LOG_ORDER)));
        router.route("/eventbus/*").handler(sockJSHandler);

        vertx.eventBus().unregisterCodec(OrderStateDtoCodec.NAME);
        vertx.eventBus().registerCodec(new OrderStateDtoCodec());

//        router.errorHandler(404, routingContext -> {
//            routingContext.response().setStatusCode(302).putHeader("Location", "/index.html").end();
//        });

        // for test
//        AtomicInteger atomicInteger = new AtomicInteger();
//        vertx.setPeriodic(1000, ignored -> vertx.eventBus().publish("LOG", atomicInteger.getAndIncrement()));
    }
}
