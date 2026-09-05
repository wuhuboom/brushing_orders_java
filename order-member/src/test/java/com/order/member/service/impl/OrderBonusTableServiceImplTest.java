package com.order.member.service.impl;

import com.order.member.domain.GoodsMemberLevel;
import com.order.member.domain.OrderBonusTable;
import com.order.member.domain.OrderUser;
import com.order.member.mapper.OrderBonusTableMapper;
import com.order.member.mapper.OrderUserMapper;
import com.order.member.service.ITransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderBonusTableServiceImplTest {
    @Mock
    private OrderBonusTableMapper bonusMapper;
    @Mock
    private OrderUserMapper userMapper;
    @Mock
    private ITransactionService transactionService;
    @InjectMocks
    private OrderBonusTableServiceImpl service;

    @Test
    void receivingOnlyChangesTheReceiptState() {
        OrderBonusTable bonus = bonus("1");
        bonus.setIsReceived("1");
        when(bonusMapper.selectBonusForUpdate(88L)).thenReturn(bonus);
        when(bonusMapper.receiveBonus(88L)).thenReturn(1);

        service.receiveBonus(88L);

        verify(bonusMapper).receiveBonus(88L);
        verify(bonusMapper, never()).distributeBonus(any());
        verify(userMapper, never()).creditBalance(any(), any());
        verify(transactionService, never()).recordFlow(any(), any(), any(), any(), any());
    }

    @Test
    void administratorCannotDistributeBeforeReceipt() {
        OrderBonusTable bonus = bonus("2");
        bonus.setIsReceived("1");
        when(bonusMapper.selectOrderBonusTableById(88L)).thenReturn(bonus);

        assertThrows(IllegalStateException.class, () -> service.distributeBonus(88L));

        verify(userMapper, never()).lockUserById(any());
        verify(bonusMapper, never()).distributeBonus(any());
        verify(userMapper, never()).creditBalance(any(), any());
        verify(transactionService, never()).recordFlow(any(), any(), any(), any(), any());
    }

    @Test
    void administratorCanDistributeDelayedBonusAfterReceiptBeforeTaskGroupCompletion() {
        OrderBonusTable bonus = bonus("2");
        OrderUser balanceUser = user(new BigDecimal("100.00"), 10L, 40L);
        when(bonusMapper.selectOrderBonusTableById(88L)).thenReturn(bonus);
        when(userMapper.lockUserById(7L)).thenReturn(7L);
        when(bonusMapper.selectBonusForUpdate(88L)).thenReturn(bonus);
        when(userMapper.selectOrderBalanceById(7L)).thenReturn(balanceUser);
        when(bonusMapper.distributeBonus(88L)).thenReturn(1);
        when(userMapper.creditBalance(7L, new BigDecimal("28.88"))).thenReturn(1);

        service.distributeBonus(88L);

        verify(userMapper, never()).selectOrderTaskUserById(any());
        verify(bonusMapper).distributeBonus(88L);
        verify(userMapper).creditBalance(7L, new BigDecimal("28.88"));
        verify(transactionService).recordFlow(
                7L,
                "rwjl",
                new BigDecimal("28.88"),
                new BigDecimal("100.00"),
                "manual-bonus:88");
    }

    @Test
    void administratorCanDistributeDelayedBonusAfterTheTaskGroupIsComplete() {
        OrderBonusTable bonus = bonus("2");
        OrderUser balanceUser = user(new BigDecimal("100.00"), 40L, 40L);
        when(bonusMapper.selectOrderBonusTableById(88L)).thenReturn(bonus);
        when(userMapper.lockUserById(7L)).thenReturn(7L);
        when(bonusMapper.selectBonusForUpdate(88L)).thenReturn(bonus);
        when(userMapper.selectOrderBalanceById(7L)).thenReturn(balanceUser);
        when(bonusMapper.distributeBonus(88L)).thenReturn(1);
        when(userMapper.creditBalance(7L, new BigDecimal("28.88"))).thenReturn(1);

        service.distributeBonus(88L);

        verify(bonusMapper).distributeBonus(88L);
        verify(userMapper).creditBalance(7L, new BigDecimal("28.88"));
        verify(transactionService).recordFlow(
                7L,
                "rwjl",
                new BigDecimal("28.88"),
                new BigDecimal("100.00"),
                "manual-bonus:88");
    }

    @Test
    void updateRejectsAnAlreadyReceivedBonusBeforeWriting() {
        OrderBonusTable existing = bonus("1");
        existing.setIsReceived("0");
        OrderBonusTable incoming = bonus("1");
        when(bonusMapper.selectOrderBonusTableById(88L)).thenReturn(existing);

        assertThrows(IllegalStateException.class, () -> service.updateOrderBonusTable(incoming));

        verify(bonusMapper, never()).updateOrderBonusTable(any());
    }

    @Test
    void batchDeleteRejectsMixedReceivedStateWithoutPartialDeletion() {
        OrderBonusTable available = bonus("1");
        available.setId(88L);
        available.setIsReceived("1");
        OrderBonusTable received = bonus("1");
        received.setId(89L);
        received.setIsReceived("0");
        when(bonusMapper.selectOrderBonusTableById(88L)).thenReturn(available);
        when(bonusMapper.selectOrderBonusTableById(89L)).thenReturn(received);

        assertThrows(
                IllegalStateException.class,
                () -> service.deleteOrderBonusTableByIds(new Long[]{88L, 89L}));

        verify(bonusMapper, never()).deleteOrderBonusTableByIds(any());
    }

    private OrderBonusTable bonus(String distributionType) {
        OrderBonusTable bonus = new OrderBonusTable();
        bonus.setId(88L);
        bonus.setUserId(7L);
        bonus.setAmount(new BigDecimal("28.88"));
        bonus.setDistributionType(distributionType);
        bonus.setIsReceived("0");
        bonus.setIsDistributed("1");
        return bonus;
    }

    private OrderUser user(BigDecimal balance, long taskProgress, long orderLimit) {
        GoodsMemberLevel level = new GoodsMemberLevel();
        level.setOrderCountPerDay(orderLimit);
        OrderUser user = new OrderUser();
        user.setId(7L);
        user.setBalance(balance);
        user.setTaskProgress(taskProgress);
        user.setMemberLevel(level);
        return user;
    }
}
