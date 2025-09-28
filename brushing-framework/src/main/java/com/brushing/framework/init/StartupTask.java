package com.brushing.framework.init;

import com.brushing.common.core.domain.entity.SysUser;
import com.brushing.common.core.redis.RedisCache;
import com.brushing.common.utils.SecurityUtils;
import com.brushing.common.utils.StringUtils;
import com.brushing.set.domain.OrderTradeControlConfig;
import com.brushing.set.service.IOrderTradeControlConfigService;
import com.brushing.system.mapper.SysUserMapper;
import com.brushing.system.service.ISysUserService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class StartupTask implements ApplicationRunner{

    @Autowired
    private IOrderTradeControlConfigService controlConfigService;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private DatabaseInitializer databaseInitializer;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        //初始话配置
        OrderTradeControlConfig orderTradeControlConfig = controlConfigService.selectOrderTradeControlConfigById(1L);
        redisCache.setCacheObject("trade_config",orderTradeControlConfig);
        //查询每次需要新增的字段 是否存在 如果不存在则需要新增
        databaseInitializer.init();
        //查询admin 账号是否存在 如果不存在则需要新增
        SysUser sysUser = userService.selectUserByUserName("admin");
        if (StringUtils.isNull(sysUser)){
            addAdmin();
        }

    }

    public void addAdmin(){
        SysUser sysUser =new SysUser();
        sysUser.setUserId(1L);
        sysUser.setUserName("admin");
        sysUser.setNickName("admin");
        sysUser.setEmail("123qq.com");
        sysUser.setPhonenumber("13412341234");
        sysUser.setSex("1");
        sysUser.setStatus("0");
        sysUser.setDelFlag("0");
        sysUser.setTotpEnabled("1");
//        Long[] roleIds = {1L};
//        sysUser.setRoleIds(roleIds);
        sysUser.setPassword(SecurityUtils.encryptPassword("qq123123"));
        userService.insertUser(sysUser);
    }
}
