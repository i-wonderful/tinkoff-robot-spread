package com.byby.trobot.cache;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CacheResult;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class AppCache {
    private static final String ACCOUNT_KEY = "ACCOUNT_KEY";

    @Inject
    @CacheName("app-cache")
    Cache cache;

    public String getAccountId() {
        return putOrGetAccountId(ACCOUNT_KEY, null);
    }

    public void putAccountId(String accountId) {
        System.out.println(">>> putAccountId");
        cache.invalidate(ACCOUNT_KEY)
                .subscribe()
                .with(unused -> putOrGetAccountId(ACCOUNT_KEY, accountId));
    }

    @CacheResult(cacheName = "app-cache")
    protected String putOrGetAccountId(@CacheKey String key, String value) {
        System.out.println(">>> Put to cache accountId = " + value);
        return value;
    }
//    public String getAccountId() {
//        return cache.get(ACCOUNT_KEY, () -> {
//            return "null";
//        });
//    }
}
