package com.byby.trobot.dto.codec;

import com.byby.trobot.dto.OrderStateDto;
import io.quarkus.vertx.LocalEventBusCodec;

public class ListCodec extends LocalEventBusCodec<OrderStateDto> {
    public static final String NAME = ListCodec.class.getName();
    public ListCodec() {
        super(NAME);
    }
}
