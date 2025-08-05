package com.brushing.api.controller;

import com.brushing.api.controller.vo.OrderVo;
import com.brushing.common.core.controller.BaseController;
import com.brushing.common.core.domain.AjaxResult;
import com.brushing.common.utils.DateUtils;
import com.brushing.common.utils.OrderNoGenerator;
import com.brushing.common.utils.StringUtils;
import com.brushing.member.domain.*;
import com.brushing.member.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单状态枚举
 */
enum OrderStatus {
    COMPLETED("0", "已完成"),
    PENDING_SUBMIT("2", "待提交"),
    FROZEN("1", "冻结");

    private final String code;
    private final String description;

    OrderStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }
}

/**
 * 佣金状态枚举
 */
enum CommissionStatus {
    PENDING("1", "待发放"),
    ISSUED("0", "已发放");

    private final String code;
    private final String description;

    CommissionStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }
}

/**
 * 账变类型枚举
 */
enum ChangeType {
    ORDER("1", "订单下单"),
    COMMISSION("5", "佣金返还"),
    REFUND("7", "本金返还");

    private final String code;
    private final String description;

    ChangeType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }
}

@Tag(name = "订单管理")
@RestController
@RequestMapping("/api/order")
public class OrderController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private IOrderMemberUserService userService;

    @Autowired
    private IOrderGoodsService orderGoodsService;

    @Autowired
    private IOrderMemberLevelService memberLevelService;

    @Autowired
    private IOrderInfoService orderInfoService;

    @Autowired
    private IOrderAccountChangeService accountChangeService;

    @Autowired
    private IOrderSeriesService seriesService;

    private static final int MAX_ATTEMPTS = 5;

    /**
     * 为认证用户创建新订单
     */
    @Transactional
    @GetMapping("/createOrder")
    @Operation(summary = "创建新订单")
    public AjaxResult createOrder(@RequestAttribute("username") String username) {
        if (StringUtils.isEmpty(username)) {
            return AjaxResult.error(1001, "用户名不能为空");
        }

        OrderMemberUser user = userService.findByUsername(username);
        if (user == null) {
            return AjaxResult.error(1002, "用户不存在");
        }

        if (orderInfoService.countUnfinishedOrders(user.getId()) > 0) {
            return AjaxResult.error(1003, "存在未完成订单");
        }

        int currentNum = user.getDealCount() + 1;
        List<OrderSeries> orderSeries = seriesService.selectSeriesListByUserId(user.getId());
        OrderSeries series = findSeriesByOrderIndex(orderSeries, currentNum);

        OrderInfo order = new OrderInfo();
        String orderNo = generateUniqueOrderNo(user.getId());
        if (orderNo == null) {
            return AjaxResult.error(1004, "订单号生成失败，请稍后重试");
        }

        OrderVo response;
        BigDecimal price;
        BigDecimal commission;
        Long productId;

        try {
            if (!orderSeries.isEmpty() && series != null) {
                // 连单逻辑
                if (series.getCommissionRatio() == null ||  series.getCommissionRatio().compareTo(0) <= 0) {
                    return AjaxResult.error(1005, "无效的连单佣金配置");
                }
                productId = series.getProductId();
                price = series.getPrice();
                BigDecimal commissionRatio = new BigDecimal(series.getCommissionRatio())
                        .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
                commission = price.multiply(commissionRatio);
                OrderGoods orderGoods = orderGoodsService.selectOrderGoodsById(productId);
                if (orderGoods == null) {
                    return AjaxResult.error(1006, "商品不存在");
                }
                order.setOrderType("1");
                order.setSeriesId(series.getId());
                order.setStatus(OrderStatus.PENDING_SUBMIT.getCode()); // 连单订单初始为待提交
                order.setQuantity(currentNum);
                setupOrderInfo(order, orderNo, user.getId(), productId, price, commission);
                orderInfoService.insertOrderInfo(order); // Insert order before creating response
                response = createOrderResponse(order, productId, price, commission, orderGoods.getName(), orderGoods.getCoverUrl(), DateUtils.getNowDate());
            } else {
                // 非连单逻辑
                OrderGoods orderGoods = orderGoodsService.selectNearestPriceGoods(user.getBalance());
                if (orderGoods == null) {
                    return AjaxResult.error(1007, "没有合适的产品或余额不足");
                }
                OrderMemberLevel level = memberLevelService.selectOrderMemberLevelById(user.getLevelId());
                if (level == null || level.getCommissionRatio() == null) {
                    return AjaxResult.error(1008, "无效的会员等级配置");
                }
                price = orderGoods.getPrice();
                BigDecimal commissionRatio = level.getCommissionRatio().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                commission = price.multiply(commissionRatio);
                productId = orderGoods.getId();
                order.setCommissionRate(level.getCommissionRatio());
                order.setQuantity(currentNum);
                order.setStatus(OrderStatus.PENDING_SUBMIT.getCode()); // 非连单订单初始为待提交
                setupOrderInfo(order, orderNo, user.getId(), productId, price, commission);
                orderInfoService.insertOrderInfo(order); // Insert order before creating response
                response = createOrderResponse(order, productId, price, commission, orderGoods.getName(), orderGoods.getCoverUrl(), DateUtils.getNowDate());
            }

            AjaxResult balanceResult = updateBalanceAndRecordChange(
                    user, price, ChangeType.ORDER.getCode(), "订单下单", currentNum
            );
            if (!balanceResult.isSuccess()) {
                throw new RuntimeException("更新用户余额失败: ");
            }

            return AjaxResult.success(response);
        } catch (Exception e) {
            logger.error("创建订单失败，用户ID: {}, 错误: {}", user.getId(), e.getMessage());
            throw new RuntimeException("创建订单失败，请稍后重试");
        }
    }

    /**
     * 提交现有订单并处理佣金和余额更新
     */
    @Transactional
    @GetMapping("/submitOrder/{id}")
    @Operation(summary = "提交订单")
    public AjaxResult submitOrder(@PathVariable("id") Long id, @RequestAttribute("username") String username) {
        if (id == null || id <= 0) {
            return AjaxResult.error(2001, "无效的订单ID");
        }
        if (StringUtils.isEmpty(username)) {
            return AjaxResult.error(2002, "用户名不能为空");
        }

        OrderInfo orderInfo = orderInfoService.selectOrderInfoById(id);
        if (orderInfo == null) {
            return AjaxResult.error(2003, "订单不存在");
        }

        OrderGoods product = orderInfo.getProduct();
        if (product == null) {
            return AjaxResult.error(2004, "订单对应的商品不存在");
        }

        OrderMemberUser user = userService.selectOrderMemberUserById(orderInfo.getUserId());
        if (user == null) {
            return AjaxResult.error(2005, "用户不存在");
        }
        if (!username.equalsIgnoreCase(user.getUsername())) {
            return AjaxResult.error(2006, "用户身份不匹配");
        }
        if (user.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            return AjaxResult.error(2007, "余额不足");
        }
        if (OrderStatus.COMPLETED.getCode().equals(orderInfo.getStatus())) {
            return AjaxResult.error(2008, "订单已完成");
        }
        if (user.getFrozenBalance().compareTo(product.getPrice()) < 0) {
            return AjaxResult.error(2009, "冻结余额不足");
        }
        if (!OrderStatus.PENDING_SUBMIT.getCode().equals(orderInfo.getStatus())) {
            return AjaxResult.error(2010, "订单状态不允许提交");
        }

        try {
            if ("1".equals(orderInfo.getOrderType())) {
                // 连单订单处理
                int currentNum = user.getDealCount() + 1;
                List<OrderSeries> orderSeries = seriesService.selectSeriesListByUserId(user.getId());
                OrderSeries series = findSeriesByOrderIndex(orderSeries, currentNum);

                if (series != null) {
                    // 创建新连单订单
                    OrderInfo newSeriesOrder = createNewSeriesOrder(user, series, currentNum);
                    if (newSeriesOrder == null) {
                        throw new RuntimeException("创建新连单订单失败");
                    }
                    orderInfo.setStatus(OrderStatus.FROZEN.getCode()); // 当前订单冻结
                    series.setStatus(OrderStatus.FROZEN.getCode());
                    orderInfoService.updateOrderInfo(orderInfo);
                    seriesService.updateOrderSeries(series);
                } else {
                    // 处理所有冻结订单
                    List<OrderInfo> orderInfos = orderInfoService.selectOrderInfoBySeries(user.getId());
                    for (OrderInfo info : orderInfos) {
                        BigDecimal balance = user.getBalance();
                        BigDecimal newBalance = balance.add(info.getCommission());
                        recordAccountChange(
                                user.getId(), user.getUsername(), ChangeType.COMMISSION.getCode(),
                                balance, info.getCommission(), newBalance, "佣金返还", info.getCommission()
                        );
                        BigDecimal price = info.getPrice();
                        BigDecimal frozenBalance = user.getFrozenBalance();
                        BigDecimal newFrozen = frozenBalance.subtract(price);
                        BigDecimal finalBalance = newBalance.add(price);
                        recordAccountChange(
                                user.getId(), user.getUsername(), ChangeType.REFUND.getCode(),
                                newBalance, price, finalBalance, "本金返还", price
                        );
                        info.setStatus(OrderStatus.COMPLETED.getCode());
                        info.setCommissionStatus(CommissionStatus.ISSUED.getCode());
                        OrderSeries series1 = seriesService.selectOrderSeriesById(info.getSeriesId());
                        if (series1 != null) {
                            series1.setStatus(OrderStatus.COMPLETED.getCode());
                            seriesService.updateOrderSeries(series1);
                        }
                        orderInfoService.updateOrderInfo(info);
                        user.setBalance(finalBalance);
                        user.setFrozenBalance(newFrozen);
                    }
                    // 更新当前订单
                    processOrderCompletion(orderInfo, user, product.getPrice());
                }
            } else {
                // 非连单订单处理
                processOrderCompletion(orderInfo, user, product.getPrice());
            }

            userService.updateOrderMemberUser(user);
            return AjaxResult.success();
        } catch (Exception e) {
            logger.error("提交订单失败，订单ID: {}, 用户ID: {}, 错误: {}", id, user.getId(), e.getMessage());
            throw new RuntimeException("提交订单失败，请稍后重试");
        }
    }

    /**
     * 生成唯一订单号
     */
    private String generateUniqueOrderNo(Long userId) {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            String orderNo = OrderNoGenerator.generateOrderNo();
            if (orderInfoService.selectOrderInfoByCode(orderNo) == null) {
                return orderNo;
            }
        }
        logger.error("无法生成唯一订单号，用户ID: {}, 尝试次数: {}", userId, MAX_ATTEMPTS);
        return null;
    }

    /**
     * 生成唯一账变号
     */
    private String generateUniqueChangeNo(Long userId) {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            String changeNo = OrderNoGenerator.generateOrderId();
            if (accountChangeService.selectOrderAccountChangeByCode(changeNo) == null) {
                return changeNo;
            }
        }
        logger.error("无法生成唯一账变号，用户ID: {}, 尝试次数: {}", userId, MAX_ATTEMPTS);
        return null;
    }

    /**
     * 构建账变描述
     */
    private String buildChangeDescription(Long userId, String username, String action, BigDecimal amount) {
        return String.format("用户ID: %d, 用户名: %s, %s, 金额: %s",
                userId, username, action, amount.toPlainString());
    }

    /**
     * 记录账户变更
     */
    private void recordAccountChange(Long userId, String username, String changeType,
                                     BigDecimal beforeAmount, BigDecimal changeAmount,
                                     BigDecimal afterAmount, String action, BigDecimal displayAmount) {
        String changeNo = generateUniqueChangeNo(userId);
        if (changeNo == null) {
            throw new RuntimeException("账变号生成失败");
        }

        OrderAccountChange change = new OrderAccountChange();
        change.setChangeNo(changeNo);
        change.setType(changeType);
        change.setUserId(userId);
        change.setBeforeAmount(beforeAmount);
        change.setChangeAmount(changeAmount);
        change.setAfterAmount(afterAmount);
        change.setDescription(buildChangeDescription(userId, username, action, displayAmount));
        change.setCreateTime(new Date());
        accountChangeService.insertOrderAccountChange(change);
    }

    /**
     * 设置订单信息
     */
    private void setupOrderInfo(OrderInfo order, String orderNo, Long userId, Long productId,
                                BigDecimal price, BigDecimal commission) {
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setPrice(price);
        order.setProductId(productId);
        order.setCommission(commission);
        order.setCommissionStatus(CommissionStatus.PENDING.getCode());
        order.setStatus(OrderStatus.PENDING_SUBMIT.getCode());
        order.setOrderTime(new Date());
        order.setCreateTime(new Date());
    }

    /**
     * 更新用户余额并记录账变
     */
    private AjaxResult updateBalanceAndRecordChange(OrderMemberUser user, BigDecimal price,
                                                    String changeType, String action, int currentNum) {
        BigDecimal balanceBefore = user.getBalance();
        BigDecimal newBalance = balanceBefore.subtract(price);
        BigDecimal newFrozen = user.getFrozenBalance().add(price);
        user.setBalance(newBalance);
        user.setFrozenBalance(newFrozen);
        user.setDealCount(currentNum);

        try {
            userService.updateOrderMemberUser(user);
            recordAccountChange(
                    user.getId(), user.getUsername(), changeType,
                    balanceBefore, price.negate(), newBalance, action, price
            );
            return AjaxResult.success();
        } catch (Exception e) {
            logger.error("更新用户余额失败，用户ID: {}, 错误: {}", user.getId(), e.getMessage());
            return AjaxResult.error(3001, "更新用户余额失败");
        }
    }

    /**
     * 创建订单响应
     */
    private OrderVo createOrderResponse(OrderInfo order, Long productId, BigDecimal price,
                                        BigDecimal commission, String productName, String coverUrl, Date createTime) {
        return new OrderVo(order.getId(), order.getOrderNo(), productName, coverUrl, price, commission, createTime);
    }

    /**
     * 根据订单索引查找连单
     */
    private OrderSeries findSeriesByOrderIndex(List<OrderSeries> seriesList, int orderIndex) {
        Map<Integer, OrderSeries> seriesMap = seriesList.stream()
                .collect(Collectors.toMap(OrderSeries::getOrderIndex, series -> series));
        return seriesMap.get(orderIndex);
    }

    /**
     * 创建新连单订单
     */
    private OrderInfo createNewSeriesOrder(OrderMemberUser user, OrderSeries series, int currentNum) {
        Long productId = series.getProductId();
        BigDecimal price = series.getPrice();
        if (series.getCommissionRatio() == null || series.getCommissionRatio().compareTo(0) <= 0) {
            logger.error("无效的连单佣金配置，系列ID: {}", series.getId());
            return null;
        }
        BigDecimal commissionRatio = new BigDecimal(series.getCommissionRatio())
                .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        BigDecimal commission = price.multiply(commissionRatio);

        String orderNo = generateUniqueOrderNo(user.getId());
        if (orderNo == null) {
            logger.error("无法生成唯一订单号，用户ID: {}", user.getId());
            return null;
        }

        OrderInfo newOrder = new OrderInfo();
        setupOrderInfo(newOrder, orderNo, user.getId(), productId, price, commission);
        newOrder.setOrderType("1");
        newOrder.setQuantity(currentNum);
        newOrder.setSeriesId(series.getId());
        newOrder.setStatus(OrderStatus.PENDING_SUBMIT.getCode()); // 新连单订单为待提交

        try {
            orderInfoService.insertOrderInfo(newOrder);
            AjaxResult balanceResult = updateBalanceAndRecordChange(
                    user, price, ChangeType.ORDER.getCode(), "交易下单", currentNum
            );
            if (!balanceResult.isSuccess()) {
                throw new RuntimeException("更新用户余额失败: ");
            }
            return newOrder;
        } catch (Exception e) {
            logger.error("创建新连单订单失败，用户ID: {}, 错误: {}", user.getId(), e.getMessage());
            return null;
        }
    }

    /**
     * 处理订单完成逻辑
     */
    private void processOrderCompletion(OrderInfo orderInfo, OrderMemberUser user, BigDecimal price) {
        orderInfo.setStatus(OrderStatus.COMPLETED.getCode());
        orderInfo.setCommissionStatus(CommissionStatus.ISSUED.getCode());

        BigDecimal balanceBeforeCommission = user.getBalance();
        user.setBalance(balanceBeforeCommission.add(orderInfo.getCommission()));
        recordAccountChange(
                user.getId(), user.getUsername(), ChangeType.COMMISSION.getCode(),
                balanceBeforeCommission, orderInfo.getCommission(), user.getBalance(),
                "佣金返还", orderInfo.getCommission()
        );

        BigDecimal balanceBeforeRefund = user.getBalance();
        user.setFrozenBalance(user.getFrozenBalance().subtract(price));
        user.setBalance(balanceBeforeRefund.add(price));
        recordAccountChange(
                user.getId(), user.getUsername(), ChangeType.REFUND.getCode(),
                balanceBeforeRefund, price, user.getBalance(),
                "本金返还", price
        );

        orderInfoService.updateOrderInfo(orderInfo);
        if (orderInfo.getSeriesId() != null) {
            OrderSeries series = seriesService.selectOrderSeriesById(orderInfo.getSeriesId());
            if (series != null) {
                series.setStatus(OrderStatus.COMPLETED.getCode());
                seriesService.updateOrderSeries(series);
            }
        }
    }
}