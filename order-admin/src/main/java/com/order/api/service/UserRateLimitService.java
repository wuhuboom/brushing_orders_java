package com.order.api.service;

import com.order.common.core.redis.RedisCache;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class UserRateLimitService {
    private static final int WINDOW_MINUTES = 10;
    private static final int PASSWORD_LIMIT = 5;

    private final RedisCache redisCache;

    public UserRateLimitService(RedisCache redisCache) {
        this.redisCache = redisCache;
    }

    public boolean isPasswordBlocked(Long userId) {
        return count(passwordKey(userId)) >= PASSWORD_LIMIT;
    }

    public boolean recordPasswordFailure(Long userId) {
        return increment(passwordKey(userId)) >= PASSWORD_LIMIT;
    }

    public void clearPasswordFailures(Long userId) {
        redisCache.deleteObject(passwordKey(userId));
    }

    public String tradePasswordKey(Long userId) {
        return "front:trade_password_fail:" + userId;
    }

    public int tradePasswordLimit() {
        return PASSWORD_LIMIT;
    }

    private long increment(String key) {
        return redisCache.incrementWithExpire(key, WINDOW_MINUTES, TimeUnit.MINUTES);
    }

    private long count(String key) {
        Number value = redisCache.getCacheObject(key);
        return value == null ? 0L : value.longValue();
    }

    private String passwordKey(Long userId) {
        return "front:password_fail:" + userId;
    }

}
