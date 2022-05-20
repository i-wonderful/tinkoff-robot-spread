package com.byby.trobot.controller;

import com.byby.trobot.cache.AppCache;
import com.byby.trobot.config.ApplicationProperties;
import com.byby.trobot.config.StrategySharesProperties;
import com.byby.trobot.executor.Executor;
import com.byby.trobot.executor.impl.RealExecutor;
import com.byby.trobot.service.impl.*;
import com.byby.trobot.strategy.impl.SpreadFindFigiService;
import com.byby.trobot.strategy.impl.SpreadStrategy;
import com.byby.trobot.cache.StrategyCacheManager;
import com.byby.trobot.strategy.impl.model.Spread;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.core.utils.MapperUtils;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * For testing
 */
@Path("/vertx")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VertxController {
    private static final Logger log = LoggerFactory.getLogger(VertxController.class);
    @Inject
    Vertx vertx;
    @Inject
    EventBus bus;

    @Inject
    ExchangeService exchangeService;
    @Inject
    SharesService sharesService;
    @Inject
    ApplicationProperties properties;
    @Inject
    SpreadService spreadService;

    @Inject
    OrderbookService orderbookService;

    //OrderbookStreamS
    @Inject
    StrategyManagerImpl strategyManager;
    @Inject
    SpreadFindFigiService findFigiService;
    @Inject
    SpreadStrategy strategy;
    @Inject
    StrategyCacheManager cacheManager;
    @Inject
    Instance<Executor> executor;

    @Inject
    AppCache appCache;

    @Inject
    RealExecutor realExecutor;

    @Inject
    StrategySharesProperties strategySharesProperties;


    //BBG004S68BR5
    @GET
    @Path("/testbus")
    public Uni<Void> testEventBus() {
        bus.send("postBuyOrder", "123");
        return Uni.createFrom().voidItem();
    }

    @GET
    @Path("/ticker")
    public Uni<String> getTickerByFigi() {
        sharesService.findByTickerSync(List.of("NMTP"));
        return sharesService.findTickerByFigi("BBG004S68BR5");
    }

    @GET
    @Path("/cancelorder")
    public void cancelOrder(@QueryParam("orderId") String orderId) {
        executor.get().cancelOrder(orderId);
    }

    @GET
    @Path("/calcprice")
    public Uni<Double> calcMinBuyPrice() {
        return orderbookService.getOrderbook("BBG00W9LF2G5")
                .onItem()
                .transformToUni(orderbook -> spreadService.calcNextBidPrice(orderbook))
                .onItem()
                .transform(price -> MapperUtils.quotationToBigDecimal(price).doubleValue());
    }

    // BBG008HNHZ07 NWLI
    @GET
    @Path("/spread")
    public Uni<Spread> getSpread() {
        return orderbookService.getOrderbook("BBG00W9LF2G5")
                .onItem()
                .transformToUni(orderbook -> spreadService.calcSpread(orderbook));
    }

//    @GET
//    @Path("/figi")
//    public Uni<List<String>> getFigi() {
//        log.info(">>> Find figi start");
//        return strategyManager.runFindFigi();
//    }

    @GET
    @Path("/cache-get")
    public List<String> getFromCache(){
        log.info(">>> GetFromCache getFigiSync");
        return cacheManager.getFigiSync();
    }


    @GET
    @Path("/cache-add")
    public Uni addToCache() {
//        cacheManager.invalidateAndAddFigi(List.of("One " + new Random().nextInt()));
        Uni uni1 = cacheManager.addFigi("First");
        Uni uni2 = cacheManager.addFigi(List.of("1", "2"));
        Uni uni3 = cacheManager.addFigi(List.of("3", "4"));
        return Uni.combine().all().unis(uni1, uni2, uni3).discardItems();
    }


    @GET
    // figi='BBG000BXQ7R1', ticker='ZNH'
    //figi='BBG00W9LF2G5', ticker='PRAX'
    @Path("/process")
    public  Uni processOrderbook() {
        return orderbookService.getOrderbook("BBG000BXQ7R1", 1)
                .onItem()
                .call(orderbook -> strategy.processOrderbook(orderbook));
    }

    @GET
    @Path("/event-bus-test")
    public void eventBusTest() {
//        OrderStateDto dto = new OrderStateDto();
//        dto.setTicker("PLAY");
//        dto.setStatus("Новая");
//        dto.setDirection("Покупка");
//        dto.setCurrency("USD");
//        dto.setInitialPrice(BigDecimal.valueOf(345.7));
//        dto.setOrderId(UUID.randomUUID().toString());
//        bus.send(GlobalBusAddress.LOG_ORDER, dto, new DeliveryOptions().setCodecName(OrderStateDtoCodec.NAME));

        bus.send("LOG", List.of("PLAY")//,
//                new DeliveryOptions().setCodecName(ListCodec.class.getName())
        );
    }

    @GET
    @Path("/timer-test")
    public void vertxTimerTest() {
        log.info(">>> Timer Test " + LocalTime.now());

        long millis = TimeUnit.MINUTES.toMillis(1);
        vertx.setTimer(millis, aLong -> {
            log.info(">>> Timer " + LocalTime.now() + ' ' + millis);
        });

    }

    @GET
    @Path("/subscribe-test")
    public void subscribeTest() {
        log.info(">>> Subscribe Test");
        List<String> figi1 = List.of("BBG000BXQ7R1");
        orderbookService.unsucscribeOrderbook(figi1);
        orderbookService.subscribeOrderBook(figi1, (orderBook) -> {
            log.info(">>> Subscribe 1 " + orderBook);
            //processOrderbook(orderBook);
        });

        List<String> figi = List.of("BBG000BXQ7R1", "BBG00W9LF2G5");
        orderbookService.unsucscribeOrderbook(figi);
        orderbookService.subscribeOrderBook(figi, (orderBook) -> {
            log.info(">>> Subscribe 2 " + orderBook);
            //processOrderbook(orderBook);
        });
    }

    @GET
    @Path("/is-has-position")
    public Uni testHasPosition(){
        return realExecutor.hasPosition("BBG000BXQ7R1");
    }


    @GET
    @Path("/exclude-tickers")
    public List<String> excludeTickers(){
        return strategySharesProperties.tickersExclude().orElse(Collections.emptyList());
    }

//    @GET
//    @Path("/get-order-pair")
//    public RestResponse getMyCurrentOpenOrders() {
//        List<OrderState> orderStates = new ArrayList<>();
//        OrderState os1 = OrderState.newBuilder().setDirection(OrderDirection.ORDER_DIRECTION_BUY)
//                .setInitialSecurityPrice(MoneyValue.newBuilder().setUnits(50).build())
//                .build();
//        OrderState os2 = OrderState.newBuilder().setDirection(OrderDirection.ORDER_DIRECTION_BUY)
//                .setInitialSecurityPrice(MoneyValue.newBuilder().setUnits(60).build())
//                .build();
//        OrderState os3 = OrderState.newBuilder().setDirection(OrderDirection.ORDER_DIRECTION_BUY)
//                .setInitialSecurityPrice(MoneyValue.newBuilder().setUnits(30).build())
//                .build();
//        orderStates.add(os1);
//        orderStates.add(os2);
//        orderStates.add(os3);
//        return RestResponse.ok(Helper.getOrderPair(orderStates).getBuy().getInitialSecurityPrice().getUnits());
//    }



}
