package com.brushing.web.controller.system;

import java.util.Date;
import java.util.List;
import java.util.Set;

import com.brushing.framework.web.domain.MfaVerifyReq;
import com.brushing.framework.web.service.GoogleAuthenticatorService;
import com.brushing.set.service.IOrderSiteConfigService;
import com.brushing.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.brushing.common.constant.Constants;
import com.brushing.common.core.domain.AjaxResult;
import com.brushing.common.core.domain.entity.SysMenu;
import com.brushing.common.core.domain.entity.SysUser;
import com.brushing.common.core.domain.model.LoginBody;
import com.brushing.common.core.domain.model.LoginUser;
import com.brushing.common.core.text.Convert;
import com.brushing.common.utils.DateUtils;
import com.brushing.common.utils.SecurityUtils;
import com.brushing.common.utils.StringUtils;
import com.brushing.framework.web.service.SysLoginService;
import com.brushing.framework.web.service.SysPermissionService;
import com.brushing.framework.web.service.TokenService;
import com.brushing.system.service.ISysConfigService;
import com.brushing.system.service.ISysMenuService;

/**
 * 登录验证
 * 
 * @author brushing
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

    @Autowired
    private GoogleAuthenticatorService googleAuthenticatorService;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private IOrderSiteConfigService siteConfigService;

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
        String token = loginService.login(loginBody.getUsername(), loginBody.getPassword(), loginBody.getCode(), loginBody.getUuid());

        String totpEnabled = siteConfigService.selectOrderSiteConfigById(1L).getTotpEnabled();

        if (totpEnabled.equals("1")){
            ajax.put(Constants.TOKEN, token);
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
                return AjaxResult.error("请输入验证码");
            }
            boolean verified = googleAuthenticatorService.verify(user.getUserId(), Integer.parseInt(totpCode));
            if (!verified) {
                return AjaxResult.error("验证码不正确");
            }
            ajax.put(Constants.TOKEN, token); // 普通登录成功，返回 Token
        }

        // 判断用户是否是首次登录
        ajax.put("isFirstLogin", user.getTotpEnabled().equals("1") ? true : false);
        return ajax;
    }


    @PostMapping("/firstLogin")
    public AjaxResult firstLogin(@RequestBody LoginBody loginBody) {
        AjaxResult ajax = AjaxResult.success();
        String token = loginService.login(loginBody.getUsername(), loginBody.getPassword(), loginBody.getCode(), loginBody.getUuid());

        SysUser user = userService.selectUserByUserName(loginBody.getUsername());

        // 验证 Google 验证码
        String totpCode = loginBody.getTotpCode();
        boolean verified = googleAuthenticatorService.verify(user.getUserId(), Integer.parseInt(totpCode));

        if (!verified) {
            return AjaxResult.error("验证码不正确");
        }

        // 启用 Google 验证器
        googleAuthenticatorService.enable(user.getUserId(), Integer.parseInt(totpCode));

        ajax.put(Constants.TOKEN, token); // 普通登录成功，返回 Token
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
}
