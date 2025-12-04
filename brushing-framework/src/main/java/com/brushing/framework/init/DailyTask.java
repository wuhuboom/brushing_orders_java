package com.brushing.framework.init;

import com.brushing.common.utils.StringUtils;
import com.brushing.member.domain.OrderLotteryPrize;
import com.brushing.member.domain.OrderMemberLevel;
import com.brushing.member.domain.OrderMemberUser;
import com.brushing.member.mapper.OrderLotteryPrizeMapper;
import com.brushing.member.mapper.OrderMemberLevelMapper;
import com.brushing.member.mapper.OrderMemberUserMapper;
import com.brushing.member.service.IOrderMemberUserService;
import com.brushing.set.domain.OrderSiteConfig;
import com.brushing.set.mapper.OrderSiteConfigMapper;
import com.brushing.system.domain.SysTimeZone;
import com.brushing.system.mapper.SysTimeZoneMapper;
import jakarta.annotation.PostConstruct;
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


    @Autowired
    public OrderSiteConfigMapper siteConfigMapper;

    @Autowired
    private TimezoneConfig timezoneConfig;


    @Scheduled(cron = "0 0 0 * * ?",zone = "#{@timezoneConfig.getDynamicTimezone()}")
    @Transactional
    public void runAtMidnight() {
        memberUserMapper.resetTodayCounts();
    }

    @Scheduled(cron = "0 0 0 * * ?",zone = "#{@timezoneConfig.getDynamicTimezone()}")
    public void autoLevel() {
        OrderLotteryPrize orderLotteryPrize = new OrderLotteryPrize();
        orderLotteryPrize.setConfigId(1L);
        List<OrderLotteryPrize> prizes = prizeMapper.selectOrderLotteryPrizeList(orderLotteryPrize);
        for (OrderLotteryPrize prize:prizes){
            prize.setRemainCount(prize.getTotalCount());
            prizeMapper.updateOrderLotteryPrize(prize);
        }
    }

    @Transactional
    @Scheduled(cron = "0 0 0 * * ?",zone = "#{@timezoneConfig.getDynamicTimezone()}")
    public void restOrderCount() {
        logger.info("当前时区："+timezoneConfig.getDynamicTimezone());
        OrderSiteConfig orderSiteConfig = siteConfigMapper.selectOrderSiteConfigById(1L);
        if (orderSiteConfig.getResetOrderCount().equals("1")){
            return;
        }
        List<OrderMemberUser> orderMemberUsers = memberUserMapper.selectAllUser();
        for (OrderMemberUser orderMemberUser:orderMemberUsers){
            Integer dealCount = orderMemberUser.getDealCount();
            if (StringUtils.isNull(dealCount)){
                dealCount = 0;
            }
            if (StringUtils.isNull(orderMemberUser.getTotalResetCount())){
                orderMemberUser.setTotalResetCount(0);
            }
            if (StringUtils.isNull(orderMemberUser.getTodayResetCount())){
                orderMemberUser.setTodayResetCount(0);
            }
            Integer orderCount =levelMapper.selectOrderMemberLevelById(orderMemberUser.getLevelId()).getOrderCount();
            if (dealCount >= orderCount) {
                orderMemberUser.setDealCount(0);
                orderMemberUser.setTotalResetCount(orderMemberUser.getTotalResetCount()+1);
                orderMemberUser.setTodayResetCount(orderMemberUser.getTodayResetCount()+1);
                memberUserMapper.updateOrderMemberUser(orderMemberUser);
            }
        }

    }

}
