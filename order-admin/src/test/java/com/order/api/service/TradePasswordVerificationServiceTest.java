package com.order.api.service;

import com.order.common.core.redis.RedisCache;
import com.order.member.domain.OrderUser;
import com.order.member.mapper.OrderUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TradePasswordVerificationServiceTest {
    private OrderUserMapper orderUserMapper;
    private PasswordEncoder passwordEncoder;
    private RedisCache redisCache;
    private UserRateLimitService rateLimitService;
    private TradePasswordVerificationService service;

    @BeforeEach
    void setUp() {
        orderUserMapper = mock(OrderUserMapper.class);
        passwordEncoder = mock(PasswordEncoder.class);
        redisCache = mock(RedisCache.class);
        rateLimitService = mock(UserRateLimitService.class);
        service = new TradePasswordVerificationService(
                passwordEncoder,
                orderUserMapper,
                redisCache,
                rateLimitService
        );
    }

    @Test
    void wrongPasswordIncrementsRedisAndPersistentFailureCount() {
        OrderUser user = authUser(7L, 2);
        when(rateLimitService.tradePasswordKey(7L)).thenReturn("trade:7");
        when(rateLimitService.tradePasswordLimit()).thenReturn(5);
        when(orderUserMapper.selectTradePasswordStateByIdForUpdate(7L)).thenReturn(user);
        when(orderUserMapper.incrementWithdrawalPasswordFailCount(7L)).thenReturn(1);
        when(redisCache.getCacheObject("trade:7")).thenReturn(0);
        when(redisCache.incrementWithExpire("trade:7", 10, TimeUnit.MINUTES)).thenReturn(1L);
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        TradePasswordVerificationService.Result result = service.verify(user, "wrong");

        assertEquals(TradePasswordVerificationService.Result.WRONG, result);
        verify(orderUserMapper).incrementWithdrawalPasswordFailCount(7L);
        verify(redisCache, never()).deleteObject("trade:7");
    }

    @Test
    void successfulVerificationResetsBothCounters() {
        OrderUser user = authUser(9L, 3);
        when(rateLimitService.tradePasswordKey(9L)).thenReturn("trade:9");
        when(rateLimitService.tradePasswordLimit()).thenReturn(5);
        when(orderUserMapper.selectTradePasswordStateByIdForUpdate(9L)).thenReturn(user);
        when(redisCache.getCacheObject("trade:9")).thenReturn(2);
        when(passwordEncoder.matches("correct", "encoded")).thenReturn(true);

        TradePasswordVerificationService.Result result = service.verify(user, "correct");

        assertEquals(TradePasswordVerificationService.Result.SUCCESS, result);
        verify(redisCache).deleteObject("trade:9");
        verify(orderUserMapper).updateWithdrawalPasswordFailCount(9L, 0);
    }

    private OrderUser authUser(Long id, int failureCount) {
        OrderUser user = new OrderUser();
        user.setId(id);
        user.setTradePassword("encoded");
        user.setWithdrawalPasswordFailCount(failureCount);
        user.setWithdrawalPasswordFailLimit(5);
        user.setAccountStatus("1");
        return user;
    }
}
