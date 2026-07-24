package com.order.api.service;

import com.order.common.core.redis.RedisCache;
import com.order.member.domain.OrderUser;
import com.order.member.mapper.OrderUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
public class TradePasswordVerificationService {
    private static final Logger log = LoggerFactory.getLogger(TradePasswordVerificationService.class);

    public enum Result {
        SUCCESS,
        MISSING,
        WRONG,
        BLOCKED
    }

    private final PasswordEncoder passwordEncoder;
    private final OrderUserMapper userMapper;
    private final RedisCache redisCache;
    private final UserRateLimitService rateLimitService;

    public TradePasswordVerificationService(
            PasswordEncoder passwordEncoder,
            OrderUserMapper userMapper,
            RedisCache redisCache,
            UserRateLimitService rateLimitService) {
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.redisCache = redisCache;
        this.rateLimitService = rateLimitService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Result verify(OrderUser user, String rawPassword) {
        if (user == null || user.getId() == null || rawPassword == null || rawPassword.isEmpty()) {
            return Result.MISSING;
        }
        OrderUser current = userMapper.selectTradePasswordStateByIdForUpdate(user.getId());
        if (current == null || current.getTradePassword() == null
                || current.getTradePassword().isEmpty()) {
            return Result.MISSING;
        }

        int persistentLimit = current.getWithdrawalPasswordFailLimit() == null
                ? 0 : current.getWithdrawalPasswordFailLimit();
        int persistentCount = current.getWithdrawalPasswordFailCount() == null
                ? 0 : current.getWithdrawalPasswordFailCount();
        String redisKey = rateLimitService.tradePasswordKey(current.getId());

        try {
            Number redisCount = redisCache.getCacheObject(redisKey);
            if ((redisCount != null
                    && redisCount.intValue() >= rateLimitService.tradePasswordLimit())
                    || (persistentLimit > 0 && persistentCount >= persistentLimit)) {
                return Result.BLOCKED;
            }
        } catch (RuntimeException ex) {
            log.error("event=trade_password_rate_limit_unavailable userId={}", current.getId(), ex);
            return Result.BLOCKED;
        }

        if (!passwordEncoder.matches(rawPassword, current.getTradePassword())) {
            int updatedPersistentCount = persistentCount + 1;
            if (userMapper.incrementWithdrawalPasswordFailCount(current.getId()) != 1) {
                throw new IllegalStateException("Unable to increment trade password failure count");
            }
            current.setWithdrawalPasswordFailCount(updatedPersistentCount);
            user.setWithdrawalPasswordFailCount(updatedPersistentCount);
            final long redisCount;
            try {
                redisCount = redisCache.incrementWithExpire(redisKey, 10, TimeUnit.MINUTES);
            } catch (RuntimeException ex) {
                log.error("event=trade_password_rate_limit_unavailable userId={}", current.getId(), ex);
                return Result.BLOCKED;
            }
            return redisCount >= rateLimitService.tradePasswordLimit()
                    || (persistentLimit > 0 && updatedPersistentCount >= persistentLimit)
                    ? Result.BLOCKED : Result.WRONG;
        }

        try {
            redisCache.deleteObject(redisKey);
        } catch (RuntimeException ex) {
            log.error("event=trade_password_rate_limit_unavailable userId={}", current.getId(), ex);
            return Result.BLOCKED;
        }
        if (persistentCount != 0) {
            userMapper.updateWithdrawalPasswordFailCount(current.getId(), 0);
            current.setWithdrawalPasswordFailCount(0);
            user.setWithdrawalPasswordFailCount(0);
        }
        return Result.SUCCESS;
    }
}
