package com.order.web.controller.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.order.common.constant.HttpStatus;
import com.order.common.annotation.Log;
import com.order.common.core.domain.AjaxResult;
import com.order.system.domain.dto.TableColumnConfigDto;
import com.order.system.service.ISysUserTableColumnConfigService;
import org.springframework.security.access.prepost.PreAuthorize;

class SysUserTableColumnConfigControllerTest
{
    private ISysUserTableColumnConfigService service;
    private SysUserTableColumnConfigController controller;

    @BeforeEach
    void setUp()
    {
        service = org.mockito.Mockito.mock(ISysUserTableColumnConfigService.class);
        controller = new CurrentUserController(service, 42L, "admin-42");
    }

    @Test
    void getReadsOnlyCurrentAuthenticatedUsersConfig()
    {
        TableColumnConfigDto config = config();
        when(service.selectTableColumnConfig(42L, "member.orderuser.main")).thenReturn(config);

        AjaxResult result = controller.get("member.orderuser.main");

        assertEquals(HttpStatus.SUCCESS, result.get(AjaxResult.CODE_TAG));
        assertEquals(config, result.get(AjaxResult.DATA_TAG));
        verify(service).selectTableColumnConfig(42L, "member.orderuser.main");
    }

    @Test
    void getWithoutStoredConfigReturnsSuccessWithoutData()
    {
        when(service.selectTableColumnConfig(42L, "member.orderuser.main")).thenReturn(null);

        AjaxResult result = controller.get("member.orderuser.main");

        assertEquals(HttpStatus.SUCCESS, result.get(AjaxResult.CODE_TAG));
        assertFalse(result.containsKey(AjaxResult.DATA_TAG));
    }

    @Test
    void saveGetsUserAndOperatorFromAuthenticatedControllerContext()
    {
        TableColumnConfigDto config = config();

        AjaxResult result = controller.save("member.orderuser.main", config);

        assertEquals(HttpStatus.SUCCESS, result.get(AjaxResult.CODE_TAG));
        verify(service).saveTableColumnConfig(42L, "admin-42", "member.orderuser.main", config);
    }

    @Test
    void deleteRemainsSuccessfulWhenServiceTreatsMissingRowAsNoOp()
    {
        AjaxResult result = controller.remove("member.orderuser.main");

        assertEquals(HttpStatus.SUCCESS, result.get(AjaxResult.CODE_TAG));
        verify(service).deleteTableColumnConfig(42L, "member.orderuser.main");
    }

    @Test
    void selfServiceEndpointsDoNotRequireBusinessPermissionOrWriteOperationLogs() throws Exception
    {
        for (String methodName : List.of("get", "save", "remove"))
        {
            java.lang.reflect.Method method = switch (methodName)
            {
                case "save" -> SysUserTableColumnConfigController.class.getMethod(
                        methodName, String.class, TableColumnConfigDto.class);
                default -> SysUserTableColumnConfigController.class.getMethod(methodName, String.class);
            };
            assertNull(method.getAnnotation(PreAuthorize.class));
            assertNull(method.getAnnotation(Log.class));
        }
    }

    private static TableColumnConfigDto config()
    {
        TableColumnConfigDto.Item item = new TableColumnConfigDto.Item();
        item.setId("id");
        item.setVisible(true);
        item.setFixed(null);
        TableColumnConfigDto config = new TableColumnConfigDto();
        config.setVersion(1);
        config.setItems(List.of(item));
        return config;
    }

    private static final class CurrentUserController extends SysUserTableColumnConfigController
    {
        private final Long userId;
        private final String username;

        private CurrentUserController(
                ISysUserTableColumnConfigService service,
                Long userId,
                String username)
        {
            super(service);
            this.userId = userId;
            this.username = username;
        }

        @Override
        public Long getUserId()
        {
            return userId;
        }

        @Override
        public String getUsername()
        {
            return username;
        }
    }
}
