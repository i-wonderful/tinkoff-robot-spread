package com.byby.trobot.dto.codec;

import io.quarkus.vertx.LocalEventBusCodec;

public class OrderStateDtoCodec extends LocalEventBusCodec<OrderStateDtoCodec> {
    public static final String NAME = OrderStateDtoCodec.class.getName();

    public OrderStateDtoCodec() {
        super(NAME);
    }
}
