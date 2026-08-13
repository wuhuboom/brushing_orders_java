package com.brushing.member.service.impl;

import com.brushing.common.exception.ServiceException;
import com.brushing.common.utils.spring.SpringUtils;
import com.brushing.member.domain.OrderMemberLevel;
import com.brushing.member.domain.OrderMemberUser;
import com.brushing.member.mapper.OrderMemberLevelMapper;
import com.brushing.member.mapper.OrderMemberUserMapper;
import com.brushing.member.mapper.WelfareConfigMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderMemberUserParentChangeServiceTest {

    @Mock
    private OrderMemberUserMapper memberMapper;

    @Mock
    private OrderMemberLevelMapper levelMapper;

    @Mock
    private WelfareConfigMapper welfareConfigMapper;

    @InjectMocks
    private OrderMemberUserServiceImpl service;

    @BeforeAll
    static void configureMessages() {
        LocaleContextHolder.setDefaultLocale(Locale.ENGLISH);
        StaticMessageSource messages = new StaticMessageSource();
        for (String key : List.of(
                "member.user.id.required",
                "member.user.not.exists",
                "member.user.parent_identifier.required",
                "member.user.version.required",
                "member.user.version.conflict",
                "member.user.parent.change_requires_endpoint",
                "member.user.hierarchy.rebuild_failed",
                "member.user.parent_id.invalid",
                "member.user.parent.self",
                "member.user.parent.descendant")) {
            messages.addMessage(key, Locale.ENGLISH, key);
        }
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("messageSource", messages);
        new SpringUtils().postProcessBeanFactory(beanFactory);
    }

    @Test
    void movesBranchAndRebuildsEveryDescendantPath() {
        OrderMemberUser target = hierarchy(10L, 1L, "0,1", 4L);
        OrderMemberUser parent = hierarchy(20L, 0L, "0", 2L);
        when(memberMapper.selectMemberHierarchyById(10L)).thenReturn(target);
        when(memberMapper.selectMemberHierarchyById(20L)).thenReturn(parent);
        when(memberMapper.selectMemberHierarchiesForUpdate(List.of(10L, 20L)))
                .thenReturn(List.of(target, parent));
        when(memberMapper.changeMemberParent(10L, 20L, "0,20", 4L)).thenReturn(1);
        when(memberMapper.selectMemberChildrenForUpdate(List.of(10L)))
                .thenReturn(List.of(hierarchy(11L, 10L, null, null)));
        when(memberMapper.selectMemberChildrenForUpdate(List.of(11L)))
                .thenReturn(List.of(hierarchy(12L, 11L, null, null)));
        when(memberMapper.selectMemberChildrenForUpdate(List.of(12L)))
                .thenReturn(List.of());
        when(memberMapper.updateMemberAncestorsBatch(anyList()))
                .thenAnswer(invocation -> invocation.<List<?>>getArgument(0).size());

        assertEquals(1, service.changeParent(10L, "20", 4L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OrderMemberUser>> paths = ArgumentCaptor.forClass(List.class);
        verify(memberMapper).updateMemberAncestorsBatch(paths.capture());
        assertEquals(2, paths.getValue().size());
        assertEquals(11L, paths.getValue().get(0).getId());
        assertEquals("0,20,10", paths.getValue().get(0).getAncestors());
        assertEquals(12L, paths.getValue().get(1).getId());
        assertEquals("0,20,10,11", paths.getValue().get(1).getAncestors());

        InOrder hierarchyWriteOrder = inOrder(memberMapper);
        hierarchyWriteOrder.verify(memberMapper)
                .selectMemberHierarchiesForUpdate(List.of(10L, 20L));
        hierarchyWriteOrder.verify(memberMapper).selectMemberChildrenForUpdate(List.of(10L));
        hierarchyWriteOrder.verify(memberMapper).selectMemberChildrenForUpdate(List.of(11L));
        hierarchyWriteOrder.verify(memberMapper).selectMemberChildrenForUpdate(List.of(12L));
        hierarchyWriteOrder.verify(memberMapper)
                .changeMemberParent(10L, 20L, "0,20", 4L);
        hierarchyWriteOrder.verify(memberMapper).updateMemberAncestorsBatch(anyList());
    }

    @Test
    void movesBranchToRoot() {
        OrderMemberUser target = hierarchy(10L, 1L, "0,1", 7L);
        when(memberMapper.selectMemberHierarchyById(10L)).thenReturn(target);
        when(memberMapper.selectMemberHierarchiesForUpdate(List.of(10L)))
                .thenReturn(List.of(target));
        when(memberMapper.changeMemberParent(10L, 0L, "0", 7L)).thenReturn(1);
        when(memberMapper.selectMemberChildrenForUpdate(List.of(10L))).thenReturn(List.of());

        assertEquals(1, service.changeParent(10L, "0", 7L));
        verify(memberMapper, never()).updateMemberAncestorsBatch(anyList());
    }

    @Test
    void rejectsRealParentIdDescendantCycle() {
        OrderMemberUser target = hierarchy(10L, 0L, "0", 3L);
        OrderMemberUser child = hierarchy(12L, 11L, "stale-path", 1L);
        when(memberMapper.selectMemberHierarchyById(10L)).thenReturn(target);
        when(memberMapper.selectMemberHierarchyById(12L)).thenReturn(child);
        when(memberMapper.selectMemberHierarchiesForUpdate(List.of(10L, 12L)))
                .thenReturn(List.of(target, child));
        when(memberMapper.selectMemberChildrenForUpdate(List.of(10L)))
                .thenReturn(List.of(hierarchy(11L, 10L, null, null)));
        when(memberMapper.selectMemberChildrenForUpdate(List.of(11L)))
                .thenReturn(List.of(hierarchy(12L, 11L, null, null)));
        when(memberMapper.selectMemberChildrenForUpdate(List.of(12L))).thenReturn(List.of());

        assertThrows(ServiceException.class, () -> service.changeParent(10L, "12", 3L));
        verify(memberMapper, never()).changeMemberParent(10L, 12L, "stale-path,12", 3L);
    }

    @Test
    void rejectsMissingTargetAndMissingParent() {
        when(memberMapper.selectMemberHierarchyById(404L)).thenReturn(null);
        assertThrows(ServiceException.class, () -> service.changeParent(404L, "0", 1L));

        OrderMemberUser target = hierarchy(10L, 0L, "0", 1L);
        when(memberMapper.selectMemberHierarchyById(10L)).thenReturn(target);
        when(memberMapper.selectMemberHierarchyById(99L)).thenReturn(null);
        when(memberMapper.selectMemberHierarchyByInviteCode("99")).thenReturn(null);
        assertThrows(ServiceException.class, () -> service.changeParent(10L, "99", 1L));
        verify(memberMapper, never()).changeMemberParent(10L, 99L, "0,99", 1L);
    }

    @Test
    void rejectsSelfAsParent() {
        OrderMemberUser target = hierarchy(10L, 0L, "0", 3L);
        when(memberMapper.selectMemberHierarchyById(10L)).thenReturn(target);
        when(memberMapper.selectMemberHierarchiesForUpdate(List.of(10L)))
                .thenReturn(List.of(target));

        assertThrows(ServiceException.class, () -> service.changeParent(10L, "10", 3L));
        verify(memberMapper, never()).selectMemberChildrenForUpdate(anyList());
        verify(memberMapper, never()).changeMemberParent(10L, 10L, "0,10", 3L);
    }

    @Test
    void rejectsStaleVersionBeforeAnyWrite() {
        OrderMemberUser snapshot = hierarchy(10L, 0L, "0", 4L);
        OrderMemberUser locked = hierarchy(10L, 0L, "0", 5L);
        when(memberMapper.selectMemberHierarchyById(10L)).thenReturn(snapshot);
        when(memberMapper.selectMemberHierarchiesForUpdate(List.of(10L)))
                .thenReturn(List.of(locked));

        assertThrows(ServiceException.class, () -> service.changeParent(10L, "0", 4L));
        verify(memberMapper, never()).changeMemberParent(10L, 0L, "0", 4L);
    }

    @Test
    void throwsWhenConditionalVersionUpdateLosesRace() {
        OrderMemberUser target = hierarchy(10L, 0L, "0", 4L);
        when(memberMapper.selectMemberHierarchyById(10L)).thenReturn(target);
        when(memberMapper.selectMemberHierarchiesForUpdate(List.of(10L)))
                .thenReturn(List.of(target));
        when(memberMapper.selectMemberChildrenForUpdate(List.of(10L))).thenReturn(List.of());
        when(memberMapper.changeMemberParent(10L, 0L, "0", 4L)).thenReturn(0);

        assertThrows(ServiceException.class, () -> service.changeParent(10L, "0", 4L));
        verify(memberMapper, never()).updateMemberAncestorsBatch(anyList());
    }

    @Test
    void rejectsIncompleteExactIdRebuild() {
        OrderMemberUser target = hierarchy(10L, 0L, "0", 4L);
        when(memberMapper.selectMemberHierarchyById(10L)).thenReturn(target);
        when(memberMapper.selectMemberHierarchiesForUpdate(List.of(10L)))
                .thenReturn(List.of(target));
        when(memberMapper.changeMemberParent(10L, 0L, "0", 4L)).thenReturn(1);
        when(memberMapper.selectMemberChildrenForUpdate(List.of(10L)))
                .thenReturn(List.of(hierarchy(11L, 10L, null, null)));
        when(memberMapper.selectMemberChildrenForUpdate(List.of(11L))).thenReturn(List.of());
        when(memberMapper.updateMemberAncestorsBatch(anyList())).thenReturn(0);

        assertThrows(ServiceException.class, () -> service.changeParent(10L, "0", 4L));
    }

    @Test
    void locksLargeFrontiersInBoundedBatchesAndNeverBatchUpdatesRoot() {
        OrderMemberUser target = hierarchy(10L, 1L, "0,1", 4L);
        List<OrderMemberUser> directChildren = LongStream.rangeClosed(1000L, 1500L)
                .mapToObj(id -> hierarchy(id, 10L, null, 2L))
                .collect(Collectors.toList());
        when(memberMapper.selectMemberHierarchyById(10L)).thenReturn(target);
        when(memberMapper.selectMemberHierarchiesForUpdate(List.of(10L)))
                .thenReturn(List.of(target));
        when(memberMapper.selectMemberChildrenForUpdate(anyList()))
                .thenAnswer(invocation -> {
                    List<Long> parentIds = invocation.getArgument(0);
                    return parentIds.equals(List.of(10L)) ? directChildren : List.of();
                });
        when(memberMapper.changeMemberParent(10L, 0L, "0", 4L)).thenReturn(1);
        when(memberMapper.updateMemberAncestorsBatch(anyList()))
                .thenAnswer(invocation -> invocation.<List<?>>getArgument(0).size());

        assertEquals(1, service.changeParent(10L, "0", 4L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> frontiers = ArgumentCaptor.forClass(List.class);
        verify(memberMapper, times(3)).selectMemberChildrenForUpdate(frontiers.capture());
        assertEquals(List.of(1, 500, 1), frontiers.getAllValues().stream()
                .map(List::size)
                .collect(Collectors.toList()));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OrderMemberUser>> updates = ArgumentCaptor.forClass(List.class);
        verify(memberMapper, times(2)).updateMemberAncestorsBatch(updates.capture());
        List<OrderMemberUser> allUpdates = updates.getAllValues().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
        assertEquals(501, allUpdates.size());
        assertTrue(allUpdates.stream().noneMatch(user -> user.getId().equals(10L)));
        assertTrue(allUpdates.stream().allMatch(user -> "0,10".equals(user.getAncestors())));
    }

    @Test
    void rejectsLegacyParentIdCycleWhileLockingFrontiers() {
        OrderMemberUser target = hierarchy(10L, 11L, "stale", 4L);
        when(memberMapper.selectMemberHierarchyById(10L)).thenReturn(target);
        when(memberMapper.selectMemberHierarchiesForUpdate(List.of(10L)))
                .thenReturn(List.of(target));
        when(memberMapper.selectMemberChildrenForUpdate(List.of(10L)))
                .thenReturn(List.of(hierarchy(11L, 10L, null, 1L)));
        when(memberMapper.selectMemberChildrenForUpdate(List.of(11L)))
                .thenReturn(List.of(hierarchy(10L, 11L, null, 4L)));

        assertThrows(ServiceException.class, () -> service.changeParent(10L, "0", 4L));
        verify(memberMapper, never()).changeMemberParent(10L, 0L, "0", 4L);
    }

    @Test
    void managementInsertLocksResolvedParentBeforeInsert() {
        OrderMemberUser parentSnapshot = hierarchy(20L, 0L, "stale", 2L);
        OrderMemberUser lockedParent = hierarchy(20L, 0L, "0", 3L);
        when(memberMapper.selectOrderMemberUserById(20L)).thenReturn(parentSnapshot);
        when(memberMapper.selectMemberHierarchiesForUpdate(List.of(20L)))
                .thenReturn(List.of(lockedParent));
        when(memberMapper.insertOrderMemberUser(org.mockito.ArgumentMatchers.any()))
                .thenReturn(1);
        OrderMemberUser newMember = new OrderMemberUser();
        newMember.setParentId(20L);
        newMember.setLevelId(1L);

        assertEquals(1, service.insertOrderMemberUser(newMember));
        assertEquals("0,20", newMember.getAncestors());
        InOrder order = inOrder(memberMapper);
        order.verify(memberMapper).selectMemberHierarchiesForUpdate(List.of(20L));
        order.verify(memberMapper).insertOrderMemberUser(newMember);
    }

    @Test
    void registrationLocksInviteParentBeforeInsert() {
        OrderMemberUser parentSnapshot = hierarchy(20L, 0L, "stale", 2L);
        parentSnapshot.setInviteCode("PARENT");
        OrderMemberUser lockedParent = hierarchy(20L, 0L, "0", 3L);
        when(memberMapper.selectOrderMemberUserByInCode("PARENT"))
                .thenReturn(parentSnapshot);
        OrderMemberLevel lowestLevel = new OrderMemberLevel();
        lowestLevel.setId(1L);
        when(levelMapper.selectLowestPriceLevel()).thenReturn(lowestLevel);
        when(memberMapper.selectMemberHierarchiesForUpdate(List.of(20L)))
                .thenReturn(List.of(lockedParent));
        OrderMemberUser newMember = new OrderMemberUser();
        newMember.setInviteCode("PARENT");

        assertEquals("200", service.register(newMember));
        assertEquals("0,20", newMember.getAncestors());
        InOrder order = inOrder(memberMapper);
        order.verify(memberMapper).selectMemberHierarchiesForUpdate(List.of(20L));
        order.verify(memberMapper).insertOrderMemberUser(newMember);
    }

    @Test
    void genericEditAcceptsUnchangedHierarchyAndPreservesCallerObject() {
        when(memberMapper.selectMemberHierarchyById(10L))
                .thenReturn(hierarchy(10L, 1L, "0,1", 2L));
        when(memberMapper.updateOrderMemberUser(org.mockito.ArgumentMatchers.any()))
                .thenReturn(1);
        OrderMemberUser update = hierarchy(10L, 1L, "0,1", 2L);
        update.setParentIdentifier(" ");

        assertEquals(1, service.updateOrderMemberUser(update));
        assertEquals(1L, update.getParentId());
        assertEquals(" ", update.getParentIdentifier());
        assertEquals("0,1", update.getAncestors());
        verify(memberMapper).updateOrderMemberUser(update);
    }

    @Test
    void genericEditRejectsHierarchyChange() {
        when(memberMapper.selectMemberHierarchyById(10L))
                .thenReturn(hierarchy(10L, 1L, "0,1", 2L));
        OrderMemberUser update = hierarchy(10L, 99L, "0,1", 2L);

        assertThrows(ServiceException.class, () -> service.updateOrderMemberUser(update));
        verify(memberMapper, never()).updateOrderMemberUser(update);
    }

    private OrderMemberUser hierarchy(Long id, Long parentId, String ancestors, Long version) {
        OrderMemberUser user = new OrderMemberUser();
        user.setId(id);
        user.setParentId(parentId);
        user.setAncestors(ancestors);
        user.setVersion(version);
        return user;
    }
}
