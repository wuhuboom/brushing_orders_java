package com.brushing.api.controller;

import com.brushing.api.controller.vo.CheckTradePassword;
import com.brushing.api.controller.vo.EditPasswordDto;
import com.brushing.api.controller.vo.EditTradePasswordDto;
import com.brushing.api.controller.vo.WithdrawalMethodDto;
import com.brushing.api.dto.FrontLoginResponse;
import com.brushing.api.dto.LoginUserDto;
import com.brushing.api.dto.RegisterDto;
import com.brushing.common.core.controller.BaseController;
import com.brushing.common.core.domain.AjaxResult;
import com.brushing.common.utils.StringUtils;
import com.brushing.common.utils.bean.BeanUtils;
import com.brushing.common.utils.ip.AddressUtils;
import com.brushing.common.utils.ip.IpUtils;
import com.brushing.framework.front.FrontJwtUtil;
import com.brushing.member.domain.OrderMemberUser;
import com.brushing.member.service.IOrderMemberUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/user")
public class AuthController extends BaseController {

    @Autowired
    private IOrderMemberUserService userService;

    @Autowired
    private FrontJwtUtil frontJwtUtil;


    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();


    @PostMapping("/login")
    @Operation(summary = "登录操作")
    public AjaxResult login(@RequestBody LoginUserDto loginRequest){
        OrderMemberUser user = userService.findByUsername(loginRequest.getUsername());
        if (StringUtils.isNull(user)){
            return error("user not find");
        }
        boolean matches = passwordEncoder.matches(loginRequest.getPassword(), user.getPassword());
        if (!matches) return error("wrong password");

        String token = frontJwtUtil.generateToken(user.getUsername());
        String ipAddr = IpUtils.getIpAddr();
        String address = AddressUtils.getRealAddressByIP(ipAddr);
        user.setLastLoginTime(new Date());
        user.setRegisterIp(ipAddr+","+address);
        userService.updateOrderMemberUser(user);

        AjaxResult result= new AjaxResult();
        result.put("token",token);
        result.put("user",user);
        return success(result);
    }

    @PostMapping("/register")
    @Operation(summary = "注册操作" ,
            description = "username:用户名，password：密码，tradePassword：交易密码，phone:电话，sex: 性别 0 男，1 女 ，inviteCode邀请码"
    )
    public AjaxResult register(@RequestBody RegisterDto registerDto){
        if (StringUtils.isEmpty(registerDto.getUsername())){
            return error("Username must not be blank");
        }
        if (StringUtils.isEmpty(registerDto.getPassword())){
            return error("Password must not be blank");
        }
        if (StringUtils.isEmpty(registerDto.getTradePassword())){
            return error("Trade password must not be blank");
        }
        if (StringUtils.isEmpty(registerDto.getPhone())){
            return error("Phone number must not be blank");
        }
        if (StringUtils.isEmpty(registerDto.getSex())){
            return error("Sex must not be blank");
        }
        if (StringUtils.isEmpty(registerDto.getInviteCode())){
            return error("Invite code must not be blank");
        }
        OrderMemberUser user = userService.findByUsername(registerDto.getUsername());
        if (StringUtils.isNotNull(user)){
            return error("username already exists");
        }
        OrderMemberUser orderMemberUser = setUser(registerDto);
        orderMemberUser.setPassword(encoder.encode(registerDto.getPassword()));
        orderMemberUser.setTradePassword(encoder.encode(registerDto.getTradePassword()));
        String register = userService.register(orderMemberUser);
        if (register.equals("200")){
            return success("register success");
        }else{
            return error("Invite code not find");
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public AjaxResult logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.isEmpty(authHeader) || !authHeader.startsWith("Bearer ")) {
            return AjaxResult.error(HttpStatus.BAD_REQUEST.value(), "Invalid authorization header");
        }

        String token = authHeader.substring("Bearer ".length());
        try {
            String username = frontJwtUtil.getUsernameFromToken(token);
            frontJwtUtil.invalidateToken(token, username);
            return success("Logout successful");
        } catch (Exception e) {
            return AjaxResult.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Logout failed: " + e.getMessage());
        }
    }

    @GetMapping("/getInfo")
    @Operation(summary = "获取用户信息",
            description =
                    "'username': '用户名', " +
                            "'phone': '手机号', " +
                            "'password': '登录密码', " +
                            "'tradePassword': '交易密码', " +
                            "'parentId': '上级用户ID', " +
                            "'email': '邮箱', " +
                            "'creditScore': '信誉分', " +
                            "'balance': '可用余额', " +
                            "'frozenBalance': '冻结余额', " +
                            "'totalBalance': '总余额', " +
                            "'inviteCode': '邀请码', " +
                            "'registerIp': '最近登录', " +
                            "'lastLoginTime': '登录时间', " +
                            "'accountStatus': '账户状态', " +
                            "'tradeStatus': '交易状态', " +
                            "'withdrawStatus': '提现状态', " +
                            "'realNameStatus': '实名状态', " +
                            "'realName': '真实姓名', " +
                            "'idCardNumber': '身份证号', " +
                            "'idCardFront': '身份证正面', " +
                            "'idCardBack': '身份证反面', " +
                            "'withdrawName': '提现姓名', " +
                            "'withdrawAddress': '提现地址', " +
                            "'withdrawType': '提现类型', " +
                            "'isReal': '是否假人', " +
                            "'sex': '性别', " +
                            "'levelId': '会员等级ID', " +
                            "'commission': '当日佣金', " +
                            "'allCommission': '累计佣金', " +
                            "'dealCount': '单数',  " +
                            "'directSubCount': '直属下级人数', " +
                            "'allSubCount': '所有下级人数', " +
                            "'todayWithdrawCount': '今日提现次数', " +
                            "'totalWithdrawCount': '历史提现次数', " +
                            "'todayResetCount': '今日重置次数', " +
                            "'totalResetCount': '总重置次数', " +
                            "'withdrawTip': '提现提示', " +
                            "'userLevel.icon': '会员图标', " +
                            "'userLevel.nameZh': '中文名称', " +
                            "'userLevel.nameEn': '英文名称' " +
                            "'userLevel.orderCount': '提现所需订单数'"
    )
    public AjaxResult getInfo(@RequestAttribute("username") String username) {
        OrderMemberUser user = userService.findByUsername(username);
        if (user == null) {
            return error("User not found");
        }
        return success(user);
    }


    @PostMapping("/editPassword")
    @Operation(summary = "修改登录密码" ,description = "oldPassword:旧密码，newPassword:新密码")
    public AjaxResult editPassword(@RequestBody EditPasswordDto passwordDto,@RequestAttribute("username") String username){
        OrderMemberUser user = userService.findByUsername(username);
        if (StringUtils.isEmpty(passwordDto.getOldPassword())||StringUtils.isEmpty(passwordDto.getNewPassword())){
            return error("Please enter password");
        }
        boolean matches = passwordEncoder.matches(passwordDto.getOldPassword(), user.getPassword());
        if (!matches){
            error("wrong password");
        }
        String encode = passwordEncoder.encode(passwordDto.getNewPassword());
        user.setPassword(encode);
        return toAjax(userService.updateOrderMemberUser(user));
    }

    @PostMapping("/editTradePassword")
    @Operation(summary = "修改交易密码" ,description = "oldTradePassword:旧密码，newTradePassword:新密码")
    public AjaxResult editTradePassword(@RequestBody EditTradePasswordDto passwordDto, @RequestAttribute("username") String username){
        OrderMemberUser user = userService.findByUsername(username);
        if (StringUtils.isEmpty(passwordDto.getOldTradePassword())||StringUtils.isEmpty(passwordDto.getNewTradePassword())){
            return error("Please enter password");
        }
        boolean matches = passwordEncoder.matches(passwordDto.getOldTradePassword(), user.getTradePassword());
        if (!matches){
           return error("wrong trade password");
        }
        String encode = passwordEncoder.encode(passwordDto.getNewTradePassword());
        user.setPassword(encode);
        return toAjax(userService.updateOrderMemberUser(user));
    }

    @PostMapping("/checkTradePassword")
    @Operation(summary = "验证交易密码")
    public AjaxResult checkTradePassword(@RequestBody CheckTradePassword checkTradePassword,@RequestAttribute("username") String username){
        OrderMemberUser user = userService.findByUsername(username);
        boolean matches = passwordEncoder.matches(checkTradePassword.getTradePassword(), user.getTradePassword());
        if (!matches){
            return error("wrong trade password");
        }
        return success();
    }

    @PostMapping("/addWithdrawalMethod")
    @Operation(summary = "添加/修改提现方式" ,description = "withdrawName:钱包，withdrawAddress：地址 ，withdrawType:网络")
    public AjaxResult addWithdrawalMethod(@RequestBody WithdrawalMethodDto methodDto, @RequestAttribute("username") String username){
        OrderMemberUser user = userService.findByUsername(username);
        if (StringUtils.isNull(user)){
            return error("User not found");
        }
        user.setWithdrawAddress(methodDto.getWithdrawAddress());
        user.setWithdrawName(methodDto.getWithdrawName());
        user.setWithdrawType(methodDto.getWithdrawType());
        return success(userService.updateOrderMemberUser(user));
    }




    public OrderMemberUser setUser(RegisterDto registerDto){
        OrderMemberUser user = new OrderMemberUser();
        user.setUsername(registerDto.getUsername());
        user.setPassword(registerDto.getPassword());
        user.setTradePassword(registerDto.getTradePassword());
        user.setPhone(registerDto.getPhone());
        user.setSex(registerDto.getSex());
        user.setInviteCode(registerDto.getInviteCode());
        return user;
    }

}
