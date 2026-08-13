package com.brushing.api.controller;

import com.brushing.api.controller.vo.UserWalletDto;
import com.brushing.common.core.domain.AjaxResult;
import com.brushing.member.domain.OrderBankWallet;
import com.brushing.member.domain.OrderMemberUser;
import com.brushing.member.service.IOrderBankWalletService;
import com.brushing.member.service.IOrderMemberUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountControllerWalletTest {

    private AccountController controller;
    private IOrderMemberUserService memberUserService;
    private IOrderBankWalletService bankWalletService;

    @BeforeEach
    void setUp() {
        controller = new AccountController();
        memberUserService = mock(IOrderMemberUserService.class);
        bankWalletService = mock(IOrderBankWalletService.class);
        ReflectionTestUtils.setField(controller, "memberUserService", memberUserService);
        ReflectionTestUtils.setField(controller, "bankWalletService", bankWalletService);
    }

    @Test
    void addWalletInsertsWhenUserHasNoWallet() {
        when(memberUserService.findByUsername("alice")).thenReturn(user(7L));
        when(bankWalletService.selectOrderBankWalletList(any(OrderBankWallet.class)))
                .thenReturn(Collections.emptyList());
        when(bankWalletService.insertOrderBankWallet(any(OrderBankWallet.class))).thenReturn(1);

        AjaxResult result = controller.addWallet(wallet("USDT", "T-address", "TRC20"), "alice");

        assertEquals(200, result.get(AjaxResult.CODE_TAG));
        ArgumentCaptor<OrderBankWallet> captor = ArgumentCaptor.forClass(OrderBankWallet.class);
        verify(bankWalletService).insertOrderBankWallet(captor.capture());
        OrderBankWallet saved = captor.getValue();
        assertEquals(7L, saved.getUserId());
        assertEquals("2", saved.getType());
        assertEquals("USDT", saved.getWalletType());
        assertEquals("T-address", saved.getWalletAddress());
        assertEquals("TRC20", saved.getBankType());
        verify(bankWalletService, never()).updateOrderBankWallet(any(OrderBankWallet.class));
    }

    @Test
    void addWalletUpdatesFirstWalletWhenUserHasSeveral() {
        when(memberUserService.findByUsername("alice")).thenReturn(user(7L));
        OrderBankWallet first = storedWallet(11L, "old-1", "old-address-1", "old-network-1");
        OrderBankWallet second = storedWallet(12L, "old-2", "old-address-2", "old-network-2");
        when(bankWalletService.selectOrderBankWalletList(any(OrderBankWallet.class)))
                .thenReturn(List.of(first, second));
        when(bankWalletService.updateOrderBankWallet(any(OrderBankWallet.class))).thenReturn(1);

        controller.addWallet(wallet("USDT", "new-address", "ERC20"), "alice");

        ArgumentCaptor<OrderBankWallet> captor = ArgumentCaptor.forClass(OrderBankWallet.class);
        verify(bankWalletService).updateOrderBankWallet(captor.capture());
        assertEquals(11L, captor.getValue().getId());
        assertEquals("USDT", captor.getValue().getWalletType());
        assertEquals("new-address", captor.getValue().getWalletAddress());
        assertEquals("ERC20", captor.getValue().getBankType());
        verify(bankWalletService, never()).insertOrderBankWallet(any(OrderBankWallet.class));
    }

    @Test
    void getWalletReturnsOnlyFirstWallet() {
        when(memberUserService.findByUsername("alice")).thenReturn(user(7L));
        when(bankWalletService.selectOrderBankWalletList(any(OrderBankWallet.class))).thenReturn(List.of(
                storedWallet(11L, "USDT", "T-address", "TRC20"),
                storedWallet(12L, "BTC", "bc-address", "Bitcoin")));

        AjaxResult result = controller.getWallet("alice");

        UserWalletDto data = (UserWalletDto) result.get(AjaxResult.DATA_TAG);
        assertEquals(11L, data.getId());
        assertEquals("USDT", data.getWallet());
        assertEquals("T-address", data.getAddress());
        assertEquals("TRC20", data.getNetwork());
    }

    @Test
    void getWalletIncludesNullDataWhenUserHasNoWallet() throws Exception {
        when(memberUserService.findByUsername("alice")).thenReturn(user(7L));
        when(bankWalletService.selectOrderBankWalletList(any(OrderBankWallet.class)))
                .thenReturn(Collections.emptyList());

        AjaxResult result = controller.getWallet("alice");

        assertEquals(200, result.get(AjaxResult.CODE_TAG));
        assertTrue(result.containsKey(AjaxResult.DATA_TAG));
        assertNull(result.get(AjaxResult.DATA_TAG));
        assertEquals("The operation was successful", result.get(AjaxResult.MSG_TAG));
        assertTrue(new ObjectMapper().writeValueAsString(result).contains("\"data\":null"));
    }

    private static OrderMemberUser user(Long id) {
        OrderMemberUser user = new OrderMemberUser();
        user.setId(id);
        return user;
    }

    private static UserWalletDto wallet(String wallet, String address, String network) {
        UserWalletDto dto = new UserWalletDto();
        dto.setWallet(wallet);
        dto.setAddress(address);
        dto.setNetwork(network);
        return dto;
    }

    private static OrderBankWallet storedWallet(Long id, String wallet, String address, String network) {
        OrderBankWallet result = new OrderBankWallet();
        result.setId(id);
        result.setWalletType(wallet);
        result.setWalletAddress(address);
        result.setBankType(network);
        return result;
    }
}
