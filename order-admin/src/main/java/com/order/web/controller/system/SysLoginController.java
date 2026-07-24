package com.order.web.controller.system;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;

import com.order.framework.web.domain.LoginResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.order.common.constant.Constants;
import com.order.common.core.domain.AjaxResult;
import com.order.common.core.domain.entity.SysMenu;
import com.order.common.core.domain.entity.SysUser;
import com.order.common.core.domain.model.LoginBody;
import com.order.common.core.domain.model.LoginUser;
import com.order.common.core.text.Convert;
import com.order.common.utils.DateUtils;
import com.order.common.utils.SecurityUtils;
import com.order.common.utils.StringUtils;
import com.order.framework.web.service.SysLoginService;
import com.order.framework.web.service.SysPermissionService;
import com.order.framework.web.service.TokenService;
import com.order.system.service.ISysConfigService;
import com.order.system.service.ISysMenuService;
import com.order.common.exception.ServiceException;

/**
 * 登录验证
 * 
 * @author order
 */
@RestController
public class SysLoginController
{
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

    /**
     * 登录方法
     * 
     * @param loginBody 登录信息
     * @return 结果
     */
    @PostMapping("/login")
    public AjaxResult login(@RequestBody LoginBody loginBody)
    {
        AjaxResult ajax = AjaxResult.success();
        // 调用新的登录方法，包含 googleCode
        LoginResponse resp = loginService.login(loginBody.getUsername(), loginBody.getPassword(), loginBody.getCode(),
                loginBody.getUuid(), loginBody.getGoogleCode());
        if (resp.isFirstTimeGoogleSetup()) {
            // 返回二维码和 otpauth URL 给前端，前端需要用户扫码并提交首次验证码
            ajax.put("firstTimeGoogleSetup", true);
            ajax.put("qrCodeBase64", resp.getQrCodeBase64());
            ajax.put("otpAuthUrl", resp.getOtpAuthUrl());
            return ajax;
        }
        // 正常登录，返回 token
        ajax.put(Constants.TOKEN, resp.getToken());
        return ajax;
    }

    @PostMapping("/google/confirm")
    public AjaxResult confirmGoogleBind(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String googleCode = body.get("googleCode");
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(googleCode)) {
            return AjaxResult.error("参数 username 或 googleCode 不能为空");
        }
        try {
            String token = loginService.confirmGoogleBind(username, googleCode);
            AjaxResult ajax = AjaxResult.success();
            ajax.put(Constants.TOKEN, token);
            return ajax;
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 获取用户信息
     * 
     * @return 用户信息
     */
    @GetMapping("getInfo")
    public AjaxResult getInfo()
    {
        LoginUser loginUser;
        try {
            loginUser = SecurityUtils.getLoginUser();
        } catch (ServiceException se) {
            return AjaxResult.error("用户未登录或会话已过期，请重新登录");
        }
        if (loginUser == null)
        {
            return AjaxResult.error("用户未登录或会话已过期，请重新登录");
        }
        SysUser user = loginUser.getUser();
        if (user == null)
        {
            return AjaxResult.error("用户信息为空，请重新登录");
        }
        // 角色集合
        Set<String> roles = permissionService.getRolePermission(user);
        // 权限集合，permissionService 可能返回 null，统一为非空集合
        Set<String> permissions;
        try {
            Set<String> perms = permissionService.getMenuPermission(user);
            permissions = (perms == null) ? new java.util.HashSet<>() : perms;
        } catch (Exception e) {
            // 如果权限服务异常，保护性处理为 empty set，避免抛出到前端
            permissions = new java.util.HashSet<>();
        }
        // null-safe comparison and refresh token if changed
        Set<String> currentPerms = loginUser.getPermissions();
        if (currentPerms == null) {
            loginUser.setPermissions(permissions);
            tokenService.refreshToken(loginUser);
        } else if (!Objects.equals(currentPerms, permissions))
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
}
