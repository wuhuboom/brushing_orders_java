package com.order.api.service;

import com.order.common.core.redis.RedisCache;
import com.order.member.mapper.OrderUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminMemberCredentialServiceTest {
    private OrderUserMapper userMapper;
    private PasswordEncoder passwordEncoder;
    private RedisCache redisCache;
    private UserRateLimitService rateLimitService;
    private AdminMemberCredentialService service;

    @BeforeEach
    void setUp() {
        userMapper = mock(OrderUserMapper.class);
        passwordEncoder = mock(PasswordEncoder.class);
        redisCache = mock(RedisCache.class);
        rateLimitService = mock(UserRateLimitService.class);
        service = new AdminMemberCredentialService(
                userMapper, passwordEncoder, redisCache, rateLimitService);
    }

    @Test
    void resetEncodesTrimmedPasswordAndClearsAllFailures() {
        when(passwordEncoder.encode("727123")).thenReturn("encoded");
        when(userMapper.resetTradePasswordByAdmin(7L, "encoded")).thenReturn(1);
        when(rateLimitService.tradePasswordKey(7L))
                .thenReturn("front:trade_password_fail:7");

        service.resetTradePassword(7L, " 727123 ");

        verify(passwordEncoder).encode("727123");
        verify(userMapper).resetTradePasswordByAdmin(7L, "encoded");
        verify(redisCache).deleteObject("front:trade_password_fail:7");
    }

    @Test
    void resetRejectsShortPasswordBeforeWriting() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.resetTradePassword(7L, " 123 "));

        verify(userMapper, never()).resetTradePasswordByAdmin(anyLong(), anyString());
        verify(redisCache, never()).deleteObject("front:trade_password_fail:7");
    }

    @Test
    void resetDoesNotClearFailuresWhenMemberDoesNotExist() {
        when(passwordEncoder.encode("727123")).thenReturn("encoded");
        when(userMapper.resetTradePasswordByAdmin(99L, "encoded")).thenReturn(0);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.resetTradePassword(99L, "727123"));

        verify(redisCache, never()).deleteObject("front:trade_password_fail:99");
    }
}
