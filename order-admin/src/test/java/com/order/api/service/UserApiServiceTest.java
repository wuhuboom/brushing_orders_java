package com.order.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.api.controller.dto.LoginUserDto;
import com.order.framework.front.FrontJwtUtil;
import com.order.framework.init.GeoIpQueryQueryService;
import com.order.member.domain.OrderUser;
import com.order.member.mapper.OrderUserMapper;
import com.order.member.service.IOrderLoginLogService;
import com.order.member.service.IOrderUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserApiServiceTest {
    @Mock private OrderUserMapper userMapper;
    @Mock private IOrderUserService userService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private FrontJwtUtil frontJwtUtil;
    @Mock private UserRateLimitService rateLimitService;
    @Mock private TradePasswordVerificationService tradePasswordVerifier;
    @Mock private IOrderLoginLogService loginLogService;
    @Mock private GeoIpQueryQueryService geoIpService;
    @Mock private ThreadPoolTaskExecutor auditExecutor;
    @Mock private HttpServletRequest servletRequest;
    @Mock private ObjectProvider<ConfigQueryService> configQueryServiceProvider;

    private UserApiService service;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode(anyString())).thenReturn("dummy-hash");
        when(servletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        service = new UserApiService(
                userMapper,
                userService,
                passwordEncoder,
                frontJwtUtil,
                rateLimitService,
                tradePasswordVerifier,
                loginLogService,
                geoIpService,
                auditExecutor,
                new ObjectMapper(),
                configQueryServiceProvider);
    }

    @Test
    void successfulLoginReturnsTokenAndPublicProfile() {
        LoginUserDto request = login("member_1", "Password123");
        OrderUser authUser = authUser();
        OrderUser profile = authUser();
        profile.setBalance(new BigDecimal("100.00"));

        when(userMapper.selectAuthUserByName("member_1")).thenReturn(authUser);
        when(passwordEncoder.matches("Password123", "stored-hash")).thenReturn(true);
        when(userMapper.selectUserProfileById(1L)).thenReturn(profile);
        when(frontJwtUtil.generateToken(authUser)).thenReturn("new-token");

        UserLoginResult result = service.login(request, servletRequest);

        assertEquals("new-token", result.token());
        assertNotNull(result.user());
        assertEquals("member_1", result.user().username());
    }

    @Test
    void unknownUserUsesGenericCredentialError() {
        LoginUserDto request = login("unknown_1", "Password123");
        when(userMapper.selectAuthUserByName("unknown_1")).thenReturn(null);
        when(passwordEncoder.matches("Password123", "dummy-hash")).thenReturn(false);
        when(geoIpService.queryByIp("127.0.0.1")).thenReturn("Local");
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(auditExecutor).execute(any(Runnable.class));

        UserApiException exception =
                assertThrows(UserApiException.class, () -> service.login(request, servletRequest));

        assertEquals(601, exception.getCode());
        verify(loginLogService).insertLoginAttempt(
                isNull(), eq("127.0.0.1"), eq("Local"), eq("0"), anyString());
    }

    @Test
    void blockedIdentifierUsesSameResultWithoutAccountLookup() {
        LoginUserDto request = login("unknown_1", "Password123");
        when(rateLimitService.isLoginBlocked("unknown_1", "127.0.0.1")).thenReturn(true);

        UserApiException exception =
                assertThrows(UserApiException.class, () -> service.login(request, servletRequest));

        assertEquals(622, exception.getCode());
    }

    private LoginUserDto login(String username, String password) {
        LoginUserDto request = new LoginUserDto();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    private OrderUser authUser() {
        OrderUser user = new OrderUser();
        user.setId(1L);
        user.setUsername("member_1");
        user.setPassword("stored-hash");
        user.setAccountStatus("0");
        return user;
    }
}
