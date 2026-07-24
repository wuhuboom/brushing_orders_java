package com.order.api.service;

import com.order.common.core.redis.RedisCache;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

@Service
public class UserRateLimitService {
    private static final int WINDOW_MINUTES = 10;
    private static final int ACCOUNT_LOGIN_LIMIT = 5;
    private static final int IP_LOGIN_LIMIT = 20;
    private static final int PASSWORD_LIMIT = 5;

    private final RedisCache redisCache;

    public UserRateLimitService(RedisCache redisCache) {
        this.redisCache = redisCache;
    }

    public boolean isLoginBlocked(String username, String ipAddress) {
        return count(accountLoginKey(username)) >= ACCOUNT_LOGIN_LIMIT
                || count(ipLoginKey(ipAddress)) >= IP_LOGIN_LIMIT;
    }

    public boolean recordLoginFailure(String username, String ipAddress) {
        long accountCount = increment(accountLoginKey(username));
        long ipCount = increment(ipLoginKey(ipAddress));
        return accountCount >= ACCOUNT_LOGIN_LIMIT || ipCount >= IP_LOGIN_LIMIT;
    }

    public void clearLoginAccountFailures(String username) {
        redisCache.deleteObject(accountLoginKey(username));
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

    private String accountLoginKey(String username) {
        return "front:login_fail:account:" + digest(username);
    }

    private String ipLoginKey(String ipAddress) {
        return "front:login_fail:ip:" + digest(ipAddress);
    }

    private String passwordKey(Long userId) {
        return "front:password_fail:" + userId;
    }

    private String digest(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] value = digest.digest(String.valueOf(input).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(value);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
