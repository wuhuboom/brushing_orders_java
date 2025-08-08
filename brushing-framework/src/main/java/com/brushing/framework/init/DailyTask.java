package com.brushing.framework.init;

import com.brushing.member.mapper.OrderMemberUserMapper;
import com.brushing.member.service.IOrderMemberUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DailyTask {

    @Autowired
    private OrderMemberUserMapper memberUserMapper;

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void runAtMidnight() {
        memberUserMapper.resetTodayCounts();
    }
}
