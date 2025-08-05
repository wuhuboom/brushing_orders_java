package com.brushing.common.utils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class OrderNoGenerator {
    // 自增序列（0~999循环）
    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Random RANDOM = new Random();
    // 时间格式：年月日时分秒（14位）
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static String generateOrderNo() {
        String timePart = LocalDateTime.now().format(FORMATTER); // 14位
        int seq = SEQUENCE.getAndIncrement() % 1000;              // 3位
        String randomPart = String.valueOf((int)(Math.random() * 90000 + 10000)); // 5位随机数
        return timePart + randomPart + String.format("%03d", seq); // 共22位（可自行扩展）
    }

    public static String generateOrderId() {
        // 获取当前时间戳 (yyyyMMdd)
        String datePrefix = LocalDateTime.now().format(DATE_FORMATTER);

        // 生成一个6位的序列号 (线程安全)
        long sequence = SEQUENCE.getAndIncrement() % 1000000;

        // 生成一个10位的随机数
        String randomPart = String.format("%010d", Math.abs(RANDOM.nextLong() % 10000000000L));

        // 拼接订单号: 时间戳(8位) + 序列号(6位) + 随机数(10位)
        return datePrefix + String.format("%06d", sequence) + randomPart;
    }

    public static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) < 0;
    }
}
