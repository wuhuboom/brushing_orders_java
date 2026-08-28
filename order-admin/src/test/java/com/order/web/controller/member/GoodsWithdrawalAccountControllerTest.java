package com.order.web.controller.member;

import com.order.api.service.WithdrawalAccountApplicationService;
import com.order.common.core.domain.AjaxResult;
import com.order.member.domain.GoodsWithdrawalAccount;
import com.order.member.service.IGoodsWithdrawalAccountService;
import com.order.member.service.IOrderWithdrawalTypeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoodsWithdrawalAccountControllerTest {
    @Mock WithdrawalAccountApplicationService applicationService;
    @Mock IOrderWithdrawalTypeService withdrawalTypeService;
    @InjectMocks GoodsWithdrawalAccountController controller;

    @Test
    void detailUsesTheDecryptingApplicationServiceAndSupportsDrawerPermissions() throws Exception {
        GoodsWithdrawalAccount account = new GoodsWithdrawalAccount();
        account.setId(3L);
        account.setUserId(9L);
        account.setIsDefault("0");
        when(applicationService.getAdmin(3L)).thenReturn(account);

        AjaxResult result = controller.getInfo(3L);

        assertTrue(result.isSuccess());
        assertSame(account, result.get(AjaxResult.DATA_TAG));
        verify(applicationService).getAdmin(3L);

        Method method = GoodsWithdrawalAccountController.class.getDeclaredMethod("getInfo", Long.class);
        assertEquals(
                "@ss.hasAnyPermi('member:withdrawalAcc:query,member:withdrawalAcc:list,member:withdrawalAcc:edit')",
                method.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void mutationsDelegateOnlyToTheOwnerAwareApplicationService() {
        GoodsWithdrawalAccount account = new GoodsWithdrawalAccount();
        account.setId(3L);
        account.setUserId(9L);

        assertTrue(controller.add(account).isSuccess());
        assertTrue(controller.edit(account).isSuccess());
        assertTrue(controller.remove(new Long[] { 3L }).isSuccess());

        verify(applicationService).createAdmin(account);
        verify(applicationService).updateAdmin(account);
        verify(applicationService).deleteAdmin(argThat(ids -> Arrays.equals(ids, new Long[] { 3L })));
        assertFalse(Arrays.stream(GoodsWithdrawalAccountController.class.getDeclaredFields())
                .anyMatch(field -> field.getType().equals(IGoodsWithdrawalAccountService.class)));
    }
}
