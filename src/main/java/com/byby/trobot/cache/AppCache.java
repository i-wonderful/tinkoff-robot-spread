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
 * Кеш.
 *
 * accountId
 * robotSessionId
 */
@ApplicationScoped
public class AppCache {
    private static final Logger log = LoggerFactory.getLogger(AppCache.class);
    private static final String ACCOUNT_KEY = "ACCOUNT_KEY";
    private static final String ROBOT_SESSION_ID_KEY = "ROBOT_SESSION_ID_KEY";

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

    public Long getRobotSessionId() {
        return putOrGetRobotSessionId(ROBOT_SESSION_ID_KEY, null);
    }

    public void putRobotSessionId(Long robotSessionId) {
        log.info(">>> Put to cache robotSessionId=" + robotSessionId);
        cache.invalidate(ROBOT_SESSION_ID_KEY)
                .subscribe()
                .with(unused -> putOrGetRobotSessionId(ACCOUNT_KEY, robotSessionId));
    }

    @CacheResult(cacheName = "app-cache")
    protected String putOrGetAccountId(@CacheKey String key, String value) {
        return value;
    }

    @CacheResult(cacheName = "app-cache")
    protected Long putOrGetRobotSessionId(@CacheKey String key, Long value) {
        return value;
    }

}
