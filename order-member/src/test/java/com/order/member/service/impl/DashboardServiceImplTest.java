package com.order.member.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.order.member.mapper.DashboardMapper;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest
{
    @Mock
    private DashboardMapper mapper;

    @InjectMocks
    private DashboardServiceImpl service;

    @Test
    void exposesPendingWithdrawalCounterInHeader()
    {
        when(mapper.getTotalOrders()).thenReturn(1979L);
        when(mapper.getPendingWithdrawals()).thenReturn(2L);

        Map<String, Object> stats = service.getHeaderStats();

        assertEquals(1979L, stats.get("totalOrders"));
        assertEquals(2L, stats.get("totalWithdrawals"));
        verify(mapper).getPendingWithdrawals();
        verify(mapper, never()).getTotalWithdrawals();
    }

    @Test
    @SuppressWarnings("unchecked")
    void fillsEveryDayInTheSevenDayDashboardWindow()
    {
        String dataDate = LocalDate.now().minusDays(3).toString();
        Map<String, Object> registration = new HashMap<>();
        registration.put("date", dataDate);
        registration.put("count", 5L);

        Map<String, Object> counts = new HashMap<>();
        counts.put("date", LocalDate.now().toString());
        counts.put("signin_count", 2L);
        counts.put("reset_count", 3L);

        when(mapper.getWeeklyRegisteredUsers()).thenReturn(Collections.singletonList(registration));
        when(mapper.getWeeklyTransactionAmounts()).thenReturn(Collections.emptyList());
        when(mapper.getWeeklyTransactionCounts()).thenReturn(Collections.singletonList(counts));

        Map<String, Object> stats = service.getStats();
        List<Map<String, Object>> registrations = (List<Map<String, Object>>) stats.get("weeklyRegisteredUsers");
        List<Map<String, Object>> transactionCounts = (List<Map<String, Object>>) stats.get("weeklyTransactionCounts");

        assertEquals(7, registrations.size());
        assertEquals(LocalDate.now().minusDays(6).toString(), registrations.get(0).get("date"));
        assertEquals(LocalDate.now().toString(), registrations.get(6).get("date"));
        assertEquals(5L, registrations.get(3).get("count"));
        assertEquals(2L, transactionCounts.get(6).get("signin_count"));
        assertEquals(3L, transactionCounts.get(6).get("reset_count"));
        assertEquals(0L, transactionCounts.get(0).get("order_count"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void convertsMissingDashboardValuesToZero()
    {
        when(mapper.getWeeklyRegisteredUsers()).thenReturn(null);
        when(mapper.getWeeklyTransactionAmounts()).thenReturn(null);
        when(mapper.getWeeklyTransactionCounts()).thenReturn(null);

        Map<String, Object> stats = service.getStats();

        assertEquals(0L, stats.get("todayRegisteredUsers"));
        assertEquals(java.math.BigDecimal.ZERO, stats.get("todayRechargeAmount"));
        assertEquals(7, ((List<Map<String, Object>>) stats.get("weeklyTransactionAmounts")).size());
    }
}
