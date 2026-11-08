package com.order.web.controller.member;

import com.order.common.core.domain.AjaxResult;
import com.order.member.domain.OrderUser;
import com.order.member.service.IOrderUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderUserProductMatchingTest {
    @Mock IOrderUserService users;
    @InjectMocks OrderUserController controller;

    @Test
    void savesAnEnabledPersonalPercentageRange() {
        OrderUser existing = member("1", null, null);
        OrderUser incoming = member("0", "25.50", "100");
        when(users.selectOrderUserById(7L)).thenReturn(existing);
        when(users.updateOrderUser(incoming)).thenReturn(1);

        assertTrue(controller.edit(incoming).isSuccess());

        verify(users).updateOrderUser(incoming);
        assertEquals(new BigDecimal("25.50"), incoming.getProductMatchMin());
        assertEquals(4L, incoming.getVersion());
    }

    @Test
    void partialRatioEditValidatesAgainstTheSavedMaximum() {
        OrderUser existing = member("0", "20", "50");
        OrderUser incoming = member(null, "60", null);
        when(users.selectOrderUserById(7L)).thenReturn(existing);

        AjaxResult result = controller.edit(incoming);

        assertTrue(result.isError());
        assertEquals("最小匹配比例不能大于最大匹配比例", result.get(AjaxResult.MSG_TAG));
        verify(users, never()).updateOrderUser(any());
    }

    @Test
    void enablesUsingAnExistingCompleteRange() {
        OrderUser existing = member("1", "20", "50");
        OrderUser incoming = member("0", null, null);
        when(users.selectOrderUserById(7L)).thenReturn(existing);
        when(users.updateOrderUser(incoming)).thenReturn(1);

        assertTrue(controller.edit(incoming).isSuccess());
        verify(users).updateOrderUser(incoming);
    }

    @Test
    void explicitEnableRejectsMissingOrZeroRatios() {
        when(users.selectOrderUserById(7L)).thenReturn(member("1", null, null));
        for (OrderUser incoming : List.of(
                member("0", null, null), member("0", "10", null),
                member("0", null, "50"), member("0", "0", "50"),
                member("0", "0", "0"))) {
            assertTrue(controller.edit(incoming).isError());
        }
        verify(users, never()).updateOrderUser(any());
    }

    @Test
    void rejectsInvalidRangesEvenWhileDisabled() {
        when(users.selectOrderUserById(7L)).thenReturn(member("1", null, null));
        for (OrderUser incoming : List.of(
                member("1", "-1", "20"), member("1", "10", "100.01"),
                member("1", "60", "50"), member("1", "1.234", "50"),
                member("unexpected", "10", "50"))) {
            assertTrue(controller.edit(incoming).isError());
        }
        verify(users, never()).updateOrderUser(any());
    }

    @Test
    void disabledMatchingAllowsEmptyAndZeroRanges() {
        when(users.selectOrderUserById(7L)).thenReturn(member("0", null, null));
        when(users.updateOrderUser(any())).thenReturn(1);
        assertTrue(controller.edit(member("1", null, null)).isSuccess());
        assertTrue(controller.edit(member("1", "0", "0")).isSuccess());
    }

    @Test
    void unrelatedEditsDoNotActivateOrRejectHistoricalEnabledMembersWithNoRatios() {
        OrderUser incoming = member(null, null, null);
        incoming.setRemarks("updated note");
        when(users.selectOrderUserById(7L)).thenReturn(member("0", null, null));
        when(users.updateOrderUser(incoming)).thenReturn(1);

        assertTrue(controller.edit(incoming).isSuccess());
        verify(users).updateOrderUser(incoming);
    }

    @Test
    void creationCannotBypassPersonalMatchingValidation() {
        assertTrue(controller.add(member("0", null, null)).isError());
        verify(users, never()).insertOrderUser(any());
        verify(users, never()).selectOrderUserByName(any());
    }

    private static OrderUser member(String enabled, String min, String max) {
        OrderUser user = new OrderUser();
        user.setId(7L);
        user.setParentId(0L);
        user.setVersion(4L);
        user.setProductMatching(enabled);
        user.setProductMatchMin(min == null ? null : new BigDecimal(min));
        user.setProductMatchMax(max == null ? null : new BigDecimal(max));
        return user;
    }
}
