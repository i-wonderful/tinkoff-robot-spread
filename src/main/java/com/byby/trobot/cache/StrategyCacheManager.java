package com.byby.trobot.cache;

import com.byby.trobot.common.EventLogger;
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
    @CacheName("my-cache-figi")
    Cache cache;

    @Inject
    EventLogger eventLogger;

    public Uni addFigi(String figi) {
        log.info(">>> addToCacheFigi " + figi);
        return addFigi(List.of(figi));
    }

    /**
     * Добавить в кеш список Figi.
     *
     * @param figiNew список для добавления
     * @return новый кеш со всеми итемами
     */
    public Uni<List<String>> addFigi(final List<String> figiNew) {
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
        log.info(">>> clearAndAddFigi " + figi);
        return cache.invalidate(FIGI_KEY)
                .onItem()
                .transformToUni(t -> cache.get(FIGI_KEY, (String key) -> figi));
    }

    public Uni<Void> clear() {
        return cache.invalidate(FIGI_KEY)
                .invoke(() -> eventLogger.log("Кеш figi очищен"));
    }

    public Uni<List<String>> getFigi() {
        return cache.get(FIGI_KEY, key -> new ArrayList<>());
    }

    public List<String> getFigiSync() {
        return cache.get(FIGI_KEY, key -> new ArrayList<String>())
                .await().indefinitely();
    }
}
