package com.byby.trobot.cache;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CacheResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Кеш accountId
 */
@ApplicationScoped
public class AppCache {
    private static final Logger log = LoggerFactory.getLogger(AppCache.class);
    private static final String ACCOUNT_KEY = "ACCOUNT_KEY";

    @Inject
    @CacheName("app-cache")
    Cache cache;

    public String getAccountId() {
        return putOrGetAccountId(ACCOUNT_KEY, null);
    }

    public void putAccountId(String accountId) {
        log.info(">>> Put to cache accountId=" + accountId);
        cache.invalidate(ACCOUNT_KEY)
                .subscribe()
                .with(unused -> putOrGetAccountId(ACCOUNT_KEY, accountId));
    }

    @CacheResult(cacheName = "app-cache")
    protected String putOrGetAccountId(@CacheKey String key, String value) {
        return value;
    }

}
