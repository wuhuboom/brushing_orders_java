package com.order.member.service.impl;

import com.order.common.exception.ServiceException;
import com.order.member.domain.OrderInfo;
import com.order.member.domain.OrderUser;
import com.order.member.mapper.GoodsExtraCommissionSettingMapper;
import com.order.member.mapper.OrderInfoMapper;
import com.order.member.mapper.OrderUserMapper;
import com.order.member.service.ITransactionService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderInfoServiceImplTest {

    private final OrderInfoMapper orderMapper = mock(OrderInfoMapper.class);
    private final OrderUserMapper userMapper = mock(OrderUserMapper.class);
    private final GoodsExtraCommissionSettingMapper extraCommissionMapper =
            mock(GoodsExtraCommissionSettingMapper.class);
    private final ITransactionService transactionService = mock(ITransactionService.class);
    private final OrderInfoServiceImpl service =
            new OrderInfoServiceImpl(
                    orderMapper, userMapper, extraCommissionMapper, transactionService);

    @Test
    void cancelNewPendingNormalOrderReturnsPrincipalWithoutChangingCompletedProgress() {
        OrderInfo snapshot = order(20L, 7L, "1");
        OrderInfo locked = order(20L, 7L, "1");
        locked.setType("0");
        locked.setOrderCount(4L);
        locked.setExtraCommissionId(77L);
        OrderUser user = new OrderUser();
        user.setId(7L);
        user.setBalance(new BigDecimal("50.00"));
        user.setTaskProgress(3L);

        when(orderMapper.selectOrderInfoById(20L)).thenReturn(snapshot);
        when(userMapper.lockUserById(7L)).thenReturn(7L);
        when(orderMapper.selectOwnedOrderForUpdate(20L, 7L)).thenReturn(locked);
        when(userMapper.selectOrderBalanceById(7L)).thenReturn(user);
        when(orderMapper.transitionStatus(20L, 7L, "1", "3")).thenReturn(1);
        when(userMapper.releaseCancelledOrderFunds(
                7L, new BigDecimal("30.00"), 0L)).thenReturn(1);
        when(extraCommissionMapper.releaseReserved(
                77L, 7L, 4L, new BigDecimal("30.00"))).thenReturn(1);

        assertEquals(1, service.cancelPendingOrder(20L));

        verify(userMapper).releaseCancelledOrderFunds(
                7L, new BigDecimal("30.00"), 0L);
        verify(extraCommissionMapper).releaseReserved(
                77L, 7L, 4L, new BigDecimal("30.00"));
        verify(transactionService).recordFlowWithoutNotification(
                7L,
                "rw",
                new BigDecimal("30.00"),
                new BigDecimal("50.00"),
                "order-cancel:O-20");
    }

    @Test
    void cancelLegacyPendingNormalOrderRollsBackItsPrematureProgress() {
        OrderInfo snapshot = order(20L, 7L, "1");
        OrderInfo locked = order(20L, 7L, "1");
        locked.setType("0");
        locked.setOrderCount(4L);
        OrderUser user = new OrderUser();
        user.setId(7L);
        user.setBalance(new BigDecimal("50.00"));
        user.setTaskProgress(4L);

        when(orderMapper.selectOrderInfoById(20L)).thenReturn(snapshot);
        when(userMapper.lockUserById(7L)).thenReturn(7L);
        when(orderMapper.selectOwnedOrderForUpdate(20L, 7L)).thenReturn(locked);
        when(userMapper.selectOrderBalanceById(7L)).thenReturn(user);
        when(orderMapper.transitionStatus(20L, 7L, "1", "3")).thenReturn(1);
        when(userMapper.releaseCancelledOrderFunds(
                7L, new BigDecimal("30.00"), 1L)).thenReturn(1);

        assertEquals(1, service.cancelPendingOrder(20L));

        verify(userMapper).releaseCancelledOrderFunds(
                7L, new BigDecimal("30.00"), 1L);
    }

    @Test
    void cancelPendingNormalOrderDoesNotReduceHigherCompletedProgress() {
        OrderInfo snapshot = order(20L, 7L, "1");
        OrderInfo locked = order(20L, 7L, "1");
        locked.setType("0");
        locked.setOrderCount(4L);
        OrderUser user = new OrderUser();
        user.setId(7L);
        user.setBalance(new BigDecimal("50.00"));
        user.setTaskProgress(5L);

        when(orderMapper.selectOrderInfoById(20L)).thenReturn(snapshot);
        when(userMapper.lockUserById(7L)).thenReturn(7L);
        when(orderMapper.selectOwnedOrderForUpdate(20L, 7L)).thenReturn(locked);
        when(userMapper.selectOrderBalanceById(7L)).thenReturn(user);
        when(orderMapper.transitionStatus(20L, 7L, "1", "3")).thenReturn(1);
        when(userMapper.releaseCancelledOrderFunds(
                7L, new BigDecimal("30.00"), 0L)).thenReturn(1);

        assertEquals(1, service.cancelPendingOrder(20L));

        verify(userMapper).releaseCancelledOrderFunds(
                7L, new BigDecimal("30.00"), 0L);
    }

    @Test
    void cancelPendingLinkedOrderDoesNotDecrementNormalTaskProgress() {
        OrderInfo snapshot = order(20L, 7L, "1");
        OrderInfo locked = order(20L, 7L, "1");
        locked.setType("1");
        OrderUser user = new OrderUser();
        user.setBalance(BigDecimal.ZERO);

        when(orderMapper.selectOrderInfoById(20L)).thenReturn(snapshot);
        when(userMapper.lockUserById(7L)).thenReturn(7L);
        when(orderMapper.selectOwnedOrderForUpdate(20L, 7L)).thenReturn(locked);
        when(userMapper.selectOrderBalanceById(7L)).thenReturn(user);
        when(orderMapper.transitionStatus(20L, 7L, "1", "3")).thenReturn(1);
        when(userMapper.releaseCancelledOrderFunds(
                7L, new BigDecimal("30.00"), 0L)).thenReturn(1);

        assertEquals(1, service.cancelPendingOrder(20L));

        verify(userMapper).releaseCancelledOrderFunds(
                7L, new BigDecimal("30.00"), 0L);
    }

    @Test
    void completedOrderCannotBeCancelled() {
        OrderInfo snapshot = order(20L, 7L, "0");
        OrderInfo locked = order(20L, 7L, "0");

        when(orderMapper.selectOrderInfoById(20L)).thenReturn(snapshot);
        when(userMapper.lockUserById(7L)).thenReturn(7L);
        when(orderMapper.selectOwnedOrderForUpdate(20L, 7L)).thenReturn(locked);

        assertThrows(ServiceException.class, () -> service.cancelPendingOrder(20L));

        verify(orderMapper, never()).transitionStatus(20L, 7L, "1", "3");
        verify(userMapper, never()).releaseCancelledOrderFunds(
                7L, new BigDecimal("30.00"), 0L);
    }

    @Test
    void cancellationFailsWhenItsReservedExtraCommissionCannotBeReleased() {
        OrderInfo snapshot = order(20L, 7L, "1");
        OrderInfo locked = order(20L, 7L, "1");
        locked.setOrderCount(4L);
        locked.setExtraCommissionId(77L);
        OrderUser user = new OrderUser();
        user.setBalance(new BigDecimal("50.00"));

        when(orderMapper.selectOrderInfoById(20L)).thenReturn(snapshot);
        when(userMapper.lockUserById(7L)).thenReturn(7L);
        when(orderMapper.selectOwnedOrderForUpdate(20L, 7L)).thenReturn(locked);
        when(userMapper.selectOrderBalanceById(7L)).thenReturn(user);
        when(orderMapper.transitionStatus(20L, 7L, "1", "3")).thenReturn(1);
        when(userMapper.releaseCancelledOrderFunds(
                7L, new BigDecimal("30.00"), 0L)).thenReturn(1);

        assertThrows(ServiceException.class, () -> service.cancelPendingOrder(20L));

        verify(extraCommissionMapper).releaseReserved(
                77L, 7L, 4L, new BigDecimal("30.00"));
        verify(transactionService, never()).recordFlowWithoutNotification(
                7L,
                "rw",
                new BigDecimal("30.00"),
                new BigDecimal("50.00"),
                "order-cancel:O-20");
    }

    private static OrderInfo order(Long id, Long userId, String status) {
        OrderInfo order = new OrderInfo();
        order.setId(id);
        order.setUserId(userId);
        order.setStatus(status);
        order.setType("0");
        order.setAmount(new BigDecimal("30.00"));
        order.setOrderNumber("O-20");
        return order;
    }
}
