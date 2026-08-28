package com.order.web.controller.member;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.order.api.controller.dto.AccountApiDtos.SensitiveWithdrawalAccountUpdateRequest;
import com.order.member.domain.OrderWithdrawal;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class OrderWithdrawalControllerPermissionTest
{
    @Test
    void listReadsFullAccountDataWithoutOpeningAccountMutation()
            throws NoSuchMethodException
    {
        Method list = OrderWithdrawalController.class.getMethod("list", OrderWithdrawal.class);
        Method detail = OrderWithdrawalController.class.getMethod("getInfo", Long.class);
        Method sensitiveRead = OrderWithdrawalController.class.getMethod(
                "getSensitiveAccount", Long.class);
        Method sensitiveUpdate = OrderWithdrawalController.class.getMethod(
                "updateSensitiveAccount",
                Long.class,
                SensitiveWithdrawalAccountUpdateRequest.class);

        assertPermission(list, "@ss.hasPermi('member:withdrawal:list')");
        assertPermission(detail, "@ss.hasPermi('member:withdrawal:query')");
        assertPermission(sensitiveRead, "@ss.hasPermi('member:withdrawal:sensitive')");
        assertPermission(sensitiveUpdate, "@ss.hasPermi('member:withdrawal:sensitive')");
    }

    private void assertPermission(Method method, String expression)
    {
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation);
        assertEquals(expression, annotation.value());
    }
}
