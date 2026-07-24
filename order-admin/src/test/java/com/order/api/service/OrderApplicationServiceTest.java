package com.order.api.service;

import com.order.api.controller.dto.OrderApiDtos.ResultType;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static com.order.api.service.OrderErrorCodes.INVALID_BONUS;
import static com.order.api.service.OrderErrorCodes.INVALID_ORDER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderApplicationServiceTest {
    @Mock OrderUserMapper userMapper;
    @Mock OrderInfoMapper orderMapper;
    @Mock OrderApiRequestMapper requestMapper;
    @Mock OrderLinkMapper linkMapper;
    @Mock OrderBonusTableMapper bonusMapper;
    @Mock GoodsMapper goodsMapper;
    @Mock OrderTradePolicyService policyService;
    @Mock IOrderSequenceManagerService sequenceService;
    @Mock ITransactionService transactionService;

    @Test
    void createsNormalOrderWithSnapshotsAndTwoDecimalMoney() {
        OrderUser user = user(new BigDecimal("100.00"), 1L);
        Goods goods = goods(31L, new BigDecimal("50.005"));
        arrangeCreate(user);
        when(goodsMapper.selectNearestPriceGoods(new BigDecimal("50.00"))).thenReturn(goods);
        when(sequenceService.generateCode("TRADE_NO")).thenReturn("O-100");
        doAnswer(invocation -> {
            OrderInfo value = invocation.getArgument(0);
            value.setId(100L);
            return 1;
        }).when(orderMapper).insertOrderInfo(any(OrderInfo.class));
        when(userMapper.reserveOrderFunds(7L, new BigDecimal("50.01"), 1L, false)).thenReturn(1);

        var result = service().create(7L);

        assertEquals(ResultType.ORDER, result.resultType());
        assertEquals(new BigDecimal("50.01"), result.order().amount());
        assertEquals(new BigDecimal("0.51"), result.order().rebate());
        assertEquals("Product snapshot", result.order().productTitle());
        assertEquals("/snapshot.jpg", result.order().productImage());
        verify(transactionService).recordFlow(
                7L, "rw", new BigDecimal("-50.01"), new BigDecimal("100.00"),
                "order-reserve:O-100");
    }

    @Test
    void returnsExistingPendingOrderWithoutCreatingAnotherOne() {
        OrderUser user = user(new BigDecimal("-20.00"), 40L);
        user.setIsBanned("0");
        OrderInfo existing = order(20L, "1");
        when(userMapper.lockUserById(7L)).thenReturn(7L);
        when(userMapper.selectOrderTaskUserById(7L)).thenReturn(user);
        when(orderMapper.hasOpenOrders(7L)).thenReturn(existing);

        var result = service().create(7L);

        assertEquals(20L, result.order().id());
        verify(policyService, never()).activePolicy();
        verify(bonusMapper, never())
                .selectActiveDistributedReceivedByUserAndOrder(any(), any());
        verify(orderMapper, never()).insertOrderInfo(any());
        verify(userMapper, never()).reserveOrderFunds(any(), any(), any(Long.class), any(Boolean.class));
    }

    @Test
    void delayedCreateRetryReturnsOriginalCompletedOrderByRequestId() {
        OrderApiRequest request = new OrderApiRequest();
        request.setUserId(7L);
        request.setOperationType("CREATE_ORDER");
        request.setRequestId("request-12345678");
        request.setResultType("ORDER");
        request.setResultId(20L);
        OrderInfo completed = order(20L, "0");
        when(userMapper.lockUserById(7L)).thenReturn(7L);
        when(requestMapper.selectByRequestKey(
                7L, "CREATE_ORDER", "request-12345678")).thenReturn(request);
        when(orderMapper.selectPublicOrderById(20L, 7L)).thenReturn(completed);

        var result = service().create(7L, "request-12345678");

        assertEquals(20L, result.order().id());
        assertEquals("0", result.order().status());
        verify(userMapper, never()).selectOrderTaskUserById(any());
        verify(orderMapper, never()).hasOpenOrders(any());
        verify(orderMapper, never()).insertOrderInfo(any());
        verify(userMapper, never()).reserveOrderFunds(any(), any(), any(Long.class), any(Boolean.class));
        verify(transactionService, never()).recordFlow(any(), any(), any(), any(), any());
    }

    @Test
    void createPersistsRequestResultInTheSameOrderTransaction() {
        OrderUser user = user(new BigDecimal("100.00"), 1L);
        Goods goods = goods(31L, new BigDecimal("50.00"));
        arrangeCreate(user);
        when(goodsMapper.selectNearestPriceGoods(new BigDecimal("50.00"))).thenReturn(goods);
        when(sequenceService.generateCode("TRADE_NO")).thenReturn("O-IDEMPOTENT");
        doAnswer(invocation -> {
            OrderInfo value = invocation.getArgument(0);
            value.setId(100L);
            return 1;
        }).when(orderMapper).insertOrderInfo(any(OrderInfo.class));
        when(userMapper.reserveOrderFunds(
                7L, new BigDecimal("50.00"), 1L, false)).thenReturn(1);
        when(requestMapper.insertOrderApiRequest(any(OrderApiRequest.class)))
                .thenReturn(1);

        service().create(7L, "request-abcdefgh");

        verify(requestMapper).insertOrderApiRequest(
                org.mockito.ArgumentMatchers.argThat(request ->
                        request.getUserId().equals(7L)
                                && request.getRequestId().equals("request-abcdefgh")
                                && request.getResultType().equals("ORDER")
                                && request.getResultId().equals(100L)));
    }

    @Test
    void exactOrderBonusHasPriorityOverCompletionBonus() {
        OrderUser user = user(new BigDecimal("100.00"), 1L);
        OrderBonusTable exact = bonus(88L, "1");
        arrangeCreate(user);
        when(bonusMapper.selectActiveDistributedReceivedByUserAndOrder(7L, 2L))
                .thenReturn(exact);

        var result = service().create(7L);

        assertEquals(ResultType.BONUS, result.resultType());
        assertEquals(88L, result.bonus().id());
        verify(bonusMapper, never()).selectNextCompletionBonus(any());
        verify(orderMapper, never()).insertOrderInfo(any());
    }

    @Test
    void linkedOrderMayReserveMoreThanCurrentBalance() {
        OrderUser user = user(new BigDecimal("20.00"), 1L);
        OrderLink link = new OrderLink();
        link.setId(9L);
        link.setProductId(31L);
        link.setPrice(new BigDecimal("100.00"));
        link.setPriceType("1");
        link.setCommissionMultiple(2);
        Goods goods = goods(31L, new BigDecimal("10.00"));
        arrangeCreate(user);
        when(linkMapper.selectNextOrderLink(7L, 1L)).thenReturn(link);
        when(goodsMapper.selectOrderGoodsById(31L)).thenReturn(goods);
        when(sequenceService.generateCode("TRADE_NO")).thenReturn("O-LINK");
        when(orderMapper.insertOrderInfo(any())).thenReturn(1);
        when(userMapper.reserveOrderFunds(7L, new BigDecimal("120.00"), 0L, true)).thenReturn(1);

        var result = service().create(7L);

        assertEquals(new BigDecimal("120.00"), result.order().amount());
        assertEquals(new BigDecimal("2.02"), result.order().rebate());
        verify(userMapper).reserveOrderFunds(7L, new BigDecimal("120.00"), 0L, true);
    }

    @Test
    void cannotSubmitAnotherUsersOrder() {
        when(userMapper.lockUserById(7L)).thenReturn(7L);
        when(orderMapper.selectOwnedOrderForUpdate(99L, 7L)).thenReturn(null);

        OrderApiException error = assertThrows(
                OrderApiException.class, () -> service().submit(7L, 99L));

        assertEquals(INVALID_ORDER, error.getBusinessCode());
        verify(orderMapper, never()).transitionStatus(any(), any(), any(), any());
        verify(userMapper, never()).settleOrderFunds(any(), any(), any());
    }

    @Test
    void submitIsIdempotentAfterCompletion() {
        when(userMapper.lockUserById(7L)).thenReturn(7L);
        when(orderMapper.selectOwnedOrderForUpdate(20L, 7L)).thenReturn(order(20L, "0"));

        var result = service().submit(7L, 20L);

        assertTrue(result.alreadyCompleted());
        verify(userMapper, never()).settleOrderFunds(any(), any(), any());
    }

    @Test
    void successfulSubmitSettlesExactlyOnceAndReferencesOrderNumber() {
        OrderInfo order = order(20L, "1");
        OrderUser balance = user(new BigDecimal("50.00"), 1L);
        balance.setFrozenBalance(new BigDecimal("30.00"));
        when(userMapper.lockUserById(7L)).thenReturn(7L);
        when(orderMapper.selectOwnedOrderForUpdate(20L, 7L)).thenReturn(order);
        when(userMapper.selectOrderBalanceById(7L)).thenReturn(balance);
        when(orderMapper.transitionStatus(20L, 7L, "1", "0")).thenReturn(1);
        when(userMapper.settleOrderFunds(
                7L, new BigDecimal("30.00"), new BigDecimal("0.30"))).thenReturn(1);

        service().submit(7L, 20L);

        verify(transactionService).recordFlow(
                7L, "bjfh", new BigDecimal("30.00"), new BigDecimal("50.00"),
                "order-principal:O-20");
        verify(transactionService).recordFlow(
                7L, "fy", new BigDecimal("0.30"), new BigDecimal("80.00"),
                "order-rebate:O-20");
    }

    @Test
    void normalOrderCannotSettleWithNegativeAvailableBalance() {
        OrderInfo order = order(20L, "1");
        order.setType("0");
        OrderUser balance = user(new BigDecimal("-1.00"), 1L);
        balance.setFrozenBalance(new BigDecimal("30.00"));
        when(userMapper.lockUserById(7L)).thenReturn(7L);
        when(orderMapper.selectOwnedOrderForUpdate(20L, 7L)).thenReturn(order);
        when(userMapper.selectOrderBalanceById(7L)).thenReturn(balance);

        assertThrows(OrderApiException.class, () -> service().submit(7L, 20L));

        verify(orderMapper, never()).transitionStatus(any(), any(), any(), any());
        verify(userMapper, never()).settleOrderFunds(any(), any(), any());
    }

    @Test
    void parentUserIsLockedBeforeAnySettlementFlowIsWritten() {
        OrderInfo order = order(20L, "1");
        order.setType("0");
        order.setUpperRebate(new BigDecimal("0.10"));
        OrderUser child = user(new BigDecimal("50.00"), 1L);
        child.setFrozenBalance(new BigDecimal("30.00"));
        child.setParentId(8L);
        OrderUser parent = user(new BigDecimal("200.00"), 1L);
        parent.setId(8L);
        when(userMapper.lockUserById(7L)).thenReturn(7L);
        when(userMapper.lockUserById(8L)).thenReturn(8L);
        when(orderMapper.selectOwnedOrderForUpdate(20L, 7L)).thenReturn(order);
        when(userMapper.selectOrderBalanceById(7L)).thenReturn(child);
        when(userMapper.selectOrderBalanceById(8L)).thenReturn(parent);
        when(orderMapper.transitionStatus(20L, 7L, "1", "0")).thenReturn(1);
        when(userMapper.settleOrderFunds(
                7L, new BigDecimal("30.00"), new BigDecimal("0.30")))
                .thenReturn(1);
        when(userMapper.creditBalance(8L, new BigDecimal("0.10"))).thenReturn(1);

        service().submit(7L, 20L);

        InOrder lockBeforeFlow = inOrder(userMapper, transactionService);
        lockBeforeFlow.verify(userMapper).lockUserById(8L);
        lockBeforeFlow.verify(transactionService).recordFlow(
                8L,
                "xjfy",
                new BigDecimal("0.10"),
                new BigDecimal("200.00"),
                "child-order-rebate:O-20");
        lockBeforeFlow.verify(transactionService).recordFlow(
                7L,
                "bjfh",
                new BigDecimal("30.00"),
                new BigDecimal("50.00"),
                "order-principal:O-20");
    }

    @Test
    void cannotClaimAnotherUsersBonus() {
        when(bonusMapper.selectOwnedBonusForUpdate(88L, 7L)).thenReturn(null);

        OrderApiException error = assertThrows(
                OrderApiException.class, () -> service().claimBonus(7L, 88L));

        assertEquals(INVALID_BONUS, error.getBusinessCode());
        verify(userMapper, never()).creditBalance(any(), any());
    }

    @Test
    void bonusClaimIsIdempotentAndDoesNotCreditTwice() {
        OrderBonusTable claimed = bonus(88L, "0");
        OrderUser balance = user(new BigDecimal("128.88"), 1L);
        when(bonusMapper.selectOwnedBonusForUpdate(88L, 7L)).thenReturn(claimed);
        when(userMapper.selectOrderBalanceById(7L)).thenReturn(balance);

        var result = service().claimBonus(7L, 88L);

        assertTrue(result.alreadyClaimed());
        assertEquals(new BigDecimal("128.88"), result.balance());
        verify(userMapper, never()).creditBalance(any(), any());
        verify(transactionService, never()).recordFlow(any(), any(), any(), any(), any());
    }

    @Test
    void bonusClaimCreditsRoundedAmountAndWritesBonusFlow() {
        OrderBonusTable available = bonus(88L, "1");
        available.setAmount(new BigDecimal("28.885"));
        OrderUser balance = user(new BigDecimal("100.00"), 1L);
        when(bonusMapper.selectOwnedBonusForUpdate(88L, 7L)).thenReturn(available);
        when(userMapper.lockUserById(7L)).thenReturn(7L);
        when(userMapper.selectOrderBalanceById(7L)).thenReturn(balance);
        when(bonusMapper.claimBonus(88L, 7L)).thenReturn(1);
        when(userMapper.creditBalance(7L, new BigDecimal("28.89"))).thenReturn(1);

        var result = service().claimBonus(7L, 88L);

        assertEquals(new BigDecimal("128.89"), result.balance());
        verify(transactionService).recordFlow(
                7L, "bonus", new BigDecimal("28.89"), new BigDecimal("100.00"),
                "order-bonus:88");
    }

    private void arrangeCreate(OrderUser user) {
        when(policyService.activePolicy()).thenReturn(new OrderTradePolicyService.TradePolicy(
                new BigDecimal("10.00"),
                new BigDecimal("5.00"),
                new OrderTradePolicyService.PercentageRange(
                        new BigDecimal("50"), new BigDecimal("50"))));
        when(userMapper.lockUserById(7L)).thenReturn(7L);
        when(userMapper.selectOrderTaskUserById(7L)).thenReturn(user);
    }

    private OrderUser user(BigDecimal balance, long taskProgress) {
        GoodsMemberLevel level = new GoodsMemberLevel();
        level.setId(2L);
        level.setOrderCountPerDay(40L);
        level.setMinCommissionRate(new BigDecimal("1.005"));
        OrderUser user = new OrderUser();
        user.setId(7L);
        user.setBalance(balance);
        user.setFrozenBalance(BigDecimal.ZERO);
        user.setTaskProgress(taskProgress);
        user.setIsBanned("1");
        user.setMemberLevel(level);
        return user;
    }

    private Goods goods(Long id, BigDecimal price) {
        Goods goods = new Goods();
        goods.setId(id);
        goods.setPrice(price);
        goods.setTitle("Product snapshot");
        goods.setImage("/snapshot.jpg");
        return goods;
    }

    private OrderInfo order(Long id, String status) {
        OrderInfo order = new OrderInfo();
        order.setId(id);
        order.setUserId(7L);
        order.setOrderNumber("O-" + id);
        order.setStatus(status);
        order.setAmount(new BigDecimal("30.00"));
        order.setRebate(new BigDecimal("0.30"));
        order.setUpperRebate(BigDecimal.ZERO);
        return order;
    }

    private OrderBonusTable bonus(Long id, String isReceived) {
        OrderBonusTable bonus = new OrderBonusTable();
        bonus.setId(id);
        bonus.setUserId(7L);
        bonus.setOrderNum(2L);
        bonus.setAmount(new BigDecimal("28.88"));
        bonus.setIsDistributed("1");
        bonus.setIsReceived(isReceived);
        return bonus;
    }

    private OrderApplicationService service() {
        return new OrderApplicationService(
                userMapper, orderMapper, requestMapper, linkMapper, bonusMapper, goodsMapper,
                policyService, sequenceService, transactionService);
    }
}
