package com.order.member.service;

import com.order.common.exception.ServiceException;
import com.order.member.domain.MemberDailyStatistics;
import com.order.member.domain.OrderUser;
import com.order.member.mapper.MemberDailyStatisticsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Populates daily totals after the main member query has consumed pagination. */
@Service
public class MemberDailyStatisticsService {
    private final MemberDailyStatisticsMapper mapper;
    private final Clock clock;

    @Autowired
    public MemberDailyStatisticsService(MemberDailyStatisticsMapper mapper) {
        this(mapper, Clock.systemUTC());
    }

    MemberDailyStatisticsService(MemberDailyStatisticsMapper mapper, Clock clock) {
        this.mapper = Objects.requireNonNull(mapper);
        this.clock = Objects.requireNonNull(clock);
    }

    public void populate(OrderUser user) {
        if (user != null) {
            populate(List.of(user));
        }
    }

    public void populate(List<OrderUser> users) {
        if (users == null || users.isEmpty()) {
            return;
        }

        Set<Long> userIds = new LinkedHashSet<>();
        for (OrderUser user : users) {
            if (user == null) {
                continue;
            }
            user.setTodayCommission(BigDecimal.ZERO);
            user.setTodayParentCommission(BigDecimal.ZERO);
            user.setTodayWithdrawalCount(0);
            if (user.getId() != null) {
                userIds.add(user.getId());
            }
        }
        if (userIds.isEmpty()) {
            return;
        }

        ZoneId zone = activeZone();
        LocalDate today = clock.instant().atZone(zone).toLocalDate();
        LocalDateTime startUtc = LocalDateTime.ofInstant(
                today.atStartOfDay(zone).toInstant(), ZoneOffset.UTC);
        LocalDateTime endUtc = LocalDateTime.ofInstant(
                today.plusDays(1).atStartOfDay(zone).toInstant(), ZoneOffset.UTC);

        List<MemberDailyStatistics> rows = mapper.selectDailyStatistics(userIds, startUtc, endUtc);
        Map<Long, MemberDailyStatistics> byUserId = new HashMap<>();
        if (rows != null) {
            for (MemberDailyStatistics row : rows) {
                if (row != null && row.getUserId() != null) {
                    byUserId.put(row.getUserId(), row);
                }
            }
        }

        for (OrderUser user : users) {
            if (user == null) {
                continue;
            }
            MemberDailyStatistics row = byUserId.get(user.getId());
            if (row == null) {
                continue;
            }
            if (row.getTodayCommission() != null) {
                user.setTodayCommission(row.getTodayCommission());
            }
            if (row.getTodayParentCommission() != null) {
                user.setTodayParentCommission(row.getTodayParentCommission());
            }
            if (row.getTodayWithdrawalCount() != null) {
                user.setTodayWithdrawalCount(row.getTodayWithdrawalCount());
            }
        }
    }

    private ZoneId activeZone() {
        String timeZone = mapper.selectActiveTimeZone();
        if (timeZone == null || timeZone.isBlank()) {
            throw new ServiceException("系统启用时区未配置，无法统计今日数据");
        }
        try {
            return ZoneId.of(timeZone.trim());
        } catch (DateTimeException exception) {
            throw new ServiceException("系统启用时区无效：" + timeZone + "，无法统计今日数据");
        }
    }
}
