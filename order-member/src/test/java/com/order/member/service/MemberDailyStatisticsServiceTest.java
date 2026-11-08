package com.order.member.service;

import com.github.pagehelper.Page;
import com.order.common.exception.ServiceException;
import com.order.member.domain.MemberDailyStatistics;
import com.order.member.domain.OrderUser;
import com.order.member.mapper.MemberDailyStatisticsMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberDailyStatisticsServiceTest {
    @Mock
    private MemberDailyStatisticsMapper mapper;

    private MemberDailyStatisticsService service;

    @BeforeEach
    void setUp() {
        service = at("2026-09-05T16:29:42Z");
    }

    @Test
    void backupInstantUsesNewYorkCalendarDayAndUpdatesExistingPageObjects() {
        when(mapper.selectActiveTimeZone()).thenReturn("America/New_York");
        MemberDailyStatistics row = statistics(247L, "581.25", "116.21", 3);
        when(mapper.selectDailyStatistics(any(), any(), any())).thenReturn(List.of(row));

        OrderUser katherine = user(247L);
        OrderUser duplicate = user(247L);
        OrderUser noActivity = user(248L);
        OrderUser noId = user(null);
        Page<OrderUser> page = new Page<>(2, 20);
        page.setTotal(95);
        page.addAll(Arrays.asList(katherine, duplicate, noActivity, noId, null));

        service.populate(page);

        verify(mapper).selectDailyStatistics(
                eq(Set.of(247L, 248L)),
                eq(LocalDateTime.parse("2026-09-05T04:00:00")),
                eq(LocalDateTime.parse("2026-09-06T04:00:00")));
        assertSame(katherine, page.get(0));
        assertEquals(2, page.getPageNum());
        assertEquals(20, page.getPageSize());
        assertEquals(95, page.getTotal());
        assertTotals(katherine, "581.25", "116.21", 3);
        assertTotals(duplicate, "581.25", "116.21", 3);
        assertTotals(noActivity, "0", "0", 0);
        assertTotals(noId, "0", "0", 0);
    }

    @Test
    void newYorkSpringDayHasTwentyThreeHours() {
        when(mapper.selectActiveTimeZone()).thenReturn("America/New_York");

        at("2026-03-08T17:00:00Z").populate(user(247L));

        verify(mapper).selectDailyStatistics(
                eq(Set.of(247L)),
                eq(LocalDateTime.parse("2026-03-08T05:00:00")),
                eq(LocalDateTime.parse("2026-03-09T04:00:00")));
    }

    @Test
    void newYorkAutumnDayHasTwentyFiveHours() {
        when(mapper.selectActiveTimeZone()).thenReturn("America/New_York");

        at("2026-11-01T17:00:00Z").populate(user(247L));

        verify(mapper).selectDailyStatistics(
                eq(Set.of(247L)),
                eq(LocalDateTime.parse("2026-11-01T04:00:00")),
                eq(LocalDateTime.parse("2026-11-02T05:00:00")));
    }

    @Test
    void activeTimeZoneChangesTakeEffectOnTheNextQuery() {
        when(mapper.selectActiveTimeZone()).thenReturn("America/New_York", "Asia/Shanghai");

        service.populate(user(247L));
        service.populate(user(247L));

        verify(mapper).selectDailyStatistics(
                eq(Set.of(247L)),
                eq(LocalDateTime.parse("2026-09-05T04:00:00")),
                eq(LocalDateTime.parse("2026-09-06T04:00:00")));
        verify(mapper).selectDailyStatistics(
                eq(Set.of(247L)),
                eq(LocalDateTime.parse("2026-09-05T16:00:00")),
                eq(LocalDateTime.parse("2026-09-06T16:00:00")));
    }

    @Test
    void nullAndEmptyInputsDoNotQueryConfigurationOrStatistics() {
        service.populate((OrderUser) null);
        service.populate((List<OrderUser>) null);
        service.populate(List.of());
        service.populate(Arrays.asList(null, null));
        OrderUser noId = user(null);
        service.populate(noId);

        verifyNoInteractions(mapper);
        assertTotals(noId, "0", "0", 0);
    }

    @Test
    void missingActiveTimeZoneFailsClearlyWithoutQueryingTotals() {
        ServiceException exception = assertThrows(
                ServiceException.class, () -> service.populate(user(247L)));

        assertTrue(exception.getMessage().contains("时区未配置"));
        verify(mapper, never()).selectDailyStatistics(any(), any(), any());
    }

    @Test
    void blankActiveTimeZoneFailsClearlyWithoutQueryingTotals() {
        when(mapper.selectActiveTimeZone()).thenReturn(" ");

        ServiceException exception = assertThrows(
                ServiceException.class, () -> service.populate(user(247L)));

        assertTrue(exception.getMessage().contains("时区未配置"));
        verify(mapper, never()).selectDailyStatistics(any(), any(), any());
    }

    @Test
    void invalidActiveTimeZoneFailsClearlyWithoutQueryingTotals() {
        when(mapper.selectActiveTimeZone()).thenReturn("Not/A_TimeZone");

        ServiceException exception = assertThrows(
                ServiceException.class, () -> service.populate(user(247L)));

        assertTrue(exception.getMessage().contains("时区无效"));
        assertTrue(exception.getMessage().contains("Not/A_TimeZone"));
        verify(mapper, never()).selectDailyStatistics(any(), any(), any());
    }

    @Test
    void absentAmountsRemainZeroAndOldDailyTotalsAreResetOnEveryCall() {
        when(mapper.selectActiveTimeZone()).thenReturn("America/New_York");
        MemberDailyStatistics partiallyEmpty = statistics(247L, null, "1.25", null);
        when(mapper.selectDailyStatistics(any(), any(), any()))
                .thenReturn(List.of(partiallyEmpty), List.of());
        OrderUser user = user(247L);

        service.populate(user);
        assertTotals(user, "0", "1.25", 0);

        service.populate(user);
        assertTotals(user, "0", "0", 0);
    }

    private MemberDailyStatisticsService at(String instant) {
        return new MemberDailyStatisticsService(
                mapper, Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
    }

    private OrderUser user(Long id) {
        OrderUser user = new OrderUser();
        user.setId(id);
        user.setTodayCommission(new BigDecimal("999.99"));
        user.setTodayParentCommission(new BigDecimal("888.88"));
        user.setTodayWithdrawalCount(7);
        return user;
    }

    private MemberDailyStatistics statistics(Long id, String commission, String parentCommission, Integer withdrawals) {
        MemberDailyStatistics row = new MemberDailyStatistics();
        row.setUserId(id);
        row.setTodayCommission(commission == null ? null : new BigDecimal(commission));
        row.setTodayParentCommission(parentCommission == null ? null : new BigDecimal(parentCommission));
        row.setTodayWithdrawalCount(withdrawals);
        return row;
    }

    private void assertTotals(OrderUser user, String commission, String parentCommission, int withdrawals) {
        assertEquals(new BigDecimal(commission), user.getTodayCommission());
        assertEquals(new BigDecimal(parentCommission), user.getTodayParentCommission());
        assertEquals(withdrawals, user.getTodayWithdrawalCount());
    }
}
