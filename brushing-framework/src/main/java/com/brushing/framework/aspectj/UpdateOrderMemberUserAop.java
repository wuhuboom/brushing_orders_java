package com.brushing.framework.aspectj;

import com.brushing.common.utils.StringUtils;
import com.brushing.set.domain.OrderSiteConfig;
import com.brushing.set.mapper.OrderSiteConfigMapper;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.brushing.member.domain.OrderMemberUser;
import com.brushing.member.domain.OrderMemberLevel;  // 假设这是等级实体类
import com.brushing.member.mapper.OrderMemberUserMapper;  // 当前 Mapper
import com.brushing.member.mapper.OrderMemberLevelMapper;  // 等级 Mapper（需自行定义）

import java.math.BigDecimal;

@Aspect
@Component
public class UpdateOrderMemberUserAop {

    @Autowired
    private OrderMemberLevelMapper levelMapper;  // 注入等级 Mapper

    @Autowired
    private OrderMemberUserMapper userMapper;  // 注入当前 Mapper，用于调用 updateUserLevel

    @Autowired
    private OrderSiteConfigMapper siteConfigMapper;

    @Around("execution(* com.brushing.member.mapper.OrderMemberUserMapper.updateOrderMemberUser(..))")
    public Object aroundUpdateOrderMemberUser(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法参数：假设第一个参数是 OrderMemberUser user
        Object[] args = joinPoint.getArgs();
        if (args.length == 0 || !(args[0] instanceof OrderMemberUser)) {
            // 参数异常，直接执行原方法
            return joinPoint.proceed();
        }

        OrderMemberUser user = (OrderMemberUser) args[0];

        // 执行前逻辑：获取 amount = 用户余额
        BigDecimal amount = user.getBalance();  // 假设 getBalance() 返回 Double 类型余额
        if (amount == null) {
            // 余额为空，直接执行原方法或返回
            return joinPoint.proceed();
        }

        OrderSiteConfig orderSiteConfig = siteConfigMapper.selectOrderSiteConfigById(1L);
        if (StringUtils.isNull(orderSiteConfig)){
            return joinPoint.proceed();
        }

        if (orderSiteConfig.getLevelStatus().equals("0")){
            // 查询对应等级
            OrderMemberLevel levelByBalance = levelMapper.findLevelByBalance(amount);
            if (levelByBalance == null) {
                // 如果查询不到等级，直接返回（跳过原方法执行）
                //     System.out.println("未查询到对应等级，跳过更新。用户ID: " + user.getId());
                return 0;  // 或抛出异常/返回自定义结果，根据业务调整
            }

            // 比较查询到的等级与用户当前等级
            Long currentLevelId = user.getLevelId();
            Long newLevelId = levelByBalance.getId();
            if (currentLevelId == null || !currentLevelId.equals(newLevelId)) {
                // 等级不同，更新用户等级
                //  int updateResult = userMapper.updateUserLevel(user.getId(), newLevelId);
                //  System.out.println("用户等级更新成功，新等级ID: " + newLevelId + "，更新结果: " + updateResult);
                // 可选：更新 user 对象中的 levelId，供后续原方法使用
                user.setLevelId(newLevelId);
            } else {
                // System.out.println("用户等级无变化，当前等级ID: " + currentLevelId);
            }
        }
        // 执行原方法（updateOrderMemberUser）
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();  // 传入修改后的 args（如果修改了 user）
        long end = System.currentTimeMillis();
      //  System.out.println("updateOrderMemberUser 执行结束，结果: " + result + "，耗时: " + (end - start) + "ms");

        return result;
    }
}
