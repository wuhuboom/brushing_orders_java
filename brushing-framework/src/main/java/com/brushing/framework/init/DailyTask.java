package com.brushing.framework.init;

import com.brushing.member.domain.OrderMemberLevel;
import com.brushing.member.domain.OrderMemberUser;
import com.brushing.member.mapper.OrderMemberLevelMapper;
import com.brushing.member.mapper.OrderMemberUserMapper;
import com.brushing.member.service.IOrderMemberUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

@Component
public class DailyTask {

    private static final Logger logger = LoggerFactory.getLogger(DailyTask.class);

    @Autowired
    private OrderMemberUserMapper memberUserMapper;

    @Autowired
    private OrderMemberLevelMapper levelMapper;

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void runAtMidnight() {
        memberUserMapper.resetTodayCounts();
    }

    @Scheduled(cron = "0 0 0 * * ?") // 每分钟执行
    public void autoLevel() {
        List<OrderMemberUser> orderMemberUsers = memberUserMapper.selectAllUser();
        int updateCount = 0;
        for (OrderMemberUser user : orderMemberUsers) {
            try {
                // 获取用户当前的余额对应的等级
                OrderMemberLevel levelByBalance = levelMapper.findLevelByBalance(user.getBalance());
                if (levelByBalance == null) {
                    logger.warn("未找到用户ID {}（余额 {}）对应的等级", user.getId(), user.getBalance());
                    continue;
                }

                // 比较查询到的等级与用户当前等级
                Long currentLevelId = user.getLevelId();
                Long newLevelId = levelByBalance.getId();
                if (currentLevelId != null && currentLevelId.equals(newLevelId)) {
                    logger.debug("用户ID {} 的等级无需更新（当前等级ID: {}）", user.getId(), currentLevelId);
                    continue; // 等级一致，跳过更新
                }

                // 更新用户等级
                OrderMemberUser newUser = new OrderMemberUser();
                newUser.setId(user.getId());
                newUser.setLevelId(newLevelId);
                memberUserMapper.updateOrderMemberUser(newUser);
                updateCount++;
                logger.debug("更新用户ID {} 的等级为ID {}（余额 {}）", user.getId(), newLevelId, user.getBalance());

            } catch (Exception e) {
                // 记录错误，继续处理下一个用户
                logger.error("处理用户ID {} 时失败: {}", user.getId(), e.getMessage(), e);
            }
        }
        logger.info("完成autoLevel任务，共处理 {} 个用户，更新 {} 个用户等级", orderMemberUsers.size(), updateCount);
    }

}
