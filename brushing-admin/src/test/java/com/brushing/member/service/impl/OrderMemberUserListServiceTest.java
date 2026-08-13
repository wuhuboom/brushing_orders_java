package com.brushing.member.service.impl;

import com.brushing.member.domain.OrderMemberUser;
import com.brushing.member.mapper.OrderMemberUserMapper;
import com.github.pagehelper.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderMemberUserListServiceTest {

    @Mock
    private OrderMemberUserMapper userMapper;

    @InjectMocks
    private OrderMemberUserServiceImpl userService;

    @Test
    void preservesPageMetadataAndMergesAggregatesWithZeroDefaults() {
        OrderMemberUser first = member(1L);
        OrderMemberUser second = member(2L);
        Page<OrderMemberUser> page = new Page<>(1, 2);
        page.setTotal(37L);
        page.add(first);
        page.add(second);

        OrderMemberUser financial = member(1L);
        financial.setTotalRecharge(new BigDecimal("100.50"));
        financial.setTotalWithdraw(new BigDecimal("30.25"));
        financial.setWithdrawFrozenAmount(new BigDecimal("12.75"));
        financial.setTodayWithdrawCount(3);

        OrderMemberUser subordinate = member(2L);
        subordinate.setDirectSubCount(2);
        subordinate.setAllSubCount(5);

        OrderMemberUser filter = new OrderMemberUser();
        when(userMapper.selectOrderMemberUserList(filter)).thenReturn(page);
        when(userMapper.selectMemberListFinancialAggregates(List.of(1L, 2L)))
                .thenReturn(List.of(financial));
        when(userMapper.selectMemberListSubordinateAggregates(List.of(1L, 2L)))
                .thenReturn(List.of(subordinate));

        List<OrderMemberUser> result = userService.selectOrderMemberUserList(filter);

        assertSame(page, result);
        assertEquals(37L, ((Page<?>) result).getTotal());
        assertEquals(new BigDecimal("100.50"), first.getTotalRecharge());
        assertEquals(new BigDecimal("30.25"), first.getTotalWithdraw());
        assertEquals(new BigDecimal("70.25"), first.getDiffAmount());
        assertEquals(new BigDecimal("12.75"), first.getWithdrawFrozenAmount());
        assertEquals(3, first.getTodayWithdrawCount());
        assertEquals(0, first.getDirectSubCount());
        assertEquals(0, first.getAllSubCount());

        assertEquals(BigDecimal.ZERO, second.getTotalRecharge());
        assertEquals(BigDecimal.ZERO, second.getTotalWithdraw());
        assertEquals(BigDecimal.ZERO, second.getDiffAmount());
        assertEquals(BigDecimal.ZERO, second.getWithdrawFrozenAmount());
        assertEquals(0, second.getTodayWithdrawCount());
        assertEquals(2, second.getDirectSubCount());
        assertEquals(5, second.getAllSubCount());
    }

    @Test
    void emptyPageSkipsEveryAggregateQuery() {
        OrderMemberUser filter = new OrderMemberUser();
        Page<OrderMemberUser> emptyPage = new Page<>(1, 10);
        emptyPage.setTotal(0L);
        when(userMapper.selectOrderMemberUserList(filter)).thenReturn(emptyPage);

        List<OrderMemberUser> result = userService.selectOrderMemberUserList(filter);

        assertSame(emptyPage, result);
        verify(userMapper, never()).selectMemberListFinancialAggregates(anyList());
        verify(userMapper, never()).selectMemberListSubordinateAggregates(anyList());
    }

    @Test
    void unpagedExportUsesBoundedAggregateBatches() {
        List<OrderMemberUser> exportRows = new ArrayList<>();
        for (long id = 1; id <= 1001; id++) {
            exportRows.add(member(id));
        }
        OrderMemberUser filter = new OrderMemberUser();
        when(userMapper.selectOrderMemberUserList(filter)).thenReturn(exportRows);
        when(userMapper.selectMemberListFinancialAggregates(anyList()))
                .thenReturn(Collections.emptyList());
        when(userMapper.selectMemberListSubordinateAggregates(anyList()))
                .thenReturn(Collections.emptyList());

        userService.selectOrderMemberUserList(filter);

        ArgumentCaptor<List<Long>> financialBatches = listCaptor();
        verify(userMapper, org.mockito.Mockito.times(3))
                .selectMemberListFinancialAggregates(financialBatches.capture());
        assertEquals(List.of(500, 500, 1),
                financialBatches.getAllValues().stream().map(List::size).toList());

        ArgumentCaptor<List<Long>> subordinateBatches = listCaptor();
        verify(userMapper, org.mockito.Mockito.times(3))
                .selectMemberListSubordinateAggregates(subordinateBatches.capture());
        assertEquals(List.of(500, 500, 1),
                subordinateBatches.getAllValues().stream().map(List::size).toList());
        assertEquals(BigDecimal.ZERO, exportRows.get(1000).getDiffAmount());
    }

    @Test
    void aggregateSortLoadsSortedIdsBeforeRowsAndPreservesPageMetadataAndOrder() {
        OrderMemberUser filter = new OrderMemberUser();
        Page<Long> sortedIds = new Page<>(2, 2);
        sortedIds.setTotal(5L);
        sortedIds.add(30L);
        sortedIds.add(10L);

        OrderMemberUser id10 = member(10L);
        OrderMemberUser id30 = member(30L);
        when(userMapper.selectOrderMemberUserAggregateSortedIds(filter)).thenReturn(sortedIds);
        when(userMapper.selectOrderMemberUserListByIds(List.of(30L, 10L)))
                .thenReturn(List.of(id10, id30));
        when(userMapper.selectMemberListFinancialAggregates(List.of(30L, 10L)))
                .thenReturn(Collections.emptyList());
        when(userMapper.selectMemberListSubordinateAggregates(List.of(30L, 10L)))
                .thenReturn(Collections.emptyList());

        List<OrderMemberUser> result = userService.selectOrderMemberUserListByAggregateSort(
                filter, "total_recharge", "desc");

        Page<?> resultPage = (Page<?>) result;
        assertEquals(2, resultPage.getPageNum());
        assertEquals(2, resultPage.getPageSize());
        assertEquals(5L, resultPage.getTotal());
        assertEquals(List.of(30L, 10L), result.stream().map(OrderMemberUser::getId).toList());
        assertEquals(BigDecimal.ZERO, result.get(0).getTotalRecharge());
        assertEquals(BigDecimal.ZERO, result.get(1).getDiffAmount());
        assertEquals(Collections.emptyMap(), filter.getParams());
    }

    @Test
    void emptyAggregateSortedPageSkipsRowAndAggregateQueries() {
        OrderMemberUser filter = new OrderMemberUser();
        Page<Long> emptyIds = new Page<>(5, 10);
        emptyIds.setTotal(0L);
        when(userMapper.selectOrderMemberUserAggregateSortedIds(filter)).thenReturn(emptyIds);

        List<OrderMemberUser> result = userService.selectOrderMemberUserListByAggregateSort(
                filter, "all_sub_count", "asc");

        assertEquals(0, result.size());
        assertEquals(0L, ((Page<?>) result).getTotal());
        verify(userMapper, never()).selectOrderMemberUserListByIds(anyList());
        verify(userMapper, never()).selectMemberListFinancialAggregates(anyList());
        verify(userMapper, never()).selectMemberListSubordinateAggregates(anyList());
    }

    @Test
    void missingSecondStageRowFailsInsteadOfReturningAShortPage() {
        OrderMemberUser filter = new OrderMemberUser();
        Page<Long> sortedIds = new Page<>(1, 2);
        sortedIds.setTotal(2L);
        sortedIds.add(30L);
        sortedIds.add(10L);
        when(userMapper.selectOrderMemberUserAggregateSortedIds(filter)).thenReturn(sortedIds);
        when(userMapper.selectOrderMemberUserListByIds(List.of(30L, 10L)))
                .thenReturn(List.of(member(30L)));

        assertThrows(com.brushing.common.exception.ServiceException.class,
                () -> userService.selectOrderMemberUserListByAggregateSort(
                        filter, "total_withdraw", "desc"));
        assertEquals(Collections.emptyMap(), filter.getParams());
    }

    @Test
    void aggregateSortRejectsAnythingOutsideTheFixedContract() {
        OrderMemberUser filter = new OrderMemberUser();

        assertThrows(com.brushing.common.exception.ServiceException.class,
                () -> userService.selectOrderMemberUserListByAggregateSort(
                        filter, "username", "desc"));
        assertThrows(com.brushing.common.exception.ServiceException.class,
                () -> userService.selectOrderMemberUserListByAggregateSort(
                        filter, "total_recharge", "desc, id asc"));
        verify(userMapper, never()).selectOrderMemberUserAggregateSortedIds(filter);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<List<Long>> listCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(List.class);
    }

    private OrderMemberUser member(Long id) {
        OrderMemberUser member = new OrderMemberUser();
        member.setId(id);
        return member;
    }
}
