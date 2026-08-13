package com.order.member.service.impl;

import com.order.member.domain.GoodsRechargeRecord;
import com.order.member.domain.GoodsTransactionFlow;
import com.order.member.domain.OrderUser;
import com.order.member.mapper.OrderUserMapper;
import com.order.member.service.IGoodsRechargeRecordService;
import com.order.member.service.IGoodsTransactionFlowService;
import com.order.member.service.IOrderSequenceManagerService;
import com.order.member.service.SiteMessageNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {
    @Mock
    private OrderUserMapper userMapper;
    @Mock
    private IGoodsRechargeRecordService rechargeRecordService;
    @Mock
    private IGoodsTransactionFlowService flowService;
    @Mock
    private IOrderSequenceManagerService sequenceService;
    @Mock
    private SiteMessageNotificationService siteMessageNotificationService;
    @InjectMocks
    private TransactionServiceImpl service;

    @Test
    void rechargeLocksUserBeforeSequenceAndChecksEveryFinancialWrite() {
        OrderUser user = user(new BigDecimal("100.00"));
        when(userMapper.lockUserById(7L)).thenReturn(7L);
        when(userMapper.selectOrderUserById(7L)).thenReturn(user);
        when(sequenceService.generateCode(anyString()))
                .thenReturn("R-1", "F-1", "F-2");
        when(rechargeRecordService.insertGoodsRechargeRecord(
                any(GoodsRechargeRecord.class))).thenReturn(1);
        when(flowService.insertGoodsTransactionFlow(
                any(GoodsTransactionFlow.class))).thenReturn(1);
        when(userMapper.creditBalance(7L, new BigDecimal("22.01"))).thenReturn(1);

        var result = service.recordRecharge(
                7L,
                new BigDecimal("20.005"),
                new BigDecimal("2.004"),
                null,
                "manual");

        assertEquals(new BigDecimal("122.01"), result.getFinalBalance());
        InOrder lockBeforeSequence = inOrder(userMapper, sequenceService);
        lockBeforeSequence.verify(userMapper).lockUserById(7L);
        lockBeforeSequence.verify(sequenceService, org.mockito.Mockito.times(3))
                .generateCode("TRADE_NO");
        ArgumentCaptor<GoodsRechargeRecord> rechargeCaptor =
                ArgumentCaptor.forClass(GoodsRechargeRecord.class);
        verify(rechargeRecordService).insertGoodsRechargeRecord(rechargeCaptor.capture());
        assertEquals("ck", rechargeCaptor.getValue().getTransactionType());
        assertEquals("manual", rechargeCaptor.getValue().getRemark());
        ArgumentCaptor<GoodsTransactionFlow> flowCaptor =
                ArgumentCaptor.forClass(GoodsTransactionFlow.class);
        verify(flowService, org.mockito.Mockito.times(2))
                .insertGoodsTransactionFlow(flowCaptor.capture());
        assertEquals(
                java.util.List.of("R-1", "R-1"),
                flowCaptor.getAllValues().stream()
                        .map(GoodsTransactionFlow::getTransactionCode)
                        .toList());
        verify(userMapper).creditBalance(7L, new BigDecimal("22.01"));
        verify(siteMessageNotificationService).createForTransaction(
                7L,
                "ck",
                new BigDecimal("20.01"),
                new BigDecimal("100.00"),
                new BigDecimal("122.01"));
        verify(siteMessageNotificationService).createForTransaction(
                7L,
                "bonus",
                new BigDecimal("2.00"),
                new BigDecimal("120.01"),
                new BigDecimal("122.01"));
        verify(siteMessageNotificationService, never()).createForTransaction(
                eq(7L),
                eq("cz"),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class));
        verify(siteMessageNotificationService, never()).createForTransaction(
                eq(7L),
                eq("zs"),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class));
    }

    @Test
    void rechargeStopsAndThrowsWhenRechargeRecordWasNotPersisted() {
        OrderUser user = user(new BigDecimal("100.00"));
        when(userMapper.lockUserById(7L)).thenReturn(7L);
        when(userMapper.selectOrderUserById(7L)).thenReturn(user);
        when(sequenceService.generateCode("TRADE_NO")).thenReturn("R-1");
        when(rechargeRecordService.insertGoodsRechargeRecord(
                any(GoodsRechargeRecord.class))).thenReturn(0);

        assertThrows(
                IllegalStateException.class,
                () -> service.recordRecharge(
                        7L,
                        new BigDecimal("20.00"),
                        BigDecimal.ZERO,
                        "cz",
                        "manual"));

        verify(flowService, never())
                .insertGoodsTransactionFlow(any(GoodsTransactionFlow.class));
        verify(userMapper, never()).creditBalance(any(), any());
    }

    @Test
    void rechargeRejectsNonPositiveAmountsBeforeTakingFinancialLocks() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.recordRecharge(
                        7L,
                        new BigDecimal("-0.01"),
                        BigDecimal.ZERO,
                        "cz",
                        "invalid"));

        verify(userMapper, never()).lockUserById(any());
        verify(sequenceService, never()).generateCode(anyString());
    }

    private OrderUser user(BigDecimal balance) {
        OrderUser user = new OrderUser();
        user.setId(7L);
        user.setBalance(balance);
        return user;
    }
}
