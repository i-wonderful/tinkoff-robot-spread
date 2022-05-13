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
import static com.byby.trobot.common.GlobalBusAddress.LOG_ORDER;

@ApplicationScoped
public class EventLogger {
    private static final Logger log = LoggerFactory.getLogger(EventLogger.class);

    @Inject
    SharesService sharesService;

    @Inject
    EventBus bus;

    public Uni log(String message) {
        log.info(message);
        bus.publish(LOG, message);
        return Uni.createFrom().voidItem();
    }

    public Uni log(String message, String figi) {
        String ticker = sharesService.findTickerByFigi(figi);
        log.info("[" + ticker + "] " + message + ", figi: " + figi);
        bus.publish(LOG, "[" + ticker + "] " + message);
        return Uni.createFrom().voidItem();
    }

    public Uni log(String message, List<String> figis) {
        String tickers = figis.stream()
                .map(figi -> sharesService.findTickerByFigi(figi))
                .collect(Collectors.joining(","));
        log.info("[" + tickers + "] " + message);
        bus.publish(LOG, "[" + tickers + "] " + message);
        return Uni.createFrom().voidItem();
    }

    public Uni logOrderBuyAdd(String orderId, double price, String figi) {
        String template = "[%s] Выставлена лимитная заявка на покупку по цене %f, orderId=%s";
        String ticker = sharesService.findTickerByFigi(figi);
        String message = String.format(template, ticker, price, orderId);
        bus.publish(LOG, message);
        bus.publish(LOG_ORDER, String.format("[%s] Add orderId=%s, price=%f", ticker, orderId, price));
        log.info(message);
        return Uni.createFrom().voidItem();
    }
}
