package com.byby.trobot.config;

import static com.byby.trobot.common.GlobalBusAddress.*;

import com.byby.trobot.dto.codec.ListCodec;
import com.byby.trobot.dto.codec.OrderStateDtoCodec;
import com.byby.trobot.service.impl.ExchangeService;
import com.byby.trobot.service.impl.SharesService;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.handler.sockjs.SockJSHandler;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.List;

@ApplicationScoped
public class ApplicationInit {
    @Inject
    Vertx vertx;
    @Inject
    SharesService sharesService;

    public void init(@Observes Router router) {
        System.out.println(">>> Init App");

        SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
        sockJSHandler.bridge(new SockJSBridgeOptions()
                .addOutboundPermitted(new PermittedOptions().setAddress(LOG))
                .addOutboundPermitted(new PermittedOptions().setAddress(LOG_ORDER)));
        router.route("/eventbus/*").handler(sockJSHandler);

        vertx.eventBus().unregisterCodec(OrderStateDtoCodec.NAME);
        vertx.eventBus().registerCodec(new OrderStateDtoCodec());
        vertx.eventBus().unregisterCodec(ListCodec.NAME);
        vertx.eventBus().registerCodec(new ListCodec());

        // init cache
        sharesService.getShares().subscribe().with(shares -> System.out.println(">>> Get Shares init"));
    }
}
