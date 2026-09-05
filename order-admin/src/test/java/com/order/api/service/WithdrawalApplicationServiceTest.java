package com.order.api.service;

import com.github.pagehelper.PageHelper;
import com.order.api.controller.dto.AccountApiDtos.SensitiveWithdrawalAccountUpdateRequest;
import com.order.api.controller.dto.AccountApiDtos.WithdrawalRequest;
import com.order.member.domain.GoodsMemberLevel;
import com.order.member.domain.GoodsWithdrawalAccount;
import com.order.member.domain.OrderUser;
import com.order.member.domain.OrderWithdrawal;
import com.order.member.mapper.GoodsRechargeRecordMapper;
import com.order.member.mapper.GoodsTransactionFlowMapper;
import com.order.member.mapper.GoodsWithdrawalAccountMapper;
import com.order.member.mapper.OrderUserMapper;
import com.order.member.mapper.OrderWithdrawalMapper;
import com.order.member.service.IOrderSequenceManagerService;
import com.order.member.service.ITransactionService;
import com.order.member.service.SiteMessageNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WithdrawalApplicationServiceTest {
    @Mock OrderUserMapper userMapper;
    @Mock OrderWithdrawalMapper withdrawalMapper;
    @Mock GoodsWithdrawalAccountMapper accountMapper;
    @Mock GoodsRechargeRecordMapper rechargeMapper;
    @Mock GoodsTransactionFlowMapper flowMapper;
    @Mock TradePasswordVerificationService passwordService;
    @Mock IOrderSequenceManagerService sequenceService;
    @Mock ITransactionService transactionService;
    @Mock SiteMessageNotificationService siteMessageNotificationService;
    @Mock TradeConfigSnapshotService snapshotService;
    @Mock AccountDataCipher cipher;

    @Test
    void idempotentRetryReturnsOriginalWithoutSecondDebit() {
        UUID requestId = UUID.randomUUID();
        OrderWithdrawal existing = withdrawal(12L, "1");
        existing.setRequestId(requestId.toString());
        existing.setWithdrawalAccountId(3L);
        when(withdrawalMapper.selectByUserAndRequestId(7L, requestId.toString())).thenReturn(existing);

        var response = service().submit(7L,
                new WithdrawalRequest(requestId, new BigDecimal("10.00"), "secret", 3L));

        assertEquals(12L, response.withdrawalId());
        verify(userMapper, never()).debitBalance(7L, new BigDecimal("10.00"));
    }

    @Test
    void idempotencyKeyCannotBeReusedWithDifferentAmount() {
        UUID requestId = UUID.randomUUID();
        OrderWithdrawal existing = withdrawal(12L, "1");
        existing.setRequestId(requestId.toString());
        existing.setWithdrawalAccountId(3L);
        when(withdrawalMapper.selectByUserAndRequestId(7L, requestId.toString())).thenReturn(existing);

        assertThrows(AccountApiException.class, () -> service().submit(7L,
                new WithdrawalRequest(requestId, new BigDecimal("11.00"), "secret", 3L)));
    }

    @Test
    void rejectsMoreThanTwoDecimalPlaces() {
        assertThrows(AccountApiException.class, () -> service().submit(7L,
                new WithdrawalRequest(UUID.randomUUID(), new BigDecimal("1.001"), "secret", 3L)));
    }

    @Test
    void feeCalculationDoesNotRoundRatePrematurely() {
        assertEquals(new BigDecimal("0.50"),
                WithdrawalApplicationService.calculateFee(new BigDecimal("100.00"), new BigDecimal("0.5")));
        assertEquals(new BigDecimal("0.10"),
                WithdrawalApplicationService.calculateFee(new BigDecimal("100.00"), new BigDecimal("0.1")));
    }

    @Test
    void strictLimitsUseHighestMinimumAndLowestMaximum() {
        assertEquals(new BigDecimal("20"),
                WithdrawalApplicationService.maxPositive(
                        new BigDecimal("10"), new BigDecimal("20")));
        assertEquals(new BigDecimal("80"),
                WithdrawalApplicationService.minPositive(
                        new BigDecimal("100"), new BigDecimal("80"), new BigDecimal("90")));
    }

    @Test
    void memberLevelLimitsOverrideLegacyGlobalFallbacks() {
        assertEquals(new BigDecimal("50"),
                WithdrawalApplicationService.levelLimitOrFallback(
                        new BigDecimal("50"), new BigDecimal("100")));
        assertEquals(new BigDecimal("10000000"),
                WithdrawalApplicationService.levelLimitOrFallback(
                        new BigDecimal("10000000"), new BigDecimal("10000")));
        assertEquals(new BigDecimal("10000"),
                WithdrawalApplicationService.levelLimitOrFallback(
                        BigDecimal.ZERO, new BigDecimal("10000")));
        assertEquals(new BigDecimal("10000000"),
                WithdrawalApplicationService.effectiveWithdrawalMaximum(
                        new BigDecimal("10000000"), new BigDecimal("10000"), BigDecimal.ZERO));
        assertEquals(new BigDecimal("10000"),
                WithdrawalApplicationService.effectiveWithdrawalMaximum(
                        BigDecimal.ZERO, new BigDecimal("10000"), BigDecimal.ZERO));
        assertEquals(new BigDecimal("15000"),
                WithdrawalApplicationService.effectiveWithdrawalMaximum(
                        new BigDecimal("10000000"), new BigDecimal("10000"), new BigDecimal("15000")));
    }

    @Test
    void withdrawalAboveLegacyGlobalMaximumUsesConfiguredMemberLevelMaximum() {
        UUID requestId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("20000.00");
        OrderUser user = new OrderUser();
        user.setId(7L);
        user.setBalance(new BigDecimal("50000.00"));
        user.setWithdrawalStatus("0");
        GoodsMemberLevel level = new GoodsMemberLevel();
        level.setId(1L);
        level.setMinWithdraw(new BigDecimal("50.00"));
        level.setMaxWithdraw(new BigDecimal("10000000.00"));
        level.setWithdrawLimitPerDay(new BigDecimal("10000000.00"));
        user.setMemberLevel(level);

        GoodsWithdrawalAccount account = new GoodsWithdrawalAccount();
        account.setId(3L);
        account.setType("1");

        when(userMapper.selectWithdrawalUserById(7L)).thenReturn(user);
        when(passwordService.verify(user, "secret"))
                .thenReturn(TradePasswordVerificationService.Result.SUCCESS);
        when(userMapper.selectWithdrawalUserByIdForUpdate(7L)).thenReturn(user);
        when(snapshotService.snapshot()).thenReturn(new TradeConfigSnapshotService.TradeConfigSnapshot(
                Map.of(
                        "memberWithdrawalStatus", "0",
                        "withdrawalRestrictLevelMinBalance", "1",
                        "withdrawalTimeRange", List.of("00:00", "00:00"),
                        "minWithdrawalAmount", 100,
                        "maxWithdrawalAmount", 10000,
                        "platformDailyMaxWithdrawal", 100000000),
                "UTC"));
        when(accountMapper.selectActiveByIdAndUserId(3L, 7L)).thenReturn(account);
        when(withdrawalMapper.reserveDailyQuota(any(), any(), any())).thenReturn(1);
        when(sequenceService.generateCode("TRADE_NO")).thenReturn("W-20000");
        when(withdrawalMapper.insertOrderWithdrawal(any(OrderWithdrawal.class))).thenReturn(1);
        when(userMapper.debitBalance(7L, amount)).thenReturn(1);

        var response = service().submit(7L,
                new WithdrawalRequest(requestId, amount, "secret", 3L));

        assertEquals(amount, response.amount());
    }

    @Test
    void withdrawalBelowLegacyGlobalMinimumUsesConfiguredMemberLevelMinimum() {
        UUID requestId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("75.00");
        OrderUser user = new OrderUser();
        user.setId(7L);
        user.setBalance(new BigDecimal("500.00"));
        user.setWithdrawalStatus("0");
        GoodsMemberLevel level = new GoodsMemberLevel();
        level.setId(1L);
        level.setMinWithdraw(new BigDecimal("50.00"));
        level.setMaxWithdraw(new BigDecimal("1000.00"));
        level.setWithdrawLimitPerDay(new BigDecimal("1000.00"));
        user.setMemberLevel(level);

        GoodsWithdrawalAccount account = new GoodsWithdrawalAccount();
        account.setId(3L);
        account.setType("1");

        when(userMapper.selectWithdrawalUserById(7L)).thenReturn(user);
        when(passwordService.verify(user, "secret"))
                .thenReturn(TradePasswordVerificationService.Result.SUCCESS);
        when(userMapper.selectWithdrawalUserByIdForUpdate(7L)).thenReturn(user);
        when(snapshotService.snapshot()).thenReturn(new TradeConfigSnapshotService.TradeConfigSnapshot(
                Map.of(
                        "memberWithdrawalStatus", "0",
                        "withdrawalRestrictLevelMinBalance", "1",
                        "withdrawalTimeRange", List.of("00:00", "00:00"),
                        "minWithdrawalAmount", 100,
                        "maxWithdrawalAmount", 10000,
                        "platformDailyMaxWithdrawal", 100000000),
                "UTC"));
        when(accountMapper.selectActiveByIdAndUserId(3L, 7L)).thenReturn(account);
        when(withdrawalMapper.reserveDailyQuota(any(), any(), any())).thenReturn(1);
        when(sequenceService.generateCode("TRADE_NO")).thenReturn("W-75");
        when(withdrawalMapper.insertOrderWithdrawal(any(OrderWithdrawal.class))).thenReturn(1);
        when(userMapper.debitBalance(7L, amount)).thenReturn(1);

        var response = service().submit(7L,
                new WithdrawalRequest(requestId, amount, "secret", 3L));

        assertEquals(amount, response.amount());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "1"})
    void legacyEnabledStatusAllowsWithdrawalAndRechargeBlockSettingsAreIgnored(
            String prohibitWithdrawalAfterRecharge) {
        UUID requestId = UUID.randomUUID();
        OrderUser user = new OrderUser();
        user.setId(7L);
        user.setBalance(new BigDecimal("100.00"));
        user.setWithdrawalStatus("0");
        user.setDepositBlockWithdrawal("0");
        GoodsMemberLevel level = new GoodsMemberLevel();
        level.setId(1L);
        user.setMemberLevel(level);

        GoodsWithdrawalAccount account = new GoodsWithdrawalAccount();
        account.setId(3L);
        account.setType("1");

        when(userMapper.selectWithdrawalUserById(7L)).thenReturn(user);
        when(passwordService.verify(user, "secret"))
                .thenReturn(TradePasswordVerificationService.Result.SUCCESS);
        when(userMapper.selectWithdrawalUserByIdForUpdate(7L)).thenReturn(user);
        when(snapshotService.snapshot()).thenReturn(new TradeConfigSnapshotService.TradeConfigSnapshot(
                Map.of(
                        "memberWithdrawalStatus", "0",
                        "prohibitWithdrawalAfterRecharge", prohibitWithdrawalAfterRecharge,
                        "withdrawalRestrictLevelMinBalance", "1",
                        "withdrawalTimeRange", List.of("00:00", "00:00")),
                "UTC"));
        when(accountMapper.selectActiveByIdAndUserId(3L, 7L)).thenReturn(account);
        when(withdrawalMapper.reserveDailyQuota(any(), any(), any())).thenReturn(1);
        when(sequenceService.generateCode("TRADE_NO")).thenReturn("W-1");
        when(withdrawalMapper.insertOrderWithdrawal(any(OrderWithdrawal.class))).thenReturn(1);
        when(userMapper.debitBalance(7L, new BigDecimal("10.00"))).thenReturn(1);

        var response = service().submit(7L,
                new WithdrawalRequest(requestId, new BigDecimal("10.00"), "secret", 3L));

        assertEquals(new BigDecimal("10.00"), response.amount());
    }

    @Test
    void missingMultiplePendingSettingKeepsPendingWithdrawalBlocked() {
        UUID requestId = UUID.randomUUID();
        OrderUser user = new OrderUser();
        user.setId(7L);
        user.setBalance(new BigDecimal("100.00"));
        user.setWithdrawalStatus("0");
        GoodsMemberLevel level = new GoodsMemberLevel();
        level.setId(1L);
        user.setMemberLevel(level);

        GoodsWithdrawalAccount account = new GoodsWithdrawalAccount();
        account.setId(3L);
        account.setType("1");

        when(userMapper.selectWithdrawalUserById(7L)).thenReturn(user);
        when(passwordService.verify(user, "secret"))
                .thenReturn(TradePasswordVerificationService.Result.SUCCESS);
        when(userMapper.selectWithdrawalUserByIdForUpdate(7L)).thenReturn(user);
        when(snapshotService.snapshot()).thenReturn(new TradeConfigSnapshotService.TradeConfigSnapshot(
                Map.of(
                        "memberWithdrawalStatus", "0",
                        "withdrawalRestrictLevelMinBalance", "1",
                        "withdrawalTimeRange", List.of("00:00", "00:00")),
                "UTC"));
        when(accountMapper.selectActiveByIdAndUserId(3L, 7L)).thenReturn(account);
        when(withdrawalMapper.existsPendingByUserId(7L)).thenReturn(1);

        AccountApiException exception = assertThrows(AccountApiException.class,
                () -> service().submit(7L, new WithdrawalRequest(
                        requestId, new BigDecimal("10.00"), "secret", 3L)));

        assertEquals(AccountErrorCodes.PENDING_WITHDRAWAL, exception.getLegacyCode());
        verify(withdrawalMapper, never()).reserveDailyQuota(any(), any(), any());
        verify(userMapper, never()).debitBalance(any(), any());
    }

    @Test
    void enabledMultiplePendingSettingAllowsAnotherWithdrawalRequest() {
        UUID requestId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("10.00");
        OrderUser user = new OrderUser();
        user.setId(7L);
        user.setBalance(new BigDecimal("100.00"));
        user.setWithdrawalStatus("0");
        GoodsMemberLevel level = new GoodsMemberLevel();
        level.setId(1L);
        user.setMemberLevel(level);

        GoodsWithdrawalAccount account = new GoodsWithdrawalAccount();
        account.setId(3L);
        account.setType("1");

        when(userMapper.selectWithdrawalUserById(7L)).thenReturn(user);
        when(passwordService.verify(user, "secret"))
                .thenReturn(TradePasswordVerificationService.Result.SUCCESS);
        when(userMapper.selectWithdrawalUserByIdForUpdate(7L)).thenReturn(user);
        when(snapshotService.snapshot()).thenReturn(new TradeConfigSnapshotService.TradeConfigSnapshot(
                Map.of(
                        "memberWithdrawalStatus", "0",
                        "allowMultiplePendingWithdrawals", "0",
                        "withdrawalRestrictLevelMinBalance", "1",
                        "withdrawalTimeRange", List.of("00:00", "00:00"),
                        "platformDailyMaxWithdrawal", 1000),
                "UTC"));
        when(accountMapper.selectActiveByIdAndUserId(3L, 7L)).thenReturn(account);
        when(withdrawalMapper.reserveDailyQuota(any(), any(), any())).thenReturn(1);
        when(sequenceService.generateCode("TRADE_NO")).thenReturn("W-2");
        when(withdrawalMapper.insertOrderWithdrawal(any(OrderWithdrawal.class))).thenReturn(1);
        when(userMapper.debitBalance(7L, amount)).thenReturn(1);

        var response = service().submit(7L,
                new WithdrawalRequest(requestId, amount, "secret", 3L));

        assertEquals(amount, response.amount());
        verify(withdrawalMapper, never()).existsPendingByUserId(7L);
        verify(withdrawalMapper).insertOrderWithdrawal(any(OrderWithdrawal.class));
        verify(userMapper).debitBalance(7L, amount);
    }

    @Test
    void legacyDisabledGlobalStatusRejectsWithdrawal() {
        OrderUser user = new OrderUser();
        user.setId(7L);
        user.setWithdrawalStatus("0");
        GoodsMemberLevel level = new GoodsMemberLevel();
        level.setId(1L);
        user.setMemberLevel(level);

        when(userMapper.selectWithdrawalUserById(7L)).thenReturn(user);
        when(passwordService.verify(user, "secret"))
                .thenReturn(TradePasswordVerificationService.Result.SUCCESS);
        when(userMapper.selectWithdrawalUserByIdForUpdate(7L)).thenReturn(user);
        when(snapshotService.snapshot()).thenReturn(new TradeConfigSnapshotService.TradeConfigSnapshot(
                Map.of("memberWithdrawalStatus", "1"), "UTC"));

        AccountApiException exception = assertThrows(AccountApiException.class,
                () -> service().submit(7L, new WithdrawalRequest(
                        UUID.randomUUID(), new BigDecimal("10.00"), "secret", 3L)));

        assertEquals(AccountErrorCodes.WITHDRAWAL_DISABLED, exception.getLegacyCode());
        verify(accountMapper, never()).selectActiveByIdAndUserId(any(), any());
    }

    @Test
    void legacyEnabledLevelMinimumBalanceRestrictionIsApplied() {
        OrderUser user = new OrderUser();
        user.setId(7L);
        user.setBalance(new BigDecimal("100.00"));
        user.setWithdrawalStatus("0");
        GoodsMemberLevel level = new GoodsMemberLevel();
        level.setId(1L);
        level.setMinBalance(new BigDecimal("95.00"));
        user.setMemberLevel(level);

        when(userMapper.selectWithdrawalUserById(7L)).thenReturn(user);
        when(passwordService.verify(user, "secret"))
                .thenReturn(TradePasswordVerificationService.Result.SUCCESS);
        when(userMapper.selectWithdrawalUserByIdForUpdate(7L)).thenReturn(user);
        when(snapshotService.snapshot()).thenReturn(new TradeConfigSnapshotService.TradeConfigSnapshot(
                Map.of(
                        "memberWithdrawalStatus", "0",
                        "withdrawalRestrictLevelMinBalance", "0",
                        "withdrawalTimeRange", List.of("00:00", "00:00")),
                "UTC"));

        AccountApiException exception = assertThrows(AccountApiException.class,
                () -> service().submit(7L, new WithdrawalRequest(
                        UUID.randomUUID(), new BigDecimal("10.00"), "secret", 3L)));

        assertEquals(AccountErrorCodes.LEVEL_MINIMUM_BALANCE, exception.getLegacyCode());
        verify(accountMapper, never()).selectActiveByIdAndUserId(any(), any());
    }

    @Test
    void overnightWithdrawalWindowCrossesMidnight() {
        List<String> range = List.of("22:00", "06:00");

        assertEquals(true, WithdrawalApplicationService.withinTimeRange(
                range, LocalTime.of(23, 30)));
        assertEquals(true, WithdrawalApplicationService.withinTimeRange(
                range, LocalTime.of(5, 30)));
        assertEquals(false, WithdrawalApplicationService.withinTimeRange(
                range, LocalTime.NOON));
    }

    @Test
    void invalidHistoryStatusDoesNotLeavePageHelperStateBehind() {
        PageHelper.clearPage();

        assertThrows(AccountApiException.class,
                () -> service().withdrawalHistory(7L, "invalid", 1, 20));

        assertNull(PageHelper.getLocalPage());
    }

    @Test
    void invalidHistoryPaginationDoesNotLeavePageHelperStateBehind() {
        PageHelper.clearPage();

        assertThrows(AccountApiException.class,
                () -> service().withdrawalHistory(7L, null, -1, 20));
        assertNull(PageHelper.getLocalPage());

        assertThrows(AccountApiException.class,
                () -> service().withdrawalHistory(7L, null, 1, 101));
        assertNull(PageHelper.getLocalPage());
    }

    @Test
    void publicWithdrawalHistoryKeepsTheStoredAccountMask() {
        PageHelper.clearPage();
        OrderWithdrawal row = withdrawal(8L, "2");
        row.setAccountMask("CashApp 0x1234****cdef");
        when(withdrawalMapper.selectPublicByUser(7L, null)).thenReturn(List.of(row));

        try {
            var page = service().withdrawalHistory(7L, null, 1, 20);
            var response = (com.order.api.controller.dto.AccountApiDtos.WithdrawalHistoryResponse)
                    page.getRows().get(0);

            assertEquals("CashApp 0x1234****cdef", response.account());
        } finally {
            PageHelper.clearPage();
        }
    }

    @Test
    void terminalWithdrawalCannotBeRefundedAgain() {
        OrderWithdrawal existing = withdrawal(8L, "3");
        when(withdrawalMapper.selectForUpdate(8L)).thenReturn(existing);

        assertThrows(AccountApiException.class, () -> service().review(8L, "3", "again"));
        verify(userMapper, never()).creditBalance(existing.getUserId(), existing.getAmount());
    }

    @Test
    void rejectionUsesStoredAmountAndRefundsOnce() {
        OrderWithdrawal existing = withdrawal(8L, "1");
        existing.setBusinessDate(LocalDate.of(2026, 7, 17));
        OrderUser user = new OrderUser();
        user.setId(7L);
        user.setBalance(new BigDecimal("90.00"));
        when(withdrawalMapper.selectForUpdate(8L)).thenReturn(existing);
        when(userMapper.selectWithdrawalUserByIdForUpdate(7L)).thenReturn(user);
        when(snapshotService.snapshot()).thenReturn(new TradeConfigSnapshotService.TradeConfigSnapshot(
                Map.of(), "UTC"));
        when(userMapper.creditBalance(7L, new BigDecimal("10.00"))).thenReturn(1);
        when(withdrawalMapper.transitionStatus(8L, "1", "3", "rejected", "system")).thenReturn(1);

        service().review(8L, "3", "rejected");

        verify(userMapper).creditBalance(7L, new BigDecimal("10.00"));
        verify(transactionService).recordFlowWithTransactionCode(
                7L, "txjd", new BigDecimal("10.00"), new BigDecimal("90.00"),
                "W-1", "withdrawal-unfreeze:W-1");
        verify(withdrawalMapper).releaseDailyQuota(LocalDate.of(2026, 7, 17),
                new BigDecimal("10.00"));
    }

    @Test
    void approvalCreatesWithdrawalNotification() {
        OrderWithdrawal existing = withdrawal(8L, "1");
        OrderUser user = new OrderUser();
        user.setId(7L);
        user.setBalance(new BigDecimal("90.00"));
        when(withdrawalMapper.selectForUpdate(8L)).thenReturn(existing);
        when(userMapper.selectWithdrawalUserByIdForUpdate(7L)).thenReturn(user);
        when(userMapper.creditBalance(7L, new BigDecimal("10.00"))).thenReturn(1);
        when(userMapper.debitBalance(7L, new BigDecimal("10.00"))).thenReturn(1);
        when(withdrawalMapper.transitionStatus(8L, "1", "2", null, "system")).thenReturn(1);

        service().review(8L, "2", null);

        verify(transactionService).recordFlowWithTransactionCode(
                7L, "txjd", new BigDecimal("10.00"), new BigDecimal("90.00"),
                "W-1", "withdrawal-unfreeze:W-1");
        verify(transactionService).recordFlowWithTransactionCode(
                7L, "tx", new BigDecimal("-10.00"), new BigDecimal("100.00"),
                "W-1", "withdrawal:W-1");
        verify(siteMessageNotificationService).createForTransaction(
                7L,
                "txwc",
                new BigDecimal("10.00"),
                new BigDecimal("90.00"),
                new BigDecimal("90.00"));
    }

    @Test
    void adminProjectionRevealsTheSelectedAccountForLegacyWithdrawals() {
        OrderWithdrawal existing = withdrawal(8L, "2");
        existing.setWithdrawalAccountId(3L);
        GoodsWithdrawalAccount account = new GoodsWithdrawalAccount();
        account.setId(3L);
        account.setUserId(7L);
        account.setType("1");
        account.setWalletName("PayPal");
        account.setWalletAddress("full-wallet-address");
        account.setWalletAddressMask("full-w****ress");
        existing.setWithdrawalAccountInfo(account);

        when(cipher.reveal(account)).thenReturn(account);

        OrderWithdrawal response = service().revealAdminAccount(existing);

        assertEquals("PayPal", response.getWithdrawalAccountInfo().getWalletName());
        assertEquals("full-wallet-address", response.getWithdrawalAccountInfo().getWalletAddress());
        assertEquals(
                "钱包名称: PayPal; 钱包地址: full-wallet-address",
                response.getAdminAccountDisplay());
        assertNull(response.getWithdrawalAccountInfo().getWalletAddressMask());
    }

    @Test
    void sensitiveEditReadStillLoadsTheOwnedLegacyAccount() {
        OrderWithdrawal existing = withdrawal(8L, "2");
        existing.setWithdrawalAccountId(3L);
        GoodsWithdrawalAccount account = new GoodsWithdrawalAccount();
        account.setId(3L);
        account.setUserId(7L);
        account.setType("1");
        account.setWalletName("PayPal");
        account.setWalletAddress("full-wallet-address");

        when(withdrawalMapper.selectOrderWithdrawalById(8L)).thenReturn(existing);
        when(accountMapper.selectActiveByIdAndUserId(3L, 7L)).thenReturn(account);
        when(cipher.reveal(account)).thenReturn(account);

        var response = service().sensitiveAccount(8L);

        assertEquals("PayPal", response.walletName());
        assertEquals("full-wallet-address", response.walletAddress());
        verify(accountMapper).selectActiveByIdAndUserId(3L, 7L);
    }

    @Test
    void adminProjectionUsesTheFullEncryptedBankSnapshot() {
        OrderWithdrawal existing = withdrawal(9L, "2");
        existing.setWithdrawalAccountId(4L);
        existing.setAccountSnapshotEncrypted("encrypted-bank-snapshot");

        when(cipher.decryptSnapshot(7L, "encrypted-bank-snapshot")).thenReturn(Map.of(
                "type", "0",
                "withdrawalType", "Bank transfer",
                "bankName", "Example Bank",
                "branchName", "Central Branch",
                "bankAccount", "6222021234567890123",
                "accountHolder", "Jane Customer",
                "accountName", "Primary account"));

        OrderWithdrawal response = service().revealAdminAccount(existing);

        GoodsWithdrawalAccount account = response.getWithdrawalAccountInfo();
        assertEquals("6222021234567890123", account.getBankAccount());
        assertEquals("Jane Customer", account.getAccountHolder());
        assertEquals("Primary account", account.getAccountName());
        assertEquals(
                "银行名称: Example Bank; 支行名称: Central Branch; 账户持有人: Jane Customer; "
                        + "账户名称: Primary account; 银行账号: 6222021234567890123",
                response.getAdminAccountDisplay());
        assertNull(account.getBankAccountMask());
        verify(cipher, never()).reveal(any());
    }

    @Test
    void adminCanUpdateTheWithdrawalAddressAndEncryptedSnapshot() {
        OrderWithdrawal existing = withdrawal(8L, "1");
        existing.setWithdrawalAccountId(3L);
        GoodsWithdrawalAccount account = new GoodsWithdrawalAccount();
        account.setId(3L);
        account.setUserId(7L);
        account.setType("1");
        account.setWalletName("Old wallet");
        account.setWalletAddress("old-address");

        when(withdrawalMapper.selectForUpdate(8L)).thenReturn(existing);
        when(accountMapper.selectActiveByIdAndUserId(3L, 7L)).thenReturn(account);
        when(cipher.reveal(account)).thenReturn(account);
        when(cipher.encryptSnapshot(account)).thenReturn("encrypted-snapshot");
        when(cipher.displayMask(account)).thenReturn("Wallet ****5678");
        when(accountMapper.updateOwnedAccount(account)).thenReturn(1);
        when(withdrawalMapper.updateAccountSnapshot(
                8L, "encrypted-snapshot", "Wallet ****5678", "admin")).thenReturn(1);

        service().updateSensitiveAccount(
                8L,
                new SensitiveWithdrawalAccountUpdateRequest(
                        null, null, null, null, null, null,
                        "Primary", "PayPal", "0x12345678", null),
                "admin");

        assertEquals("Primary", account.getAccountName());
        assertEquals("PayPal", account.getWalletName());
        assertEquals("0x12345678", account.getWalletAddress());
        verify(cipher).protect(account);
        verify(accountMapper).updateOwnedAccount(account);
        verify(withdrawalMapper).updateAccountSnapshot(
                8L, "encrypted-snapshot", "Wallet ****5678", "admin");
    }

    private OrderWithdrawal withdrawal(Long id, String status) {
        OrderWithdrawal withdrawal = new OrderWithdrawal();
        withdrawal.setId(id);
        withdrawal.setUserId(7L);
        withdrawal.setAmount(new BigDecimal("10.00"));
        withdrawal.setFee(new BigDecimal("0.50"));
        withdrawal.setNetAmount(new BigDecimal("9.50"));
        withdrawal.setStatus(status);
        withdrawal.setOrderNumber("W-1");
        withdrawal.setCreateTime(new Date());
        return withdrawal;
    }

    private WithdrawalApplicationService service() {
        return new WithdrawalApplicationService(
                userMapper, withdrawalMapper, accountMapper, rechargeMapper, flowMapper,
                passwordService, sequenceService, transactionService,
                siteMessageNotificationService, snapshotService, cipher);
    }
}
