package com.order.api.service;

import com.order.common.core.redis.RedisCache;
import com.order.member.mapper.OrderUserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminMemberCredentialService {
    private final OrderUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RedisCache redisCache;
    private final UserRateLimitService rateLimitService;

    public AdminMemberCredentialService(
            OrderUserMapper userMapper,
            PasswordEncoder passwordEncoder,
            RedisCache redisCache,
            UserRateLimitService rateLimitService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.redisCache = redisCache;
        this.rateLimitService = rateLimitService;
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetTradePassword(Long userId, String rawPassword) {
        if (userId == null) {
            throw new IllegalArgumentException("Member id is required");
        }
        String normalizedPassword = rawPassword == null ? "" : rawPassword.trim();
        if (normalizedPassword.length() < 6) {
            throw new IllegalArgumentException(
                    "Trade password must contain at least 6 characters");
        }

        String encodedPassword = passwordEncoder.encode(normalizedPassword);
        if (userMapper.resetTradePasswordByAdmin(userId, encodedPassword) != 1) {
            throw new IllegalArgumentException("Member does not exist");
        }
        redisCache.deleteObject(rateLimitService.tradePasswordKey(userId));
    }
}
