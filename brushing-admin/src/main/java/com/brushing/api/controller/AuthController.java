package com.brushing.api.controller;

import com.brushing.api.dto.LoginUserDto;
import com.brushing.api.dto.RegisterDto;
import com.brushing.common.core.controller.BaseController;
import com.brushing.common.core.domain.AjaxResult;
import com.brushing.common.utils.DateUtils;
import com.brushing.common.utils.StringUtils;
import com.brushing.common.utils.ip.IpUtils;
import com.brushing.framework.front.FrontJwtUtil;
import com.brushing.framework.init.GeoIpQueryQueryService;
import com.brushing.member.domain.OrderMemberUser;
import com.brushing.member.domain.OrderUserLoginLog;
import com.brushing.member.service.IOrderMemberUserService;
import com.brushing.member.service.IOrderUserLoginLogService;
import com.brushing.api.controller.vo.*;
import com.brushing.set.domain.OrderSiteConfig;
import com.brushing.set.service.IOrderSiteConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Tag(
        name = "用户管理",
        description =
                "错误码对照表：\n" +
                        "601: User not found （用户不存在）\n" +
                        "602: Wrong password （密码错误）\n" +
                        "603: Username must not be blank （用户名不能为空）\n" +
                        "604: Password must not be blank （密码不能为空）\n" +
                        "605: Trade password must not be blank （交易密码不能为空）\n" +
                        "606: Phone number must not be blank （手机号不能为空）\n" +
                        "607: Sex must not be blank （性别不能为空）\n" +
                        "608: Invite code must not be blank （邀请码不能为空）\n" +
                        "609: Username already exists （用户名已存在）\n" +
                        "610: Invite code not found （邀请码不存在）\n" +
                        "611: Please enter password （请输入密码）\n" +
                        "612: Trade password length must be between 6 and 18 characters （交易密码长度需在6-18位）\n" +
                        "613: Wrong trade password （交易密码错误）\n" +
                        "614: User not found or avatar is blank （用户不存在或头像为空）\n" +
                        "615: Invalid authorization header \n (请求头无效)" +
                        "616: Logout failed \n (退出登录失败)" +
                        "617: Unknown error \n (登录失败，未知错误)"+
                        "618: The account disabled \n (账户禁用)"+
                        "621: 电话号码已经存在"+
                        "401: (token失效，或者未登录) \n "
)
@RestController
@RequestMapping("/api/user")
public class AuthController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private IOrderMemberUserService userService;

    @Autowired
    private FrontJwtUtil frontJwtUtil;

    @Autowired
    private GeoIpQueryQueryService queryService;

    @Autowired
    private IOrderUserLoginLogService userLoginLogService;

    @Autowired
    private IOrderSiteConfigService siteConfigService;



    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @PostMapping("/login")
    @Operation(summary = "登录操作")
    public AjaxResult login(@RequestBody LoginUserDto loginRequest, HttpServletRequest request){
        String username = loginRequest.getUsername();
        OrderMemberUser user = userService.findByUsername(username);

        // prepare log object (will set more fields as we go)
        OrderUserLoginLog loginLog = new OrderUserLoginLog();
        // set creation time early so mapper includes create_time
        loginLog.setCreateTime(DateUtils.getNowDate());
        // set userId now if user exists (null otherwise)
        loginLog.setUserId(user != null ? user.getId() : null);
        try {
            String ipAddr = IpUtils.getIpAddress(request);
             String address = queryService.queryByIp(ipAddr);
             loginLog.setIp(ipAddr);
             loginLog.setLocation(address);
             // loginDomain from Host header or server name
             String host = request.getHeader("Host");
             if (host == null) host = request.getServerName();
             loginLog.setLoginDomain(host);
             // User-Agent parsing: put raw UA into browser field and parse OS
             String ua = request.getHeader("User-Agent");
             if (ua != null) {
                 loginLog.setBrowser(ua);
                 String os = "Unknown";
                 if (ua.contains("Windows")) os = "Windows";
                 else if (ua.contains("Mac")) os = "Mac";
                 else if (ua.contains("Android")) os = "Android";
                 else if (ua.contains("iPhone") || ua.contains("iPad")) os = "iOS";
                 loginLog.setOs(os);
             }
         } catch (Exception ex) {
             log.warn("Failed to resolve client IP/location or user-agent for login log", ex);
         }

         if (StringUtils.isNull(user)){
             loginLog.setStatus("1");
             loginLog.setUserId(null);
             try {
                 log.debug("Prepared loginLog for unknown user: {}", loginLog);
                 int rows = userLoginLogService.insertOrderUserLoginLogNewTx(loginLog);
                 if (rows <= 0) {
                     log.warn("insertOrderUserLoginLog returned {} when logging failed login for unknown user {}", rows, username);
                 } else {
                     log.info("login log inserted for unknown user {}", username);
                 }
             } catch (Exception e) {
                 log.error("Failed to insert login log for unknown user: {}", username, e);
             }
             return AjaxResult.error(601, "The account or password is incorrect");
         }
        boolean matches = passwordEncoder.matches(loginRequest.getPassword(), user.getPassword());
        if (!matches) {
            loginLog.setStatus("1");
            loginLog.setUserId(user.getId());
            try {
                log.debug("Prepared loginLog for failed password: {}", loginLog);
                 int rows = userLoginLogService.insertOrderUserLoginLogNewTx(loginLog);
                 if (rows <= 0) {
                     log.warn("insertOrderUserLoginLog returned {} when logging failed login for user id {}", rows, user.getId());
                 } else {
                     log.info("login log inserted for failed login user id {}", user.getId());
                 }
              } catch (Exception e) {
                  log.error("Failed to insert login log for user id {}: {}", user.getId(), e.getMessage(), e);
              }
             return AjaxResult.error(602, "The account or password is incorrect");
         }
        if (user.getAccountStatus().equals("1")){
            loginLog.setStatus("1");
            loginLog.setUserId(user.getId());
            try {
                log.debug("Prepared loginLog for disabled account: {}", loginLog);
                 int rows = userLoginLogService.insertOrderUserLoginLogNewTx(loginLog);
                 if (rows <= 0) log.warn("insertOrderUserLoginLog returned {} when logging disabled account for user {}", rows, user.getId());
             } catch (Exception e) {
                 log.error("Failed to insert login log for disabled account user {}", user.getId(), e);
             }
            return  AjaxResult.error(618, "The account disabled");
        }
        try {
            String token = frontJwtUtil.generateToken(user.getUsername());
            String ipAddr = loginLog.getIp();
            if (ipAddr == null) {
                ipAddr = IpUtils.getIpAddr();
            }
            String address = queryService.queryByIp(ipAddr);
            user.setLastLoginTime(DateUtils.getNowDate());
            user.setRegisterIp(ipAddr + "," + address);
            userService.updateOrderMemberUser(user);

            // record success login
            loginLog.setStatus("0");
            loginLog.setUserId(user.getId());
            try {
                log.debug("Prepared loginLog for success: {}", loginLog);
                 int rows = userLoginLogService.insertOrderUserLoginLogNewTx(loginLog);
                 if (rows <= 0) {
                     log.warn("insertOrderUserLoginLog returned {} when inserting success login for user id {}", rows, user.getId());
                 } else {
                     log.info("login log inserted for successful login user id {}", user.getId());
                 }
              } catch (Exception e) {
                  log.error("Failed to insert login log after successful login for user id {}", user.getId(), e);
              }

            AjaxResult result = new AjaxResult();
            result.put("token", token);
            result.put("user", user);
            return success(result);
        }catch (Exception e){
            // record failed login due to exception
            loginLog.setStatus("1");
            loginLog.setUserId(user != null ? user.getId() : null);
            try {
                log.debug("Prepared loginLog for exception handler: {}", loginLog);
                 int rows = userLoginLogService.insertOrderUserLoginLogNewTx(loginLog);
                 if (rows <= 0) {
                     log.warn("insertOrderUserLoginLog returned {} when logging exception for user {}", rows, user != null ? user.getId() : username);
                 } else {
                     log.info("login log inserted in exception handler for user {}", user != null ? user.getId() : username);
                 }
              } catch (Exception ex) {
                  log.error("Failed to insert login log in exception handler for user {}", user != null ? user.getId() : username, ex);
              }
             return AjaxResult.error(617,"Unknown error");
        }
    }

    @PostMapping("/register")
    @Operation(
            summary = "注册操作",
            description = "username:用户名，password：密码，tradePassword：交易密码，phone:电话，sex: 性别 0 男，1 女 ，inviteCode邀请码"
    )
    public  AjaxResult register(@RequestBody RegisterDto registerDto){
        if (StringUtils.isEmpty(registerDto.getUsername())){
            return AjaxResult.error(603, "Username must not be blank");
        }
        if (StringUtils.isEmpty(registerDto.getPassword())){
            return AjaxResult.error(604, "Password must not be blank");
        }
       OrderSiteConfig orderSiteConfig = siteConfigService.selectOrderSiteConfigById(1L);

        if(orderSiteConfig.getNeedPhone().equals("0") ){
            if (StringUtils.isEmpty(registerDto.getPhone())){
                return AjaxResult.error(606, "Phone number must not be blank");
            }
        }

//        if (StringUtils.isEmpty(registerDto.getTradePassword())){
//            return AjaxResult.error(605, "Trade password must not be blank");
//        }
        /*if (StringUtils.isEmpty(registerDto.getPhone())){
            return AjaxResult.error(606, "Phone number must not be blank");
        }*/
        if (userService.existsPhone(registerDto.getPhone())){
            return AjaxResult.error(621, "Phone number already exists");
        }
        if (StringUtils.isEmpty(registerDto.getSex())){
            return AjaxResult.error(607, "Sex must not be blank");
        }
        if (StringUtils.isEmpty(registerDto.getInviteCode())){
            return AjaxResult.error(608, "Invite code must not be blank");
        }
        OrderMemberUser user = userService.findByUsername(registerDto.getUsername().trim());
        if (StringUtils.isNotNull(user)){
            return AjaxResult.error(609, "Username already exists");
        }
        OrderMemberUser orderMemberUser = setUser(registerDto);
        orderMemberUser.setPassword(encoder.encode(registerDto.getPassword()));
        orderMemberUser.setSex(registerDto.getSex());
        if (orderSiteConfig == null||orderSiteConfig.getNewUserCanTask().equals("0")) {
            orderMemberUser.setTaskStatus("0");
        } else {
            orderMemberUser.setTaskStatus("1");
        }
        if (StringUtils.isNotEmpty(registerDto.getTradePassword())){
            orderMemberUser.setTradePassword(encoder.encode(registerDto.getTradePassword()));
        }
        String register = userService.register(orderMemberUser);
        if ("200".equals(register)){
            return success("register success");
        } else {
            return AjaxResult.error(610, "Invite code not found");
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public AjaxResult logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.isEmpty(authHeader) || !authHeader.startsWith("Bearer ")) {
            return AjaxResult.error(615, "Invalid authorization header");
        }

        String token = authHeader.substring("Bearer ".length());
        try {
            String username = frontJwtUtil.getUsernameFromToken(token);
            frontJwtUtil.invalidateToken(token, username);
            return success("Logout successful");
        } catch (Exception e) {
            return AjaxResult.error(616, "Logout failed");
        }
    }

    @GetMapping("/getInfo")
    @Operation(
            summary = "获取用户信息",
            description =
                    "'username': '用户名', 'phone': '手机号', 'password': '登录密码', 'tradePassword': '交易密码', " +
                            "'parentId': '上级用户ID', 'email': '邮箱', 'creditScore': '信誉分', 'balance': '可用余额', 'frozenBalance': '冻结余额', 'totalBalance': '总余额', " +
                            "'inviteCode': '邀请码', 'registerIp': '最近登录', 'lastLoginTime': '登录时间', 'accountStatus': '账户状态', 'tradeStatus': '交易状态', " +
                            "'withdrawStatus': '提现状态', 'realNameStatus': '实名状态', 'realName': '真实姓名', 'idCardNumber': '身份证号', 'idCardFront': '身份证正面', 'idCardBack': '身份证反面', " +
                            "'withdrawName': '提现姓名', 'withdrawAddress': '提现地址', 'withdrawType': '提现类型', 'isReal': '是否假人', 'sex': '性别', 'levelId': '会员等级ID', " +
                            "'commission': '当日佣金', 'allCommission': '累计佣金', 'dealCount': '单数', 'directSubCount': '直属下级人数', 'allSubCount': '所有下级人数', " +
                            "'todayWithdrawCount': '今日提现次数', 'totalWithdrawCount': '历史提现次数', 'todayResetCount': '今日重置次数', 'totalResetCount': '总重置次数', " +
                            "'withdrawTip': '提现提示', 'avatar': '用户头像', 'cardNumber': '卡单数量', 'userLevel.icon': '会员图标', 'userLevel.nameZh': '中文名称', 'userLevel.nameEn': '英文名称', 'userLevel.orderCount': '提现所需订单数'"
    )
    public AjaxResult getInfo(@RequestAttribute("username") String username) {
        OrderMemberUser user = userService.findByUsername(username);
        if (user == null) {
            return AjaxResult.error(601, "User not found");
        }
        return success(user);
    }

    @PostMapping("/editPassword")
    @Operation(summary = "修改登录密码", description = "oldPassword:旧密码，newPassword:新密码")
    public AjaxResult editPassword(@RequestBody EditPasswordDto passwordDto, @RequestAttribute("username") String username){
        OrderMemberUser user = userService.findByUsername(username);
        if (StringUtils.isEmpty(passwordDto.getOldPassword()) || StringUtils.isEmpty(passwordDto.getNewPassword())){
            return AjaxResult.error(611, "Please enter password");
        }
        boolean matches = passwordEncoder.matches(passwordDto.getOldPassword(), user.getPassword());
        if (!matches){
            return AjaxResult.error(602, "Wrong password");
        }
        String encode = passwordEncoder.encode(passwordDto.getNewPassword());
        user.setPassword(encode);
        return toAjax(userService.updateOrderMemberUser(user));
    }

    @PostMapping("/editTradePassword")
    @Operation(summary = "修改交易密码", description = "oldTradePassword:旧密码，newTradePassword:新密码")
    public AjaxResult editTradePassword(@RequestBody EditTradePasswordDto passwordDto,
                                        @RequestAttribute("username") String username) {
        OrderMemberUser user = userService.findByUsername(username);
        String oldPwd = passwordDto.getOldTradePassword();
        String newPwd = passwordDto.getNewTradePassword();

        // 空值校验
        if (StringUtils.isEmpty(oldPwd) || StringUtils.isEmpty(newPwd)) {
            return AjaxResult.error(611, "Please enter password");
        }

        // 长度校验：6-18位
        if (newPwd.length() < 6 || newPwd.length() > 18) {
            return AjaxResult.error(612, "Trade password length must be between 6 and 18 characters");
        }

        // 旧密码校验
        boolean matches = passwordEncoder.matches(oldPwd, user.getTradePassword());
        if (!matches) {
            return AjaxResult.error(613, "Wrong trade password");
        }

        // 更新交易密码
        String encode = passwordEncoder.encode(newPwd);
        user.setTradePassword(encode);
        return toAjax(userService.updateOrderMemberUser(user));
    }

    @PostMapping("/checkTradePassword")
    @Operation(summary = "验证交易密码")
    public AjaxResult checkTradePassword(@RequestBody CheckTradePassword checkTradePassword,
                                         @RequestAttribute("username") String username){
        OrderMemberUser user = userService.findByUsername(username);
        boolean matches = passwordEncoder.matches(checkTradePassword.getTradePassword(), user.getTradePassword());
        if (!matches){
            return AjaxResult.error(613, "Wrong trade password");
        }
        return success();
    }

    @PostMapping("/addWithdrawalMethod")
    @Operation(summary = "添加/修改提现方式", description = "withdrawName:钱包，withdrawAddress：地址 ，withdrawType:网络")
    public AjaxResult addWithdrawalMethod(@RequestBody WithdrawalMethodDto methodDto,
                                          @RequestAttribute("username") String username){
        OrderMemberUser user = userService.findByUsername(username);
        if (StringUtils.isNull(user)){
            return AjaxResult.error(601, "User not found");
        }
        user.setWithdrawAddress(methodDto.getWithdrawAddress());
        user.setWithdrawName(methodDto.getWithdrawName());
        user.setWithdrawType(methodDto.getWithdrawType());
        return success(userService.updateOrderMemberUser(user));
    }

    @PostMapping("/updateAvatar")
    @Operation(summary = "添加/修改用户头像", description = "avatar:用户头像地址")
    public AjaxResult updateAvatar(@RequestBody AvatarDto dto, @RequestAttribute("username") String username){
        OrderMemberUser user = userService.findByUsername(username);
        if (StringUtils.isNull(user) || StringUtils.isEmpty(dto.getAvatar())){
            return AjaxResult.error(614, "User not found or avatar is blank");
        }
        user.setAvatar(dto.getAvatar());
        return success(userService.updateOrderMemberUser(user));
    }

    public OrderMemberUser setUser(RegisterDto registerDto){
        OrderMemberUser user = new OrderMemberUser();
        user.setUsername(registerDto.getUsername().trim());
        user.setPassword(registerDto.getPassword());
        user.setTradePassword(registerDto.getTradePassword());
        user.setPhone(registerDto.getPhone());
        user.setSex(registerDto.getSex());
        user.setInviteCode(registerDto.getInviteCode());
        user.setEmail(registerDto.getEmail());
        return user;
    }
}
