package com.byby.trobot.controller;

import com.byby.trobot.common.GlobalBusAddress;
import com.byby.trobot.config.ApplicationProperties;
import com.byby.trobot.dto.OrderStateDto;
import com.byby.trobot.dto.codec.OrderStateDtoCodec;
import com.byby.trobot.executor.Executor;
import com.byby.trobot.service.impl.*;
import com.byby.trobot.strategy.impl.SpreadStrategy;
import com.byby.trobot.strategy.impl.model.Spread;
import io.smallrye.mutiny.Uni;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.mutiny.core.eventbus.EventBus;
import ru.tinkoff.piapi.contract.v1.GetOrderBookResponse;
import ru.tinkoff.piapi.core.utils.MapperUtils;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

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
    SpreadService spreadService;
    @Inject
    OrderbookService orderbookService;
    @Inject
    StrategyManagerImpl strategyManager;
    @Inject
    SpreadStrategy strategy;
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

    @GET
    @Path("/calcprice")
    public double calcMinBuyPrice() {
        GetOrderBookResponse orderbook = orderbookService.getOrderbook("BBG00W9LF2G5");
        var price = spreadService.calcNextBidPrice(orderbook);
        return MapperUtils.quotationToBigDecimal(price).doubleValue();
    }

    // BBG008HNHZ07 NWLI
    @GET
    @Path("/spread")
    public Spread getSpread() {
        GetOrderBookResponse orderbook = orderbookService.getOrderbook("BBG00W9LF2G5");
        System.out.println(orderbook);
        return spreadService.calcSpread(orderbook);
    }

    @GET
    @Path("/figi")
    public List<String> getFigi() {
        return strategyManager.findFigi();
    }

    @GET
    // figi='BBG000BXQ7R1', ticker='ZNH'
    //figi='BBG00W9LF2G5', ticker='PRAX'
    @Path("/process")
    public void processOrderbook() {
        GetOrderBookResponse orderbook = orderbookService.getOrderbook("BBG00W9LF2G5", 5);
        strategy.processOrderbookTEST(orderbook);
    }

    @GET
    @Path("/event-bus-test")
    public void eventBusTest(){
        OrderStateDto dto = new OrderStateDto();
        dto.setTicker("PLAY");
        dto.setStatus("Новая");
        dto.setDirection("Покупка");
        dto.setCurrency("USD");
        dto.setInitialPrice(BigDecimal.valueOf(345.7));
        dto.setOrderId(UUID.randomUUID().toString());
        bus.send(GlobalBusAddress.LOG_ORDER, dto, new DeliveryOptions().setCodecName(OrderStateDtoCodec.NAME));
    }
}
