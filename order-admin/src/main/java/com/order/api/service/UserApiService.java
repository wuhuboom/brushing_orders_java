package com.order.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.api.controller.dto.AvatarDto;
import com.order.api.controller.dto.CheckTradePassword;
import com.order.api.controller.dto.EditPasswordDto;
import com.order.api.controller.dto.EditTradePasswordDto;
import com.order.api.controller.dto.LoginUserDto;
import com.order.api.controller.dto.RegisterDto;
import com.order.api.controller.dto.UserProfileResponse;
import com.order.common.i18n.SupportedLocale;
import com.order.common.utils.ExceptionUtil;
import com.order.common.utils.ip.IpUtils;
import com.order.framework.front.FrontJwtUtil;
import com.order.framework.init.GeoIpQueryQueryService;
import com.order.member.domain.OrderUser;
import com.order.member.mapper.OrderUserMapper;
import com.order.member.service.IOrderLoginLogService;
import com.order.member.service.IOrderUserService;
import com.order.member.service.RegistrationResult;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

@Service
public class UserApiService {
    private static final Logger log = LoggerFactory.getLogger(UserApiService.class);
    private static final int UNKNOWN_ERROR = 617;

    private final OrderUserMapper userMapper;
    private final IOrderUserService userService;
    private final PasswordEncoder passwordEncoder;
    private final FrontJwtUtil frontJwtUtil;
    private final UserRateLimitService rateLimitService;
    private final TradePasswordVerificationService tradePasswordVerifier;
    private final IOrderLoginLogService loginLogService;
    private final GeoIpQueryQueryService geoIpService;
    private final ThreadPoolTaskExecutor auditExecutor;
    private final ObjectMapper objectMapper;
    private final String dummyPasswordHash;

    private final ObjectProvider<ConfigQueryService> configQueryServiceProvider;

    public UserApiService(
            OrderUserMapper userMapper,
            IOrderUserService userService,
            PasswordEncoder passwordEncoder,
            FrontJwtUtil frontJwtUtil,
            UserRateLimitService rateLimitService,
            TradePasswordVerificationService tradePasswordVerifier,
            IOrderLoginLogService loginLogService,
            GeoIpQueryQueryService geoIpService,
            @Qualifier("userAuditExecutor") ThreadPoolTaskExecutor auditExecutor,
            ObjectMapper objectMapper,
            ObjectProvider<ConfigQueryService> configQueryServiceProvider) {
        this.userMapper = userMapper;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.frontJwtUtil = frontJwtUtil;
        this.rateLimitService = rateLimitService;
        this.tradePasswordVerifier = tradePasswordVerifier;
        this.loginLogService = loginLogService;
        this.geoIpService = geoIpService;
        this.auditExecutor = auditExecutor;
        this.objectMapper = objectMapper;
        this.configQueryServiceProvider = configQueryServiceProvider;
        this.dummyPasswordHash = passwordEncoder.encode("front-user-dummy-password");
    }

    public UserLoginResult login(LoginUserDto request, HttpServletRequest servletRequest) {
        String username = request.getUsername();
        String ipAddress = IpUtils.getIpAddr(servletRequest);
        Map<String, String> safeHeaders = safeHeaders(servletRequest);

        if (rateLimitService.isLoginBlocked(username, ipAddress)) {
            auditLogin(null, ipAddress, false, safeHeaders);
            log.warn("event=front_login_blocked reason=rate_limit");
            throw new UserApiException(622, "The account is temporarily locked, please try again later");
        }

        OrderUser authUser = userMapper.selectAuthUserByName(username);
        boolean passwordMatches;
        if (authUser == null) {
            passwordMatches = passwordEncoder.matches(request.getPassword(), dummyPasswordHash);
        } else {
            passwordMatches = passwordEncoder.matches(request.getPassword(), authUser.getPassword());
        }

        if (authUser == null || !passwordMatches || "1".equals(authUser.getAccountStatus())) {
            boolean blocked = rateLimitService.recordLoginFailure(username, ipAddress);
            auditLogin(authUser == null ? null : authUser.getId(), ipAddress, false, safeHeaders);
            log.warn("event=front_login_failed reason=credentials_or_account");
            if (blocked) {
                throw new UserApiException(
                        622, "The account is temporarily locked, please try again later");
            }
            throw new UserApiException(601, "The account or password is incorrect");
        }

        UserProfileResponse profile = loadProfile(
                authUser.getId(),
                SupportedLocale.resolve(null, servletRequest.getHeader("Accept-Language")));
        rateLimitService.clearLoginAccountFailures(username);
        String token = frontJwtUtil.generateToken(authUser);
        auditLogin(authUser.getId(), ipAddress, true, safeHeaders);
        log.info("event=front_login_succeeded userId={}", authUser.getId());
        return new UserLoginResult(token, profile);
    }

    public void register(RegisterDto request) {
        if (userMapper.selectAuthUserByName(request.getUsername()) != null) {
            throw new UserApiException(609, "Username already exists");
        }
        if (Boolean.TRUE.equals(userMapper.existsPhone(request.getPhoneNumber()))) {
            throw new UserApiException(621, "Phone number already exists");
        }

        OrderUser user = new OrderUser();
        user.setUsername(request.getUsername());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setGender(request.getGender());
        user.setInviteCode(request.getInviteCode());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setTradePassword(passwordEncoder.encode(request.getTradePassword()));

        try {
            RegistrationResult result = userService.register(user);
            if (result == RegistrationResult.INVITE_NOT_FOUND) {
                throw new UserApiException(610, "Invite code not found");
            }
            if (result == RegistrationResult.MEMBER_LEVEL_NOT_FOUND) {
                throw new UserApiException(UNKNOWN_ERROR, "Registration is currently unavailable");
            }
        } catch (DuplicateKeyException ex) {
            if (userMapper.selectAuthUserByName(request.getUsername()) != null) {
                throw new UserApiException(609, "Username already exists");
            }
            if (Boolean.TRUE.equals(userMapper.existsPhone(request.getPhoneNumber()))) {
                throw new UserApiException(621, "Phone number already exists");
            }
            log.warn("event=front_registration_conflict reason=invite_code_collision");
            throw new UserApiException(UNKNOWN_ERROR, "Please retry registration");
        }
    }

    public void logout(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return;
        }
        try {
            frontJwtUtil.invalidateTokenIfPresent(authorizationHeader.substring("Bearer ".length()));
        } catch (RuntimeException ex) {
            // Logout is an idempotent client cleanup operation.
            log.error("event=front_logout_store_failure error={}",
                    ExceptionUtil.getConciseErrorMessage(ex));
        }
    }

    public UserProfileResponse userInfo(Long userId) {
        return loadProfile(userId, SupportedLocale.EN_US);
    }

    public UserProfileResponse userInfo(Long userId, SupportedLocale locale) {
        return loadProfile(userId, locale);
    }

    public int updateAvatar(Long userId, AvatarDto request) {
        int rows = userMapper.updateAvatarById(userId, request.getAvatar());
        if (rows == 0) {
            throw new UserApiException(614, "User not found or avatar is blank");
        }
        return rows;
    }

    @Transactional(rollbackFor = Exception.class)
    public void editPassword(Long userId, EditPasswordDto request) {
        OrderUser user = requireAuthUser(userId);
        if (rateLimitService.isPasswordBlocked(userId)) {
            throw new UserApiException(622, "Too many password attempts, please try again later");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            boolean blocked = rateLimitService.recordPasswordFailure(userId);
            throw new UserApiException(
                    blocked ? 622 : 602,
                    blocked
                            ? "Too many password attempts, please try again later"
                            : "Wrong password");
        }
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new UserApiException(612, "New password must be different from old password");
        }

        String newPassword = passwordEncoder.encode(request.getNewPassword());
        if (userMapper.updatePasswordById(userId, user.getPassword(), newPassword) == 0) {
            throw new UserApiException(UNKNOWN_ERROR, "Password was changed concurrently, please retry");
        }
        rateLimitService.clearPasswordFailures(userId);
        frontJwtUtil.invalidateAllTokens(userId, user.getUsername());
    }

    public void editTradePassword(Long userId, EditTradePasswordDto request) {
        OrderUser user = requireAuthUser(userId);
        TradePasswordVerificationService.Result result =
                tradePasswordVerifier.verify(user, request.getOldTradePassword());
        requireTradePasswordSuccess(result);
        if (request.getOldTradePassword().equals(request.getNewTradePassword())) {
            throw new UserApiException(612, "New trade password must be different from old trade password");
        }
        String newPassword = passwordEncoder.encode(request.getNewTradePassword());
        if (userMapper.updateTradePasswordById(userId, user.getTradePassword(), newPassword) == 0) {
            throw new UserApiException(
                    UNKNOWN_ERROR, "Trade password was changed concurrently, please retry");
        }
    }

    public void checkTradePassword(Long userId, CheckTradePassword request) {
        OrderUser user = requireAuthUser(userId);
        requireTradePasswordSuccess(tradePasswordVerifier.verify(user, request.getTradePassword()));
    }

    private OrderUser requireAuthUser(Long userId) {
        OrderUser user = userMapper.selectAuthUserById(userId);
        if (user == null) {
            throw new UserApiException(601, "User not found");
        }
        return user;
    }

    private UserProfileResponse loadProfile(Long userId, SupportedLocale locale) {
        OrderUser user = userMapper.selectUserProfileById(userId);
        if (user == null) {
            throw new UserApiException(601, "User not found");
        }
        ConfigQueryService configQueryService = configQueryServiceProvider.getIfAvailable();
        if (configQueryService != null) {
            configQueryService.localizeMemberLevelInPlace(user.getMemberLevel(), locale);
        }
        return UserProfileResponse.from(user);
    }

    private void requireTradePasswordSuccess(TradePasswordVerificationService.Result result) {
        if (result == TradePasswordVerificationService.Result.SUCCESS) {
            return;
        }
        if (result == TradePasswordVerificationService.Result.BLOCKED) {
            throw new UserApiException(
                    622, "Too many trade password attempts, please try again later");
        }
        if (result == TradePasswordVerificationService.Result.MISSING) {
            throw new UserApiException(611, "Please enter password");
        }
        throw new UserApiException(613, "Wrong trade password");
    }

    private Map<String, String> safeHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        addHeader(headers, "User-Agent", request.getHeader("User-Agent"));
        addHeader(headers, "Accept-Language", request.getHeader("Accept-Language"));
        addHeader(headers, "Referer", request.getHeader("Referer"));
        return headers;
    }

    private void addHeader(Map<String, String> headers, String name, String value) {
        if (value != null && !value.isBlank()) {
            headers.put(name, value.length() > 512 ? value.substring(0, 512) : value);
        }
    }

    private void auditLogin(
            Long userId,
            String ipAddress,
            boolean success,
            Map<String, String> safeHeaders) {
        final String headersJson;
        try {
            headersJson = objectMapper.writeValueAsString(safeHeaders);
        } catch (JsonProcessingException ex) {
            log.warn("event=front_login_audit_headers_failed");
            return;
        }

        try {
            auditExecutor.execute(() -> {
                try {
                    String address = geoIpService.queryByIp(ipAddress);
                    loginLogService.insertLoginAttempt(
                            userId, ipAddress, address, success ? "1" : "0", headersJson);
                } catch (Exception ex) {
                    log.error("event=front_login_audit_failed userId={} error={}",
                            userId, ExceptionUtil.getConciseErrorMessage(ex));
                }
            });
        } catch (RejectedExecutionException ex) {
            log.warn("event=front_login_audit_dropped reason=queue_full");
        }
    }
}
