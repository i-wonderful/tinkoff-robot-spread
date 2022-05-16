package com.byby.trobot.strategy.impl;

import io.quarkus.cache.*;
import io.smallrye.mutiny.Uni;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class StrategyCacheManager {
    private static final Logger log = LoggerFactory.getLogger(StrategyCacheManager.class);
    private static final String FIGI_KEY = "figi";

    @Inject
    CacheManager cacheManager;

    @Inject
    @CacheName("my-cache-figi")
    Cache cache;

    public Uni addFigi(String figi) {
        log.info(">>> addToCacheFigi " + figi);
        return addFigi(List.of(figi));
    }

    public Uni<List<String>> addFigi(List<String> figiNew) {
        log.info(">>> Add to cache: " + figiNew);
        return getFigi()
                .onItem()
                .transformToUni(
                        (figiCached) -> {
                            figiCached.addAll(figiNew);
                            return clearAndAddFigi(figiCached);
                        });
    }

    public Uni<List<String>> clearAndAddFigi(final List<String> figi) {
        return cache.invalidate(FIGI_KEY)
                .onItem()
                .transformToUni(t -> cache.get(FIGI_KEY, (String key) -> figi));
    }

    public Uni<Void> clear() {
        return cache.invalidate(FIGI_KEY);
    }

    public Uni<List<String>> getFigi() {
        return cache.get(FIGI_KEY, key -> {
                    log.info(">>> Empty cache");
                    return new ArrayList<>();
                }
        );
    }

    public List<String> getFigiSync() {
        return cache.get(FIGI_KEY, key -> new ArrayList<String>())
                .await().indefinitely();
    }
}
