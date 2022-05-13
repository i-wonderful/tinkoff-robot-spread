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
    private static final String TEMPLATE_ADD_ORDER_BUY = "[%s] Выставлена лимитная заявка на покупку по цене %f, orderId=%s";
    private static final String TEMPLATE_ADD_ORDER_SELL = "[%s] Выставлена лимитная заявка на продажу по цене %f, orderId=%s";


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
        String ticker = sharesService.findTickerByFigi(figi);
        String messageLog = String.format(TEMPLATE_ADD_ORDER_BUY, ticker, price, orderId);
        String messageLogOrder = String.format("[%s] Add BUY orderId=%s, price=%f", ticker, orderId, price);
        bus.publish(LOG, messageLog);
        bus.publish(LOG_ORDER, messageLogOrder);
        log.info(messageLog);
        return Uni.createFrom().voidItem();
    }

    public Uni logOrderSellAdd(String orderId, double price, String figi) {
        String ticker = sharesService.findTickerByFigi(figi);
        String messageLog = String.format(TEMPLATE_ADD_ORDER_SELL, ticker, price, orderId);
        String messageLogOrder = String.format("[%s] Add SELL orderId=%s, price=%f", ticker, orderId, price);
        bus.publish(LOG, messageLog);
        bus.publish(LOG_ORDER, messageLogOrder);
        log.info(messageLog);
        return Uni.createFrom().voidItem();
    }

    public Uni logOrderCancel(String orderId, String figi){
        String template = "[%s] Отменена заявка. orderId=%s";
        String ticker = sharesService.findTickerByFigi(figi);
        String message = String.format(template, ticker, orderId);
        bus.publish(LOG, message);
        bus.publish(LOG_ORDER, String.format("[%s] Cancel orderId=%s", ticker, orderId));
        log.info(message);
        return Uni.createFrom().voidItem();
    }
}
