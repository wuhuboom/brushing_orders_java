package com.brushing.framework.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TableExistenceChecker {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final Map<String, Boolean> cache = new ConcurrentHashMap<>();

    /**
     * 快速检查当前数据库中是否存在指定表（缓存结果）
     */
    public boolean hasTable(String tableName) {
        return cache.computeIfAbsent(tableName, this::checkTableExists);
    }

    private boolean checkTableExists(String tableName) {
        try {
            String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?";
            Integer cnt = jdbcTemplate.queryForObject(sql, new Object[]{tableName}, Integer.class);
            return cnt != null && cnt > 0;
        } catch (Exception ex) {
            // any exception -> assume table does not exist; cache negative result to avoid repeated errors
            return false;
        }
    }

    /**
     * 清除缓存（如果你动态建表/删表后需要重新检测）
     */
    public void clearCache() {
        cache.clear();
    }
}
