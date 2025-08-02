package com.brushing.api.controller;

import com.brushing.common.core.controller.BaseController;
import com.brushing.common.core.domain.AjaxResult;
import com.brushing.common.utils.OrderNoGenerator;
import com.brushing.common.utils.StringUtils;
import com.brushing.member.domain.OrderGoods;
import com.brushing.member.domain.OrderInfo;
import com.brushing.member.domain.OrderMemberLevel;
import com.brushing.member.domain.OrderMemberUser;
import com.brushing.member.service.IOrderGoodsService;
import com.brushing.member.service.IOrderInfoService;
import com.brushing.member.service.IOrderMemberLevelService;
import com.brushing.member.service.IOrderMemberUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

@Tag(name = "订单管理")
@RestController
@RequestMapping("/api/order")
public class OrderController extends BaseController {

    @Autowired
    private IOrderMemberUserService userService;

    @Autowired
    private IOrderGoodsService orderGoodsService;

    @Autowired
    private IOrderMemberLevelService memberLevelService;

    @Autowired
    private IOrderInfoService orderInfoService;

    @GetMapping("/createOrder")
    public AjaxResult createOrder(@RequestAttribute("username") String username){
        OrderMemberUser user = userService.findByUsername(username);
        BigDecimal balance = user.getBalance();
        OrderGoods orderGoods = orderGoodsService.selectNearestPriceGoods(balance);
        if (StringUtils.isNull(orderGoods)){
            return error("No suitable products or Insufficient balance");
        }

        OrderMemberLevel orderMemberLevel = memberLevelService.selectOrderMemberLevelById(user.getLevelId());
        if (StringUtils.isNull(orderMemberLevel)){
            return error("System error");
        }
        //佣金比列
        BigDecimal commissionRatio = orderMemberLevel.getCommissionRatio().divide(new BigDecimal("100"),2, RoundingMode.HALF_UP);
        BigDecimal price = orderGoods.getPrice();
        BigDecimal multiply = price.multiply(commissionRatio);//佣金
        //生成订单
        OrderInfo orderInfo= new OrderInfo();
        String orderNum = OrderNoGenerator.generateOrderNo();
        orderInfo.setOrderNo(orderNum);
        orderInfo.setUserId(user.getId());
        orderInfo.setProductId(orderGoods.getId());
        orderInfo.setCommission(multiply);
        orderInfo.setCommissionRate(orderMemberLevel.getCommissionRatio());
        orderInfo.setOrderTime(new Date());
        orderInfo.setCreateTime(new Date());
        orderInfoService.insertOrderInfo(orderInfo);
        //生成账变信息


        return AjaxResult.success(user);
    }


}
