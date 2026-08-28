package com.order.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.api.controller.dto.AccountApiDtos.WithdrawalAccountRequest;
import com.order.common.exception.ServiceException;
import com.order.member.domain.GoodsWithdrawalAccount;
import com.order.member.domain.OrderWithdrawalType;
import com.order.member.mapper.GoodsWithdrawalAccountMapper;
import com.order.member.mapper.OrderUserMapper;
import com.order.member.mapper.OrderWithdrawalMapper;
import com.order.member.service.IOrderConfigService;
import com.order.member.service.IOrderWithdrawalTypeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        when(accountMapper.setDefaultByIdAndUserId(33L, 9L)).thenReturn(1);
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
        when(accountMapper.setDefaultByIdAndUserId(34L, 9L)).thenReturn(1);

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
    void adminListRevealsRowsWithoutReplacingThePageHelperList() {
        GoodsWithdrawalAccount criteria = new GoodsWithdrawalAccount();
        criteria.setUserId(9L);
        criteria.setIsDefault("0");
        GoodsWithdrawalAccount account = new GoodsWithdrawalAccount();
        account.setId(3L);
        account.setUserId(9L);
        account.setBankAccountMask("****0123");
        account.setAccountHolderMask("J***");
        account.setAccountNameMask("P***");
        account.setWalletAddressMask("0x1234****cdef");
        List<GoodsWithdrawalAccount> mapperList = new ArrayList<>(List.of(account));
        when(accountMapper.selectGoodsWithdrawalAccountList(criteria)).thenReturn(mapperList);

        List<GoodsWithdrawalAccount> result = service().listAdmin(criteria);

        assertSame(mapperList, result);
        assertNull(account.getBankAccountMask());
        assertNull(account.getAccountHolderMask());
        assertNull(account.getAccountNameMask());
        assertNull(account.getWalletAddressMask());
        verify(cipher).reveal(account);
    }

    @Test
    void adminCreateUsesOwnedEncryptedWriteAndKeepsStringDefaultContract() {
        OrderWithdrawalType type = bankType();
        when(typeService.selectOrderWithdrawalTypeById(2L)).thenReturn(type);
        when(userMapper.lockUserById(9L)).thenReturn(9L);
        when(accountMapper.existsDefaultByUserId(9L)).thenReturn(1);
        AtomicReference<GoodsWithdrawalAccount> saved = new AtomicReference<>();
        when(accountMapper.insertGoodsWithdrawalAccount(any())).thenAnswer(invocation -> {
            GoodsWithdrawalAccount account = invocation.getArgument(0);
            account.setId(33L);
            saved.set(account);
            return 1;
        });
        when(accountMapper.selectActiveByIdAndUserId(33L, 9L))
                .thenAnswer(invocation -> saved.get());
        GoodsWithdrawalAccount existingDefault = bankAccount(8L, 9L, "0");
        when(accountMapper.selectActiveByUserId(9L)).thenReturn(List.of(existingDefault));
        when(accountMapper.setDefaultByIdAndUserId(8L, 9L)).thenReturn(1);

        GoodsWithdrawalAccount result = service().createAdmin(bankAccount(null, 9L, "1"));

        assertSame(saved.get(), result);
        assertEquals("1", result.getIsDefault());
        assertEquals(9L, result.getUserId());
        verify(cipher).protect(saved.get());
        verify(cipher).reveal(saved.get());
        verify(configService, never()).getConfigValue(any(), any());
    }

    @Test
    void adminCreateRoundTripsThroughEncryptedOnlyCipher() {
        when(typeService.selectOrderWithdrawalTypeById(2L)).thenReturn(bankType());
        when(userMapper.lockUserById(9L)).thenReturn(9L);
        when(accountMapper.existsDefaultByUserId(9L)).thenReturn(0);
        when(accountMapper.setDefaultByIdAndUserId(33L, 9L)).thenReturn(1);
        AtomicReference<GoodsWithdrawalAccount> persisted = new AtomicReference<>();
        AtomicReference<String> plaintextAtInsert = new AtomicReference<>();
        AtomicReference<String> ciphertextAtInsert = new AtomicReference<>();
        when(accountMapper.insertGoodsWithdrawalAccount(any())).thenAnswer(invocation -> {
            GoodsWithdrawalAccount account = invocation.getArgument(0);
            account.setId(33L);
            plaintextAtInsert.set(account.getBankAccount());
            ciphertextAtInsert.set(account.getBankAccountEncrypted());
            persisted.set(account);
            return 1;
        });
        when(accountMapper.selectActiveByIdAndUserId(33L, 9L))
                .thenAnswer(invocation -> persisted.get());
        String key = Base64.getEncoder().encodeToString(new byte[32]);
        AccountDataCipher encryptedOnlyCipher = new AccountDataCipher(
                "ENCRYPTED_ONLY", "v1", "v1:" + key, new ObjectMapper());
        WithdrawalAccountApplicationService service = new WithdrawalAccountApplicationService(
                accountMapper,
                withdrawalMapper,
                userMapper,
                typeService,
                configService,
                encryptedOnlyCipher);

        GoodsWithdrawalAccount result = service.createAdmin(bankAccount(null, 9L, "0"));

        assertNull(plaintextAtInsert.get());
        assertTrue(ciphertextAtInsert.get().startsWith("v1.v1."));
        assertEquals("12345678", result.getBankAccount());
        assertEquals("0", result.getIsDefault());
    }

    @Test
    void adminUpdateBypassesCustomerSettingButChecksOwnerAndPendingWithdrawal() {
        GoodsWithdrawalAccount existing = bankAccount(3L, 9L, "0");
        when(userMapper.lockUserById(9L)).thenReturn(9L);
        when(accountMapper.selectActiveByIdAndUserId(3L, 9L)).thenReturn(existing);
        when(withdrawalMapper.existsPendingByAccountId(3L)).thenReturn(0);
        when(typeService.selectOrderWithdrawalTypeById(2L)).thenReturn(bankType());
        when(accountMapper.updateOwnedAccount(any())).thenReturn(1);
        when(accountMapper.setDefaultByIdAndUserId(3L, 9L)).thenReturn(1);

        GoodsWithdrawalAccount result = service().updateAdmin(bankAccount(3L, 9L, "0"));

        ArgumentCaptor<GoodsWithdrawalAccount> update = ArgumentCaptor.forClass(GoodsWithdrawalAccount.class);
        verify(accountMapper).updateOwnedAccount(update.capture());
        assertEquals(3L, update.getValue().getId());
        assertEquals(9L, update.getValue().getUserId());
        assertEquals("0", update.getValue().getIsDefault());
        assertSame(existing, result);
        verify(accountMapper).clearDefaultByUserId(9L);
        verify(cipher).protect(update.getValue());
        verify(configService, never()).getConfigValue(any(), any());
    }

    @Test
    void adminUpdateRefusesAnAccountOwnedByAnotherUser() {
        when(userMapper.lockUserById(10L)).thenReturn(10L);
        when(accountMapper.selectActiveByIdAndUserId(3L, 10L)).thenReturn(null);

        assertThrows(ServiceException.class,
                () -> service().updateAdmin(bankAccount(3L, 10L, "1")));

        verify(accountMapper, never()).updateOwnedAccount(any());
        verify(cipher, never()).protect(any());
    }

    @Test
    void adminDeleteIsSoftOwnedPromotesFallbackAndIgnoresCustomerSetting() {
        GoodsWithdrawalAccount existing = bankAccount(3L, 9L, "0");
        when(accountMapper.selectActiveById(3L)).thenReturn(existing);
        when(userMapper.lockUserById(9L)).thenReturn(9L);
        when(accountMapper.selectActiveByIdAndUserId(3L, 9L)).thenReturn(existing);
        when(withdrawalMapper.existsPendingByAccountId(3L)).thenReturn(0);
        when(accountMapper.softDeleteOwned(3L, 9L)).thenReturn(1);

        service().deleteAdmin(new Long[] { 3L, 3L });

        verify(accountMapper).softDeleteOwned(3L, 9L);
        verify(accountMapper).setNewestActiveAsDefault(9L);
        verify(accountMapper, never()).deleteGoodsWithdrawalAccountById(any());
        verify(accountMapper, never()).deleteGoodsWithdrawalAccountByIds(any());
        verify(configService, never()).getConfigValue(any(), any());
    }

    @Test
    void adminMutationStopsWhenPendingWithdrawalUsesTheAccount() {
        GoodsWithdrawalAccount existing = bankAccount(3L, 9L, "1");
        when(userMapper.lockUserById(9L)).thenReturn(9L);
        when(accountMapper.selectActiveByIdAndUserId(3L, 9L)).thenReturn(existing);
        when(withdrawalMapper.existsPendingByAccountId(3L)).thenReturn(1);

        assertThrows(ServiceException.class,
                () -> service().updateAdmin(bankAccount(3L, 9L, "1")));

        verify(accountMapper, never()).updateOwnedAccount(any());
        verify(cipher, never()).protect(any());
    }

    @Test
    void adminDeleteStopsBeforeSoftDeleteWhenPendingWithdrawalUsesTheAccount() {
        GoodsWithdrawalAccount existing = bankAccount(3L, 9L, "0");
        when(accountMapper.selectActiveById(3L)).thenReturn(existing);
        when(userMapper.lockUserById(9L)).thenReturn(9L);
        when(accountMapper.selectActiveByIdAndUserId(3L, 9L)).thenReturn(existing);
        when(withdrawalMapper.existsPendingByAccountId(3L)).thenReturn(1);

        assertThrows(ServiceException.class,
                () -> service().deleteAdmin(new Long[] { 3L }));

        verify(accountMapper, never()).softDeleteOwned(any(), any());
        verify(accountMapper, never()).deleteGoodsWithdrawalAccountById(any());
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

    private OrderWithdrawalType bankType() {
        OrderWithdrawalType type = new OrderWithdrawalType();
        type.setId(2L);
        type.setType("0");
        type.setName("Bank");
        return type;
    }

    private GoodsWithdrawalAccount bankAccount(Long id, Long userId, String isDefault) {
        GoodsWithdrawalAccount account = new GoodsWithdrawalAccount();
        account.setId(id);
        account.setUserId(userId);
        account.setType("0");
        account.setWithdrawalTypeId("2");
        account.setIsDefault(isDefault);
        account.setBankName("Bank A");
        account.setBankAccount("12345678");
        account.setAccountHolder("Alice");
        return account;
    }
}
