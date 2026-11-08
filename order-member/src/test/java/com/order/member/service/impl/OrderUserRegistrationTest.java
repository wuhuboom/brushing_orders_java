package com.order.member.service.impl;

import com.order.member.domain.GoodsMemberLevel;
import com.order.member.domain.OrderUser;
import com.order.member.mapper.GoodsMemberLevelMapper;
import com.order.member.mapper.OrderUserMapper;
import com.order.member.service.IOrderConfigService;
import com.order.member.service.IOrderInfoService;
import com.order.member.service.ITransactionService;
import com.order.member.service.RegistrationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderUserRegistrationTest {
    @Mock private OrderUserMapper userMapper;
    @Mock private GoodsMemberLevelMapper memberLevelMapper;
    @Mock private IOrderConfigService configService;
    @Mock private ITransactionService transactionService;
    @Mock private IOrderInfoService orderInfoService;
    @InjectMocks private OrderUserServiceImpl service;

    @Test
    void missingSwitchDefaultsToZeroAndOverridesSuppliedProgress() {
        prepareLevel(1L, 40L);
        OrderUser user = registration();
        user.setTaskProgress(999L);

        assertEquals(RegistrationResult.SUCCESS, service.register(user));

        assertEquals(0L, user.getTaskProgress());
        verify(userMapper).insertOrderUser(user);
        verifyNoInteractions(transactionService, orderInfoService);
    }

    @Test
    void disabledSwitchStartsAtZero() {
        assertProgress("0", 40L, 0L);
    }

    @Test
    void booleanFalseStartsAtZero() {
        assertProgress(false, 40L, 0L);
    }

    @Test
    void numericZeroStartsAtZero() {
        assertProgress(0, 40L, 0L);
    }

    @Test
    void unrecognizedSwitchDoesNotEnableFullOrders() {
        assertProgress("enabled", 40L, 0L);
    }

    @Test
    void enabledSwitchInitializesVipOneAtForty() {
        assertProgress("1", 40L, 40L);
    }

    @Test
    void fullOrdersUseTheAssignedVipLevelInsteadOfTheSuppliedVip() {
        prepareLevel(2L, 45L);
        when(configService.getConfigValue("website", "registerFullOrder"))
                .thenReturn(Optional.of("1"));
        OrderUser user = registration();
        user.setVipId(99L);

        assertEquals(RegistrationResult.SUCCESS, service.register(user));

        assertEquals(2L, user.getVipId());
        assertEquals(45L, user.getTaskProgress());
        verify(userMapper).insertOrderUser(user);
        verifyNoInteractions(transactionService, orderInfoService);
    }

    @Test
    void numericOneEnablesFullOrders() {
        assertProgress(1, 45L, 45L);
    }

    @Test
    void booleanTrueEnablesFullOrders() {
        assertProgress(true, 45L, 45L);
    }

    @Test
    void zeroOrderLimitIsValid() {
        assertProgress("1", 0L, 0L);
    }

    @Test
    void missingOrderLimitRejectsRegistrationBeforeInsert() {
        assertInvalidLimit(null);
    }

    @Test
    void negativeOrderLimitRejectsRegistrationBeforeInsert() {
        assertInvalidLimit(-1L);
    }

    @Test
    void disabledSwitchDoesNotRequireAnOrderLimit() {
        assertProgress("0", null, 0L);
    }

    @Test
    void fullOrdersKeepRegistrationBonusAndReferralBehavior() {
        prepareLevel(1L, 40L);
        when(configService.getConfigValue("website", "registerFullOrder"))
                .thenReturn(Optional.of("1"));
        when(configService.getConfigValue("trade", "registerBonusAmount"))
                .thenReturn(Optional.of("25.50"));
        OrderUser user = registration();
        doAnswer(invocation -> {
            invocation.<OrderUser>getArgument(0).setId(100L);
            return 1;
        }).when(userMapper).insertOrderUser(user);

        assertEquals(RegistrationResult.SUCCESS, service.register(user));

        assertEquals(40L, user.getTaskProgress());
        assertEquals(new BigDecimal("25.50"), user.getBalance());
        assertEquals(7L, user.getParentId());
        assertEquals("0,7", user.getAncestors());
        assertNotNull(user.getCreateTime());
        verify(transactionService).recordFlow(
                100L, "zczs", new BigDecimal("25.50"), BigDecimal.ZERO, "Registration bonus");
        verifyNoInteractions(orderInfoService);
    }

    @Test
    void invalidInvitationStillRejectsRegistration() {
        assertEquals(RegistrationResult.INVITE_NOT_FOUND, service.register(registration()));

        verify(userMapper, never()).insertOrderUser(any());
        verifyNoInteractions(configService, memberLevelMapper, transactionService);
    }

    @Test
    void missingVipLevelStillRejectsRegistration() {
        when(userMapper.selectReferralByInviteCode("PARENT")).thenReturn(parent());

        assertEquals(RegistrationResult.MEMBER_LEVEL_NOT_FOUND, service.register(registration()));

        verify(userMapper, never()).insertOrderUser(any());
        verifyNoInteractions(configService, transactionService);
    }

    private void assertProgress(Object enabled, Long limit, long expectedProgress) {
        prepareLevel(1L, limit);
        when(configService.getConfigValue("website", "registerFullOrder"))
                .thenReturn(Optional.of(enabled));
        OrderUser user = registration();

        assertEquals(RegistrationResult.SUCCESS, service.register(user));

        assertEquals(expectedProgress, user.getTaskProgress());
        verify(userMapper).insertOrderUser(user);
        verifyNoInteractions(transactionService, orderInfoService);
    }

    private void assertInvalidLimit(Long limit) {
        prepareLevel(1L, limit);
        when(configService.getConfigValue("website", "registerFullOrder"))
                .thenReturn(Optional.of("1"));

        assertThrows(IllegalStateException.class, () -> service.register(registration()));

        verify(userMapper, never()).insertOrderUser(any());
        verifyNoInteractions(transactionService, orderInfoService);
    }

    private void prepareLevel(Long id, Long limit) {
        when(userMapper.selectReferralByInviteCode("PARENT")).thenReturn(parent());
        GoodsMemberLevel level = new GoodsMemberLevel();
        level.setId(id);
        level.setOrderCountPerDay(limit);
        when(memberLevelMapper.selectLowestPriceLevel()).thenReturn(level);
    }

    private OrderUser parent() {
        OrderUser parent = new OrderUser();
        parent.setId(7L);
        parent.setAncestors("0");
        return parent;
    }

    private OrderUser registration() {
        OrderUser user = new OrderUser();
        user.setUsername("new-member");
        user.setInviteCode("PARENT");
        return user;
    }
}
