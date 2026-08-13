package com.order.web.controller.member;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import com.order.common.core.domain.entity.SysUser;
import com.order.common.core.domain.model.LoginUser;
import com.order.framework.web.service.PermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class MemberSensitivePermissionContractTest
{
    @AfterEach
    void clearSecurityContext()
    {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void linkEndpointsUseGranularPermissions()
    {
        assertPermission(OrderLinkController.class, "list", "@ss.hasPermi('member:orderlink:list')");
        assertPermission(OrderLinkController.class, "export", "@ss.hasPermi('member:orderlink:export')");
        assertPermission(OrderLinkController.class, "getInfo", "@ss.hasPermi('member:orderlink:query')");
        assertPermission(OrderLinkController.class, "add", "@ss.hasPermi('member:orderlink:add')");
        assertPermission(OrderLinkController.class, "edit", "@ss.hasPermi('member:orderlink:edit')");
        assertPermission(OrderLinkController.class, "remove", "@ss.hasPermi('member:orderlink:remove')");
    }

    @Test
    void bonusEndpointsSeparateReadCrudAndBalanceMutations()
    {
        assertPermission(OrderBonusTableController.class, "list", "@ss.hasPermi('member:bonus:list')");
        assertPermission(OrderBonusTableController.class, "export", "@ss.hasPermi('member:bonus:export')");
        assertPermission(OrderBonusTableController.class, "getInfo", "@ss.hasPermi('member:bonus:query')");
        assertPermission(OrderBonusTableController.class, "add", "@ss.hasPermi('member:bonus:add')");
        assertPermission(OrderBonusTableController.class, "edit", "@ss.hasPermi('member:bonus:edit')");
        assertPermission(OrderBonusTableController.class, "remove", "@ss.hasPermi('member:bonus:remove')");
        assertPermission(OrderBonusTableController.class, "receive", "@ss.hasPermi('member:bonus:receive')");
        assertPermission(OrderBonusTableController.class, "given", "@ss.hasPermi('member:bonus:give')");
    }

    @Test
    void extraCommissionEndpointsUseGranularPermissions()
    {
        assertPermission(GoodsExtraCommissionSettingController.class, "list", "@ss.hasPermi('member:extracommission:list')");
        assertPermission(GoodsExtraCommissionSettingController.class, "export", "@ss.hasPermi('member:extracommission:export')");
        assertPermission(GoodsExtraCommissionSettingController.class, "getInfo", "@ss.hasPermi('member:extracommission:query')");
        assertPermission(GoodsExtraCommissionSettingController.class, "add", "@ss.hasPermi('member:extracommission:add')");
        assertPermission(GoodsExtraCommissionSettingController.class, "edit", "@ss.hasPermi('member:extracommission:edit')");
        assertPermission(GoodsExtraCommissionSettingController.class, "remove", "@ss.hasPermi('member:extracommission:remove')");
    }

    @Test
    void withdrawalAccountMetadataRequiresDrawerPermission()
    {
        assertPermission(
                GoodsWithdrawalAccountController.class,
                "getType",
                "@ss.hasAnyPermi('member:withdrawalAcc:list,member:withdrawalAcc:add,member:withdrawalAcc:edit')");
    }

    @Test
    void memberSensitiveHelpersAndMutationsAreNotAvailableToListOnlyRole()
    {
        assertPermission(
                OrderUserController.class,
                "getLevel",
                "@ss.hasAnyPermi('member:orderuser:query,member:orderuser:add,member:orderuser:edit')");
        assertPermission(
                OrderUserController.class,
                "getAllUser",
                "@ss.hasAnyPermi('member:orderuser:query,member:orderuser:edit,member:bonus:add,member:bonus:edit')");
        assertPermission(OrderUserController.class, "editPassword", "@ss.hasPermi('member:orderuser:edit')");
        assertPermission(OrderUserController.class, "editTradePassword", "@ss.hasPermi('member:orderuser:edit')");
        assertPermission(OrderUserController.class, "editParentId", "@ss.hasPermi('member:orderuser:edit')");
        assertPermission(OrderUserController.class, "transaction", "@ss.hasPermi('member:orderuser:edit')");
        assertPermission(OrderUserController.class, "resetOrder", "@ss.hasPermi('member:orderuser:edit')");
        assertPermission(OrderUserController.class, "giftAmount", "@ss.hasPermi('member:orderuser:edit')");
        assertPermission(OrderUserController.class, "selectChildrenById", "@ss.hasPermi('member:orderuser:query')");
    }

    @Test
    void migrationIsIdempotentAndDoesNotElevateListOnlyRole() throws Exception
    {
        Path migration = Path.of("sql", "2026-08-08_member_sensitive_permissions.sql");
        if (!Files.exists(migration)) {
            migration = Path.of("..", "sql", "2026-08-08_member_sensitive_permissions.sql");
        }
        assertTrue(Files.exists(migration), "Missing sensitive member permission migration");

        String sql = Files.readString(migration, StandardCharsets.UTF_8);
        assertTrue(sql.contains("existing.menu_id IS NULL"));
        assertTrue(sql.contains("NOT EXISTS ("));
        assertTrue(sql.contains("source_menu.perms IN ('member:orderuser:query', 'member:orderuser:edit')"));
        assertTrue(sql.contains("source_menu.perms = 'member:orderuser:edit'"));
        assertTrue(sql.contains("source_menu.perms = 'member:orderuser:export'"));
        assertFalse(sql.contains("source_menu.perms = 'member:orderuser:list'"));
        assertFalse(sql.contains("source_menu.perms IN ('member:orderuser:list'"));

        for (String permission : new String[] {
                "member:orderlink:list", "member:orderlink:query", "member:orderlink:add",
                "member:orderlink:edit", "member:orderlink:remove", "member:orderlink:export",
                "member:bonus:list", "member:bonus:query", "member:bonus:add", "member:bonus:edit",
                "member:bonus:remove", "member:bonus:export", "member:bonus:receive", "member:bonus:give",
                "member:extracommission:list", "member:extracommission:query",
                "member:extracommission:add", "member:extracommission:edit",
                "member:extracommission:remove", "member:extracommission:export",
                "member:withdrawalAcc:list", "member:withdrawalAcc:query",
                "member:withdrawalAcc:add", "member:withdrawalAcc:edit",
                "member:withdrawalAcc:remove", "member:withdrawalAcc:export"
        }) {
            assertTrue(sql.contains("'" + permission + "'"), "Missing menu permission " + permission);
        }
    }

    @Test
    void listOnlyPermissionIsDeniedWhileSuperAdminWildcardStillPasses()
    {
        PermissionService permissionService = new PermissionService();
        authenticate(Set.of("member:orderuser:list"));

        assertTrue(permissionService.hasPermi("member:orderuser:list"));
        for (String permission : new String[] {
                "member:orderuser:query", "member:orderuser:edit",
                "member:orderlink:list", "member:orderlink:add",
                "member:bonus:list", "member:bonus:receive", "member:bonus:give",
                "member:extracommission:list", "member:extracommission:add",
                "member:withdrawalAcc:list", "member:withdrawalAcc:edit"
        }) {
            assertFalse(permissionService.hasPermi(permission),
                    "A member list-only role must not receive " + permission);
        }
        assertFalse(permissionService.hasAnyPermi(
                "member:orderuser:query,member:orderuser:add,member:orderuser:edit"));

        authenticate(Set.of("*:*:*"));
        assertTrue(permissionService.hasPermi("member:orderlink:add"));
        assertTrue(permissionService.hasPermi("member:bonus:give"));
        assertTrue(permissionService.hasPermi("member:extracommission:edit"));
        assertTrue(permissionService.hasPermi("member:withdrawalAcc:edit"));
        assertTrue(permissionService.hasAnyPermi(
                "member:orderuser:query,member:orderuser:add,member:orderuser:edit"));
    }

    private static void authenticate(Set<String> permissions)
    {
        LoginUser loginUser = new LoginUser(new SysUser(), permissions);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    private static void assertPermission(Class<?> controllerType, String methodName, String expression)
    {
        Method method = Arrays.stream(controllerType.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst()
                .orElseGet(() -> {
                    fail("Missing controller method " + controllerType.getSimpleName() + "." + methodName);
                    return null;
                });
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation, () -> "Missing @PreAuthorize on "
                + controllerType.getSimpleName() + "." + methodName);
        assertEquals(expression, annotation.value());
    }
}
