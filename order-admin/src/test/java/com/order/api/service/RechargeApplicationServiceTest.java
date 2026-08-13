package com.order.api.service;

import com.order.member.domain.GoodsRechargeRecord;
import com.order.member.domain.OrderUser;
import com.order.member.mapper.GoodsRechargeRecordMapper;
import com.order.member.mapper.OrderUserMapper;
import com.order.member.service.ITransactionService;
import com.order.member.service.SiteMessageNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RechargeApplicationServiceTest {
    @Mock
    GoodsRechargeRecordMapper rechargeMapper;
    @Mock
    OrderUserMapper userMapper;
    @Mock
    ITransactionService transactionService;
    @Mock
    SiteMessageNotificationService siteMessageNotificationService;

    @Test
    void approvalCreditsExactlyTheRecordedAmountAndGift() {
        GoodsRechargeRecord recharge = pendingRecharge();
        OrderUser user = new OrderUser();
        user.setId(7L);
        user.setBalance(new BigDecimal("100.00"));
        when(rechargeMapper.selectForUpdate(9L)).thenReturn(recharge);
        when(userMapper.selectWithdrawalUserByIdForUpdate(7L)).thenReturn(user);
        when(userMapper.creditBalance(7L, new BigDecimal("55.00"))).thenReturn(1);
        when(rechargeMapper.transitionStatus(9L, "1", "2", null, "system")).thenReturn(1);

        service().review(9L, "2", null);

        verify(transactionService).recordFlowWithTransactionCodeWithoutNotification(
                7L, "ck", new BigDecimal("50.00"), new BigDecimal("100.00"),
                "R-9", "recharge:R-9");
        verify(transactionService).recordFlowWithTransactionCodeWithoutNotification(
                7L, "zs", new BigDecimal("5.00"), new BigDecimal("150.00"),
                "R-9", "recharge:R-9");
        verify(userMapper).creditBalance(7L, new BigDecimal("55.00"));
        verify(siteMessageNotificationService).createForTransaction(
                7L, "ck", new BigDecimal("50.00"),
                new BigDecimal("100.00"), new BigDecimal("155.00"));
        verify(siteMessageNotificationService).createForTransaction(
                7L, "bonus", new BigDecimal("5.00"),
                new BigDecimal("150.00"), new BigDecimal("155.00"));
    }

    @Test
    void terminalRecordCannotBeCreditedAgain() {
        GoodsRechargeRecord recharge = pendingRecharge();
        recharge.setStatus("2");
        when(rechargeMapper.selectForUpdate(9L)).thenReturn(recharge);

        assertThrows(AccountApiException.class, () -> service().review(9L, "2", null));

        verify(userMapper, never()).creditBalance(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any());
    }

    private RechargeApplicationService service() {
        return new RechargeApplicationService(
                rechargeMapper, userMapper, transactionService, siteMessageNotificationService);
    }

    private GoodsRechargeRecord pendingRecharge() {
        GoodsRechargeRecord recharge = new GoodsRechargeRecord();
        recharge.setId(9L);
        recharge.setUserId(7L);
        recharge.setAmount(new BigDecimal("50.00"));
        recharge.setGiftAmount(new BigDecimal("5.00"));
        recharge.setReceivedAmount(new BigDecimal("55.00"));
        recharge.setStatus("1");
        recharge.setTransactionType(null);
        recharge.setOrderNumber("R-9");
        return recharge;
    }
}
