package com.byby.trobot.controller.dto.codec;

import com.byby.trobot.controller.dto.OrderStateDto;
import io.quarkus.vertx.LocalEventBusCodec;

public class OrderStateDtoCodec extends LocalEventBusCodec<OrderStateDto> {
    public static final String NAME = OrderStateDtoCodec.class.getName();

    public OrderStateDtoCodec() {
        super(NAME);
    }
}
