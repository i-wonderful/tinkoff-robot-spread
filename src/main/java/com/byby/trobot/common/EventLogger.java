package com.byby.trobot.common;

import com.byby.trobot.service.impl.SharesService;
import io.quarkus.vertx.ConsumeEvent;
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
    private static final Logger log = LoggerFactory.getLogger(">>>");

    @Inject
    SharesService sharesService;

    @Inject
    EventBus bus;

    public void log(String message) {
        log.info(message);
        bus.publish(LOG.name(), message);
    }

    public void log(String message, String figi) {
        String ticker = sharesService.findTickerByFigi(figi);
        log.info("[" + ticker + "] " + message);
        bus.publish(LOG.name(), "[" + ticker + "] " + message);
    }

    public void log(String message, List<String> figis) {
        String tickers = figis.stream()
                .map(figi -> sharesService.findTickerByFigi(figi))
                .collect(Collectors.joining(","));
        log.info("[" + tickers + "] " + message);
        bus.publish(LOG.name(), "[" + tickers + "] " + message);
    }
}
