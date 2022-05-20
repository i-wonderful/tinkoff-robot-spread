package com.byby.trobot.config;

import static com.byby.trobot.common.GlobalBusAddress.*;

import com.byby.trobot.controller.VertxController;
import com.byby.trobot.dto.codec.ListCodec;
import com.byby.trobot.dto.codec.OrderStateDtoCodec;
import com.byby.trobot.executor.Executor;
import com.byby.trobot.service.StrategyManager;
import com.byby.trobot.service.impl.ExchangeService;
import com.byby.trobot.service.impl.SharesService;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.handler.sockjs.SockJSHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.core.exception.ApiRuntimeException;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.List;
import java.util.function.Consumer;

@ApplicationScoped
public class ApplicationInit {
    private static final Logger log = LoggerFactory.getLogger(ApplicationInit.class);
    @Inject
    Vertx vertx;
    @Inject
    SharesService sharesService;
    @Inject
    StrategyManager strategyManager;
    @Inject
    Instance<Executor> executor;

    public void init(@Observes Router router) {
        SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
        sockJSHandler.bridge(new SockJSBridgeOptions()
                .addOutboundPermitted(new PermittedOptions().setAddress(LOG))
                .addOutboundPermitted(new PermittedOptions().setAddress(LOG_ORDER)));
        router.route("/eventbus/*").handler(sockJSHandler);

        vertx.eventBus().unregisterCodec(OrderStateDtoCodec.NAME);
        vertx.eventBus().registerCodec(new OrderStateDtoCodec());
        vertx.eventBus().unregisterCodec(ListCodec.NAME);
        vertx.eventBus().registerCodec(new ListCodec());

        Consumer<Throwable> handler = exception -> {
            System.out.println(">>>>>>> Handler E Vertx " + exception.getMessage());
            log.error(">>> Error Vertx " + exception.getMessage());
        };

        vertx.exceptionHandler(handler);

        // init cache
        Uni.combine().all().unis(
                        executor.get().loadAccountId(),
                        sharesService.getShares())
                .discardItems()
                .subscribe()
                .with((t) -> System.out.println(">>> Init App"));
    }

    @PreDestroy
    public void preDestroy() {
        strategyManager.stop()
                .subscribe()
                .with(unused -> System.out.println(">>> Stop strategy"))
        ;
    }
}
