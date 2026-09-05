package com.order.member.service.impl;

import java.util.List;
import java.util.Set;

import com.order.common.exception.ServiceException;
import com.order.member.domain.GoodsMemberLevel;
import com.order.member.domain.OrderInfo;
import com.order.member.domain.OrderUser;
import com.order.member.mapper.OrderUserMapper;
import com.order.member.service.IOrderInfoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderUserServiceImplTest
{
    @Mock
    private OrderUserMapper orderUserMapper;

    @Mock
    private IOrderInfoService orderInfoService;

    @InjectMocks
    private OrderUserServiceImpl orderUserService;

    @Test
    void usernameBatchLookupSkipsAnEmptyIdCollection()
    {
        assertEquals(List.of(), orderUserService.selectUsernamesByIds(List.of()));

        verify(orderUserMapper, never()).selectUsernamesByIds(any());
    }

    @Test
    void usernameBatchLookupDelegatesTheWholeIdCollectionOnce()
    {
        Set<Long> ids = Set.of(7L, 8L);
        when(orderUserMapper.selectUsernamesByIds(ids)).thenReturn(List.of("member-a", "member-b"));

        assertEquals(
                List.of("member-a", "member-b"),
                orderUserService.selectUsernamesByIds(ids));
        verify(orderUserMapper).selectUsernamesByIds(ids);
        verify(orderUserMapper, never()).selectOrderUserById(any());
    }

    @Test
    void updateParentRebuildsAncestorsWithEachCurrentParentId()
    {
        OrderUser movedUser = user(10L, "0,2");
        OrderUser child = user(20L, "0,5,10");
        OrderUser grandchild = user(30L, "0,5,10,20");

        when(orderUserMapper.updateParentIdAndAncestors(10L, 2L, 7L)).thenReturn(1);
        when(orderUserMapper.selectOrderUserById(10L)).thenReturn(movedUser);
        when(orderUserMapper.selectOrderUserById(20L)).thenReturn(child);
        when(orderUserMapper.selectOrderUserById(30L)).thenReturn(grandchild);
        when(orderUserMapper.selectChildrenById(10L, "direct")).thenReturn(List.of(child));
        when(orderUserMapper.selectChildrenById(20L, "direct")).thenReturn(List.of(grandchild));
        when(orderUserMapper.selectChildrenById(30L, "direct")).thenReturn(List.of());
        when(orderUserMapper.updateOrderUser(child)).thenReturn(1);
        when(orderUserMapper.updateOrderUser(grandchild)).thenReturn(1);

        assertEquals(1, orderUserService.updateParentIdAndAncestors(10L, 2L, 7L));

        assertEquals("0,2,10", child.getAncestors());
        assertEquals("0,2,10,20", grandchild.getAncestors());
        verify(orderUserMapper).updateOrderUser(child);
        verify(orderUserMapper).updateOrderUser(grandchild);
    }

    @Test
    void adjustingProgressAppliesTheNewValueWhenThereAreNoActiveOrders()
    {
        OrderUser current = progressUser(12L, 4L, 40L);

        when(orderUserMapper.lockUserById(7L)).thenReturn(7L);
        when(orderUserMapper.selectOrderUserById(7L)).thenReturn(current);
        when(orderUserMapper.updateTaskProgress(7L, 6L)).thenReturn(1);

        assertEquals(1, orderUserService.adjustTaskProgress(7L, 6L, 4L));

        verify(orderUserMapper).updateTaskProgress(7L, 6L);
    }

    @Test
    void adjustingProgressRejectsAnExistingPendingOrder()
    {
        OrderUser current = progressUser(12L, 4L, 40L);
        OrderInfo pending = new OrderInfo();
        pending.setId(20L);
        pending.setOrderCount(12L);

        when(orderUserMapper.lockUserById(7L)).thenReturn(7L);
        when(orderUserMapper.selectOrderUserById(7L)).thenReturn(current);
        when(orderInfoService.hasOpenOrders(7L)).thenReturn(pending);

        ServiceException error = assertThrows(
                ServiceException.class,
                () -> orderUserService.adjustTaskProgress(7L, 6L, 4L));

        assertEquals("会员存在待提交订单，请先完成或取消该订单后再修改单数", error.getMessage());
        verify(orderInfoService, never()).cancelPendingOrder(20L);
        verify(orderInfoService, never()).hasFrozenLinkedOrders(7L);
        verify(orderUserMapper, never()).updateTaskProgress(7L, 6L);
    }

    @Test
    void adjustingTheSameStoredProgressStillRejectsAnExistingPendingOrder()
    {
        OrderUser current = progressUser(6L, 4L, 40L);
        OrderInfo pending = new OrderInfo();
        pending.setId(20L);
        pending.setOrderCount(7L);

        when(orderUserMapper.lockUserById(7L)).thenReturn(7L);
        when(orderUserMapper.selectOrderUserById(7L)).thenReturn(current);
        when(orderInfoService.hasOpenOrders(7L)).thenReturn(pending);

        assertThrows(
                ServiceException.class,
                () -> orderUserService.adjustTaskProgress(7L, 6L, 4L));

        verify(orderInfoService, never()).cancelPendingOrder(20L);
        verify(orderInfoService, never()).hasFrozenLinkedOrders(7L);
        verify(orderUserMapper, never()).updateTaskProgress(7L, 6L);
    }

    @Test
    void adjustingProgressRejectsAPartiallyFrozenLinkedOrderGroup()
    {
        OrderUser current = progressUser(11L, 4L, 40L);
        when(orderUserMapper.lockUserById(7L)).thenReturn(7L);
        when(orderUserMapper.selectOrderUserById(7L)).thenReturn(current);
        when(orderInfoService.hasFrozenLinkedOrders(7L)).thenReturn(true);

        assertThrows(
                ServiceException.class,
                () -> orderUserService.adjustTaskProgress(7L, 6L, 4L));

        verify(orderInfoService).hasOpenOrders(7L);
        verify(orderInfoService, never()).cancelPendingOrder(org.mockito.ArgumentMatchers.anyLong());
        verify(orderUserMapper, never()).updateTaskProgress(7L, 6L);
    }

    @Test
    void adjustingProgressRejectsAStaleAdministrationVersion()
    {
        OrderUser current = progressUser(12L, 5L, 40L);
        when(orderUserMapper.lockUserById(7L)).thenReturn(7L);
        when(orderUserMapper.selectOrderUserById(7L)).thenReturn(current);

        assertEquals(0, orderUserService.adjustTaskProgress(7L, 6L, 4L));

        verify(orderInfoService, never()).hasFrozenLinkedOrders(7L);
        verify(orderUserMapper, never()).updateTaskProgress(7L, 6L);
    }

    @Test
    void adjustingProgressCannotExceedTheMemberLevelLimit()
    {
        OrderUser current = progressUser(12L, 4L, 40L);
        when(orderUserMapper.lockUserById(7L)).thenReturn(7L);
        when(orderUserMapper.selectOrderUserById(7L)).thenReturn(current);

        assertThrows(
                ServiceException.class,
                () -> orderUserService.adjustTaskProgress(7L, 41L, 4L));

        verify(orderUserMapper, never()).updateTaskProgress(7L, 41L);
    }

    @Test
    void resettingProgressUsesTheSameActiveOrderGuard()
    {
        OrderUser current = progressUser(40L, 4L, 40L);
        OrderInfo pending = new OrderInfo();
        pending.setId(20L);

        when(orderUserMapper.lockUserById(7L)).thenReturn(7L);
        when(orderUserMapper.selectOrderUserById(7L)).thenReturn(current);
        when(orderInfoService.hasOpenOrders(7L)).thenReturn(pending);

        assertThrows(ServiceException.class, () -> orderUserService.resetTaskProgress(7L));

        verify(orderUserMapper, never()).resetTaskProgress(7L);
    }

    @Test
    void resettingCompletedProgressUpdatesCountersAtomically()
    {
        OrderUser current = progressUser(40L, 4L, 40L);
        when(orderUserMapper.lockUserById(7L)).thenReturn(7L);
        when(orderUserMapper.selectOrderUserById(7L)).thenReturn(current);
        when(orderUserMapper.resetTaskProgress(7L)).thenReturn(1);

        assertEquals(1, orderUserService.resetTaskProgress(7L));

        verify(orderUserMapper).resetTaskProgress(7L);
    }

    private static OrderUser user(Long id, String ancestors)
    {
        OrderUser user = new OrderUser();
        user.setId(id);
        user.setAncestors(ancestors);
        user.setVersion(1L);
        return user;
    }

    private static OrderUser progressUser(Long progress, Long version, Long limit)
    {
        GoodsMemberLevel level = new GoodsMemberLevel();
        level.setOrderCountPerDay(limit);

        OrderUser user = new OrderUser();
        user.setId(7L);
        user.setTaskProgress(progress);
        user.setVersion(version);
        user.setMemberLevel(level);
        return user;
    }
}
