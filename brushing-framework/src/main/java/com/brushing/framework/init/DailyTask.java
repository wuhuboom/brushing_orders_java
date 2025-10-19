package com.brushing.framework.init;

import com.brushing.member.domain.OrderLotteryPrize;
import com.brushing.member.domain.OrderMemberLevel;
import com.brushing.member.domain.OrderMemberUser;
import com.brushing.member.mapper.OrderLotteryPrizeMapper;
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

    @Autowired
    private OrderLotteryPrizeMapper prizeMapper;

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void runAtMidnight() {
        memberUserMapper.resetTodayCounts();
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void autoLevel() {
        OrderLotteryPrize orderLotteryPrize = new OrderLotteryPrize();
        orderLotteryPrize.setConfigId(1L);
        List<OrderLotteryPrize> prizes = prizeMapper.selectOrderLotteryPrizeList(orderLotteryPrize);
        for (OrderLotteryPrize prize:prizes){
            prize.setRemainCount(prize.getTotalCount());
            prizeMapper.updateOrderLotteryPrize(prize);
        }


    }

}
