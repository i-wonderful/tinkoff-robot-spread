package com.byby.trobot.db.mapper;

import com.byby.trobot.controller.dto.OrderDoneDto;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class OrderDoneMapper {

    @Inject
    SharesService sharesService;

    public OrderDone toEntity(OrderTrades orderTrades) {
        OrderDone orderDone = new OrderDone();
        orderDone.setOrderId(orderTrades.getOrderId());
        orderDone.setFigi(orderTrades.getFigi());
        orderDone.setDirection(mapDirection(orderTrades.getDirection()));
//        orderDone.setTicker(sharesService.findTickerByFigiSync(orderDone.getFigi())); // todo

        // мы торгуем по одной сделке
        if (orderTrades.getTradesCount() == 1) {

            OrderTrade ot = orderTrades.getTrades(0);
            orderDone.setQuantity(ot.getQuantity());
            orderDone.setDateTimeDone(mapDatetime(ot.getDateTime()));
            orderDone.setPrice(MapperUtils.quotationToBigDecimal(ot.getPrice()));
        }
        return orderDone;
    }

    public static List<OrderDoneDto> toDto(List<OrderDone> entities) {
        return Optional.ofNullable(entities).orElse(Collections.emptyList())
                .stream()
                .map(OrderDoneMapper::toDto)
                .collect(Collectors.toList());
    }

    public static OrderDoneDto toDto(OrderDone entity) {
        if (entity == null) {
            return null;
        }

        OrderDoneDto dto = new OrderDoneDto();
        dto.setPrice(entity.getPrice());
        dto.setFigi(entity.getFigi());
        dto.setOrderId(entity.getOrderId());
        dto.setDirection(entity.getDirection());
        dto.setDateTimeDone(entity.getDateTimeDone());
        dto.setQuantity(entity.getQuantity());
        dto.setTicker(entity.getTicker());
        return dto;

    }

    private ZonedDateTime mapDatetime(Timestamp ts) {
        Instant instant = Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos());
        ZoneId z = ZoneId.systemDefault(); // todo Get from settings
        ZonedDateTime zdt = instant.atZone(z);

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
