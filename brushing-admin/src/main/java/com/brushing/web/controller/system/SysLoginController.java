package com.brushing.web.controller.system;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.brushing.framework.web.service.GoogleAuthenticatorService;
import com.brushing.set.service.IOrderSiteConfigService;
import com.brushing.system.service.ISysUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.brushing.common.constant.Constants;
import com.brushing.common.core.domain.AjaxResult;
import com.brushing.common.core.domain.entity.SysMenu;
import com.brushing.common.core.domain.entity.SysUser;
import com.brushing.common.core.domain.model.LoginBody;
import com.brushing.common.core.domain.model.LoginUser;
import com.brushing.common.core.text.Convert;
import com.brushing.common.utils.DateUtils;
import com.brushing.common.utils.MessageUtils;
import com.brushing.common.utils.SecurityUtils;
import com.brushing.common.utils.StringUtils;
import com.brushing.framework.web.service.SysLoginService;
// import com.brushing.framework.web.service.SysPasswordService;
import com.brushing.framework.web.service.SysPermissionService;
import com.brushing.framework.web.service.TokenService;
import com.brushing.system.service.ISysConfigService;
import com.brushing.system.service.ISysMenuService;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import jakarta.servlet.http.Cookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 登录验证
 * 
 * @author brushing
 */
@RestController
public class SysLoginController
{
    private static final Logger log = LoggerFactory.getLogger(SysLoginController.class);
    @Autowired
    private SysLoginService loginService;

    @Autowired
    private ISysMenuService menuService;

    @Autowired
    private SysPermissionService permissionService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private GoogleAuthenticatorService googleAuthenticatorService;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private IOrderSiteConfigService siteConfigService;

    // @Autowired
    // private SysPasswordService passwordService;

    /**
     * 登录方法
     * 
     * @param loginBody 登录信息
     * @return 结果
     */
    @PostMapping("/login")
    public AjaxResult login(@RequestBody LoginBody loginBody)
    {
        // TODO: 临时重置 admin 密码为 admin123，用完请删除
        // if ("admin".equals(loginBody.getUsername())) {
        //     passwordService.clearLoginRecordCache("admin");
        //     SysUser adminUser = userService.selectUserByUserName("admin");
        //     if (adminUser != null) {
        //         userService.resetUserPwd(adminUser.getUserId(), SecurityUtils.encryptPassword("admin123"));
        //     }
        // }

        AjaxResult ajax = AjaxResult.success();
        LoginUser loginUser = loginService.authenticate(
                loginBody.getUsername(), loginBody.getPassword(), loginBody.getCode(), loginBody.getUuid());

        String totpEnabled = siteConfigService.selectOrderSiteConfigById(1L).getTotpEnabled();

        if (totpEnabled.equals("1")){
            ajax.put(Constants.TOKEN, tokenService.createToken(loginUser));
            return ajax;
        }
        // 获取当前登录用户的信息
        SysUser user = userService.selectUserByUserName(loginBody.getUsername());

        // 如果用户需要 Google 验证器绑定（首次登录），返回二维码链接
        if (user.getTotpEnabled().equals("1")) {
            String otpauthUri = googleAuthenticatorService.startSetup(user.getUserId(), loginBody.getUsername(), "brushing");
            ajax.put("mfaRequired", true); // 告知前端需要进行 Google 验证器绑定
            ajax.put("otpauthUri", otpauthUri); // 返回二维码链接
        } else {
            // 如果不是首次登录，验证 Google 验证码
            String totpCode = loginBody.getTotpCode();
            if (StringUtils.isEmpty(totpCode)) {
                return AjaxResult.error(MessageUtils.message("login.please_enter_captcha"));
            }
            boolean verified = googleAuthenticatorService.verify(user.getUserId(), Integer.parseInt(totpCode));
            if (!verified) {
                return AjaxResult.error(MessageUtils.message("login.captcha_invalid"));
            }
            ajax.put(Constants.TOKEN, tokenService.createToken(loginUser)); // 普通登录成功，返回 Token
        }

        // 判断用户是否是首次登录
        ajax.put("isFirstLogin", user.getTotpEnabled().equals("1"));
        return ajax;
    }


    @PostMapping("/firstLogin")
    public AjaxResult firstLogin(@RequestBody LoginBody loginBody) {
        AjaxResult ajax = AjaxResult.success();
        LoginUser loginUser = loginService.authenticate(
                loginBody.getUsername(), loginBody.getPassword(), loginBody.getCode(), loginBody.getUuid());

        SysUser user = userService.selectUserByUserName(loginBody.getUsername());

        // 验证 Google 验证码
        String totpCode = loginBody.getTotpCode();
        boolean verified = googleAuthenticatorService.verify(user.getUserId(), Integer.parseInt(totpCode));

        if (!verified) {
            return AjaxResult.error(MessageUtils.message("login.captcha_invalid"));
        }

        // 启用 Google 验证器
        googleAuthenticatorService.enable(user.getUserId(), Integer.parseInt(totpCode));

        ajax.put(Constants.TOKEN, tokenService.createToken(loginUser)); // 普通登录成功，返回 Token
        return ajax;
    }




    /**
     * 获取用户信息
     * 
     * @return 用户信息
     */
    @GetMapping("getInfo")
    public AjaxResult getInfo()
    {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        SysUser user = loginUser.getUser();
        // 角色集合
        Set<String> roles = permissionService.getRolePermission(user);
        // 权限集合
        Set<String> permissions = permissionService.getMenuPermission(user);
        if (!loginUser.getPermissions().equals(permissions))
        {
            loginUser.setPermissions(permissions);
            tokenService.refreshToken(loginUser);
        }
        AjaxResult ajax = AjaxResult.success();
        ajax.put("user", user);
        ajax.put("roles", roles);
        ajax.put("permissions", permissions);
        ajax.put("isDefaultModifyPwd", initPasswordIsModify(user.getPwdUpdateDate()));
        ajax.put("isPasswordExpired", passwordIsExpiration(user.getPwdUpdateDate()));
        return ajax;
    }

    /**
     * 获取路由信息
     * 
     * @return 路由信息
     */
    @GetMapping("getRouters")
    public AjaxResult getRouters()
    {
        Long userId = SecurityUtils.getUserId();
        List<SysMenu> menus = menuService.selectMenuTreeByUserId(userId);
        return AjaxResult.success(menuService.buildMenus(menus));
    }
    
    // 检查初始密码是否提醒修改
    public boolean initPasswordIsModify(Date pwdUpdateDate)
    {
        Integer initPasswordModify = Convert.toInt(configService.selectConfigByKey("sys.account.initPasswordModify"));
        return initPasswordModify != null && initPasswordModify == 1 && pwdUpdateDate == null;
    }

    // 检查密码是否过期
    public boolean passwordIsExpiration(Date pwdUpdateDate)
    {
        Integer passwordValidateDays = Convert.toInt(configService.selectConfigByKey("sys.account.passwordValidateDays"));
        if (passwordValidateDays != null && passwordValidateDays > 0)
        {
            if (StringUtils.isNull(pwdUpdateDate))
            {
                // 如果从未修改过初始密码，直接提醒过期
                return true;
            }
            Date nowDate = DateUtils.getNowDate();
            return DateUtils.differentDaysByMillisecond(nowDate, pwdUpdateDate) > passwordValidateDays;
        }
        return false;
    }

    @GetMapping("/changeLanguage")
    public AjaxResult changeLanguage(HttpServletRequest request, HttpServletResponse response, String lang)
    {
        LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
        Locale newLocale;
        if (lang == null || lang.trim().isEmpty()) {
            if (localeResolver != null) {
                newLocale = localeResolver.resolveLocale(request);
            } else {
                newLocale = LocaleContextHolder.getLocale();
            }
        } else {
            // 支持 zh 或 zh_CN 或 zh-CN 等格式
            String normalized = lang.replace('-', '_');
            String[] parts = normalized.split("_");
            if (parts.length == 1) {
                newLocale = new Locale(parts[0]);
            } else {
                newLocale = new Locale(parts[0], parts[1]);
            }
        }
        try {
            if (localeResolver != null) {
              //  log.debug("LocaleResolver impl in controller: {}", localeResolver.getClass().getName());
            } else {
              //  log.warn("No LocaleResolver available in controller");
            }
            // SessionLocaleResolver 支持 setLocale，会把选择存入 session / cookie
            if (localeResolver != null) {
                localeResolver.setLocale(request, response, newLocale);
            }
            log.debug("Called localeResolver.setLocale with {}", newLocale);
            // 同时设置当前线程的 LocaleContextHolder，这样 MessageUtils 会立即使用新 Locale
            LocaleContextHolder.setLocale(newLocale);
            // 额外写一个 LOCALE cookie，确保客户端能接收到（若 CookieLocaleResolver 未写入或被代理过滤时作为兜底）
            String localeTag = newLocale.toLanguageTag();
            Cookie localeCookie = new Cookie("LOCALE", localeTag);
            localeCookie.setPath("/");
            int maxAge = 60 * 60 * 24 * 30; // 30 天
            localeCookie.setMaxAge(maxAge);
            localeCookie.setHttpOnly(false);
            response.addCookie(localeCookie);
      //      log.debug("Added LOCALE cookie with value {}", localeCookie.getValue());
            // also add a response header and include cookie value in body for debugging
            response.setHeader("X-LOCALE-SET", localeTag);
            // Additionally set Set-Cookie header explicitly with SameSite attribute to help cross-site scenarios.
            // Note: browsers require SameSite=None; Secure for third-party cookies (HTTPS). We add Secure only when request.isSecure().
            StringBuilder setCookieHeader = new StringBuilder();
            setCookieHeader.append("LOCALE=").append(localeCookie.getValue());
            setCookieHeader.append("; Path=/; Max-Age=").append(maxAge);
            // For local debugging include SameSite=None so browsers will consider this cookie for cross-site requests.
            // WARNING: In production you should set SameSite=None; Secure and use HTTPS. Some browsers may ignore SameSite=None without Secure.
            setCookieHeader.append("; SameSite=None");
            if (request.isSecure()) {
                setCookieHeader.append("; Secure");
            }
       //     log.debug("Setting explicit Set-Cookie header: {}", setCookieHeader);
             // HttpOnly is false so front-end can read during debugging; if you want it hidden set HttpOnly
             response.addHeader("Set-Cookie", setCookieHeader.toString());
             AjaxResult ajax = AjaxResult.success(newLocale);
             ajax.put("localeCookie", localeTag);
             System.out.println("Locale changed to: " + newLocale);
             return ajax;
        } catch (UnsupportedOperationException e) {
            // 如果 resolver 不支持 setLocale（例如 AcceptHeaderResolver），可以记录或返回错误信息
            System.err.println("LocaleResolver does not support setLocale: " + e.getMessage());
        }
        return AjaxResult.success(newLocale);
     }

    @GetMapping("/test/i18n")
    public AjaxResult testI18n(HttpServletRequest request) {
        LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
        Locale resolved = null;
        if (localeResolver != null) {
            resolved = localeResolver.resolveLocale(request);
        }
        if (resolved == null) {
            resolved = LocaleContextHolder.getLocale();
        }
        log.debug("/test/i18n resolved locale: {}", resolved);
        String msg = MessageUtils.message("login.captcha_invalid");
        AjaxResult ajax = AjaxResult.success(msg);
        ajax.put("resolvedLocale", resolved.toString());
        return ajax;
    }

}
