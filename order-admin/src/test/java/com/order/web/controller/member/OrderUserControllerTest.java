package com.order.web.controller.member;

import com.github.pagehelper.PageHelper;
import com.order.common.core.domain.AjaxResult;
import com.order.common.core.page.TableDataInfo;
import com.order.common.core.redis.RedisCache;
import com.order.member.domain.GoodsMemberLevel;
import com.order.member.domain.OrderUser;
import com.order.member.service.IOrderUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderUserControllerTest
{
    @Mock
    private IOrderUserService orderUserService;

    @Mock
    private RedisCache redisCache;

    @InjectMocks
    private OrderUserController controller;

    @AfterEach
    void clearRequestState()
    {
        RequestContextHolder.resetRequestAttributes();
        PageHelper.clearPage();
    }

    @Test
    void defaultListMarksOnlineMembersWithoutPerMemberDatabaseLookups()
    {
        when(redisCache.keys("front:user_tokens:*")).thenReturn(List.of(
                "front:user_tokens:7",
                "front:user_tokens:8"));
        when(redisCache.keys("user:tokens:*")).thenReturn(List.of("user:tokens:legacy-member"));

        OrderUser idTokenUser = user(7L, "id-member");
        OrderUser legacyTokenUser = user(9L, "legacy-member");
        OrderUser offlineUser = user(10L, "offline-member");
        when(orderUserService.selectOrderUserList(any(OrderUser.class)))
                .thenReturn(List.of(idTokenUser, legacyTokenUser, offlineUser));

        TableDataInfo result = controller.list(new OrderUser(), pagedRequest(null));

        assertEquals(3, result.getRows().size());
        assertEquals("1", idTokenUser.getIsOnline());
        assertEquals("1", legacyTokenUser.getIsOnline());
        assertEquals("0", offlineUser.getIsOnline());
        verify(orderUserService, never()).selectUsernamesByIds(any());
        verify(orderUserService, never()).selectOrderUserById(anyLong());
        verify(orderUserService).selectOrderUserList(any(OrderUser.class));
    }

    @Test
    void onlineFilterResolvesAllIdTokensWithOneLightweightBatchLookup()
    {
        when(redisCache.keys("front:user_tokens:*")).thenReturn(List.of(
                "front:user_tokens:7",
                "front:user_tokens:8"));
        when(redisCache.keys("user:tokens:*")).thenReturn(List.of("user:tokens:legacy-member"));
        when(orderUserService.selectUsernamesByIds(Set.of(7L, 8L)))
                .thenReturn(List.of("id-member", "second-id-member"));

        OrderUser idTokenUser = user(7L, "id-member");
        OrderUser legacyTokenUser = user(9L, "legacy-member");
        when(orderUserService.selectOrderUserList(any(OrderUser.class)))
                .thenReturn(List.of(idTokenUser, legacyTokenUser));

        controller.list(new OrderUser(), pagedRequest("1"));

        verify(orderUserService).selectUsernamesByIds(Set.of(7L, 8L));
        verify(orderUserService, never()).selectOrderUserById(anyLong());
        ArgumentCaptor<OrderUser> filter = ArgumentCaptor.forClass(OrderUser.class);
        verify(orderUserService).selectOrderUserList(filter.capture());
        assertEquals(
                Set.of("id-member", "second-id-member", "legacy-member"),
                Set.of(filter.getValue().getUsernameList().split(",")));
        assertEquals("1", idTokenUser.getIsOnline());
        assertEquals("1", legacyTokenUser.getIsOnline());
    }

    @Test
    void offlineFilterRemovesBothCurrentAndLegacyOnlineMembers()
    {
        when(redisCache.keys("front:user_tokens:*")).thenReturn(List.of("front:user_tokens:7"));
        when(redisCache.keys("user:tokens:*")).thenReturn(List.of("user:tokens:legacy-member"));
        when(orderUserService.selectUsernamesByIds(Set.of(7L))).thenReturn(List.of("id-member"));
        when(orderUserService.selectOrderUserList(any(OrderUser.class)))
                .thenReturn(List.of(user(10L, "offline-member")));

        OrderUser query = new OrderUser();
        query.setUsernameList("id-member,legacy-member,offline-member");
        controller.list(query, pagedRequest("0"));

        verify(orderUserService).selectUsernamesByIds(Set.of(7L));
        verify(orderUserService, never()).selectOrderUserById(anyLong());
        ArgumentCaptor<OrderUser> filter = ArgumentCaptor.forClass(OrderUser.class);
        verify(orderUserService).selectOrderUserList(filter.capture());
        assertEquals("offline-member", filter.getValue().getUsernameList());
    }

    @Test
    void onlineFilterWithNoTokensReturnsImmediatelyWithoutAReportingQuery()
    {
        TableDataInfo result = controller.list(new OrderUser(), pagedRequest("1"));

        assertEquals(0, result.getTotal());
        assertTrue(result.getRows().isEmpty());
        verify(orderUserService, never()).selectUsernamesByIds(any());
        verify(orderUserService, never()).selectOrderUserById(anyLong());
        verify(orderUserService, never()).selectOrderUserList(any(OrderUser.class));
    }

    @Test
    void resetOrderReturnsErrorWhenOptimisticUpdateDoesNotChangeARow()
    {
        when(orderUserService.resetTaskProgress(7L)).thenReturn(0);

        AjaxResult result = controller.resetOrder(7L);

        assertTrue(result.isError());
        assertEquals("重置失败，请刷新后重试", result.get(AjaxResult.MSG_TAG));
        verify(orderUserService).resetTaskProgress(7L);
    }

    @Test
    void resetOrderReturnsSuccessAfterTheUpdateSucceeds()
    {
        when(orderUserService.resetTaskProgress(7L)).thenReturn(1);

        AjaxResult result = controller.resetOrder(7L);

        assertTrue(result.isSuccess());
        assertEquals("重置成功", result.get(AjaxResult.MSG_TAG));
        verify(orderUserService).resetTaskProgress(7L);
    }

    @Test
    void fullMemberDetailsRemainRestrictedToQueryOrEditPermission() throws Exception
    {
        Method method = OrderUserController.class.getDeclaredMethod("getInfo", Long.class);
        PreAuthorize authorization = method.getAnnotation(PreAuthorize.class);

        assertEquals(
                "@ss.hasAnyPermi('member:orderuser:query,member:orderuser:edit')",
                authorization.value());
    }

    @Test
    void operationSummaryExposesOnlyTheFieldsNeededByAuxiliaryDrawers() throws Exception
    {
        OrderUser user = completedUser();
        user.setUsername("member-a");
        user.setPhoneNumber("123");
        user.setBalance(java.math.BigDecimal.TEN);
        user.setFrozenBalance(java.math.BigDecimal.ONE);
        user.setIdentityNumber("secret-id");
        user.setLastLoginIp("127.0.0.1");
        when(orderUserService.selectOrderUserById(7L)).thenReturn(user);

        AjaxResult result = controller.getOperationSummary(7L);

        assertTrue(result.isSuccess());
        Object summary = result.get(AjaxResult.DATA_TAG);
        Set<String> fields = Arrays.stream(summary.getClass().getRecordComponents())
                .map(component -> component.getName())
                .collect(Collectors.toSet());
        assertEquals(Set.of(
                "id", "username", "phoneNumber", "balance", "frozenBalance",
                "totalBalance", "taskProgress", "orderCountPerDay", "lastLoginTime"), fields);
        assertTrue(!fields.contains("identityNumber"));
        assertTrue(!fields.contains("lastLoginIp"));

        Method method = OrderUserController.class.getDeclaredMethod("getOperationSummary", Long.class);
        assertEquals(
                "@ss.hasAnyPermi('member:orderlink:list,member:bonus:list,member:extracommission:list')",
                method.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void editRejectsAStaleClientVersionBeforeWritingHiddenOrVisibleFields()
    {
        OrderUser existing = new OrderUser();
        existing.setId(7L);
        existing.setVersion(4L);
        OrderUser incoming = new OrderUser();
        incoming.setId(7L);
        incoming.setVersion(3L);
        when(orderUserService.selectOrderUserById(7L)).thenReturn(existing);

        AjaxResult result = controller.edit(incoming);

        assertTrue(result.isError());
        assertEquals("会员资料已变化，请刷新后重试", result.get(AjaxResult.MSG_TAG));
        verify(orderUserService, never()).updateOrderUser(incoming);
    }

    @Test
    void taskProgressAdjustmentUsesTheDedicatedAtomicService()
    {
        OrderUser incoming = new OrderUser();
        incoming.setId(7L);
        incoming.setTaskProgress(6L);
        incoming.setVersion(4L);
        when(orderUserService.adjustTaskProgress(7L, 6L, 4L)).thenReturn(1);

        AjaxResult result = controller.adjustTaskProgress(incoming);

        assertTrue(result.isSuccess());
        assertEquals("修改成功", result.get(AjaxResult.MSG_TAG));
        verify(orderUserService).adjustTaskProgress(7L, 6L, 4L);
        verify(orderUserService, never()).updateOrderUser(incoming);
    }

    @Test
    void taskProgressAdjustmentReportsAVersionConflict()
    {
        OrderUser incoming = new OrderUser();
        incoming.setId(7L);
        incoming.setTaskProgress(6L);
        incoming.setVersion(4L);
        when(orderUserService.adjustTaskProgress(7L, 6L, 4L)).thenReturn(0);

        AjaxResult result = controller.adjustTaskProgress(incoming);

        assertTrue(result.isError());
        assertEquals("会员资料已变化，请刷新后重试", result.get(AjaxResult.MSG_TAG));
    }

    @Test
    void legacyGenericEditCannotBypassTheAtomicTaskProgressAdjustment()
    {
        OrderUser incoming = new OrderUser();
        incoming.setId(7L);
        incoming.setTaskProgress(6L);
        incoming.setVersion(4L);
        when(orderUserService.adjustTaskProgress(7L, 6L, 4L)).thenReturn(1);

        AjaxResult result = controller.edit(incoming);

        assertTrue(result.isSuccess());
        verify(orderUserService).adjustTaskProgress(7L, 6L, 4L);
        verify(orderUserService, never()).selectOrderUserById(7L);
        verify(orderUserService, never()).updateOrderUser(incoming);
    }

    @Test
    void loginPasswordRejectsWhitespaceBeforeHashingOrWriting()
    {
        OrderUser incoming = new OrderUser();
        incoming.setId(7L);
        incoming.setPassword("      ");

        AjaxResult result = controller.editPassword(incoming);

        assertTrue(result.isError());
        assertEquals("登录密码长度不能少于6位", result.get(AjaxResult.MSG_TAG));
        verify(orderUserService, never()).selectOrderUserById(7L);
        verify(orderUserService, never()).updateOrderUser(incoming);
    }

    private static OrderUser completedUser()
    {
        GoodsMemberLevel level = new GoodsMemberLevel();
        level.setOrderCountPerDay(5L);

        OrderUser user = new OrderUser();
        user.setId(7L);
        user.setVersion(3L);
        user.setTaskProgress(5L);
        user.setTodayRest(2);
        user.setTotalRest(5);
        user.setMemberLevel(level);
        return user;
    }

    private static OrderUser user(Long id, String username)
    {
        OrderUser user = new OrderUser();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private static MockHttpServletRequest pagedRequest(String onlineStatus)
    {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("pageNum", "1");
        request.setParameter("pageSize", "10");
        if (onlineStatus != null) {
            request.setParameter("isOnline", onlineStatus);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        return request;
    }
}
