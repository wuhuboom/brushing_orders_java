package com.order.member.service.impl;

import com.order.common.exception.ServiceException;
import com.order.member.domain.OrderUser;
import com.order.member.mapper.OrderUserMapper;
import com.order.member.service.MemberDailyStatisticsService;
import com.order.member.service.MemberTaskProgressService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderUserExportServiceTest {
    @Mock private OrderUserMapper mapper;
    @Mock private MemberDailyStatisticsService dailyStatistics;
    @Mock private MemberTaskProgressService taskProgressService;
    @InjectMocks private OrderUserServiceImpl service;

    @Test
    void oneBatchMatchesFinancialTotalsByIdAndPreservesDailyAndBaseAmounts() {
        OrderUser first = member(7L);
        first.setBalance(new BigDecimal("-12.34"));
        OrderUser second = member(8L);
        List<OrderUser> members = List.of(first, second);
        when(mapper.selectOrderUserExportBatch(any(OrderUser.class))).thenReturn(members);
        doAnswer(invocation -> {
            first.setTodayCommission(new BigDecimal("1.25"));
            return null;
        }).when(dailyStatistics).populate(members);
        when(mapper.selectOrderUserExportStatistics(List.of(7L, 8L))).thenReturn(List.of(
                totals(8L, "8000000000.12", "8.88", "0.00"),
                totals(7L, "123.45", "20.00", "3.50")));

        List<OrderUser> exported = service.selectOrderUserExportBatch(new OrderUser(), 0L, 20L, 500);

        assertSame(members, exported);
        assertEquals(new BigDecimal("123.45"), first.getTotalCommission());
        assertEquals(new BigDecimal("20.00"), first.getLuckyBonus());
        assertEquals(new BigDecimal("3.50"), first.getPendingBonusAmount());
        assertEquals(new BigDecimal("8000000000.12"), second.getTotalCommission());
        assertEquals(new BigDecimal("-12.34"), first.getBalance());
        assertEquals(new BigDecimal("1.25"), first.getTodayCommission());
        verify(dailyStatistics).populate(members);
        verify(mapper, never()).selectOrderUserList(any());
        verify(mapper, never()).selectOrderUserById(anyLong());
    }

    @Test
    void cursorAndBoundAreAppliedToACopyWithoutChangingActiveFilters() {
        OrderUser query = new OrderUser();
        query.setKeyword(" member-a ");
        query.setUsernameList("member-a,member-b");
        query.setBalanceMin(BigDecimal.ZERO);
        query.getParams().put("beginTime", "2026-11-01");
        when(mapper.selectOrderUserExportBatch(any())).thenReturn(List.of());

        service.selectOrderUserExportBatch(query, 100L, 900L, 200);

        ArgumentCaptor<OrderUser> captured = ArgumentCaptor.forClass(OrderUser.class);
        verify(mapper).selectOrderUserExportBatch(captured.capture());
        OrderUser batchQuery = captured.getValue();
        assertNotSame(query, batchQuery);
        assertEquals("member-a", batchQuery.getKeyword());
        assertEquals(query.getUsernameList(), batchQuery.getUsernameList());
        assertEquals(BigDecimal.ZERO, batchQuery.getBalanceMin());
        assertEquals(Map.of("beginTime", "2026-11-01", "exportAfterId", 100L,
                "exportMaxId", 900L, "exportBatchSize", 200), batchQuery.getParams());
        assertEquals(" member-a ", query.getKeyword());
        assertEquals(Map.of("beginTime", "2026-11-01"), query.getParams());
        verifyNoInteractions(dailyStatistics);
        verify(mapper, never()).selectOrderUserExportStatistics(anyCollection());
    }

    @Test
    void noMatchingMembersSkipAdditionalFinancialQueries() {
        when(mapper.selectOrderUserExportBatch(any())).thenReturn(List.of());
        assertEquals(List.of(), service.selectOrderUserExportBatch(new OrderUser(), 0L, 10L, 500));
        verifyNoInteractions(dailyStatistics);
        verify(mapper, never()).selectOrderUserExportStatistics(anyCollection());
        verify(mapper, never()).selectOrderUserList(any());
    }

    @Test
    void finishedOrEmptyIdRangeDoesNotRunAnotherQuery() {
        assertEquals(List.of(), service.selectOrderUserExportBatch(null, 10L, 10L, 500));
        assertEquals(List.of(), service.selectOrderUserExportBatch(null, 11L, 10L, 500));
        assertEquals(List.of(), service.selectOrderUserExportBatch(null, null, null, 500));
        verifyNoInteractions(mapper, dailyStatistics);
    }

    @Test
    void oversizedOrInvalidBatchesCannotReachTheDatabase() {
        for (int size : List.of(0, -1, 501, Integer.MAX_VALUE)) {
            assertThrows(ServiceException.class,
                    () -> service.selectOrderUserExportBatch(null, 0L, 1000L, size));
        }
        assertThrows(ServiceException.class,
                () -> service.selectOrderUserExportBatch(null, -1L, 1000L, 500));
        verifyNoInteractions(mapper, dailyStatistics);
    }

    @Test
    void highWaterMarkDoesNotMaterializeAnyMemberIds() {
        when(mapper.selectOrderUserExportMaxId()).thenReturn(null, 123456789012345L);
        assertEquals(0L, service.selectOrderUserExportMaxId());
        assertEquals(123456789012345L, service.selectOrderUserExportMaxId());
        verify(mapper, times(2)).selectOrderUserExportMaxId();
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void missingStatisticsFailTheExportInsteadOfReportingInventedZeroAmounts() {
        when(mapper.selectOrderUserExportBatch(any())).thenReturn(List.of(member(7L), member(8L)));
        when(mapper.selectOrderUserExportStatistics(anyCollection()))
                .thenReturn(List.of(totals(7L, "0.00", "0.00", "0.00")));
        ServiceException error = assertThrows(ServiceException.class,
                () -> service.selectOrderUserExportBatch(new OrderUser(), 0L, 10L, 500));
        assertEquals("会员资金统计缺失，请重试导出", error.getMessage());
        verify(mapper, never()).selectOrderUserList(any());
    }

    @Test
    void eachBatchEndsItsOwnTransactionAndSessionCache() throws Exception {
        Transactional transaction = OrderUserServiceImpl.class.getMethod(
                "selectOrderUserExportBatch", OrderUser.class, Long.class, Long.class, int.class)
                .getAnnotation(Transactional.class);
        assertNotNull(transaction);
        assertTrue(transaction.readOnly());
        assertEquals(Propagation.REQUIRES_NEW, transaction.propagation());
    }

    private static OrderUser member(long id) {
        OrderUser user = new OrderUser();
        user.setId(id);
        user.setUsername("member-" + id);
        return user;
    }

    private static OrderUser totals(long id, String commission, String distributed, String pending) {
        OrderUser user = member(id);
        user.setTotalCommission(new BigDecimal(commission));
        user.setLuckyBonus(new BigDecimal(distributed));
        user.setPendingBonusAmount(new BigDecimal(pending));
        return user;
    }
}
