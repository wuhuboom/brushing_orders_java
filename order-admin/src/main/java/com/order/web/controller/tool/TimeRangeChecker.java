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
        if (configValue.isPresent()) {
            // 假设 configValue 中包含一个时间范围，类型是 List<String>
            List<String> timeRange = (List<String>) configValue.get();

            if (timeRange.size() == 2) {
                // 解析开始时间和结束时间
                String startTimeString = timeRange.get(0);
                String endTimeString = timeRange.get(1);

                // 将字符串转换为 LocalTime 对象
                LocalTime startTime = LocalTime.parse(startTimeString, TIME_FORMATTER);
                LocalTime endTime = LocalTime.parse(endTimeString, TIME_FORMATTER);

                // 获取指定时区的当前时间
                LocalTime currentTime = LocalTime.now(ZoneId.of(tzName));

                // 判断当前时间是否在范围内
                return currentTime.isAfter(startTime) && currentTime.isBefore(endTime);
            } else {
                throw new IllegalArgumentException("Time range configuration is incorrect, it should contain exactly two time values.");
            }
        } else {
            throw new IllegalStateException("Config value is not present.");
        }
    }
}
