package com.brushing.api.controller;

import com.brushing.api.dto.FrontLoginResponse;
import com.brushing.api.dto.LoginUserDto;
import com.brushing.common.core.controller.BaseController;
import com.brushing.common.core.domain.AjaxResult;
import com.brushing.common.utils.StringUtils;
import com.brushing.common.utils.ip.AddressUtils;
import com.brushing.common.utils.ip.IpUtils;
import com.brushing.framework.front.FrontJwtUtil;
import com.brushing.member.domain.OrderMemberUser;
import com.brushing.member.service.IOrderMemberUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/user")
public class AuthController extends BaseController {

    @Autowired
    private IOrderMemberUserService userService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();


    @PostMapping("/login")
    @Operation(summary = "登录操作")
    public AjaxResult login(@RequestBody LoginUserDto loginRequest){
        OrderMemberUser user = userService.findByUsername(loginRequest.getUsername());
        if (StringUtils.isNull(user)){
            return error("user not find");
        }
        boolean matches = passwordEncoder.matches(loginRequest.getPassword(), user.getPassword());
        if (!matches) return error("wrong password");

        String token = FrontJwtUtil.generateToken(user.getUsername());
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

    @PostMapping("/getInfo")
    @Operation(summary = "登录操作")
    public AjaxResult getInfo(){

        return success();
    }

}
