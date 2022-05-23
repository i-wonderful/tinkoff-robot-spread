package com.byby.trobot.controller.dto.codec;

import com.byby.trobot.controller.dto.OrderStateDto;
import io.quarkus.vertx.LocalEventBusCodec;

public class ListCodec extends LocalEventBusCodec<OrderStateDto> {
    public static final String NAME = ListCodec.class.getName();
    public ListCodec() {
        super(NAME);
    }
}
