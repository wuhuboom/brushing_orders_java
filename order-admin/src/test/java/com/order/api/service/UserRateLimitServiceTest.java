package com.order.api.service;

import com.order.common.core.redis.RedisCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRateLimitServiceTest {
    @Mock
    private RedisCache redisCache;

    private UserRateLimitService service;

    @BeforeEach
    void setUp() {
        service = new UserRateLimitService(redisCache);
    }

    @Test
    void passwordChangeFailuresStillRespectTheirLimit() {
        when(redisCache.incrementWithExpire(anyString(), org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.eq(TimeUnit.MINUTES))).thenReturn(4L, 5L);
        assertFalse(service.recordPasswordFailure(1L));
        assertTrue(service.recordPasswordFailure(1L));
    }
}
