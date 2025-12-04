package com.order.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.api.controller.dto.*;
import com.order.common.core.controller.BaseController;
import com.order.common.core.domain.AjaxResult;
import com.order.common.utils.StringUtils;
import com.order.common.utils.ip.IpUtils;
import com.order.framework.front.FrontJwtUtil;
import com.order.framework.init.GeoIpQueryQueryService;
import com.order.member.domain.OrderUser;
import com.order.member.service.IOrderLoginLogService;
import com.order.member.service.IOrderUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "用户管理", description = "接口返回码说明: 200=成功; 601=账号或密码错误/用户未找到;" +
        " 603=用户名不能为空; 604=密码不能为空; 605=交易密码不能为空; 606=手机号不能为空; 607=性别不能为空; " +
        "608=邀请码不能为空; 609=用户名已存在; 610=邀请码不存在; 615=无效的Authorization头; " +
        "616=退出失败; 617=未知错误")
@RestController
@RequestMapping("/api/user")
public class AuthController extends BaseController {

    @Autowired
    private IOrderUserService orderUserService;

    @Autowired
    private FrontJwtUtil frontJwtUtil;

    @Autowired
    private GeoIpQueryQueryService queryService;

    @Autowired
    private IOrderLoginLogService loginLogService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private static final ObjectMapper objectMapper = new ObjectMapper(); // 静态实例，线程安全

    @PostMapping("/login")
    @Operation(summary = "登录操作")
    @Transactional
    public AjaxResult login(@RequestBody LoginUserDto loginRequest, HttpServletRequest request) {
        OrderUser user = orderUserService.selectOrderUserByName(loginRequest.getUsername());
        if (StringUtils.isNull(user)) {
            return AjaxResult.error(601, "The account or password is incorrect");
        }
        boolean matches = passwordEncoder.matches(loginRequest.getPassword(), user.getPassword());
        if (!matches) {
            return AjaxResult.error(601, "The account or password is incorrect");
        }
       try{
           String token = frontJwtUtil.generateToken(user.getUsername());
           String ipAddr = IpUtils.getIpAddr();
           String address = queryService.queryByIp(ipAddr);
           //记录登录日志
           Map<String, String> headers = new HashMap<>();
           request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
               String value = request.getHeader(headerName);
               if (!"authorization".equalsIgnoreCase(headerName) && !"cookie".equalsIgnoreCase(headerName)) {
                   headers.put(headerName, value);
               }
           });
           String headersJson = objectMapper.writeValueAsString(headers);
           loginLogService.insertOrderLoginLog(user.getId(), ipAddr, address,headersJson);
           AjaxResult result = new AjaxResult();
           result.put("token", token);
           result.put("user", user);
           return success(result);
       }catch (Exception e){
           return AjaxResult.error(617,"Unknown error");
       }
    }

    @PostMapping("/register")
    @Operation(
            summary = "注册操作",
            description = "username:用户名，password：密码，tradePassword：交易密码，phoneNumber:电话，gender: 性别 0 男，1 女 ，inviteCode邀请码"
    )
    public  AjaxResult register(@RequestBody RegisterDto registerDto){
        if (StringUtils.isEmpty(registerDto.getUsername())){
            return AjaxResult.error(603, "Username must not be blank");
        }
        String username = registerDto.getUsername().trim();
        if (StringUtils.isEmpty(registerDto.getPassword())){
            return AjaxResult.error(604, "Password must not be blank");
        }
        if (StringUtils.isEmpty(registerDto.getTradePassword())){
            return AjaxResult.error(605, "Trade password must not be blank");
        }
        if (StringUtils.isEmpty(registerDto.getPhoneNumber())){
            return AjaxResult.error(606, "Phone number must not be blank");
        }
        if (StringUtils.isEmpty(registerDto.getGender())){
            return AjaxResult.error(607, "Sex must not be blank");
        }
        if (StringUtils.isEmpty(registerDto.getInviteCode())){
            return AjaxResult.error(608, "Invite code must not be blank");
        }
        OrderUser user = orderUserService.selectOrderUserByName(username);
        if (StringUtils.isNotNull(user)){
            return AjaxResult.error(609, "Username already exists");
        }
        OrderUser orderUser = setUser(registerDto);
        orderUser.setPassword(encoder.encode(registerDto.getPassword()));
        orderUser.setTradePassword(encoder.encode(registerDto.getTradePassword()));
        String register = orderUserService.register(orderUser);
        if (register.equals("500")){
            return AjaxResult.error(610, "Invite code not found");
        }
        return success("register success");
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
            description = """
        ### 返回字段说明

        #### 基本用户信息
        - **username**: 用户名
        - **phoneNumber**: 手机号
        - **avatar**: 头像URL
        - **vipId**: VIP 等级ID
        - **parentId**: 上级用户ID
        - **parentUsername**: 上级用户名
        - **parentInviteCode**: 上级邀请码
        - **inviteCode**: 用户邀请码
        - **balance**: 余额
        - **frozenBalance**: 冻结余额
        - **baseSalary**: 底薪
        - **taskProgress**: 任务进度
        - **reputationScore**: 信誉分
        - **gender**: 性别
        - **email**: 邮箱
        - **birthday**: 生日
        - **isEnabled**: 是否启用
        - **allowInvite**: 是否允许邀请
        - **isFrozen**: 是否冻结
        - **isFake**: 是否假人
        - **isBanned**: 是否禁止工作
        - **workLimit**: 工作限额
        - **isWithdrawalNotification**: 关闭提现通知
        - **accountStatus**: 账户状态
        - **transactionStatus**: 交易状态
        - **withdrawalStatus**: 提现状态
        - **depositBlockWithdrawal**: 充值后禁止提现
        - **remarks**: 备注
        - **version**: 版本号
        - **createTime/updateTime**: 创建/更新时间
        - **assistWithdrawalStatus**: 协助金提现状态
        - **scope**: 权限或范围信息

        #### memberLevel（用户等级）详情
        - **id**: 等级ID
        - **name**: 名称
        - **level**: 等级数值
        - **icon**: 图标URL
        - **price**: 等级价格
        - **minBalance**: 最低余额
        - **inviteCount**: 自动升级所需邀请人数
        - **orderCountPerDay**: 接单次数/天
        - **minCommissionRate**: 最低返佣百分比
        - **maxCommissionRate**: 最高返佣百分比
        - **taskCountPerDay**: 任务完成组数/天
        - **withdrawCountPerDay**: 提现次数/天
        - **withdrawFeeRate**: 提现手续费率
        - **minWithdraw**: 最低提现金额
        - **maxWithdraw**: 最高提现金额
        - **description**: 描述
        """)
    public AjaxResult userInfo(@RequestAttribute("username") String username){
        OrderUser orderUser = orderUserService.selectOrderUserByName(username);
        if (orderUser == null) {
            return AjaxResult.error(601, "User not found");
        }
        return  success(orderUser);
    }


    @PostMapping("/updateAvatar")
    @Operation(summary = "添加/修改用户头像", description = "avatar:用户头像地址")
    public AjaxResult updateAvatar(@RequestBody AvatarDto dto, @RequestAttribute("username") String username){
        OrderUser orderUser = orderUserService.selectOrderUserByName(username);
        if (StringUtils.isNull(orderUser) || StringUtils.isEmpty(dto.getAvatar())){
            return AjaxResult.error(614, "User not found or avatar is blank");
        }
        orderUser.setAvatar(dto.getAvatar());
        return success(orderUserService.updateOrderUser(orderUser));
    }

    @PostMapping("/editPassword")
    @Operation(summary = "修改登录密码", description = "oldPassword:旧密码，newPassword:新密码")
    public AjaxResult editPassword(@RequestBody EditPasswordDto passwordDto, @RequestAttribute("username") String username){
        OrderUser user = orderUserService.selectOrderUserByName(username);
        if (StringUtils.isEmpty(passwordDto.getOldPassword()) || StringUtils.isEmpty(passwordDto.getNewPassword())){
            return AjaxResult.error(611, "Please enter password");
        }
        boolean matches = passwordEncoder.matches(passwordDto.getOldPassword(), user.getPassword());
        if (!matches){
            return AjaxResult.error(602, "Wrong password");
        }
        String encode = passwordEncoder.encode(passwordDto.getNewPassword());
        user.setPassword(encode);
        return toAjax(orderUserService.updateOrderUser(user));
    }

    @PostMapping("/editTradePassword")
    @Operation(summary = "修改交易密码", description = "oldTradePassword:旧密码，newTradePassword:新密码")
    public AjaxResult editTradePassword(@RequestBody EditTradePasswordDto passwordDto,
                                        @RequestAttribute("username") String username) {
        OrderUser user = orderUserService.selectOrderUserByName(username);
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
        return toAjax(orderUserService.updateOrderUser(user));
    }

    @PostMapping("/checkTradePassword")
    @Operation(summary = "验证交易密码")
    public AjaxResult checkTradePassword(@RequestBody CheckTradePassword checkTradePassword,
                                         @RequestAttribute("username") String username){
        OrderUser user = orderUserService.selectOrderUserByName(username);
        boolean matches = passwordEncoder.matches(checkTradePassword.getTradePassword(), user.getTradePassword());
        if (!matches){
            return AjaxResult.error(613, "Wrong trade password");
        }
        return success();
    }



    public OrderUser setUser(RegisterDto registerDto){
        OrderUser user = new OrderUser();
        user.setUsername(registerDto.getUsername().trim());
        user.setPhoneNumber(registerDto.getPhoneNumber());
        user.setGender(registerDto.getGender());
        user.setInviteCode(registerDto.getInviteCode());
        return user;
    }




}
