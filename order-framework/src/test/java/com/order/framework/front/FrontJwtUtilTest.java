package com.order.framework.front;

import com.order.common.core.redis.RedisCache;
import com.order.member.domain.OrderUser;
import com.order.member.mapper.OrderUserMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class FrontJwtUtilTest {
    private static final long VALID_TOKEN_LIFETIME_MS = TimeUnit.DAYS.toMillis(3);

    private static final String SECRET =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ab";

    @Mock private RedisCache redisCache;
    @Mock private OrderUserMapper userMapper;

    private FrontJwtUtil jwtUtil;

    @BeforeEach
    void setUp() throws Exception {
        jwtUtil = new FrontJwtUtil(redisCache, userMapper);
        setField("secretKey", SECRET);
        setField("expirationSeconds", 259200);
    }

    @Test
    void newTokenIsStoredByJtiInsteadOfFullBearerValue() {
        OrderUser user = activeUser();

        String token = jwtUtil.generateToken(user);

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(redisCache).setCacheObject(
                key.capture(), eq("1:member_1"), eq(259200), eq(TimeUnit.SECONDS));
        assertTrue(key.getValue().startsWith("front:token:"));
        assertFalse(key.getValue().contains(token));
        assertEquals("member_1", jwtUtil.getUsernameFromToken(token));
    }

    @Test
    void legacyTokenRemainsValidDuringCompatibilityWindow() {
        String token = Jwts.builder()
                .setSubject("member_1")
                .setExpiration(new Date(System.currentTimeMillis() + VALID_TOKEN_LIFETIME_MS))
                .signWith(SignatureAlgorithm.HS512, SECRET)
                .compact();
        when(redisCache.hasKey("token:valid:" + token)).thenReturn(true);
        when(userMapper.selectAuthUserByName("member_1")).thenReturn(activeUser());

        FrontJwtUtil.FrontPrincipal principal = jwtUtil.authenticate(token);

        assertNotNull(principal);
        assertEquals(1L, principal.userId());
        assertEquals("member_1", principal.username());
        assertTrue(principal.legacy());
    }

    @Test
    void redisFailureIsUnavailableRatherThanInvalidToken() {
        when(redisCache.getCacheObject("front:token:probe"))
                .thenThrow(new IllegalStateException("redis unavailable"));
        assertThrows(FrontAuthenticationUnavailableException.class,
                () -> jwtUtil.authenticate(memberToken(VALID_TOKEN_LIFETIME_MS)));
        verifyNoInteractions(userMapper);
    }

    @Test
    void databaseFailureIsUnavailableRatherThanInvalidToken() {
        when(redisCache.getCacheObject("front:token:probe")).thenReturn("1:member_1");
        when(userMapper.selectAuthUserById(1L)).thenThrow(new IllegalStateException("database unavailable"));
        assertThrows(FrontAuthenticationUnavailableException.class,
                () -> jwtUtil.authenticate(memberToken(VALID_TOKEN_LIFETIME_MS)));
    }

    @Test
    void expiredTokenNeverAccessesDependencies() {
        assertNull(jwtUtil.authenticate(memberToken(-60_000)));
        verifyNoInteractions(redisCache, userMapper);
    }

    @Test
    void removedSessionRemainsUnauthorized() {
        assertNull(jwtUtil.authenticate(memberToken(VALID_TOKEN_LIFETIME_MS)));
        verifyNoInteractions(userMapper);
    }

    @Test
    void validMemberStillAuthenticates() {
        when(redisCache.getCacheObject("front:token:probe")).thenReturn("1:member_1");
        when(userMapper.selectAuthUserById(1L)).thenReturn(activeUser());
        assertEquals(1L, jwtUtil.authenticate(memberToken(VALID_TOKEN_LIFETIME_MS)).userId());
    }

    private String memberToken(long lifetimeMs) {
        return Jwts.builder().setId("probe").setSubject("member_1").claim("uid", 1L)
                .setExpiration(new Date(System.currentTimeMillis() + lifetimeMs))
                .signWith(SignatureAlgorithm.HS512, SECRET).compact();
    }

    private OrderUser activeUser() {
        OrderUser user = new OrderUser();
        user.setId(1L);
        user.setUsername("member_1");
        user.setAccountStatus("0");
        return user;
    }

    private void setField(String name, Object value) throws Exception {
        Field field = FrontJwtUtil.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(jwtUtil, value);
    }
}
