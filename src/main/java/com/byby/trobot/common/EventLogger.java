package com.byby.trobot.common;

import com.byby.trobot.service.impl.SharesService;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.util.List;
import java.util.stream.Collectors;

import static com.byby.trobot.common.GlobalBusAddress.LOG;

@ApplicationScoped
public class EventLogger {
    private static final Logger log = LoggerFactory.getLogger(EventLogger.class);

    @Inject
    SharesService sharesService;

    @Inject
    EventBus bus;

    public Uni<Void> log(String message) {
        log.info(message);
        bus.publish(LOG.name(), message);
        return Uni.createFrom().voidItem();
    }

    public Uni<Void> log(String message, String figi) {
        String ticker = sharesService.findTickerByFigi(figi);
        log.info("[" + ticker + "] " + message + ", figi: " + figi);
        bus.publish(LOG.name(), "[" + ticker + "] " + message);
        return Uni.createFrom().voidItem();
    }

    public Uni<Void> log(String message, List<String> figis) {
        String tickers = figis.stream()
                .map(figi -> sharesService.findTickerByFigi(figi))
                .collect(Collectors.joining(","));
        log.info("[" + tickers + "] " + message);
        bus.publish(LOG.name(), "[" + tickers + "] " + message);
        return Uni.createFrom().voidItem();
    }
}
