package com.byby.trobot.dto.codec;

import com.byby.trobot.dto.OrderStateDto;
import io.quarkus.vertx.LocalEventBusCodec;

public class OrderStateDtoCodec extends LocalEventBusCodec<OrderStateDto> {
    public static final String NAME = OrderStateDtoCodec.class.getName();

    public OrderStateDtoCodec() {
        super(NAME);
    }
}
