package com.brushing.api.controller;

import com.brushing.common.core.controller.BaseController;
import com.brushing.common.core.domain.AjaxResult;
import com.brushing.common.utils.StringUtils;
import com.brushing.common.utils.ip.IpUtils;
import com.brushing.member.domain.OrderLotteryConfig;
import com.brushing.member.domain.OrderLotteryPrize;
import com.brushing.member.domain.OrderLotteryRecord;
import com.brushing.member.domain.OrderMemberUser;
import com.brushing.member.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "抽奖管理",description = "错误码说明:801、804说明后台的抽奖配置有问题，802：未开启抽奖，803：无抽奖次数")
@RestController
@RequestMapping("/api/lottery")
public class LotteryController extends BaseController {

    @Autowired
    private IOrderMemberUserService userService;

    @Autowired
    private IOrderLotteryPrizeService prizeService;

    @Autowired
    private IOrderLotteryConfigService configService;

    @Autowired
    private IOrderLotteryRecordService recordService;

    @Autowired
    private IOrderAccountChangeService changeService;

    @GetMapping("/draw")
    @Transactional
    @Operation(summary = "抽奖",description = "code:200,data ：返回的金额就是中奖金额，如果是0 则未中奖")
    public AjaxResult draw(@RequestAttribute("username") String username){
        OrderMemberUser user = userService.findByUsername(username);
        if (StringUtils.isNull(user)){
            return AjaxResult.error(904, "The user does not exist");
        }
        OrderLotteryConfig orderLotteryConfig = configService.selectOrderLotteryConfigById(1L);
        if (StringUtils.isNull(orderLotteryConfig)){
            return AjaxResult.error(801, "No lottery configuration");
        }
        if (orderLotteryConfig.getStatus().equals("1")){
            return AjaxResult.error(802, "The lottery is not open");
        }
        int todayDraws = recordService.countTodayDraws(user.getId());
        if (todayDraws >= 1) {
            return AjaxResult.error(803, "No draws yet");
        }
        List<OrderLotteryPrize> prizes = prizeService.selectOrderLotteryPrizeList();
        if (prizes.isEmpty()) {
            return AjaxResult.error(804, "No lottery configuration");
        }

        Integer totalRemain = prizeService.getTotalRemainCount(1L);
        double random = Math.random();
        double cumulativeProb = 0.0;
        OrderLotteryPrize winPrize = null;
        for (OrderLotteryPrize prize : prizes) {
            double prob = (double) prize.getRemainCount() / totalRemain;
            cumulativeProb += prob;
            if (random <= cumulativeProb) {
                winPrize = prize;
                break;
            }
        }
        if (winPrize == null) {
            winPrize = prizes.stream()
                    .filter(p -> p.getAmount().compareTo(BigDecimal.ZERO) == 0)
                    .findFirst()
                    .orElse(prizes.get(prizes.size() - 1));
        }
        BigDecimal amount = winPrize.getAmount();
        Long prizeId = winPrize.getId();
        if (winPrize.getRemainCount() > 0) {
            int newRemain = winPrize.getRemainCount() - 1;
            winPrize.setRemainCount(newRemain);
            prizeService.updateOrderLotteryPrize(winPrize);
        }

        // 7. 记录抽奖日志（其余不变）
        OrderLotteryRecord record = new OrderLotteryRecord();
        record.setConfigId(1L);
        record.setUserId(user.getId());
        record.setPrizeId(prizeId);
        record.setAmount(amount);
        record.setIpAddress(IpUtils.getIpAddr());
        recordService.insertOrderLotteryRecord(record);
        if (amount.compareTo(BigDecimal.ZERO) == 0){
            return  success(amount);
        }
        //更新用户余额
        BigDecimal add = user.getBalance().add(amount);
        OrderMemberUser upuser =new OrderMemberUser();
        upuser.setId(user.getId());
        upuser.setBalance(add);
        userService.updateOrderMemberUser(upuser);
        //记录账变信息
        changeService.recordAccountChange(user.getId(),username,"9",user.getBalance(),amount,add,"用户ID:"+ user.getId()+", 用户名: "+username +",抽奖，中奖金额为："+amount);
        return success(amount);
    }

    @GetMapping("/getLotteryConfig")
    @Operation(summary = "获取抽奖配置",description = "prizes: 抽奖奖项配置，symbol：货币符号")
    public AjaxResult getLotteryConfig(){
        OrderLotteryConfig orderLotteryConfig = configService.selectOrderLotteryConfigById(1L);
        if (StringUtils.isNull(orderLotteryConfig)){
            return AjaxResult.error(801, "No lottery configuration");
        }
        if (orderLotteryConfig.getStatus().equals("1")){
            return AjaxResult.error(802, "The lottery is not open");
        }
        List<OrderLotteryPrize> prizes = prizeService.selectOrderLotteryPrizeList();
        if (prizes.isEmpty()) {
            return AjaxResult.error(804, "No lottery configuration");
        }
        List<BigDecimal> data = prizes.stream()
                .map(OrderLotteryPrize::getAmount) // 提取 amount 字段
                .collect(Collectors.toList());
        AjaxResult ajaxResult=new AjaxResult();
        ajaxResult.put("code",200);
        ajaxResult.put("prizes",data);
        ajaxResult.put("symbol",orderLotteryConfig.getActivityName());
        return ajaxResult;
    }

    @GetMapping("/getUserDraws")
    @Operation(summary = "用户是否有抽奖次数",description = "code:200 则有抽奖次数，code:803 则无抽奖次数")
    public AjaxResult getUserDraws(@RequestAttribute("username") String username){
        OrderMemberUser user = userService.findByUsername(username);
        if (StringUtils.isNull(user)){
            return AjaxResult.error(904, "The user does not exist");
        }
        int todayDraws = recordService.countTodayDraws(user.getId());
        if (todayDraws >= 1) {
            return AjaxResult.error(803, "No draws yet");
        }
        return success();
    }

}
