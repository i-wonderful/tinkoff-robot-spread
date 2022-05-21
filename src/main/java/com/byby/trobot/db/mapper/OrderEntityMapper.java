package com.byby.trobot.db.mapper;

import com.byby.trobot.db.entity.OrderDone;
import com.byby.trobot.db.entity.OrderDoneDirection;
import com.byby.trobot.service.impl.SharesService;
import com.google.protobuf.Timestamp;
import ru.tinkoff.piapi.contract.v1.OrderDirection;
import ru.tinkoff.piapi.contract.v1.OrderTrade;
import ru.tinkoff.piapi.contract.v1.OrderTrades;
import ru.tinkoff.piapi.core.utils.MapperUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@ApplicationScoped
public class OrderEntityMapper {

    @Inject
    SharesService sharesService;

    public OrderDone toEntity(OrderTrades orderTrades) {
        OrderDone orderDone = new OrderDone();
        orderDone.setOrderId(orderTrades.getOrderId());
        orderDone.setFigi(orderTrades.getFigi());
        orderDone.setDirection(mapDirection(orderTrades.getDirection()));
        orderDone.setTicker(sharesService.findTickerByFigiSync(orderDone.getFigi()));

        // мы торгуем по одной акции
        if (orderTrades.getTradesCount() == 1) {
            OrderTrade ot = orderTrades.getTrades(0);
            orderDone.setDateTimeDone(mapDatetime(ot.getDateTime()));
            orderDone.setPrice(MapperUtils.quotationToBigDecimal(ot.getPrice()));
        }
        return orderDone;
    }

    private ZonedDateTime mapDatetime(Timestamp ts) {
        Instant instant = Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos());
        ZoneId z = ZoneId.systemDefault(); // todo Get from settings
        ZonedDateTime zdt = instant.atZone(z);

        // todo проверить тут на реальных сделках
        return zdt;
    }

    private OrderDoneDirection mapDirection(OrderDirection orderDirection) {
        switch (orderDirection) {
            case ORDER_DIRECTION_BUY:
                return OrderDoneDirection.BUY;
            case ORDER_DIRECTION_SELL:
                return OrderDoneDirection.SELL;
            default:
                return OrderDoneDirection.UNRECOGNIZED;
        }
    }
}
