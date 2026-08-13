package com.brushing.framework.web.service;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import com.brushing.common.constant.CacheConstants;
import com.brushing.common.constant.Constants;
import com.brushing.common.core.domain.entity.SysUser;
import com.brushing.common.core.domain.model.LoginUser;
import com.brushing.common.core.redis.RedisCache;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenServiceTest
{
    private static final String SECRET = "abcdefghijklmnopqrstuvwxyz";

    private TokenService tokenService;
    private RedisCache redisCache;
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void setUp()
    {
        tokenService = new TokenServiceWithoutUserAgent();
        redisCache = mock(RedisCache.class);
        stringRedisTemplate = mock(StringRedisTemplate.class);

        ReflectionTestUtils.setField(tokenService, "secret", SECRET);
        ReflectionTestUtils.setField(tokenService, "header", "Authorization");
        ReflectionTestUtils.setField(tokenService, "expireTime", 30);
        ReflectionTestUtils.setField(tokenService, "redisCache", redisCache);
        ReflectionTestUtils.setField(tokenService, "stringRedisTemplate", stringRedisTemplate);
    }

    @Test
    void createTokenInvalidatesPreviousSession()
    {
        LoginUser loginUser = createLoginUser(1L, "admin", null);
        when(stringRedisTemplate.execute(
                org.mockito.ArgumentMatchers.<RedisScript<String>>any(),
                anyList(),
                any(Object[].class))).thenReturn("old-token");

        tokenService.createToken(loginUser);

        verify(redisCache).setCacheObject(
                eq(CacheConstants.LOGIN_TOKEN_KEY + loginUser.getToken()),
                same(loginUser),
                eq(30),
                eq(TimeUnit.MINUTES));
        verify(redisCache).deleteObject(CacheConstants.LOGIN_TOKEN_KEY + "old-token");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getLoginUserRejectsSessionReplacedByLaterLogin()
    {
        LoginUser oldLoginUser = createLoginUser(1L, "admin", "old-token");
        when(redisCache.<LoginUser>getCacheObject(CacheConstants.LOGIN_TOKEN_KEY + "old-token"))
                .thenReturn(oldLoginUser);

        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(CacheConstants.LOGIN_USER_SESSION_KEY + "1")).thenReturn("new-token");

        String jwt = Jwts.builder()
                .setSubject("admin")
                .claim(Constants.LOGIN_USER_KEY, "old-token")
                .signWith(SignatureAlgorithm.HS512, SECRET)
                .compact();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);

        assertNull(tokenService.getLoginUser(request));
        verify(redisCache).deleteObject(CacheConstants.LOGIN_TOKEN_KEY + "old-token");
    }

    private LoginUser createLoginUser(Long userId, String username, String token)
    {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setUserName(username);
        LoginUser loginUser = new LoginUser(userId, null, user, Collections.emptySet());
        loginUser.setToken(token);
        return loginUser;
    }

    private static class TokenServiceWithoutUserAgent extends TokenService
    {
        @Override
        public void setUserAgent(LoginUser loginUser)
        {
            // 单元测试不依赖当前 HTTP 请求。
        }
    }
}
