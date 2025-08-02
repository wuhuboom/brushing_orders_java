package com.brushing.common.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

public class OrderNoGenerator {
    // 自增序列（0~999循环）
    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);

    // 时间格式：年月日时分秒（14位）
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static String generateOrderNo() {
        String timePart = LocalDateTime.now().format(FORMATTER); // 14位
        int seq = SEQUENCE.getAndIncrement() % 1000;              // 3位
        String randomPart = String.valueOf((int)(Math.random() * 90000 + 10000)); // 5位随机数
        return timePart + randomPart + String.format("%03d", seq); // 共22位（可自行扩展）
    }
}
