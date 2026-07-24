package com.order.web.controller.tool;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class TimeRangeChecker {

    // 默认的时间格式，可以根据需要修改
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * 判断指定时区的当前时间是否在指定的时间范围内
     *
     * @param configValue 包含时间范围的 Optional 对象，时间范围应该是一个包含开始和结束时间的 List<String>
     * @param tzName 时区名称，例如 "Asia/Shanghai"
     * @return true 如果指定时区的当前时间在范围内，false 如果不在范围内
     */
    public static boolean isCurrentTimeInRange(Optional<Object> configValue, String tzName) {
        if (configValue.isEmpty()) {
            throw new IllegalStateException("Config value is not present.");
        }
        if (!(configValue.get() instanceof List<?> timeRange)
                || timeRange.size() != 2
                || !(timeRange.get(0) instanceof String startValue)
                || !(timeRange.get(1) instanceof String endValue)) {
            throw new IllegalArgumentException(
                    "Time range configuration is incorrect, it should contain exactly two time values.");
        }

        LocalTime startTime = LocalTime.parse(startValue, TIME_FORMATTER);
        LocalTime endTime = LocalTime.parse(endValue, TIME_FORMATTER);
        LocalTime currentTime = LocalTime.now(ZoneId.of(tzName));

        if (startTime.equals(endTime)) {
            return true;
        }
        if (startTime.isBefore(endTime)) {
            return !currentTime.isBefore(startTime) && !currentTime.isAfter(endTime);
        }
        return !currentTime.isBefore(startTime) || !currentTime.isAfter(endTime);
    }
}
