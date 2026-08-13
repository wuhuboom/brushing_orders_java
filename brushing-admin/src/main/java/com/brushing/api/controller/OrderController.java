package com.brushing.api.controller;

import com.brushing.api.controller.enumvo.ChangeType;
import com.brushing.api.controller.enumvo.CommissionStatus;
import com.brushing.api.controller.enumvo.OrderStatus;
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
import com.brushing.set.domain.OrderSiteConfig;
import com.brushing.set.domain.OrderTradeControlConfig;
import com.brushing.set.service.IOrderSiteConfigService;
import com.brushing.system.domain.SysTimeZone;
import com.brushing.system.service.ISysTimeZoneService;
import com.github.pagehelper.PageHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
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
                        "919: We'll be happy to help and get back to you once we're online(我们期待在工作时间内为您服务)" +
                        "921: Please contact customer service to start the task (请联系客服开启任务)" +
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

    @Autowired
    private ISysTimeZoneService sysTimeZoneService;

    @Autowired
    private IOrderSiteConfigService siteConfigService;

    private static final int MAX_ATTEMPTS = 5;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private com.brushing.api.service.OrderTransactionalService orderTransactionalService;





    /** 为认证用户创建新订单 */
    //@Transactional  // transaction moved to service to ensure single transactional boundary
    @GetMapping("/createOrder")
    @Operation(summary = "创建新订单")
    public AjaxResult createOrder(@RequestAttribute("username") String username) {
        // keep the original locking strategy in controller, delegate DB changes to transactional service
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
        if (user.getTaskStatus().equals("1")) {
            return AjaxResult.error(921, "Please contact customer service to start the task");
        }
        Boolean checkUserBalance = userService.checkUserBalance(user);
        if (orderSiteConfig.getVipAutoShop().equals("0")){
            if (!checkUserBalance){
                return AjaxResult.error(916, "User balance is insufficient");
            }
        }
        // 添加Redis锁
        String lockKey = "order_lock:" + user.getId();
        String lockValue = java.util.UUID.randomUUID().toString();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, 5, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            logger.warn("用户{}创建订单被锁，请稍后重试", user.getId());
            return AjaxResult.error(908, "Please try again later (system busy)");
        }

        try {
            return orderTransactionalService.createOrderTransactional(username);
        } finally {
            // atomic release via Lua script: only delete if value matches
            String lua = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptText(lua);
            script.setResultType(Long.class);
            try {
                redisTemplate.execute(script, Collections.singletonList(lockKey), lockValue);
            } catch (Exception e) {
                logger.warn("释放 Redis 锁失败 key={} err={}", lockKey, e.getMessage());
            }
        }
    }




    /** 提交现有订单并处理佣金和余额更新 */
    /** 提交现有订单并处理佣金和余额更新 */
    //@Transactional  // transaction moved to service to ensure single transactional boundary
    @GetMapping("/submitOrder/{id}")
    @Operation(summary = "提交订单", description = "返回code说明，201：如果用户存在连单 会返回新的订单信息，2007：用户余额不足，200：表示用户无新的订单，且订单提交成功。")
    public AjaxResult submitOrder(@PathVariable("id") Long id, @RequestAttribute("username") String username) {
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

        // 添加Redis锁：按订单ID锁，防同一订单双提交
        String lockKey = "submit_lock:" + id;
        String lockValue = java.util.UUID.randomUUID().toString();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, 5, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            logger.warn("订单{}提交被锁，请稍后重试", id);
            return AjaxResult.error(918, "The order status does not allow submission (system busy)");  // 复用918，避免暴露锁
        }

        try {
            return orderTransactionalService.submitOrderTransactional(id, username);
        } finally {
            String lua = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptText(lua);
            script.setResultType(Long.class);
            try {
                redisTemplate.execute(script, Collections.singletonList(lockKey), lockValue);
            } catch (Exception e) {
                logger.warn("释放 Redis 锁失败 key={} err={}", lockKey, e.getMessage());
            }
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
                    int i1 = userService.updateOrderMemberUser(parentUser);
                    if (i1  == 0) {
                        throw new RuntimeException("Please try again later");
                    }

                    accountChangeService.insertOrderAccountChange(commissionChange);

                } catch (NumberFormatException e) {
                    throw new RuntimeException("Please try again later");
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
    /** 创建新连单订单（推迟余额扣除） */
    private OrderInfo createNewSeriesOrder(OrderMemberUser user, OrderSeries series, int currentNum) {
        Long productId = series.getProductId();
        BigDecimal price = new BigDecimal("0");
        if (series.getType().equals("1")){
            price = series.getPrice();
        }else{
            BigDecimal price1 = series.getPrice();
            price = user.getBalance().add(price1);
        }
        if (series.getCommissionRatio() == null || series.getCommissionRatio().compareTo(0) <= 0) {
            logger.error("无效的连单佣金配置，系列ID: {}", series.getId());
            return null;
        }
        OrderSiteConfig orderSiteConfig = siteConfigService.selectOrderSiteConfigById(1L);
        if (StringUtils.isNull(orderSiteConfig)){
           throw new RuntimeException("Please try again later");
        }
        BigDecimal commission ;
        if (orderSiteConfig.getSeriesStatus().equals("0")){
            BigDecimal commissionRatio = new BigDecimal(series.getCommissionRatio())
                    .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            commission= price.multiply(commissionRatio);
        }else{
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
        newOrder.setStatus(OrderStatus.PENDING_SUBMIT.getCode()); // 新连单订单为待提交

        try {
            orderInfoService.insertOrderInfo(newOrder);
            // 移除：AjaxResult balanceResult = updateBalanceAndRecordChange(...);  // 推迟到submitOrder末尾
            // 移除：if (!balanceResult.isSuccess()) throw ...;
            return newOrder;
        } catch (Exception e) {
            logger.error("创建新连单订单失败，用户ID: {}, 错误: {}", user.getId(), e.getMessage());
            throw new RuntimeException("Please try again later");
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
    @Operation(summary = "获取用户订单记录", description = "balance:当前订单下单后的用户余额，frozenBalance:当前订单下单后的用户冻结余额，orderNo:编号，goodsName：商品名称 ，coverUrl：图片地址，price:价格,commission:佣金,createTime:创建时间, status:状态 0：完成 1：冻结 2 待提交；传 1 或 2 均返回冻结和待提交的数据，如果不传则返回全部")
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

    // 根据订单 ID 获取订单详情
    @GetMapping("/getOrderInfo/{id}")
    @Operation(
            summary = "获取订单详情",
            description = "根据订单 ID 获取当前用户的订单详情。" +
                    "id：订单ID，orderNo：订单编号，userId：用户ID，productId：商品ID，" +
                    "quantity：商品数量，price：订单价格，balance：下单后的用户可用余额，" +
                    "frozenBalance：下单后的用户冻结余额，commission：佣金金额，" +
                    "commissionRate：佣金比例，commissionStatus：佣金发放状态，" +
                    "orderTime：下单时间，submitTime：提交时间，orderType：订单类型，" +
                    "status：订单状态（0：完成，1：冻结，2：待提交），seriesId：连单ID，" +
                    "username：用户名，phone：手机号，dealCount：用户交易次数，" +
                    "createTime：创建时间，updateTime：更新时间，product：商品详情"
    )
    public AjaxResult getOrderInfo(@PathVariable("id") Long id,
                                   @RequestAttribute("username") String username) {
        if (id == null || id <= 0) {
            return AjaxResult.error(913, "Invalid orders");
        }

        OrderMemberUser user = userService.findByUsername(username);
        if (user == null) {
            return AjaxResult.error(914, "Invalid users");
        }

        OrderInfo orderInfo = orderInfoService.selectOrderInfoById(id);
        if (orderInfo == null || !Objects.equals(orderInfo.getUserId(), user.getId())) {
            return AjaxResult.error(913, "Invalid orders");
        }

        return success(orderInfo);
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
