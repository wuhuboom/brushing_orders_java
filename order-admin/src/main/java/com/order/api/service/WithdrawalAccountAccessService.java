package com.order.api.service;

import com.order.common.core.redis.RedisCache;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.order.api.service.AccountErrorCodes.WITHDRAWAL_ACCOUNT_ACCESS;

/**
 * Short-lived, user-bound access granted after a successful trade-password check.
 */
@Service
public class WithdrawalAccountAccessService {
    static final int ACCESS_MINUTES = 10;
    private static final String KEY_PREFIX = "front:withdrawal-account:access:";

    private final RedisCache redisCache;

    public WithdrawalAccountAccessService(RedisCache redisCache) {
        this.redisCache = redisCache;
    }

    public String issue(Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        redisCache.setCacheObject(key(token), String.valueOf(userId), ACCESS_MINUTES, TimeUnit.MINUTES);
        return token;
    }

    public void require(Long userId, String token) {
        String owner = token == null || token.isBlank()
                ? null
                : redisCache.getCacheObject(key(token.trim()));
        if (owner == null || !owner.equals(String.valueOf(userId))) {
            throw AccountApiException.forbidden(
                    WITHDRAWAL_ACCOUNT_ACCESS,
                    "Please verify the transaction password again");
        }
    }

    private String key(String token) {
        return KEY_PREFIX + token;
    }
}
