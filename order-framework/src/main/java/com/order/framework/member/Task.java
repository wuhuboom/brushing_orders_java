package com.order.framework.member;


import com.order.member.mapper.OrderUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

//定时任务
@Configuration
public class Task {

    @Autowired
    private OrderUserMapper userMapper;

    @Scheduled(cron = "0 0 0 * * ?")
    public void resetTodayRestBatch() {
        userMapper.resetTodayRestBatch();
    }

}
