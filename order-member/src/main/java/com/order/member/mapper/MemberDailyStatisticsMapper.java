package com.order.member.mapper;

import com.order.member.domain.MemberDailyStatistics;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface MemberDailyStatisticsMapper {
    String selectActiveTimeZone();

    List<MemberDailyStatistics> selectDailyStatistics(
            @Param("userIds") Collection<Long> userIds,
            @Param("startUtc") LocalDateTime startUtc,
            @Param("endUtc") LocalDateTime endUtc);
}
