package com.order.api.controller;


import com.github.pagehelper.PageHelper;
import com.order.api.controller.dto.WithdrawalPage;
import com.order.common.core.controller.BaseController;
import com.order.common.core.domain.AjaxResult;
import com.order.common.core.page.TableDataInfo;
import com.order.common.utils.StringUtils;
import com.order.member.domain.*;
import com.order.member.service.*;
import com.order.system.domain.SysTimeZone;
import com.order.system.service.ISysTimeZoneService;
import com.order.web.controller.tool.TimeRangeChecker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

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
                        "919: We'll be happy to help and get back to you once we're online(我们期待在工作时间内为您服务)"
)
@RestController
@RequestMapping("/api/order")
public class OrderController extends BaseController {


    @Autowired
    private ISysTimeZoneService sysTimeZoneService;

    @Autowired
    private IOrderConfigService orderConfigService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private IOrderUserService orderUserService;

    @Autowired
    private IOrderInfoService orderInfoService;

    @Autowired
    private IOrderLinkService orderLinkService;

    @Autowired
    private IGoodsService goodsService;

    @Autowired
    private ITransactionService transactionService;

    @Autowired
    private IOrderSequenceManagerService orderSequenceManagerService;




    @Transactional
    @GetMapping("/createOrder")
    @Operation(summary = "创建新订单",
            description = "id:\"主键ID\"\n" +
                    "orderNumber:\"订单标号\"\n" +
                    "userId:\"用户ID\"\n" +
                    "type:\"类型（0正常 1连单）\"\n" +
                    "orderCount:\"单数\"\n" +
                    "amount:\"金额\"\n" +
                    "rebatePercentage:\"返佣百分比\"\n" +
                    "rebate:\"返佣金额\"\n" +
                    "upperRebatePercentage:\"上级返佣百分比\"\n" +
                    "upperRebate:\"上级返佣金额\"\n" +
                    "status:\"订单状态（0完成 1待提交 2已冻结）\"\n" +
                    "expiryTime:\"过期时间\"\n" +
                    "productId:\"商品ID\"\n" +
                    "extraCommissionId:\"额外佣金ID\"\n" +
                    "linkId:\"连单ID\"\n" +
                    "remarks:\"备注\"\n" +
                    "commentId:\"评论ID\"\n" +
                    "username:\"用户名\"\n" +
                    "productImage:\"商品图片\"\n" +
                    "productTitle:\"商品标题\"\n" +
                    "createTime:\"创建时间\" ")
    public AjaxResult createOrder(@RequestAttribute("username") String username) {
        // 添加Redis锁
        String lockKey = "order_lock:" + username;
        String lockValue = java.util.UUID.randomUUID().toString();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, 5, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            logger.warn("用户{}创建订单被锁，请稍后重试", username);
            return AjaxResult.error(908, "Please try again later (system busy)");
        }
        try{
            //查询是否在抢单时间范围内
            SysTimeZone active = sysTimeZoneService.getActive();
            Optional<Object> configValue = orderConfigService.getConfigValue("trade", "tradeTimeRange");
            boolean currentTimeInRange = TimeRangeChecker.isCurrentTimeInRange(configValue,active.getTzName());
            if (!currentTimeInRange) {
                return AjaxResult.error(902, "Not within the time frame for grabbing orders");
            }
            //查询用户信息
            OrderUser user = orderUserService.selectOrderUserByName(username);
            if (user == null) {
                return AjaxResult.error(904, "The user does not exist");
            }
            //检查用户是否允许抢单,订单数 是否超过上限
            String isBanned = user.getIsBanned();
            Long taskProgress = user.getTaskProgress();
            GoodsMemberLevel memberLevel = user.getMemberLevel();
            Long orderCountPerDay = memberLevel.getOrderCountPerDay();
            if (isBanned.equals("0")||taskProgress >= orderCountPerDay){
                return AjaxResult.error(905, "This user is not allowed to grab orders");
            }
            //检查用户余额是否满足最低交易额度
            Optional<Object> minTradeAmountOpt = orderConfigService.getConfigValue("trade", "minTradeBalance");
            if (minTradeAmountOpt.isEmpty()) {
                return AjaxResult.error(901, "System configuration is not available");
            }
            BigDecimal bigDecimal = new BigDecimal((Integer) minTradeAmountOpt.get());
            if (user.getBalance().compareTo(bigDecimal) < 0) {
                return AjaxResult.error(906, "The minimum transaction amount is insufficient");
            }
            //检查是否有未完成订单
            int i = orderInfoService.hasOpenOrders(user.getId());
            if (i > 0) {
                return AjaxResult.error(907, "There is an open order");
            }
            //创建订单 检查用户是否存在连单
            List<OrderLink> orderLinks = orderLinkService.selectOrderLinkByUserId(user.getId(), taskProgress);
            if (orderLinks.isEmpty()) {
                //随机获取一个商品信息
                Optional<Object> matchRange = orderConfigService.getConfigValue("trade", "matchRangePercentage");
                if (matchRange.isEmpty()) {
                    return AjaxResult.error(908, "Please try again later (system busy)");
                }else{
                    String parts = (String)matchRange.get();
                    String[] split = parts.split("-");
                    BigDecimal percentRaw = randomBetween(new BigDecimal(split[0]), new BigDecimal(split[1]), 2);
                    BigDecimal percent = percentRaw.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                    BigDecimal amount = user.getBalance().multiply(percent).setScale(4, RoundingMode.DOWN);
                    Goods goods = goodsService.selectNearestPriceGoods(amount);
                    if (goods == null) {
                        return AjaxResult.error(911, "No suitable product or insufficient balance");
                    }
                    //商品价格
                    BigDecimal price = goods.getPrice();
                    //计算返利比例
                    BigDecimal minCommissionRate1 = memberLevel.getMinCommissionRate();
                    BigDecimal minCommissionRate = minCommissionRate1.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                    //返利金额
                    BigDecimal multiply = price.multiply(minCommissionRate);
                    //上级返利比例
                    Optional<Object> parentRebate = orderConfigService.getConfigValue("trade", "parentRebatePercentage");
                    //上级返利金额
                    BigDecimal parentRebateRate = new BigDecimal((Integer) parentRebate.get());
                    BigDecimal parentRebateRes = parentRebateRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                    BigDecimal parentRebateAmount = multiply.multiply(parentRebateRes);
                    //创建订单信息
                    OrderInfo orderInfo = createOrderInfo(user.getId(), "0", user.getTaskProgress() + 1, price, goods.getId(), minCommissionRate1, multiply,
                            parentRebateRate, parentRebateAmount, null,
                            null, "1");
                    //记录账变信息
                    BigDecimal balance = user.getBalance();
                    BigDecimal after = balance.subtract(price);
                    user.setBalance(after);
                    user.setFrozenBalance(user.getFrozenBalance().add(price));
                    user.setTaskProgress(user.getTaskProgress() + 1);
                    transactionService.recordFlow(user.getId(),"rw",new BigDecimal(0).subtract(price),balance,"-");
                    orderUserService.updateOrderUser(user);
                    orderInfoService.insertOrderInfo(orderInfo);
                    return success(orderInfo);
                }
            }else{
                //连单处理
                OrderLink orderLink = orderLinks.get(0);
                BigDecimal price = orderLink.getPrice();
                //计算返利比例倍数
                int Rate = orderLink.getCommissionMultiple();
                BigDecimal minCommissionRate1 = memberLevel.getMinCommissionRate();
                BigDecimal minCommissionRate = minCommissionRate1.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                //返利金额
                BigDecimal multiply = price.multiply(minCommissionRate.multiply(new BigDecimal(Rate)));
                //上级返利比例
                Optional<Object> parentRebate = orderConfigService.getConfigValue("trade", "parentRebatePercentage");
                //上级返利金额
                BigDecimal bigDecimal1 = new BigDecimal((Integer) parentRebate.get());
                BigDecimal parentRebateRate = bigDecimal1.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                BigDecimal parentRebateAmount = multiply.multiply(parentRebateRate);

                if (orderLink.getPriceType().equals("1")) {
                    price = price.add(user.getBalance());
                }
                //创建订单信息
                OrderInfo orderInfo = createOrderInfo(user.getId(), "1", user.getTaskProgress(), price, orderLink.getProductId(),minCommissionRate1, multiply,
                        bigDecimal1, parentRebateAmount, orderLink.getId(),
                        orderLink.getId(), "1");
                //记录账变信息
                BigDecimal balance = user.getBalance();
                BigDecimal after = balance.subtract(price);
                user.setBalance(after);
                user.setFrozenBalance(user.getFrozenBalance().add(price));
                transactionService.recordFlow(user.getId(),"rw",new BigDecimal(0).subtract(price),balance,"-");
                orderUserService.updateOrderUser(user);
                orderInfoService.insertOrderInfo(orderInfo);
                //连单改为待提交
                orderLink.setStatus("1");
                orderLinkService.updateOrderLink(orderLink);
                return success(orderInfo);
            }
        } catch (Exception e) {
                throw new RuntimeException(e);
        } finally {
            // 释放锁
            String currentValue = redisTemplate.opsForValue().get(lockKey);
            if (lockValue.equals(currentValue)) {
                redisTemplate.delete(lockKey);
            }
        }
    }

    @Transactional
    @GetMapping("/submitOrder/{id}")
    @Operation(summary = "提交订单", description = "返回code说明，201：如果用户存在连单 会返回新的订单信息，2007：用户余额不足，200：表示用户无新的订单，且订单提交成功。")
    public AjaxResult submitOrder(@PathVariable("id") Long id, @RequestAttribute("username") String username){
        String lockKey = "submit_lock:" + id;
        String lockValue = java.util.UUID.randomUUID().toString();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, 5, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            logger.warn("订单{}提交被锁，请稍后重试", id);
            return AjaxResult.error(918, "The order status does not allow submission (system busy)");  // 复用918，避免暴露锁
        }
        try{

            SysTimeZone active = sysTimeZoneService.getActive();
            Optional<Object> configValue = orderConfigService.getConfigValue("trade", "tradeTimeRange");
            boolean currentTimeInRange = TimeRangeChecker.isCurrentTimeInRange(configValue,active.getTzName());
            if (!currentTimeInRange) {
                return AjaxResult.error(902, "Not within the time frame for grabbing orders");
            }
            //查询用户信息
            OrderUser user = orderUserService.selectOrderUserByName(username);
            if (user == null) {
                return AjaxResult.error(904, "The user does not exist");
            }
            OrderInfo orderInfo = orderInfoService.selectOrderInfoById(id);
            if (orderInfo == null) {
                return AjaxResult.error(913, "Invalid orders");
            }
            Goods goods = goodsService.selectGoodsById(orderInfo.getProductId());
            if (goods == null) {
                return AjaxResult.error(913, "Invalid orders");
            }
            if (user.getBalance().compareTo(BigDecimal.ZERO) < 0) {
                return AjaxResult.error(916, "The balance is insufficient");
            }
            if (!orderInfo.getStatus().equals("1")) {
                return AjaxResult.error(918, "The order status does not allow submission");
            }
            //更新订单状态
            orderInfo.setStatus("0");
            orderInfoService.updateOrderInfo(orderInfo);
            //更新用户余额
            BigDecimal amount = orderInfo.getAmount();
            BigDecimal balance = user.getBalance();
            BigDecimal after = balance.add(amount);
            user.setBalance(after);
            user.setFrozenBalance(user.getFrozenBalance().subtract(amount));
            transactionService.recordFlow(user.getId(),"bjfh",amount,balance,"-");
            //返佣
            BigDecimal rebate = orderInfo.getRebate();
            BigDecimal userBalance = user.getBalance();
            user.setBalance(userBalance.add(rebate));
            transactionService.recordFlow(user.getId(),"fy",rebate,userBalance,"-");
            orderUserService.updateOrderUser(user);
            //上级返佣
            OrderUser parentUser = orderUserService.selectOrderUserById(user.getParentId());
            if (StringUtils.isNotNull(parentUser)) {
                BigDecimal upperRebate = orderInfo.getUpperRebate();
                if (upperRebate.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal parentBalance = parentUser.getBalance();
                    parentUser.setBalance(parentBalance.add(upperRebate));
                    transactionService.recordFlow(parentUser.getId(),"xjfy",upperRebate,parentBalance,"-");
                    orderUserService.updateOrderUser(parentUser);
                }
            }
            if (orderInfo.getType().equals("1")) {
                OrderLink orderLink = orderLinkService.selectOrderLinkById(orderInfo.getLinkId());
                if (orderLink != null) {
                    orderLink.setStatus("0");
                    orderLinkService.updateOrderLink(orderLink);
                }
            }
            return success();
        } catch (Exception e) {
                throw new RuntimeException(e);
        }finally {
            // 释放锁
            String currentValue = redisTemplate.opsForValue().get(lockKey);
            if (lockValue.equals(currentValue)) {
                redisTemplate.delete(lockKey);
            }
        }
    }
    @GetMapping("/getOrderInfos")
    @Operation(summary = "获取用户订单记录")
    public TableDataInfo getOrderInfos(WithdrawalPage page, @RequestAttribute("username") String username){
        OrderUser user = orderUserService.selectOrderUserByName(username);
        PageHelper.startPage(page.getPageNum(), page.getPageSize());
        OrderInfo orderInfo= new OrderInfo();
        if (!StringUtils.isEmpty(page.getStatus())){
            orderInfo.setStatus(page.getStatus());
        }
        List<OrderInfo> orderInfos = orderInfoService.selectOrderInfoList(orderInfo);
        return getDataTable(orderInfos);
    }


    public BigDecimal randomBetween(BigDecimal min, BigDecimal max, int scale) {
        if (min.compareTo(max) > 0) { BigDecimal t = min; min = max; max = t; }
        BigDecimal range = max.subtract(min);
        BigDecimal r = BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble()); // [0,1)
        return min.add(range.multiply(r)).setScale(scale, RoundingMode.HALF_UP);
    }

    /**
     * 创建订单信息对象
     * @param userId 用户id
     * @param type 类型 0正常 1连单
     * @param orderCount 单数
     * @param amount 金额
     * @param productId 商品id
     * @param rebatePercentage 返利百分比
     * @param rebate 返利金额
     * @param upperRebatePercentage 上级返利百分比
     * @param upperRebate 上级返利金额
     * @param extraCommissionId 额外佣金id
     * @param linkId 连单id
     * @param status 状态 订单状态 0完成 1待提交 2已冻结
     * @return
     */
    public OrderInfo createOrderInfo(Long userId,String type,Long orderCount,BigDecimal amount,
                                     Long productId,BigDecimal rebatePercentage,BigDecimal rebate,
                                     BigDecimal upperRebatePercentage,BigDecimal upperRebate,Long extraCommissionId,Long linkId,String status) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderNumber(orderSequenceManagerService.generateCode("TRADE_NO"));
        orderInfo.setUserId(userId);
        orderInfo.setType(type);
        orderInfo.setOrderCount(orderCount);
        orderInfo.setAmount(amount);
        orderInfo.setProductId(productId);
        orderInfo.setRebatePercentage(rebatePercentage);
        orderInfo.setRebate(rebate);
        orderInfo.setUpperRebatePercentage(upperRebatePercentage);
        orderInfo.setUpperRebate(upperRebate);
        orderInfo.setExtraCommissionId(extraCommissionId);
        orderInfo.setLinkId(linkId);
        orderInfo.setStatus(status);
        return orderInfo;
    }

}
