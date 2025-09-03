package com.brushing.api.controller;

import com.brushing.api.controller.enumvo.ChangeType;
import com.brushing.api.controller.enumvo.CommissionStatus;
import com.brushing.api.controller.enumvo.OrderStatus;
import com.brushing.api.controller.enumvo.SeriesStatus;
import com.brushing.api.controller.vo.OrderVo;
import com.brushing.api.controller.vo.WithrawalPage;
import com.brushing.common.core.controller.BaseController;
import com.brushing.common.core.domain.AjaxResult;
import com.brushing.common.core.page.TableDataInfo;
import com.brushing.common.core.redis.RedisCache;
import com.brushing.common.utils.DateUtils;
import com.brushing.common.utils.OrderNoGenerator;
import com.brushing.common.utils.StringUtils;
import com.brushing.member.domain.*;
import com.brushing.member.service.*;
import com.brushing.set.domain.OrderTradeControlConfig;
import com.github.pagehelper.PageHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Tag(
        name = "订单管理",
        description =
                "错误码对照表：\n" +
                        "901: System configuration is not available（系统配置不可用）\n" +
                        "902: Not within the time frame for grabbing orders（不在允许的抢单时间范围内）\n" +
                        "903: Usernames cannot be empty（用户名不能为空）\n" +
                        "904: The user does not exist（用户不存在）\n" +
                        "905: This user is not allowed to grab orders（该用户不允许抢单）\n" +
                        "906: The minimum transaction amount is insufficient（余额低于最低交易额度）\n" +
                        "907: There is an open order（存在未完成订单）\n" +
                        "908: Please try again later（请稍后再试）\n" +
                        "909: The number of orders is full（今日可接单数已满）\n" +
                        "910: Invalid configuration（配置无效）\n" +
                        "911: No suitable product or insufficient balance（无合适商品或余额不足）\n" +
                        "912: Invalid membership tier configuration（会员等级配置无效）\n" +
                        "913: Invalid orders（订单无效）\n" +
                        "914: Invalid users（用户无效）\n" +
                        "915: User identity mismatch（用户身份不匹配）\n" +
                        "916: The balance is insufficient（余额不足）\n" +
                        "917: The order is completed（订单已完成）\n" +
                        "918: The order status does not allow submission（订单状态不允许提交）\n" +
                        "特殊：2007 Contact your administrator（联系管理员，常用于冻结金额不足）"
)
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

    @Autowired
    private RedisCache redisCache;

    private static final int MAX_ATTEMPTS = 5;

    /** 为认证用户创建新订单 */
    @Transactional
    @GetMapping("/createOrder")
    @Operation(summary = "创建新订单")
    public AjaxResult createOrder(@RequestAttribute("username") String username) {
        OrderTradeControlConfig controlConfig = redisCache.getCacheObject("trade_config");
        if (controlConfig == null) {
            return AjaxResult.error(901, "System configuration is not available");
        }
        OrderMemberUser user = userService.findByUsername(username);
        LocalTime orderTimeStart = controlConfig.getOrderTimeStart();
        LocalTime orderTimeEnd = controlConfig.getOrderTimeEnd();
        LocalTime now = LocalTime.now();
        if (!isWithinWithdrawTimeRange(now, orderTimeStart, orderTimeEnd)) {
            return AjaxResult.error(902, "Not within the time frame for grabbing orders");
        }
        if (StringUtils.isEmpty(username)) {
            return AjaxResult.error(903, "Usernames cannot be empty");
        }
        if (user == null) {
            return AjaxResult.error(904, "The user does not exist");
        }
        if (orderInfoService.countUnfinishedOrders(user.getId()) > 0) {
            return AjaxResult.error(907, "There is an open order");
        }
        OrderMemberLevel userLevel = user.getUserLevel();
        if (!"0".equals(user.getTradeStatus())||(user.getDealCount()==user.getCardNumber()&&user.getCardNumber()>0)){
            return AjaxResult.error(905, "This user is not allowed to grab orders");
        }
        BigDecimal minUserBalance = userLevel.getMinBalance();
        if (user.getBalance().compareTo(minUserBalance) < 0) {
            return AjaxResult.error(906, "The minimum transaction amount is: " + minUserBalance);
        }

        int currentNum = user.getDealCount() + 1;
        List<OrderSeries> orderSeries = seriesService.selectSeriesListByUserId(user.getId());
        OrderSeries series = findSeriesByOrderIndex(orderSeries, currentNum);

        OrderInfo order = new OrderInfo();
        String orderNo = generateUniqueOrderNo(user.getId());
        if (orderNo == null) {
            return AjaxResult.error(908, "Please try again later");
        }
        if (user.getDealCount() == user.getUserLevel().getOrderCount()){
            return AjaxResult.error(909, "The number of orders is full");
        }

        OrderVo response;
        BigDecimal price;
        BigDecimal commission;
        Long productId;

        try {
            if (!orderSeries.isEmpty() && series != null) {
                // 连单逻辑
                if (series.getCommissionRatio() == null ||  series.getCommissionRatio().compareTo(0) <= 0) {
                    return AjaxResult.error(910, "Invalid configuration");
                }
                productId = series.getProductId();
                price = series.getPrice();
                BigDecimal commissionRatio = new BigDecimal(series.getCommissionRatio())
                        .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
                commission = price.multiply(commissionRatio);
                OrderGoods orderGoods = orderGoodsService.selectOrderGoodsById(productId);
                if (orderGoods == null) {
                    return AjaxResult.error(908, "Please try again later");
                }
                order.setOrderType("1");
                order.setSeriesId(series.getId());
                order.setStatus(OrderStatus.PENDING_SUBMIT.getCode()); // 连单订单初始为待提交
                order.setQuantity(currentNum);
                setupOrderInfo(order, orderNo, user.getId(), productId, price, commission, new BigDecimal(series.getCommissionRatio()));
                orderInfoService.insertOrderInfo(order);
                series.setStatus(OrderStatus.PENDING_SUBMIT.getCode());
                seriesService.updateOrderSeries(series);
                response = createOrderResponse(order, productId, price, commission,
                        orderGoods.getName(), orderGoods.getCoverUrl(), DateUtils.getNowDate(), order.getStatus());
            } else {
                // 非连单逻辑
                BigDecimal percentRaw = randomBetween(controlConfig.getTradeRangePercentMin(),
                        controlConfig.getTradeRangePercentMax(), 4);
                BigDecimal percent = percentRaw.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

                BigDecimal amount  = user.getBalance().multiply(percent).setScale(2, RoundingMode.DOWN);
                OrderGoods orderGoods = orderGoodsService.selectNearestPriceGoods(amount);
                if (orderGoods == null) {
                    return AjaxResult.error(911, "No suitable product or insufficient balance");
                }
                OrderMemberLevel level = memberLevelService.selectOrderMemberLevelById(user.getLevelId());
                if (level == null || level.getCommissionRatio() == null) {
                    return AjaxResult.error(912, "Invalid membership tier configuration");
                }
                price = orderGoods.getPrice();
                BigDecimal commissionRatio = level.getCommissionRatio()
                        .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                commission = price.multiply(commissionRatio);
                productId = orderGoods.getId();
                order.setCommissionRate(level.getCommissionRatio());
                order.setQuantity(currentNum);
                order.setStatus(SeriesStatus.PENDING_SUBMIT.getCode()); // 非连单订单初始为待提交
                setupOrderInfo(order, orderNo, user.getId(), productId, price, commission, level.getCommissionRatio());
                orderInfoService.insertOrderInfo(order);
                response = createOrderResponse(order, productId, price, commission,
                        orderGoods.getName(), orderGoods.getCoverUrl(), DateUtils.getNowDate(), order.getStatus());
            }

            AjaxResult balanceResult = updateBalanceAndRecordChange(
                    user, price, ChangeType.ORDER.getCode(), "订单下单", currentNum
            );
            if (!balanceResult.isSuccess()) {
                throw new RuntimeException("Please try again later");
            }

            return AjaxResult.success(response);
        } catch (Exception e) {
            logger.error("创建订单失败，用户ID: {}, 错误: {}", user.getId(), e.getMessage());
            throw new RuntimeException("Please try again later");
        }
    }

    /** 提交现有订单并处理佣金和余额更新 */
    @Transactional
    @GetMapping("/submitOrder/{id}")
    @Operation(summary = "提交订单", description = "返回code说明，201：如果用户存在连单 会返回新的订单信息，2007：用户余额不足，200：表示用户无新的订单，且订单提交成功。")
    public AjaxResult submitOrder(@PathVariable("id") Long id, @RequestAttribute("username") String username) {
        if (id == null || id <= 0) {
            return AjaxResult.error(913, "Invalid orders");
        }
        if (StringUtils.isEmpty(username)) {
            return AjaxResult.error(914, "Invalid users");
        }

        OrderInfo orderInfo = orderInfoService.selectOrderInfoById(id);
        if (orderInfo == null) {
            return AjaxResult.error(913, "Invalid orders");
        }

        OrderGoods product = orderInfo.getProduct();
        if (product == null) {
            return AjaxResult.error(913, "Invalid orders");
        }

        OrderMemberUser user = userService.selectOrderMemberUserById(orderInfo.getUserId());
        if (user == null) {
            return AjaxResult.error(914, "Invalid users");
        }
        if (!username.equalsIgnoreCase(user.getUsername())) {
            return AjaxResult.error(915, "User identity mismatch");
        }
        if (user.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            return AjaxResult.error(916, "The balance is insufficient");
        }
        if (OrderStatus.COMPLETED.getCode().equals(orderInfo.getStatus())) {
            return AjaxResult.error(917, "The order is completed");
        }
        if (user.getFrozenBalance().compareTo(product.getPrice()) < 0) {
            return AjaxResult.error(2007, "Contact your administrator");
        }
        if (!OrderStatus.PENDING_SUBMIT.getCode().equals(orderInfo.getStatus())) {
            return AjaxResult.error(918, "The order status does not allow submission");
        }

        OrderVo response = null;
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
                        throw new RuntimeException("Please try again later");
                    }
                    orderInfo.setStatus(OrderStatus.FROZEN.getCode()); // 当前订单冻结
                    // 新订单待提交
                    series.setStatus(SeriesStatus.PENDING_SUBMIT.getCode());
                    seriesService.updateOrderSeries(series);
                    orderInfo.setSubmitTime(DateUtils.getNowDate());
                    orderInfoService.updateOrderInfo(orderInfo);
                    // 旧的连单标记冻结
                    Long seriesId = orderInfo.getSeriesId();
                    OrderSeries series1 = seriesService.selectOrderSeriesById(seriesId);
                    series1.setStatus(SeriesStatus.FROZEN.getCode());
                    seriesService.updateOrderSeries(series1);
                    OrderGoods orderGoods = orderGoodsService.selectOrderGoodsById(newSeriesOrder.getProductId());
                    response = createOrderResponse(
                            newSeriesOrder, orderGoods.getId(), newSeriesOrder.getPrice(),
                            newSeriesOrder.getCommission(), orderGoods.getName(), orderGoods.getCoverUrl(),
                            newSeriesOrder.getCreateTime(), newSeriesOrder.getStatus()
                    );
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
                            series1.setStatus(SeriesStatus.COMPLETED.getCode());
                            seriesService.updateOrderSeries(series1);
                        }
                        orderInfoService.updateOrderInfo(info);
                        user.setAllCommission(user.getAllCommission().add(info.getCommission()));
                        user.setCommission(user.getCommission().add(info.getCommission()));
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
            if (response == null){
                return AjaxResult.success();
            } else {
                AjaxResult result = new AjaxResult();
                result.put("code", 201);
                result.put("data", response);
                result.put("msg", "You have a new order");
                return result;
            }

        } catch (Exception e) {
            logger.error("提交订单失败，订单ID: {}, 用户ID: {}, 错误: {}", id, user.getId(), e.getMessage());
            throw new RuntimeException("Please try again later");
        }
    }

    /** 生成唯一订单号 */
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

    /** 生成唯一账变号 */
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

    /** 构建账变描述 */
    private String buildChangeDescription(Long userId, String username, String action, BigDecimal amount) {
        return String.format("用户ID: %d, 用户名: %s, %s, 金额: %s",
                userId, username, action, amount.toPlainString());
    }

    /** 记录账户变更 */
    private void recordAccountChange(Long userId, String username, String changeType,
                                     BigDecimal beforeAmount, BigDecimal changeAmount,
                                     BigDecimal afterAmount, String action, BigDecimal displayAmount) {
        String changeNo = generateUniqueChangeNo(userId);
        if (changeNo == null) {
            throw new RuntimeException("Please try again later");
        }

        // 原始账变记录
        OrderAccountChange change = new OrderAccountChange();
        change.setChangeNo(changeNo);
        change.setType(changeType);
        change.setUserId(userId);
        change.setBeforeAmount(beforeAmount);
        change.setChangeAmount(changeAmount);
        change.setAfterAmount(afterAmount);
        change.setDescription(buildChangeDescription(userId, username, action, displayAmount));
        change.setCreateTime(DateUtils.getNowDate());
        accountChangeService.insertOrderAccountChange(change);

        // 返佣给上级返点（类型 5）
        if (changeType.equals("5")) {
            OrderTradeControlConfig controlConfig = redisCache.getCacheObject("trade_config");
            OrderMemberUser orderMemberUser = userService.selectOrderMemberUserById(userId);
            String ancestors = orderMemberUser.getAncestors();

            if (ancestors == null || ancestors.trim().isEmpty()) {
                return;
            }

            String[] ancestorIds = ancestors.trim().split("\\s*,\\s*");
            List<String> ancestorList = new ArrayList<>(Arrays.asList(ancestorIds));
            Collections.reverse(ancestorList);
            ancestorIds = ancestorList.toArray(new String[0]);

            BigDecimal[] commissionPercents = {
                    controlConfig.getLevel1CommissionPercent(),
                    controlConfig.getLevel2CommissionPercent(),
                    controlConfig.getLevel3CommissionPercent(),
                    controlConfig.getLevel4CommissionPercent(),
                    controlConfig.getLevel5CommissionPercent()
            };

            int maxLevel = Math.min(ancestorIds.length, commissionPercents.length);
            for (int i = 0; i < maxLevel; i++) {
                if (commissionPercents[i] == null || commissionPercents[i].compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                try {
                    Long parentUserId = Long.parseLong(ancestorIds[i]);
                    if (parentUserId <= 0) {
                        continue;
                    }

                    OrderMemberUser parentUser = userService.selectOrderMemberUserById(parentUserId);
                    if (parentUser == null) {
                        continue;
                    }

                    BigDecimal commissionAmount = changeAmount
                            .multiply(commissionPercents[i])
                            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

                    BigDecimal parentBeforeAmount = parentUser.getBalance() != null ?
                            parentUser.getBalance() : BigDecimal.ZERO;
                    BigDecimal parentAfterAmount = parentBeforeAmount.add(commissionAmount);

                    OrderAccountChange commissionChange = new OrderAccountChange();
                    String commissionChangeNo = generateUniqueChangeNo(parentUserId);
                    if (commissionChangeNo == null) {
                        throw new RuntimeException("Please try again later");
                    }

                    commissionChange.setChangeNo(commissionChangeNo);
                    commissionChange.setType("6"); // 返佣类型
                    commissionChange.setUserId(parentUserId);
                    commissionChange.setBeforeAmount(parentBeforeAmount);
                    commissionChange.setChangeAmount(commissionAmount);
                    commissionChange.setAfterAmount(parentAfterAmount);
                    commissionChange.setDescription("用户id：" + parentUser.getId() + ", 用户名为：" + parentUser.getUsername()
                            + " 下级交易返佣, 第" + (i + 1) + "级返佣, 下级返佣金额为:" + commissionAmount);
                    commissionChange.setCreateTime(DateUtils.getNowDate());

                    parentUser.setBalance(parentAfterAmount);
                    userService.updateOrderMemberUser(parentUser);

                    accountChangeService.insertOrderAccountChange(commissionChange);

                } catch (NumberFormatException e) {
                    continue;
                }
            }
        }
    }

    /** 设置订单信息 */
    private void setupOrderInfo(OrderInfo order, String orderNo, Long userId, Long productId,
                                BigDecimal price, BigDecimal commission, BigDecimal commissionRate) {
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setPrice(price);
        order.setProductId(productId);
        order.setCommission(commission);
        order.setCommissionRate(commissionRate);
        order.setCommissionStatus(CommissionStatus.PENDING.getCode());
        order.setStatus(OrderStatus.PENDING_SUBMIT.getCode());
        order.setOrderTime(DateUtils.getNowDate());
        order.setCreateTime(DateUtils.getNowDate());
    }

    /** 更新用户余额并记录账变 */
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
            return AjaxResult.error(908, "Please try again later");
        }
    }

    /** 创建订单响应 */
    private OrderVo createOrderResponse(OrderInfo order, Long productId, BigDecimal price,
                                        BigDecimal commission, String productName, String coverUrl,
                                        Date createTime, String status) {
        return new OrderVo(order.getId(), order.getOrderNo(), productName, coverUrl, price, commission, createTime, status);
    }

    /** 根据订单索引查找连单 */
    private OrderSeries findSeriesByOrderIndex(List<OrderSeries> seriesList, int orderIndex) {
        Map<Integer, OrderSeries> seriesMap = seriesList.stream()
                .collect(Collectors.toMap(OrderSeries::getOrderIndex, series -> series));
        return seriesMap.get(orderIndex);
    }

    /** 创建新连单订单 */
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
        setupOrderInfo(newOrder, orderNo, user.getId(), productId, price, commission, new BigDecimal(series.getCommissionRatio()));
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
                throw new RuntimeException("Please try again later");
            }
            return newOrder;
        } catch (Exception e) {
            logger.error("创建新连单订单失败，用户ID: {}, 错误: {}", user.getId(), e.getMessage());
            return null;
        }
    }

    /** 处理订单完成逻辑 */
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
        // 添加今日/累计佣金
        user.setCommission(user.getCommission().add(orderInfo.getCommission()));
        user.setAllCommission(user.getAllCommission().add(orderInfo.getCommission()));

        BigDecimal balanceBeforeRefund = user.getBalance();
        user.setFrozenBalance(user.getFrozenBalance().subtract(price));
        user.setBalance(balanceBeforeRefund.add(price));
        recordAccountChange(
                user.getId(), user.getUsername(), ChangeType.REFUND.getCode(),
                balanceBeforeRefund, price, user.getBalance(),
                "本金返还", price
        );
        orderInfo.setSubmitTime(DateUtils.getNowDate());
        orderInfoService.updateOrderInfo(orderInfo);
        if (orderInfo.getSeriesId() != null) {
            OrderSeries series = seriesService.selectOrderSeriesById(orderInfo.getSeriesId());
            if (series != null) {
                series.setStatus(OrderStatus.COMPLETED.getCode());
                seriesService.updateOrderSeries(series);
            }
        }
    }

    // 获取订单列表
    @GetMapping("/getOrderInfos")
    @Operation(summary = "获取用户订单记录", description = "orderNo:编号，goodsName：商品名称 ，coverUrl：图片地址，price:价格,commission:佣金,createTime:创建时间, status:状态 0：完成 1：冻结 2 待提交, 如果不传 默认就是全部")
    public TableDataInfo getOrderInfos(WithrawalPage page, @RequestAttribute("username") String username) {
        OrderMemberUser user = userService.findByUsername(username);
        PageHelper.startPage(page.getPageNum(), page.getPageSize());
        OrderInfo orderInfo= new OrderInfo();
        orderInfo.setUserId(user.getId());
        if (!StringUtils.isEmpty(page.getStatus())){
            orderInfo.setStatus(page.getStatus());
        }
        List<OrderInfo> orderInfos = orderInfoService.selectOrderInfosByUser(orderInfo);
        return getDataTable(orderInfos);
    }

    private boolean isWithinWithdrawTimeRange(LocalTime now, LocalTime start, LocalTime end) {
        return !now.isBefore(start) && !now.isAfter(end);
    }

    public BigDecimal randomBetween(BigDecimal min, BigDecimal max, int scale) {
        if (min.compareTo(max) > 0) { BigDecimal t = min; min = max; max = t; }
        BigDecimal range = max.subtract(min);
        BigDecimal r = BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble()); // [0,1)
        return min.add(range.multiply(r)).setScale(scale, RoundingMode.HALF_UP);
    }
}
