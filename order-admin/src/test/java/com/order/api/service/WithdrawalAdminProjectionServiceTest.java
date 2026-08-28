package com.order.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.order.member.domain.GoodsWithdrawalAccount;
import com.order.member.domain.OrderWithdrawal;
import com.order.member.mapper.GoodsRechargeRecordMapper;
import com.order.member.mapper.GoodsTransactionFlowMapper;
import com.order.member.mapper.GoodsWithdrawalAccountMapper;
import com.order.member.mapper.OrderUserMapper;
import com.order.member.mapper.OrderWithdrawalMapper;
import com.order.member.service.IOrderSequenceManagerService;
import com.order.member.service.ITransactionService;
import com.order.member.service.SiteMessageNotificationService;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WithdrawalAdminProjectionServiceTest
{
    private final AccountDataCipher cipher = mock(AccountDataCipher.class);

    @Test
    void legacyWalletAccountIsRevealedAndMaskFieldsAreRemoved()
    {
        OrderWithdrawal withdrawal = withdrawal(8L);
        GoodsWithdrawalAccount account = new GoodsWithdrawalAccount();
        account.setId(3L);
        account.setUserId(7L);
        account.setType("1");
        account.setAccountName("Primary");
        account.setWalletName("PayPal");
        account.setWalletAddress("full-wallet-address");
        account.setWalletAddressMask("full-w****ress");
        withdrawal.setWithdrawalAccountInfo(account);
        when(cipher.reveal(account)).thenReturn(account);

        OrderWithdrawal response = service().revealAdminAccount(withdrawal);

        assertEquals("full-wallet-address", response.getWithdrawalAccountInfo().getWalletAddress());
        assertEquals(
                "账户名称: Primary; 钱包名称: PayPal; 钱包地址: full-wallet-address",
                response.getAdminAccountDisplay());
        assertNull(response.getWithdrawalAccountInfo().getWalletAddressMask());
    }

    @Test
    void encryptedBankSnapshotIsReturnedInFullForAdmin()
    {
        OrderWithdrawal withdrawal = withdrawal(9L);
        withdrawal.setWithdrawalAccountId(4L);
        withdrawal.setAccountSnapshotEncrypted("encrypted-bank-snapshot");
        when(cipher.decryptSnapshot(7L, "encrypted-bank-snapshot")).thenReturn(Map.of(
                "type", "0",
                "withdrawalType", "Bank transfer",
                "bankName", "Example Bank",
                "branchName", "Central Branch",
                "bankAccount", "6222021234567890123",
                "accountHolder", "Jane Customer",
                "accountName", "Primary account"));

        OrderWithdrawal response = service().revealAdminAccount(withdrawal);

        GoodsWithdrawalAccount account = response.getWithdrawalAccountInfo();
        assertEquals("6222021234567890123", account.getBankAccount());
        assertEquals("Jane Customer", account.getAccountHolder());
        assertEquals("Primary account", account.getAccountName());
        assertEquals(
                "银行名称: Example Bank; 支行名称: Central Branch; 账户持有人: Jane Customer; "
                        + "账户名称: Primary account; 银行账号: 6222021234567890123",
                response.getAdminAccountDisplay());
        assertNull(account.getBankAccountMask());
        verify(cipher, never()).reveal(any(GoodsWithdrawalAccount.class));
    }

    private OrderWithdrawal withdrawal(Long id)
    {
        OrderWithdrawal withdrawal = new OrderWithdrawal();
        withdrawal.setId(id);
        withdrawal.setUserId(7L);
        return withdrawal;
    }

    private WithdrawalApplicationService service()
    {
        return new WithdrawalApplicationService(
                mock(OrderUserMapper.class),
                mock(OrderWithdrawalMapper.class),
                mock(GoodsWithdrawalAccountMapper.class),
                mock(GoodsRechargeRecordMapper.class),
                mock(GoodsTransactionFlowMapper.class),
                mock(TradePasswordVerificationService.class),
                mock(IOrderSequenceManagerService.class),
                mock(ITransactionService.class),
                mock(SiteMessageNotificationService.class),
                mock(TradeConfigSnapshotService.class),
                cipher);
    }
}
