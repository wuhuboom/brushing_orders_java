package com.brushing.api.service;

import com.brushing.api.controller.enumvo.ChangeType;
import com.brushing.api.controller.enumvo.CommissionStatus;
import com.brushing.api.controller.enumvo.OrderStatus;
import com.brushing.api.controller.enumvo.SeriesStatus;
import com.brushing.common.core.domain.AjaxResult;
import com.brushing.common.core.redis.RedisCache;
import com.brushing.common.utils.DateUtils;
import com.brushing.common.utils.OrderNoGenerator;
import com.brushing.common.utils.StringUtils;
import com.brushing.member.domain.*;
import com.brushing.member.service.*;
import com.brushing.set.domain.OrderSiteConfig;
import com.brushing.set.domain.OrderTradeControlConfig;
import com.brushing.set.service.IOrderSiteConfigService;
import com.brushing.system.domain.SysTimeZone;
import com.brushing.system.service.ISysTimeZoneService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class OrderTransactionalService {
    private static final Logger logger = LoggerFactory.getLogger(OrderTransactionalService.class);

    private static final int MAX_ATTEMPTS = 5;

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

    @Autowired
    private ISysTimeZoneService sysTimeZoneService;

    @Autowired
    private IOrderSiteConfigService siteConfigService;

    /**
     * Create order logic moved here and executed inside a service transaction.
     */
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult createOrderTransactional(String username) {
        OrderTradeControlConfig controlConfig = redisCache.getCacheObject("trade_config");
        OrderSiteConfig orderSiteConfig = siteConfigService.selectOrderSiteConfigById(1L);
        if (controlConfig == null) {
            return AjaxResult.error(901, "System configuration is not available");
        }
        OrderMemberUser user = userService.findByUsername(username);
        LocalTime orderTimeStart = controlConfig.getOrderTimeStart();
        LocalTime orderTimeEnd = controlConfig.getOrderTimeEnd();
        SysTimeZone active = sysTimeZoneService.getActive();
        String tzName = active.getTzName();
        LocalTime now = LocalTime.now(ZoneId.of(tzName));
        if (!isWithinWithdrawTimeRange(now, orderTimeStart, orderTimeEnd)) {
            return AjaxResult.error(902, "Not within the time frame for grabbing orders");
        }
        if (StringUtils.isEmpty(username)) {
            return AjaxResult.error(903, "Usernames cannot be empty");
        }
        if (user == null) {
            return AjaxResult.error(904, "The user does not exist");
        }
        Boolean checkUserBalance = userService.checkUserBalance(user);
        if (orderSiteConfig.getVipAutoShop().equals("0")){
            if (!checkUserBalance){
                return AjaxResult.error(916, "User balance is insufficient");
            }
        }

        try {
            if (orderInfoService.countUnfinishedOrders(user.getId()) > 0) {
                return AjaxResult.error(907, "There is an open order");
            }
            OrderMemberLevel userLevel = user.getUserLevel();
            if (!"0".equals(user.getTradeStatus())){
                return AjaxResult.error(905, "This user is not allowed to grab orders");
            }
            if (user.getDealCount() == user.getCardNumber() && (user.getCardNumber() != null && user.getCardNumber() > 0)) {
                return AjaxResult.error(2000, "order is full");
            }
            BigDecimal minUserBalance = userLevel.getMinBalance();
            if (orderSiteConfig.getMinBalance().equals("1")){
                if (user.getBalance().compareTo(minUserBalance) < 0) {
                    return AjaxResult.error(906, "The minimum transaction amount is: " + minUserBalance);
                }
            }
            int currentNum = user.getDealCount() + 1;
            List<OrderSeries> orderSeries = seriesService.selectSeriesListByUserId(user.getId());
            OrderSeries series = findSeriesByOrderIndex(orderSeries, currentNum);
            OrderInfo order = new OrderInfo();
            String orderNo = generateUniqueOrderNo(user.getId());
            if (orderNo == null) {
                return AjaxResult.error(908, "Please try again later");
            }
            if (user.getDealCount() >= user.getUserLevel().getOrderCount()){
                AjaxResult ajaxResult= new AjaxResult();
                ajaxResult.put("code",909);
                ajaxResult.put("msg","The number of orders is full");
                ajaxResult.put("data",user.getUserLevel().getOrderCount());
                return ajaxResult;
            }
            OrderVoWrapper response;
            BigDecimal price;
            BigDecimal commission ;
            Long productId;
            try {
                if (!orderSeries.isEmpty() && series != null) {
                    if (series.getCommissionRatio() == null || series.getCommissionRatio().compareTo(0) <= 0) {
                        return AjaxResult.error(910, "Invalid configuration");
                    }
                    productId = series.getProductId();
                    if (series.getType().equals("1")){
                        price = series.getPrice();
                    }else{
                        BigDecimal price1 = series.getPrice();
                        price = user.getBalance().add(price1);
                    }

                    if (orderSiteConfig == null){
                        return AjaxResult.error(908, "Please try again later");
                    }
                    BigDecimal commissionRatio ;
                    OrderGoods orderGoods;
                    if (orderSiteConfig.getSeriesStatus().equals("0")){
                        commissionRatio = userLevel.getStreakCommissionRatio()
                                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                        commission = price.multiply(commissionRatio);
                    }else{
                        // convert percentage to decimal safely
                        BigDecimal commissionRatio1 = userLevel.getCommissionRatio().movePointLeft(2);
                        BigDecimal multiply = commissionRatio1.multiply(new BigDecimal(series.getCommissionRatio()));
                        commission = price.multiply(multiply);
                    }
                    orderGoods = orderGoodsService.selectOrderGoodsById(productId);
                    if (orderGoods == null) {
                        return AjaxResult.error(908, "Please try again later");
                    }
                    order.setOrderType("1");
                    order.setSeriesId(series.getId());
                    order.setStatus(OrderStatus.PENDING_SUBMIT.getCode());
                    order.setQuantity(currentNum);
                    setupOrderInfo(order, orderNo, user.getId(), productId, price, commission, new BigDecimal(series.getCommissionRatio()));
                    // try insert with DuplicateKeyException retry (race condition safety for order_no)
                    boolean orderInserted = false;
                    for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
                        try {
                            orderInfoService.insertOrderInfo(order);
                            orderInserted = true;
                            break;
                        } catch (org.springframework.dao.DuplicateKeyException dk) {
                            logger.warn("订单号冲突，重试生成订单号 attempt={} userId={}", attempt, user.getId());
                            String newOrderNo = generateUniqueOrderNo(user.getId());
                            if (newOrderNo == null) throw new RuntimeException("Please try again later");
                            order.setOrderNo(newOrderNo);
                        }
                    }
                    if (!orderInserted) {
                        throw new RuntimeException("Please try again later");
                    }
                    series.setStatus(OrderStatus.PENDING_SUBMIT.getCode());
                    seriesService.updateOrderSeries(series);
                    response = new OrderVoWrapper(order, productId, price, commission, orderGoods.getName(), orderGoods.getCoverUrl(), DateUtils.getNowDate(), order.getStatus());
                } else {
                    BigDecimal percentRaw = randomBetween(controlConfig.getTradeRangePercentMin(), controlConfig.getTradeRangePercentMax(), 4);
                    BigDecimal percent = percentRaw.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                    BigDecimal amount = user.getBalance().multiply(percent).setScale(2, RoundingMode.DOWN);
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
                    order.setStatus(SeriesStatus.PENDING_SUBMIT.getCode());
                    setupOrderInfo(order, orderNo, user.getId(), productId, price, commission, level.getCommissionRatio());
                    boolean orderInserted2 = false;
                    for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
                        try {
                            orderInfoService.insertOrderInfo(order);
                            orderInserted2 = true;
                            break;
                        } catch (org.springframework.dao.DuplicateKeyException dk) {
                            logger.warn("订单号冲突，重试生成订单号 attempt={} userId={}", attempt, user.getId());
                            String newOrderNo = generateUniqueOrderNo(user.getId());
                            if (newOrderNo == null) throw new RuntimeException("Please try again later");
                            order.setOrderNo(newOrderNo);
                        }
                    }
                    if (!orderInserted2) {
                        throw new RuntimeException("Please try again later");
                    }
                    response = new OrderVoWrapper(order, productId, price, commission, orderGoods.getName(), orderGoods.getCoverUrl(), DateUtils.getNowDate(), order.getStatus());
                }
                AjaxResult balanceResult = updateBalanceAndRecordChange(user, price, ChangeType.ORDER.getCode(), "订单下单", currentNum );
                if (!balanceResult.isSuccess()) {
                    throw new RuntimeException("Please try again later");
                }
                // 验证数据库中用户状态与内存预期一致，否则回滚
                assertUserBalanceConsistency(user);
                return AjaxResult.success(response);
            } catch (Exception e) {
                logger.error("创建订单失败，用户ID: {}, 错误: {}", user.getId(), e.getMessage());
                throw new RuntimeException("Please try again later");
            }
        } catch (Exception ex) {
            logger.error("createOrderTransactional error: {}", ex.getMessage());
            throw ex;
        }
    }

    /**
     * Submit order logic moved here and executed inside a service transaction.
     */
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult submitOrderTransactional(Long id, String username) {
        OrderTradeControlConfig controlConfig = redisCache.getCacheObject("trade_config");
        if (controlConfig == null) {
            return AjaxResult.error(901, "System configuration is not available");
        }
        LocalTime orderTimeStart = controlConfig.getOrderTimeStart();
        LocalTime orderTimeEnd = controlConfig.getOrderTimeEnd();
        SysTimeZone active = sysTimeZoneService.getActive();
        String tzName = active.getTzName();
        LocalTime now = LocalTime.now(ZoneId.of(tzName));
        if (!isWithinWithdrawTimeRange(now, orderTimeStart, orderTimeEnd)) {
            return AjaxResult.error(919, "We'll be happy to help and get back to you once we're online");
        }
        if (id == null || id <= 0) {
            return AjaxResult.error(913, "Invalid orders");
        }
        if (StringUtils.isEmpty(username)) {
            return AjaxResult.error(914, "Invalid users");
        }

        try {
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
            if (user.getBalance().compareTo(BigDecimal.ZERO) < 0) {
                return AjaxResult.error(916, "The balance is insufficient");
            }
            if (!OrderStatus.PENDING_SUBMIT.getCode().equals(orderInfo.getStatus())) {
                return AjaxResult.error(918, "The order status does not allow submission");
            }
            if (user.getFrozenBalance().compareTo(product.getPrice()) < 0) {
                return AjaxResult.error(2007, "Contact your administrator");
            }
            OrderVoWrapper response = null;
            OrderMemberUser modifiedUser = user; // accumulate changes
            BigDecimal newOrderPrice = null;
            try {
                if ("1".equals(orderInfo.getOrderType())) { // series
                    int currentNum = user.getDealCount() + 1;
                    List<OrderSeries> orderSeries = seriesService.selectSeriesListByUserId(user.getId());
                    OrderSeries series = findSeriesByOrderIndex(orderSeries, currentNum);
                    if (series != null) { // create new series order
                        OrderInfo newSeriesOrder = createNewSeriesOrder(modifiedUser, series, currentNum);
                        if (newSeriesOrder == null) {
                            throw new RuntimeException("Please try again later");
                        }
                        newOrderPrice = newSeriesOrder.getPrice();
                        orderInfo.setStatus(OrderStatus.FROZEN.getCode());
                        series.setStatus(SeriesStatus.PENDING_SUBMIT.getCode());
                        seriesService.updateOrderSeries(series);
                        orderInfo.setSubmitTime(DateUtils.getNowDate());
                        orderInfoService.updateOrderInfo(orderInfo);
                        Long seriesId = orderInfo.getSeriesId();
                        OrderSeries series1 = seriesService.selectOrderSeriesById(seriesId);
                        series1.setStatus(SeriesStatus.FROZEN.getCode());
                        seriesService.updateOrderSeries(series1);
                        OrderGoods orderGoods = orderGoodsService.selectOrderGoodsById(newSeriesOrder.getProductId());
                        response = new OrderVoWrapper(newSeriesOrder, orderGoods.getId(), newSeriesOrder.getPrice(), newSeriesOrder.getCommission(), orderGoods.getName(), orderGoods.getCoverUrl(), newSeriesOrder.getCreateTime(), newSeriesOrder.getStatus());
                    } else {
                        List<OrderInfo> orderInfos = orderInfoService.selectOrderInfoBySeries(user.getId());
                        for (OrderInfo info : orderInfos) {
                            BigDecimal balance = modifiedUser.getBalance();
                            BigDecimal newBalance = balance.add(info.getCommission());
                            recordAccountChange(
                                    modifiedUser.getId(), modifiedUser.getUsername(), ChangeType.COMMISSION.getCode(),
                                    balance, info.getCommission(), newBalance, "佣金返还", info.getCommission()
                            );
                            BigDecimal price = info.getPrice();
                            BigDecimal frozenBalance = modifiedUser.getFrozenBalance();
                            BigDecimal newFrozen = frozenBalance.subtract(price);
                            BigDecimal finalBalance = newBalance.add(price);
                            recordAccountChange(
                                    modifiedUser.getId(), modifiedUser.getUsername(), ChangeType.REFUND.getCode(),
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
                            modifiedUser.setAllCommission(modifiedUser.getAllCommission().add(info.getCommission()));
                            modifiedUser.setCommission(modifiedUser.getCommission().add(info.getCommission()));
                            modifiedUser.setBalance(finalBalance);
                            modifiedUser.setFrozenBalance(newFrozen);
                        }
                        processOrderCompletion(orderInfo, modifiedUser, product.getPrice());
                    }
                } else {
                    processOrderCompletion(orderInfo, modifiedUser, product.getPrice());
                }
                if (newOrderPrice != null) {
                    BigDecimal balanceBefore = modifiedUser.getBalance();
                    BigDecimal newBalance = balanceBefore.subtract(newOrderPrice);
                    BigDecimal newFrozen = modifiedUser.getFrozenBalance().add(newOrderPrice);
                    modifiedUser.setBalance(newBalance);
                    modifiedUser.setFrozenBalance(newFrozen);
                    modifiedUser.setDealCount(modifiedUser.getDealCount() + 1);
                    recordAccountChange(
                            modifiedUser.getId(), modifiedUser.getUsername(), ChangeType.ORDER.getCode(),
                            balanceBefore, newOrderPrice.negate(), newBalance, "订单下单", newOrderPrice
                    );
                }
                OrderMemberUser latestUser = userService.selectOrderMemberUserById(modifiedUser.getId());
                if (latestUser == null) {
                    throw new RuntimeException("User not found");
                }
                latestUser.setBalance(modifiedUser.getBalance());
                latestUser.setFrozenBalance(modifiedUser.getFrozenBalance());
                latestUser.setDealCount(modifiedUser.getDealCount());
                latestUser.setCommission(modifiedUser.getCommission());
                latestUser.setAllCommission(modifiedUser.getAllCommission());
                int updatedRows = userService.updateOrderMemberUser(latestUser);
                if (updatedRows == 0) {
                    throw new RuntimeException("Please try again later (version conflict)");
                }
                // 在返回前再次断言数据库中用户的余额/冻结/计数/佣金与我们内存中的一致（在同一事务内）
                assertUserBalanceConsistency(latestUser);
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
        } catch (Exception ex) {
            logger.error("submitOrderTransactional error: {}", ex.getMessage());
            throw ex;
        }
    }

    // --- helpers copied from controller (kept private to this service) ---

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

    private String buildChangeDescription(Long userId, String username, String action, BigDecimal amount) {
        return String.format("用户ID: %d, 用户名: %s, %s, 金额: %s",
                userId, username, action, amount.toPlainString());
    }

    private void recordAccountChange(Long userId, String username, String changeType,
                                     BigDecimal beforeAmount, BigDecimal changeAmount,
                                     BigDecimal afterAmount, String action, BigDecimal displayAmount) {
        String changeNo = generateUniqueChangeNo(userId);
        if (changeNo == null) {
            throw new RuntimeException("Please try again later");
        }

        OrderAccountChange change = new OrderAccountChange();
        change.setChangeNo(changeNo);
        change.setType(changeType);
        change.setUserId(userId);
        change.setBeforeAmount(beforeAmount);
        change.setChangeAmount(changeAmount);
        change.setAfterAmount(afterAmount);
        change.setDescription(buildChangeDescription(userId, username, action, displayAmount));
        change.setCreateTime(DateUtils.getNowDate());
        // insert account change with retry on DuplicateKeyException
        boolean changeInserted = false;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                accountChangeService.insertOrderAccountChange(change);
                changeInserted = true;
                break;
            } catch (org.springframework.dao.DuplicateKeyException dk) {
                logger.warn("账变号冲突，重试生成账变号 attempt={} userId={}", attempt, userId);
                String newChangeNo = generateUniqueChangeNo(userId);
                if (newChangeNo == null) throw new RuntimeException("Please try again later");
                change.setChangeNo(newChangeNo);
            }
        }
        if (!changeInserted) {
            throw new RuntimeException("Please try again later");
        }

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
                    commissionChange.setType("6");
                    commissionChange.setUserId(parentUserId);
                    commissionChange.setBeforeAmount(parentBeforeAmount);
                    commissionChange.setChangeAmount(commissionAmount);
                    commissionChange.setAfterAmount(parentAfterAmount);
                    commissionChange.setDescription("用户id：" + parentUser.getId() + ", 用户名为：" + parentUser.getUsername()
                            + " 下级交易返佣, 第" + (i + 1) + "级返佣, 下级返佣金额为:" + commissionAmount);
                    commissionChange.setCreateTime(DateUtils.getNowDate());

                    parentUser.setBalance(parentAfterAmount);
                    int i1 = userService.updateOrderMemberUser(parentUser);
                    if (i1  == 0) {
                        throw new RuntimeException("Please try again later");
                    }

                    boolean commissionInserted = false;
                    for (int attempt2 = 0; attempt2 < MAX_ATTEMPTS; attempt2++) {
                        try {
                            accountChangeService.insertOrderAccountChange(commissionChange);
                            commissionInserted = true;
                            break;
                        } catch (org.springframework.dao.DuplicateKeyException dk) {
                            logger.warn("返佣账变号冲突，重试生成账变号 attempt={} parentUserId={}", attempt2, parentUserId);
                            String newChangeNo = generateUniqueChangeNo(parentUserId);
                            if (newChangeNo == null) throw new RuntimeException("Please try again later");
                            commissionChange.setChangeNo(newChangeNo);
                        }
                    }
                    if (!commissionInserted) {
                        throw new RuntimeException("Please try again later");
                    }

                } catch (NumberFormatException e) {
                    throw new RuntimeException("Please try again later");
                }
            }
        }
    }

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

    @Transactional(propagation = Propagation.REQUIRED)
    public AjaxResult updateBalanceAndRecordChange(OrderMemberUser user, BigDecimal price,
                                                   String changeType, String action, int currentNum) {
        BigDecimal balanceBefore = user.getBalance();
        BigDecimal newBalance = balanceBefore.subtract(price);
        BigDecimal newFrozen = user.getFrozenBalance().add(price);
        user.setBalance(newBalance);
        user.setFrozenBalance(newFrozen);
        user.setDealCount(currentNum);

        try {
            int updatedRows = userService.updateOrderMemberUser(user);
            if (updatedRows == 0) {
                throw new RuntimeException("Please try again later");
            }
            recordAccountChange(
                    user.getId(), user.getUsername(), changeType,
                    balanceBefore, price.negate(), newBalance, action, price
            );
            return AjaxResult.success();
        } catch (Exception e) {
            logger.error("更新用户余额失败，用户ID: {}, 错误: {}", user.getId(), e.getMessage());
            throw new RuntimeException("Please try again later");
        }
    }

    private OrderVoWrapper createOrderResponse(OrderInfo order, Long productId, BigDecimal price,
                                                BigDecimal commission, String productName, String coverUrl,
                                                Date createTime, String status) {
         return new OrderVoWrapper(order, productId, price, commission, productName, coverUrl, createTime, status);
     }

    // Assert that the expected in-memory user state matches the persisted DB state.
    // If mismatch, throw RuntimeException to cause transaction rollback.
    private void assertUserBalanceConsistency(OrderMemberUser expected) {
        if (expected == null || expected.getId() == null) return;
        OrderMemberUser persisted = userService.selectOrderMemberUserById(expected.getId());
        if (persisted == null) {
            logger.error("一致性校验失败：用户在数据库中未找到，userId={}", expected.getId());
            throw new RuntimeException("Consistency check failed");
        }

        boolean mismatch = false;
        if (expected.getBalance() == null) expected.setBalance(BigDecimal.ZERO);
        if (expected.getFrozenBalance() == null) expected.setFrozenBalance(BigDecimal.ZERO);
        if (persisted.getBalance() == null) persisted.setBalance(BigDecimal.ZERO);
        if (persisted.getFrozenBalance() == null) persisted.setFrozenBalance(BigDecimal.ZERO);

        if (persisted.getBalance().compareTo(expected.getBalance()) != 0) {
            mismatch = true;
        }
        if (persisted.getFrozenBalance().compareTo(expected.getFrozenBalance()) != 0) {
            mismatch = true;
        }
        if (!Objects.equals(persisted.getDealCount(), expected.getDealCount())) {
            mismatch = true;
        }
        if (expected.getCommission() == null) expected.setCommission(BigDecimal.ZERO);
        if (expected.getAllCommission() == null) expected.setAllCommission(BigDecimal.ZERO);
        if (persisted.getCommission() == null) persisted.setCommission(BigDecimal.ZERO);
        if (persisted.getAllCommission() == null) persisted.setAllCommission(BigDecimal.ZERO);
        if (persisted.getCommission().compareTo(expected.getCommission()) != 0) {
            mismatch = true;
        }
        if (persisted.getAllCommission().compareTo(expected.getAllCommission()) != 0) {
            mismatch = true;
        }

        if (mismatch) {
            logger.error("一致性校验失败：用户余额/冻结/计数/佣金 在内存与数据库不匹配，userId={}, expected=[balance={}, frozen={}, dealCount={}, commission={}, allCommission={}] persisted=[balance={}, frozen={}, dealCount={}, commission={}, allCommission={}]",
                    expected.getId(), expected.getBalance(), expected.getFrozenBalance(), expected.getDealCount(), expected.getCommission(), expected.getAllCommission(),
                    persisted.getBalance(), persisted.getFrozenBalance(), persisted.getDealCount(), persisted.getCommission(), persisted.getAllCommission());
            throw new RuntimeException("Consistency check failed");
        }
    }

    private OrderSeries findSeriesByOrderIndex(List<OrderSeries> seriesList, int orderIndex) {
        Map<Integer, OrderSeries> seriesMap = seriesList.stream()
                .collect(Collectors.toMap(OrderSeries::getOrderIndex, series -> series));
        return seriesMap.get(orderIndex);
    }

    private OrderInfo createNewSeriesOrder(OrderMemberUser user, OrderSeries series, int currentNum) {
        Long productId = series.getProductId();
        BigDecimal price = new BigDecimal("0");
        if (series.getType().equals("1")){
            price = series.getPrice();
        }else{
            BigDecimal price1 = series.getPrice();
            price = user.getBalance().add(price1);
        }
        if (series.getCommissionRatio() == null) {
            logger.error("无效的连单佣金配置，系列ID: {}", series.getId());
            return null;
        }
        try {
            if (new BigDecimal(series.getCommissionRatio()).compareTo(BigDecimal.ZERO) <= 0) {
                logger.error("无效的连单佣金配置，系列ID: {}", series.getId());
                return null;
            }
        } catch (NumberFormatException ex) {
            logger.error("连单佣金比例格式错误，系列ID: {}, ratio: {}", series.getId(), series.getCommissionRatio());
            return null;
        }
        OrderSiteConfig orderSiteConfig = siteConfigService.selectOrderSiteConfigById(1L);
        if (orderSiteConfig == null){
           throw new RuntimeException("Please try again later");
        }
        BigDecimal commission ;
        if (orderSiteConfig.getSeriesStatus().equals("0")){
            BigDecimal commissionRatio = new BigDecimal(series.getCommissionRatio()).movePointLeft(2);
            commission = price.multiply(commissionRatio);
        } else {
            OrderMemberLevel userLevel = user.getUserLevel();
            BigDecimal divide = userLevel.getCommissionRatio().movePointLeft(2);
            BigDecimal multiply = new BigDecimal(series.getCommissionRatio()).multiply(divide);
            commission = price.multiply(multiply);
        }
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
        newOrder.setStatus(OrderStatus.PENDING_SUBMIT.getCode());

        try {
            boolean newOrderInserted = false;
            for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
                try {
                    orderInfoService.insertOrderInfo(newOrder);
                    newOrderInserted = true;
                    break;
                } catch (org.springframework.dao.DuplicateKeyException dk) {
                    logger.warn("连单新订单号冲突，重试生成订单号 attempt={} userId={}", attempt, user.getId());
                    String newOrderNo = generateUniqueOrderNo(user.getId());
                    if (newOrderNo == null) throw new RuntimeException("Please try again later");
                    newOrder.setOrderNo(newOrderNo);
                }
            }
            if (!newOrderInserted) {
                throw new RuntimeException("Please try again later");
            }
            return newOrder;
        } catch (Exception e) {
            logger.error("创建新连单订单失败，用户ID: {}, 错误: {}", user.getId(), e.getMessage());
            throw new RuntimeException("Please try again later");
        }
    }

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

    private boolean isWithinWithdrawTimeRange(LocalTime now, LocalTime start, LocalTime end) {
        return !now.isBefore(start) && !now.isAfter(end);
    }

    public BigDecimal randomBetween(BigDecimal min, BigDecimal max, int scale) {
        if (min.compareTo(max) > 0) { BigDecimal t = min; min = max; max = t; }
        BigDecimal range = max.subtract(min);
        BigDecimal r = BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble());
        return min.add(range.multiply(r)).setScale(scale, RoundingMode.HALF_UP);
    }

    // small wrapper to avoid importing controller VO class directly in service layer
    public static class OrderVoWrapper extends com.brushing.api.controller.vo.OrderVo {
        public OrderVoWrapper(OrderInfo order, Long productId, BigDecimal price,
                              BigDecimal commission, String productName, String coverUrl,
                              Date createTime, String status) {
            super(order.getId(), order.getOrderNo(), productName, coverUrl, price, commission, createTime, status);
        }
        // keep a convenience constructor if callers ever pass explicit fields
        public OrderVoWrapper(Long id, String orderNo, String productName, String coverUrl,
                              BigDecimal price, BigDecimal commission, Date createTime, String status) {
            super(id, orderNo, productName, coverUrl, price, commission, createTime, status);
        }
    }
 }
