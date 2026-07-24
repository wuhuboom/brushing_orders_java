package com.order.framework.web.service;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import com.order.common.constant.CacheConstants;
import com.order.common.constant.Constants;
import com.order.common.constant.UserConstants;
import com.order.common.core.domain.entity.SysUser;
import com.order.common.core.domain.model.LoginUser;
import com.order.common.core.redis.RedisCache;
import com.order.common.exception.ServiceException;
import com.order.common.exception.user.BlackListException;
import com.order.common.exception.user.CaptchaException;
import com.order.common.exception.user.CaptchaExpireException;
import com.order.common.exception.user.UserNotExistsException;
import com.order.common.exception.user.UserPasswordNotMatchException;
import com.order.common.utils.DateUtils;
import com.order.common.utils.MessageUtils;
import com.order.common.utils.StringUtils;
import com.order.common.utils.ip.IpUtils;
import com.order.framework.manager.AsyncManager;
import com.order.framework.manager.factory.AsyncFactory;
import com.order.framework.security.context.AuthenticationContextHolder;
import com.order.system.service.ISysConfigService;
import com.order.system.service.ISysUserService;
import com.order.common.utils.GoogleAuthUtils;
import com.order.common.utils.CryptoUtils;
import com.order.framework.web.domain.LoginResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 登录校验方法
 * 
 * @author order
 */
@Component
public class SysLoginService
{
    private static final Logger log = LoggerFactory.getLogger(SysLoginService.class);

    @Autowired
    private TokenService tokenService;

    @Resource
    private AuthenticationManager authenticationManager;

    @Autowired
    private RedisCache redisCache;
    
    @Autowired
    private ISysUserService userService;

    @Autowired
    private SysPermissionService permissionService;

    @Autowired
    private ISysConfigService configService;

    /**
     * 登录验证
     * 
     * @param username 用户名
     * @param password 密码
     * @param code 验证码
     * @param uuid 唯一标识
     * @return 结果
     */
    public LoginResponse login(String username, String password, String code, String uuid, String googleCode)
    {
        // 验证码校验
      //  validateCaptcha(username, code, uuid);
        // 登录前置校验
        loginPreCheck(username, password);

        // 用户验证
        Authentication authentication = null;
        try
        {
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
            AuthenticationContextHolder.setContext(authenticationToken);
            // 该方法会去调用UserDetailsServiceImpl.loadUserByUsername
            authentication = authenticationManager.authenticate(authenticationToken);
        }
        catch (Exception e)
        {
            if (e instanceof BadCredentialsException)
            {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.not.match")));
                throw new UserPasswordNotMatchException();
            }
            else
            {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, e.getMessage()));
                throw new ServiceException(e.getMessage());
            }
        }
        finally
        {
            AuthenticationContextHolder.clearContext();
        }
        AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success")));
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        recordLoginInfo(loginUser.getUserId());

        // 处理 Google Auth
        SysUser sysUser = loginUser.getUser();
        LoginResponse response = new LoginResponse();

        // googleEnabled: 0=启用, 1=未启用
        boolean googleEnabled = "0".equals(sysUser.getGoogleEnabled());

        if (googleEnabled) {
            // 如果启用但 secret 为空 -> 首次配置，生成 secret 并返回 qr
            if (sysUser.getGoogleAuthSecret() == null || sysUser.getGoogleAuthSecret().trim().isEmpty()) {
                try {
                    String secret = GoogleAuthUtils.generateSecret();
                    String otpAuthUrl = GoogleAuthUtils.getOtpAuthUrl(secret, sysUser.getUserName(), "order-app");
                    String qrBase64 = GoogleAuthUtils.generateQRCodeBase64(otpAuthUrl, 300, 300);
                    // 保存 secret 到 Redis 临时键，等待前端确认绑定后再写入 DB
                    String enc = CryptoUtils.encrypt(secret);
                    String tmpKey = "google:tmp:" + sysUser.getUserId();
                    redisCache.setCacheObject(tmpKey, enc, 5, TimeUnit.MINUTES); // 5 分钟有效

                    response.setFirstTimeGoogleSetup(true);
                    response.setQrCodeBase64(qrBase64);
                    response.setOtpAuthUrl(otpAuthUrl);
                    // 不颁发 token，要求前端先完成首次绑定（提交 googleCode 到确认接口）
                    return response;
                } catch (Exception e) {
                    throw new ServiceException("生成二维码失败: " + e.getMessage());
                }
            } else {
                // 已配置 Secret，必须校验前端提交的 googleCode
                if (googleCode == null || googleCode.trim().isEmpty()) {
                    throw new ServiceException("需要 Google 验证码");
                }
                // 从数据库读出的 secret 是加密的，先解密再校验
                String decryptedSecret;
                try {
                    decryptedSecret = CryptoUtils.decrypt(sysUser.getGoogleAuthSecret());
                } catch (Exception ex) {
                    throw new ServiceException("Google secret 解密失败");
                }
                boolean ok = GoogleAuthUtils.validateTotp(decryptedSecret, googleCode, 1);
                if (!ok) {
                    AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, "Google 验证失败"));
                    throw new ServiceException("Google 验证失败");
                }
            }
        }

        // 未启用 Google 或 验证通过，生成 token
        String token = tokenService.createToken(loginUser);
        response.setToken(token);
        return response;
    }

    /**
     * 确认 Google 绑定：前端在扫码并获得首次验证码后调用此方法完成绑定。
     * - 从 Redis 读取临时 secret，加密形式
     * - 解密后校验 googleCode
     * - 成功则持久化 encrypted secret 到 DB（encrypted），并更新用户绑定标识
     * @param username 用户名
     * @param googleCode 前端提交的一次性验证码
     * @return 绑定成功后生成的 token（直接登录）
     */
    public String confirmGoogleBind(String username, String googleCode) {
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(googleCode)) {
            throw new ServiceException("参数错误");
        }
        SysUser user = userService.selectUserByUserName(username);
        if (user == null) throw new ServiceException("用户不存在");
        String tmpKey = "google:tmp:" + user.getUserId();
        String enc = redisCache.getCacheObject(tmpKey);
        if (enc == null) {
            throw new ServiceException("临时 secret 已过期或不存在，请重新生成二维码并扫码");
        }
        String secret;
        try {
            secret = CryptoUtils.decrypt(enc);
        } catch (Exception e) {
            throw new ServiceException("临时 secret 解密失败");
        }
        boolean ok = GoogleAuthUtils.validateTotp(secret, googleCode, 1);
        if (!ok) {
            throw new ServiceException("Google 验证失败");
        }
        // 校验成功，持久化 encrypted secret 到 DB，并标记已绑定
        // 使用专用方法更新 Google 相关字段，避免影响角色等其他字段
        userService.updateUserGoogleAuthSecret(user.getUserId(), enc);
        // 删除临时缓存
        redisCache.deleteObject(tmpKey);

        // 重新从数据库读取用户，确保角色/部门等信息完整
        SysUser persisted = userService.selectUserById(user.getUserId());

        // 记录调试信息：用户角色和权限
        try {
            if (persisted != null) {
                if (persisted.getRoles() != null) {
                    log.info("confirmGoogleBind: userId={} persisted roles count={}", persisted.getUserId(), persisted.getRoles().size());
                } else {
                    log.info("confirmGoogleBind: userId={} persisted roles is null", persisted.getUserId());
                }
                Set<String> perms = permissionService.getMenuPermission(persisted);
                log.info("confirmGoogleBind: userId={} computed permissions={}", persisted.getUserId(), perms);
            } else {
                log.warn("confirmGoogleBind: persisted user is null for userId={}", user.getUserId());
            }
        } catch (Exception ex) {
            log.error("confirmGoogleBind: failed to compute permissions for userId={} error={}", user.getUserId(), ex.getMessage());
        }

        // 自动创建 token 并返回：构造 LoginUser 时要确保 permissions 非空，避免后续 getInfo 中 NPE
        Set<String> permissions = new HashSet<>();
        try {
            if (persisted != null) {
                Set<String> perms = permissionService.getMenuPermission(persisted);
                if (perms != null) {
                    permissions = perms;
                }
            }
        } catch (Exception ex) {
            // 保守处理：保持空集合，避免 NPE
        }
        LoginUser loginUser = new LoginUser(persisted != null ? persisted.getUserId() : user.getUserId(), persisted != null ? persisted.getDeptId() : user.getDeptId(), persisted != null ? persisted : user, permissions);
        String token = tokenService.createToken(loginUser);
        log.info("confirmGoogleBind: Created token for userId={}, tokenKey={}", loginUser.getUserId(), token);
        return token;
    }

    /**
     * 校验验证码
     * 
     * @param username 用户名
     * @param code 验证码
     * @param uuid 唯一标识
     * @return 结果
     */
    public void validateCaptcha(String username, String code, String uuid)
    {
        boolean captchaEnabled = configService.selectCaptchaEnabled();
        if (captchaEnabled)
        {
            String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + StringUtils.nvl(uuid, "");
            String captcha = redisCache.getCacheObject(verifyKey);
            if (captcha == null)
            {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire")));
                throw new CaptchaExpireException();
            }
            redisCache.deleteObject(verifyKey);
            if (!code.equalsIgnoreCase(captcha))
            {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.error")));
                throw new CaptchaException();
            }
        }
    }

    /**
     * 登录前置校验
     * @param username 用户名
     * @param password 用户密码
     */
    public void loginPreCheck(String username, String password)
    {
        // 用户名或密码为空 错误
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password))
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("not.null")));
            throw new UserNotExistsException();
        }
        // 密码如果不在指定范围内 错误
        if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH)
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.not.match")));
            throw new UserPasswordNotMatchException();
        }
        // 用户名不在指定范围内 错误
        if (username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH)
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.not.match")));
            throw new UserPasswordNotMatchException();
        }
        // IP黑名单校验
        String blackStr = configService.selectConfigByKey("sys.login.blackIPList");
        if (IpUtils.isMatchedIp(blackStr, IpUtils.getIpAddr()))
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("login.blocked")));
            throw new BlackListException();
        }
    }

    /**
     * 记录登录信息
     *
     * @param userId 用户ID
     */
    public void recordLoginInfo(Long userId)
    {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(userId);
        sysUser.setLoginIp(IpUtils.getIpAddr());
        sysUser.setLoginDate(DateUtils.getNowDate());
        userService.updateUserProfile(sysUser);
    }
}
