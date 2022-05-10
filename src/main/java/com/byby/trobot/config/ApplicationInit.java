package com.byby.trobot.config;

import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.handler.sockjs.SockJSHandler;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class ApplicationInit {
    @Inject
    Vertx vertx;

    public void init(@Observes Router router) {
        System.out.println(">>> Init App");

        SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
        sockJSHandler.bridge(new SockJSBridgeOptions()
                .addOutboundPermitted(new PermittedOptions().setAddress("LOG")));
        router.route("/eventbus/*").handler(sockJSHandler);

        router.errorHandler(404, routingContext -> {
            routingContext.response().setStatusCode(302).putHeader("Location", "/index.html").end();
        });

        // for test
//        AtomicInteger atomicInteger = new AtomicInteger();
//        vertx.setPeriodic(1000, ignored -> vertx.eventBus().publish("LOG", atomicInteger.getAndIncrement()));
    }
}