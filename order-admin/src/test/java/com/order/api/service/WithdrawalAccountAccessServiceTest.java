package com.order.api.service;

import com.order.common.core.redis.RedisCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WithdrawalAccountAccessServiceTest {
    @Mock
    RedisCache redisCache;

    @Test
    void issuedTokenIsUserBoundAndExpires() {
        WithdrawalAccountAccessService service = new WithdrawalAccountAccessService(redisCache);

        String token = service.issue(7L);

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(redisCache).setCacheObject(
                key.capture(),
                org.mockito.ArgumentMatchers.eq("7"),
                org.mockito.ArgumentMatchers.eq(10),
                org.mockito.ArgumentMatchers.eq(TimeUnit.MINUTES));
        assertTrue(key.getValue().endsWith(token));
    }

    @Test
    void rejectsMissingExpiredOrOtherUsersToken() {
        WithdrawalAccountAccessService service = new WithdrawalAccountAccessService(redisCache);
        when(redisCache.<String>getCacheObject(
                "front:withdrawal-account:access:valid")).thenReturn("8");

        AccountApiException exception = assertThrows(
                AccountApiException.class,
                () -> service.require(7L, "valid"));

        assertEquals(526, exception.getLegacyCode());
        assertThrows(AccountApiException.class, () -> service.require(7L, ""));
    }
}
