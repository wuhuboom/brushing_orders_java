package com.order.api.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.order.api.controller.dto.OrderApiDtos.BonusClaimResponse;
import com.order.api.controller.dto.OrderApiDtos.CreationResult;
import com.order.api.controller.dto.OrderApiDtos.OrderResponse;
import com.order.api.controller.dto.OrderApiDtos.ResultType;
import com.order.api.controller.dto.OrderApiDtos.SubmitResponse;
import com.order.common.core.page.TableDataInfo;
import com.order.common.utils.DateUtils;
import com.order.member.domain.Goods;
import com.order.member.domain.GoodsMemberLevel;
import com.order.member.domain.OrderApiRequest;
import com.order.member.domain.OrderBonusTable;
import com.order.member.domain.OrderInfo;
import com.order.member.domain.OrderLink;
import com.order.member.domain.OrderUser;
import com.order.member.mapper.GoodsMapper;
import com.order.member.mapper.OrderApiRequestMapper;
import com.order.member.mapper.OrderBonusTableMapper;
import com.order.member.mapper.OrderInfoMapper;
import com.order.member.mapper.OrderLinkMapper;
import com.order.member.mapper.OrderUserMapper;
import com.order.member.service.IOrderSequenceManagerService;
import com.order.member.service.ITransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.order.api.service.OrderErrorCodes.BONUS_UNAVAILABLE;
import static com.order.api.service.OrderErrorCodes.INSUFFICIENT_BALANCE;
import static com.order.api.service.OrderErrorCodes.INVALID_BONUS;
import static com.order.api.service.OrderErrorCodes.INVALID_CONFIG;
import static com.order.api.service.OrderErrorCodes.INVALID_MEMBER_LEVEL;
import static com.order.api.service.OrderErrorCodes.INVALID_ORDER;
import static com.order.api.service.OrderErrorCodes.INVALID_REQUEST;
import static com.order.api.service.OrderErrorCodes.INVALID_USER;
import static com.order.api.service.OrderErrorCodes.MINIMUM_BALANCE;
import static com.order.api.service.OrderErrorCodes.NO_SUITABLE_PRODUCT;
import static com.order.api.service.OrderErrorCodes.ORDER_STATE_CONFLICT;
import static com.order.api.service.OrderErrorCodes.SYSTEM_BUSY;
import static com.order.api.service.OrderErrorCodes.USER_NOT_FOUND;
import static com.order.api.service.OrderErrorCodes.WORK_NOT_ALLOWED;

@Service
public class OrderApplicationService {
    private static final Logger log = LoggerFactory.getLogger(OrderApplicationService.class);
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final int MONEY_SCALE = 2;
    private static final String CREATE_OPERATION = "CREATE_ORDER";

    private final OrderUserMapper userMapper;
    private final OrderInfoMapper orderMapper;
    private final OrderApiRequestMapper requestMapper;
    private final OrderLinkMapper linkMapper;
    private final OrderBonusTableMapper bonusMapper;
    private final GoodsMapper goodsMapper;
    private final OrderTradePolicyService policyService;
    private final IOrderSequenceManagerService sequenceService;
    private final ITransactionService transactionService;

    public OrderApplicationService(
            OrderUserMapper userMapper,
            OrderInfoMapper orderMapper,
            OrderApiRequestMapper requestMapper,
            OrderLinkMapper linkMapper,
            OrderBonusTableMapper bonusMapper,
            GoodsMapper goodsMapper,
            OrderTradePolicyService policyService,
            IOrderSequenceManagerService sequenceService,
            ITransactionService transactionService) {
        this.userMapper = userMapper;
        this.orderMapper = orderMapper;
        this.requestMapper = requestMapper;
        this.linkMapper = linkMapper;
        this.bonusMapper = bonusMapper;
        this.goodsMapper = goodsMapper;
        this.policyService = policyService;
        this.sequenceService = sequenceService;
        this.transactionService = transactionService;
    }

    @Transactional(rollbackFor = Exception.class)
    public CreationResult create(Long userId) {
        return create(userId, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public CreationResult create(Long userId, String requestId) {
        requireUserId(userId);
        String normalizedRequestId = normalizeRequestId(requestId);

        if (userMapper.lockUserById(userId) == null) {
            throw OrderApiException.notFound(USER_NOT_FOUND, "The user does not exist");
        }
        CreationResult previous = restoreCreateResult(userId, normalizedRequestId);
        if (previous != null) {
            return previous;
        }
        OrderUser user = userMapper.selectOrderTaskUserById(userId);
        if (user == null) {
            throw OrderApiException.notFound(USER_NOT_FOUND, "The user does not exist");
        }

        OrderInfo openOrder = orderMapper.hasOpenOrders(userId);
        if (openOrder != null) {
            CreationResult result = CreationResult.order(openOrder);
            rememberCreateResult(
                    userId, normalizedRequestId, result.resultType().name(), openOrder.getId());
            return result;
        }

        OrderTradePolicyService.TradePolicy policy = policyService.activePolicy();
        validateCreateUser(user, policy);
        long taskProgress = valueOrZero(user.getTaskProgress());
        long nextOrderIndex = Math.addExact(taskProgress, 1L);
        OrderBonusTable bonus = bonusMapper.selectActiveDistributedReceivedByUserAndOrder(
                userId, nextOrderIndex);
        if (bonus == null
                && nextOrderIndex >= user.getMemberLevel().getOrderCountPerDay()) {
            bonus = bonusMapper.selectNextCompletionBonus(userId);
        }
        if (bonus != null) {
            CreationResult result = CreationResult.bonus(bonus);
            rememberCreateResult(
                    userId, normalizedRequestId, result.resultType().name(), bonus.getId());
            return result;
        }

        OrderLink link = linkMapper.selectNextOrderLink(userId, taskProgress);
        PreparedOrder prepared = link == null
                ? prepareNormalOrder(user, policy, nextOrderIndex)
                : prepareLinkedOrder(user, policy, link);

        OrderInfo order = prepared.order();
        if (orderMapper.insertOrderInfo(order) != 1) {
            throw new IllegalStateException("Unable to create order");
        }
        if (userMapper.reserveOrderFunds(
                userId,
                order.getAmount(),
                prepared.progressDelta(),
                prepared.allowNegative()) != 1) {
            throw OrderApiException.conflict(
                    INSUFFICIENT_BALANCE,
                    "The balance is insufficient or was updated concurrently");
        }
        transactionService.recordFlow(
                userId,
                "rw",
                order.getAmount().negate(),
                user.getBalance(),
                "order-reserve:" + order.getOrderNumber());
        rememberCreateResult(
                userId, normalizedRequestId, ResultType.ORDER.name(), order.getId());
        log.info("event=order_created userId={} orderId={} type={}",
                userId, order.getId(), order.getType());
        return CreationResult.order(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public SubmitResponse submit(Long userId, Long orderId) {
        requireUserId(userId);
        if (orderId == null || orderId <= 0) {
            throw OrderApiException.badRequest(INVALID_REQUEST, "Invalid order id");
        }

        if (userMapper.lockUserById(userId) == null) {
            throw OrderApiException.notFound(INVALID_USER, "Invalid users");
        }
        OrderInfo order = orderMapper.selectOwnedOrderForUpdate(orderId, userId);
        if (order == null) {
            throw OrderApiException.notFound(INVALID_ORDER, "Invalid orders");
        }
        if ("0".equals(order.getStatus())) {
            return new SubmitResponse(orderId, "0", true);
        }
        if (!"1".equals(order.getStatus())) {
            throw OrderApiException.conflict(
                    ORDER_STATE_CONFLICT,
                    "The order status does not allow submission");
        }

        // Retain the existing rule that a pending order can only be submitted
        // during an active, valid trade window. Completed retries stay idempotent.
        policyService.activePolicy();
        OrderUser user = userMapper.selectOrderBalanceById(userId);
        validateSettlement(user, order);

        if (orderMapper.transitionStatus(orderId, userId, "1", "0") != 1) {
            throw OrderApiException.conflict(
                    ORDER_STATE_CONFLICT,
                    "The order status was updated concurrently");
        }
        if (userMapper.settleOrderFunds(
                userId, order.getAmount(), order.getRebate()) != 1) {
            throw OrderApiException.conflict(
                    INSUFFICIENT_BALANCE,
                    "The balance is insufficient or frozen funds are inconsistent");
        }

        // Acquire every user row before the first sequence/flow write. This
        // avoids a sequence-lock -> parent-user-lock inversion.
        creditParentCommission(user, order);
        BigDecimal principalBalance = money(user.getBalance().add(order.getAmount()));
        transactionService.recordFlow(
                userId,
                "bjfh",
                order.getAmount(),
                user.getBalance(),
                "order-principal:" + order.getOrderNumber());
        transactionService.recordFlow(
                userId,
                "fy",
                order.getRebate(),
                principalBalance,
                "order-rebate:" + order.getOrderNumber());

        if ("1".equals(order.getType()) && order.getLinkId() != null
                && linkMapper.completeOrderLink(order.getLinkId(), userId) != 1) {
            throw new IllegalStateException("Unable to complete linked order");
        }
        log.info("event=order_submitted userId={} orderId={}", userId, orderId);
        return new SubmitResponse(orderId, "0", false);
    }

    @Transactional(rollbackFor = Exception.class)
    public BonusClaimResponse claimBonus(Long userId, Long bonusId) {
        requireUserId(userId);
        if (bonusId == null || bonusId <= 0) {
            throw OrderApiException.badRequest(INVALID_REQUEST, "Invalid bonus id");
        }

        OrderBonusTable bonus = bonusMapper.selectOwnedBonusForUpdate(bonusId, userId);
        if (bonus == null) {
            throw OrderApiException.notFound(INVALID_BONUS, "Invalid bonus");
        }
        if ("0".equals(bonus.getIsReceived())) {
            OrderUser current = userMapper.selectOrderBalanceById(userId);
            return new BonusClaimResponse(
                    bonusId,
                    moneyOrZero(bonus.getAmount()),
                    current == null ? null : current.getBalance(),
                    true);
        }
        if (!"1".equals(bonus.getIsDistributed())
                || bonus.getExpiryTime() != null
                && bonus.getExpiryTime().toInstant().isBefore(Instant.now())) {
            throw OrderApiException.conflict(BONUS_UNAVAILABLE, "Bonus is expired or unavailable");
        }
        BigDecimal amount = moneyOrZero(bonus.getAmount());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw OrderApiException.conflict(BONUS_UNAVAILABLE, "Bonus is unavailable");
        }

        if (userMapper.lockUserById(userId) == null) {
            throw OrderApiException.notFound(INVALID_USER, "Invalid users");
        }
        OrderUser user = userMapper.selectOrderBalanceById(userId);
        if (user == null || user.getBalance() == null) {
            throw OrderApiException.notFound(INVALID_USER, "Invalid users");
        }
        if (bonusMapper.claimBonus(bonusId, userId) != 1) {
            throw OrderApiException.conflict(BONUS_UNAVAILABLE, "Bonus was claimed concurrently");
        }
        if (userMapper.creditBalance(userId, amount) != 1) {
            throw new IllegalStateException("Unable to credit bonus");
        }
        transactionService.recordFlow(
                userId,
                "bonus",
                amount,
                user.getBalance(),
                "order-bonus:" + bonusId);
        BigDecimal balance = money(user.getBalance().add(amount));
        log.info("event=order_bonus_claimed userId={} bonusId={}", userId, bonusId);
        return new BonusClaimResponse(bonusId, amount, balance, false);
    }

    @Transactional(readOnly = true)
    public TableDataInfo orders(Long userId, String status, int pageNum, int pageSize) {
        requireUserId(userId);
        String normalizedStatus = normalizeStatus(status);
        PageHelper.startPage(
                ApiPagination.pageNumber(pageNum),
                ApiPagination.pageSize(pageSize));
        List<OrderInfo> rows = orderMapper.selectPublicOrderInfoList(userId, normalizedStatus);
        long total = new PageInfo<>(rows).getTotal();
        List<OrderResponse> result = rows.stream().map(OrderResponse::from).toList();
        TableDataInfo page = new TableDataInfo();
        page.setCode(200);
        page.setMsg("Query successful");
        page.setRows(result);
        page.setTotal(total);
        return page;
    }

    private PreparedOrder prepareNormalOrder(
            OrderUser user,
            OrderTradePolicyService.TradePolicy policy,
            long nextOrderIndex) {
        BigDecimal percentage = randomPercentage(policy.matchRange());
        BigDecimal targetPrice = user.getBalance()
                .multiply(percentage)
                .divide(ONE_HUNDRED, MONEY_SCALE, RoundingMode.DOWN);
        Goods goods = goodsMapper.selectNearestPriceGoods(targetPrice);
        if (goods == null || goods.getPrice() == null) {
            throw OrderApiException.conflict(
                    NO_SUITABLE_PRODUCT,
                    "No suitable product or insufficient balance");
        }
        return new PreparedOrder(
                newOrder(
                        user,
                        policy,
                        "0",
                        nextOrderIndex,
                        money(goods.getPrice()),
                        money(goods.getPrice()),
                        goods,
                        BigDecimal.ONE,
                        null),
                1L,
                false);
    }

    private PreparedOrder prepareLinkedOrder(
            OrderUser user,
            OrderTradePolicyService.TradePolicy policy,
            OrderLink link) {
        if (link.getPrice() == null
                || link.getPrice().compareTo(BigDecimal.ZERO) <= 0
                || link.getCommissionMultiple() == null
                || link.getCommissionMultiple() <= 0) {
            throw OrderApiException.unavailable(INVALID_CONFIG, "Invalid linked order configuration");
        }
        Goods goods = goodsMapper.selectOrderGoodsById(link.getProductId());
        if (goods == null) {
            throw OrderApiException.conflict(NO_SUITABLE_PRODUCT, "Linked order product is unavailable");
        }
        BigDecimal basePrice = money(link.getPrice());
        BigDecimal amount = "1".equals(link.getPriceType())
                ? money(basePrice.add(user.getBalance()))
                : basePrice;
        return new PreparedOrder(
                newOrder(
                        user,
                        policy,
                        "1",
                        valueOrZero(user.getTaskProgress()),
                        amount,
                        basePrice,
                        goods,
                        BigDecimal.valueOf(link.getCommissionMultiple()),
                        link.getId()),
                0L,
                true);
    }

    private OrderInfo newOrder(
            OrderUser user,
            OrderTradePolicyService.TradePolicy policy,
            String type,
            long orderCount,
            BigDecimal amount,
            BigDecimal commissionBase,
            Goods goods,
            BigDecimal commissionMultiple,
            Long linkId) {
        BigDecimal commissionPercentage = percentage(user.getMemberLevel().getMinCommissionRate());
        BigDecimal rebate = money(
                commissionBase.multiply(commissionPercentage)
                        .multiply(commissionMultiple)
                        .divide(ONE_HUNDRED, 8, RoundingMode.HALF_UP));
        BigDecimal parentPercentage = percentage(policy.parentRebatePercentage());
        BigDecimal upperRebate = money(
                rebate.multiply(parentPercentage)
                        .divide(ONE_HUNDRED, 8, RoundingMode.HALF_UP));

        OrderInfo order = new OrderInfo();
        order.setOrderNumber(sequenceService.generateCode("TRADE_NO"));
        order.setUserId(user.getId());
        order.setType(type);
        order.setOrderCount(orderCount);
        order.setAmount(amount);
        order.setProductId(goods.getId());
        order.setProductImage(goods.getImage());
        order.setProductTitle(goods.getTitle());
        order.setRebatePercentage(commissionPercentage);
        order.setRebate(rebate);
        order.setUpperRebatePercentage(parentPercentage);
        order.setUpperRebate(upperRebate);
        order.setExtraCommissionId(linkId);
        order.setLinkId(linkId);
        order.setStatus("1");
        order.setCreateTime(DateUtils.getNowDate());
        return order;
    }

    private void creditParentCommission(OrderUser user, OrderInfo order) {
        Long parentId = user.getParentId();
        BigDecimal commission = moneyOrZero(order.getUpperRebate());
        if (parentId == null || parentId <= 0 || commission.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        if (userMapper.lockUserById(parentId) == null) {
            return;
        }
        OrderUser parent = userMapper.selectOrderBalanceById(parentId);
        if (parent == null || parent.getBalance() == null) {
            throw new IllegalStateException("Invalid parent balance");
        }
        if (userMapper.creditBalance(parentId, commission) != 1) {
            throw new IllegalStateException("Unable to credit parent commission");
        }
        transactionService.recordFlow(
                parentId,
                "xjfy",
                commission,
                parent.getBalance(),
                "child-order-rebate:" + order.getOrderNumber());
    }

    private void validateCreateUser(
            OrderUser user,
            OrderTradePolicyService.TradePolicy policy) {
        if (user == null) {
            throw OrderApiException.notFound(USER_NOT_FOUND, "The user does not exist");
        }
        if (user.getBalance() == null || user.getFrozenBalance() == null) {
            throw OrderApiException.unavailable(INVALID_USER, "Invalid users");
        }
        GoodsMemberLevel level = user.getMemberLevel();
        if (level == null
                || level.getId() == null
                || level.getOrderCountPerDay() == null
                || level.getOrderCountPerDay() <= 0
                || level.getMinCommissionRate() == null) {
            throw OrderApiException.unavailable(
                    INVALID_MEMBER_LEVEL,
                    "Invalid membership tier configuration");
        }
        long taskProgress = valueOrZero(user.getTaskProgress());
        if ("0".equals(user.getIsBanned()) || taskProgress >= level.getOrderCountPerDay()) {
            throw OrderApiException.forbidden(
                    WORK_NOT_ALLOWED,
                    "This user is not allowed to grab orders");
        }
        if (user.getBalance().compareTo(policy.minimumBalance()) < 0) {
            throw OrderApiException.forbidden(
                    MINIMUM_BALANCE,
                    "The minimum transaction amount is insufficient");
        }
        percentage(level.getMinCommissionRate());
    }

    private void validateSettlement(OrderUser user, OrderInfo order) {
        if (user == null || user.getBalance() == null || user.getFrozenBalance() == null) {
            throw OrderApiException.notFound(INVALID_USER, "Invalid users");
        }
        if (order.getAmount() == null
                || order.getAmount().compareTo(BigDecimal.ZERO) <= 0
                || order.getRebate() == null
                || order.getRebate().compareTo(BigDecimal.ZERO) < 0) {
            throw OrderApiException.conflict(ORDER_STATE_CONFLICT, "Invalid order amount");
        }
        order.setAmount(money(order.getAmount()));
        order.setRebate(money(order.getRebate()));
        order.setUpperRebate(moneyOrZero(order.getUpperRebate()));
        if (user.getBalance().compareTo(BigDecimal.ZERO) < 0
                || user.getFrozenBalance().compareTo(order.getAmount()) < 0) {
            throw OrderApiException.conflict(
                    INSUFFICIENT_BALANCE,
                    "The balance is insufficient or frozen funds are inconsistent");
        }
    }

    private BigDecimal percentage(BigDecimal value) {
        if (value == null
                || value.compareTo(BigDecimal.ZERO) < 0
                || value.compareTo(ONE_HUNDRED) > 0) {
            throw OrderApiException.unavailable(INVALID_CONFIG, "Invalid commission configuration");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal randomPercentage(OrderTradePolicyService.PercentageRange range) {
        long minimum = range.minimum()
                .movePointRight(2)
                .setScale(0, RoundingMode.CEILING)
                .longValueExact();
        long maximum = range.maximum()
                .movePointRight(2)
                .setScale(0, RoundingMode.FLOOR)
                .longValueExact();
        long selected = minimum == maximum
                ? minimum
                : ThreadLocalRandom.current().nextLong(minimum, maximum + 1);
        return BigDecimal.valueOf(selected, 2);
    }

    private String normalizeStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (!List.of("0", "1", "2").contains(value)) {
            throw OrderApiException.badRequest(INVALID_REQUEST, "Invalid order status");
        }
        return value;
    }

    private String normalizeRequestId(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isBlank()
                || normalized.length() < 8
                || normalized.length() > 64
                || !normalized.matches("[A-Za-z0-9._:-]+")) {
            throw OrderApiException.badRequest(
                    INVALID_REQUEST,
                    "Invalid Idempotency-Key");
        }
        return normalized;
    }

    private CreationResult restoreCreateResult(Long userId, String requestId) {
        if (requestId == null) {
            return null;
        }
        OrderApiRequest request = requestMapper.selectByRequestKey(
                userId, CREATE_OPERATION, requestId);
        if (request == null) {
            return null;
        }
        if (ResultType.ORDER.name().equals(request.getResultType())) {
            OrderInfo order = orderMapper.selectPublicOrderById(
                    request.getResultId(), userId);
            if (order != null) {
                return CreationResult.order(order);
            }
        } else if (ResultType.BONUS.name().equals(request.getResultType())) {
            OrderBonusTable bonus = bonusMapper.selectOrderBonusTableById(
                    request.getResultId());
            if (bonus != null && userId.equals(bonus.getUserId())) {
                return CreationResult.bonus(bonus);
            }
        }
        throw OrderApiException.conflict(
                SYSTEM_BUSY,
                "The original idempotent order result is unavailable");
    }

    private void rememberCreateResult(
            Long userId,
            String requestId,
            String resultType,
            Long resultId) {
        if (requestId == null) {
            return;
        }
        if (resultId == null) {
            throw new IllegalStateException("Idempotent order result id is missing");
        }
        OrderApiRequest request = new OrderApiRequest();
        request.setUserId(userId);
        request.setOperationType(CREATE_OPERATION);
        request.setRequestId(requestId);
        request.setResultType(resultType);
        request.setResultId(resultId);
        request.setCreateTime(DateUtils.getNowDate());
        if (requestMapper.insertOrderApiRequest(request) != 1) {
            throw new IllegalStateException("Unable to persist order idempotency result");
        }
    }

    private void requireUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw OrderApiException.notFound(USER_NOT_FOUND, "The user does not exist");
        }
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal moneyOrZero(BigDecimal value) {
        return money(value == null ? BigDecimal.ZERO : value);
    }

    private record PreparedOrder(OrderInfo order, long progressDelta, boolean allowNegative) {
    }
}
