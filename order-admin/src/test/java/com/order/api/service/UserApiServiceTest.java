package com.order.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.api.controller.dto.LoginUserDto;
import com.order.api.controller.dto.UserProfileResponse;
import com.order.framework.front.FrontJwtUtil;
import com.order.framework.init.GeoIpQueryQueryService;
import com.order.member.domain.OrderUser;
import com.order.member.mapper.OrderUserMapper;
import com.order.member.service.IOrderLoginLogService;
import com.order.member.service.IOrderUserService;
import com.order.member.service.MemberDailyStatisticsService;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserApiServiceTest {
    @Mock private OrderUserMapper userMapper;
    @Mock private IOrderUserService userService;
    @Mock private MemberDailyStatisticsService dailyStatisticsService;
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
        service = new UserApiService(
                userMapper,
                userService,
                dailyStatisticsService,
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
        profile.setTotalCommission(new BigDecimal("1581.25"));

        when(userMapper.selectAuthUserByName("member_1")).thenReturn(authUser);
        when(passwordEncoder.matches("Password123", "stored-hash")).thenReturn(true);
        when(userMapper.selectUserProfileById(1L)).thenReturn(profile);
        when(frontJwtUtil.generateToken(authUser)).thenReturn("new-token");
        doAnswer(invocation -> {
            invocation.<OrderUser>getArgument(0).setTodayCommission(new BigDecimal("581.25"));
            return null;
        }).when(dailyStatisticsService).populate(profile);

        UserLoginResult result = service.login(request, servletRequest);

        assertEquals("new-token", result.token());
        assertNotNull(result.user());
        assertEquals("member_1", result.user().username());
        assertEquals(new BigDecimal("581.25"), result.user().todayCommission());
        assertEquals(new BigDecimal("1581.25"), result.user().totalCommission());
        verify(dailyStatisticsService).populate(profile);
    }

    @Test
    void userInfoReturnsCumulativeCommissionAlongsideDailyCommission() {
        OrderUser profile = authUser();
        profile.setTotalCommission(new BigDecimal("1581.25"));
        when(userMapper.selectUserProfileById(1L)).thenReturn(profile);
        doAnswer(invocation -> {
            invocation.<OrderUser>getArgument(0).setTodayCommission(new BigDecimal("581.25"));
            return null;
        }).when(dailyStatisticsService).populate(profile);

        UserProfileResponse result = service.userInfo(1L);

        assertEquals(new BigDecimal("581.25"), result.todayCommission());
        assertEquals(new BigDecimal("1581.25"), result.totalCommission());
        verify(dailyStatisticsService).populate(profile);
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
    void repeatedWrongPasswordsAllowImmediateSuccessfulLogin() {
        OrderUser user = authUser();
        when(userMapper.selectAuthUserByName("member_1")).thenReturn(user);
        when(passwordEncoder.matches("wrong", "stored-hash")).thenReturn(false);
        for (int i = 0; i < 25; i++) {
            UserApiException error = assertThrows(UserApiException.class,
                    () -> service.login(login("member_1", "wrong"), servletRequest));
            assertEquals(601, error.getCode());
        }
        when(passwordEncoder.matches("correct", "stored-hash")).thenReturn(true);
        when(userMapper.selectUserProfileById(1L)).thenReturn(user);
        when(frontJwtUtil.generateToken(user)).thenReturn("new-token");
        assertEquals("new-token", service.login(login("member_1", "correct"), servletRequest).token());
        verifyNoInteractions(rateLimitService);
    }

    @Test
    void repeatedUnknownAccountsDoNotBlockTheIp() {
        for (int i = 0; i < 25; i++) {
            final String username = "unknown_" + i;
            UserApiException error = assertThrows(UserApiException.class,
                    () -> service.login(login(username, "wrong"), servletRequest));
            assertEquals(601, error.getCode());
        }
        verifyNoInteractions(rateLimitService);
        verifyNoInteractions(frontJwtUtil);
    }

    private LoginUserDto login(String username, String password) {
        when(servletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
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
