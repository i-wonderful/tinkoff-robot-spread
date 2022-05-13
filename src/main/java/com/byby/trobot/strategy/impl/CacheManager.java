package com.byby.trobot.strategy.impl;

import com.byby.trobot.strategy.impl.model.OrderPair;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CacheManager {

    @CacheResult(cacheName = "order-pairs")
    public OrderPair put(@CacheKey String figi, OrderPair orderPair){
        return orderPair;
    }
}
