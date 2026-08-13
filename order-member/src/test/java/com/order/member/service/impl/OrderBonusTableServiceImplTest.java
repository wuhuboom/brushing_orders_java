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
    void delayedBonusCannotBeDistributedBeforeTheTaskGroupIsComplete() {
        OrderBonusTable bonus = bonus("2");
        OrderUser balanceUser = user(new BigDecimal("100.00"), 10L, 40L);
        when(bonusMapper.selectOrderBonusTableById(88L)).thenReturn(bonus);
        when(userMapper.lockUserById(7L)).thenReturn(7L);
        when(bonusMapper.selectBonusForUpdate(88L)).thenReturn(bonus);
        when(userMapper.selectOrderBalanceById(7L)).thenReturn(balanceUser);
        when(userMapper.selectOrderTaskUserById(7L)).thenReturn(balanceUser);

        assertThrows(IllegalStateException.class, () -> service.distributeBonus(88L));

        verify(bonusMapper, never()).distributeBonus(any());
        verify(userMapper, never()).creditBalance(any(), any());
        verify(transactionService, never()).recordFlow(any(), any(), any(), any(), any());
    }

    @Test
    void administratorCanDistributeDelayedBonusAfterTheTaskGroupIsComplete() {
        OrderBonusTable bonus = bonus("2");
        OrderUser balanceUser = user(new BigDecimal("100.00"), 40L, 40L);
        when(bonusMapper.selectOrderBonusTableById(88L)).thenReturn(bonus);
        when(userMapper.lockUserById(7L)).thenReturn(7L);
        when(bonusMapper.selectBonusForUpdate(88L)).thenReturn(bonus);
        when(userMapper.selectOrderBalanceById(7L)).thenReturn(balanceUser);
        when(userMapper.selectOrderTaskUserById(7L)).thenReturn(balanceUser);
        when(bonusMapper.distributeBonus(88L)).thenReturn(1);
        when(userMapper.creditBalance(7L, new BigDecimal("28.88"))).thenReturn(1);

        service.distributeBonus(88L);

        verify(bonusMapper).distributeBonus(88L);
        verify(userMapper).creditBalance(7L, new BigDecimal("28.88"));
        verify(transactionService).recordFlow(
                7L,
                "bonus",
                new BigDecimal("28.88"),
                new BigDecimal("100.00"),
                "manual-bonus:88");
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
