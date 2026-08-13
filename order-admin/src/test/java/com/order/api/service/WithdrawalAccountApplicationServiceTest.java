package com.order.api.service;

import com.order.api.controller.dto.AccountApiDtos.WithdrawalAccountRequest;
import com.order.member.domain.GoodsWithdrawalAccount;
import com.order.member.domain.OrderWithdrawalType;
import com.order.member.mapper.GoodsWithdrawalAccountMapper;
import com.order.member.mapper.OrderUserMapper;
import com.order.member.mapper.OrderWithdrawalMapper;
import com.order.member.service.IOrderConfigService;
import com.order.member.service.IOrderWithdrawalTypeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicReference;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WithdrawalAccountApplicationServiceTest {
    @Mock GoodsWithdrawalAccountMapper accountMapper;
    @Mock OrderWithdrawalMapper withdrawalMapper;
    @Mock OrderUserMapper userMapper;
    @Mock IOrderWithdrawalTypeService typeService;
    @Mock IOrderConfigService configService;
    @Mock AccountDataCipher cipher;

    @Test
    void derivesTypeAndMakesFirstAccountDefault() {
        OrderWithdrawalType type = new OrderWithdrawalType();
        type.setId(2L);
        type.setType("0");
        type.setName("Bank");
        when(typeService.selectOrderWithdrawalTypeById(2L)).thenReturn(type);
        when(userMapper.lockUserById(9L)).thenReturn(9L);
        when(accountMapper.existsDefaultByUserId(9L)).thenReturn(0);
        AtomicReference<GoodsWithdrawalAccount> saved = new AtomicReference<>();
        when(accountMapper.insertGoodsWithdrawalAccount(any())).thenAnswer(invocation -> {
            GoodsWithdrawalAccount account = invocation.getArgument(0);
            account.setId(33L);
            saved.set(account);
            return 1;
        });
        when(accountMapper.selectActiveByIdAndUserId(33L, 9L))
                .thenAnswer(invocation -> saved.get());
        WithdrawalAccountApplicationService service = service();

        var response = service.create(9L, new WithdrawalAccountRequest(
                "2", false, "Bank A", null, null, null,
                "12345678", "Alice", null, null, null, null));

        assertEquals("0", saved.get().getType());
        assertEquals("0", saved.get().getIsDefault());
        assertEquals(true, response.isDefault());
    }

    @Test
    void walletAttachmentIsSavedAndReturned() {
        OrderWithdrawalType type = new OrderWithdrawalType();
        type.setId(3L);
        type.setType("1");
        type.setName("USDT");
        when(typeService.selectOrderWithdrawalTypeById(3L)).thenReturn(type);
        when(userMapper.lockUserById(9L)).thenReturn(9L);
        when(accountMapper.existsDefaultByUserId(9L)).thenReturn(0);
        AtomicReference<GoodsWithdrawalAccount> saved = new AtomicReference<>();
        when(accountMapper.insertGoodsWithdrawalAccount(any())).thenAnswer(invocation -> {
            GoodsWithdrawalAccount account = invocation.getArgument(0);
            account.setId(34L);
            saved.set(account);
            return 1;
        });
        when(accountMapper.selectActiveByIdAndUserId(34L, 9L))
                .thenAnswer(invocation -> saved.get());

        var response = service().create(9L, new WithdrawalAccountRequest(
                "3", true, null, null, null, null,
                null, null, null, "USDT", "0x123456",
                "/profile/upload/wallet-proof.png"));

        assertEquals("/profile/upload/wallet-proof.png", saved.get().getAttachment());
        assertEquals("/profile/upload/wallet-proof.png", response.attachment());
    }

    @Test
    void publicListMasksSensitiveAccountFields() {
        GoodsWithdrawalAccount account = new GoodsWithdrawalAccount();
        account.setId(3L);
        account.setUserId(9L);
        account.setType("0");
        account.setBankAccount("6222021234567890");
        account.setAccountHolder("Alice");
        account.setAccountName("Primary");
        when(accountMapper.selectActiveByUserId(9L)).thenReturn(List.of(account));

        var response = service().list(9L).get(0);

        assertEquals("****7890", response.bankAccount());
        assertEquals("A***", response.accountHolder());
        assertEquals("P***", response.accountName());
    }

    @Test
    void listsConfiguredWithdrawalTypesWithANonNullFilterAndStableOrder() {
        OrderWithdrawalType usdt = new OrderWithdrawalType();
        usdt.setId(3L);
        usdt.setName("USDT");
        usdt.setType("1");
        usdt.setSortOrder(20L);
        OrderWithdrawalType bank = new OrderWithdrawalType();
        bank.setId(2L);
        bank.setName("Bank");
        bank.setType("0");
        bank.setSortOrder(10L);
        when(typeService.selectOrderWithdrawalTypeList(any(OrderWithdrawalType.class)))
                .thenReturn(List.of(usdt, bank));

        var response = service().listTypes();

        assertEquals(List.of("Bank", "USDT"),
                response.stream().map(item -> item.typeName()).toList());
        verify(typeService).selectOrderWithdrawalTypeList(any(OrderWithdrawalType.class));
    }

    @Test
    void deletingDefaultAccountPromotesNewestRemainingAccount() {
        GoodsWithdrawalAccount account = new GoodsWithdrawalAccount();
        account.setId(3L);
        account.setUserId(9L);
        account.setIsDefault("0");
        when(configService.getConfigValue("trade", "allowModifyWithdrawalAddress"))
                .thenReturn(Optional.of("1"));
        when(userMapper.lockUserById(9L)).thenReturn(9L);
        when(accountMapper.selectActiveByIdAndUserId(3L, 9L)).thenReturn(account);
        when(withdrawalMapper.existsPendingByAccountId(3L)).thenReturn(0);
        when(accountMapper.softDeleteOwned(3L, 9L)).thenReturn(1);

        service().delete(9L, 3L);

        verify(accountMapper).setNewestActiveAsDefault(9L);
    }

    @Test
    void configurationCanDisableExistingAccountChanges() {
        when(configService.getConfigValue("trade", "allowModifyWithdrawalAddress"))
                .thenReturn(Optional.of("0"));

        assertThrows(AccountApiException.class, () -> service().delete(9L, 3L));

        verify(userMapper, never()).lockUserById(9L);
    }

    private WithdrawalAccountApplicationService service() {
        return new WithdrawalAccountApplicationService(
                accountMapper, withdrawalMapper, userMapper, typeService, configService, cipher);
    }
}
