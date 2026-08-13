package com.brushing.member.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
// no @Service here to avoid creating a conflicting bean with the framework implementation

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TableExistenceChecker {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final Map<String, Boolean> cache = new ConcurrentHashMap<>();

    public boolean hasTable(String tableName) {
        return cache.computeIfAbsent(tableName, this::checkTableExists);
    }

    private boolean checkTableExists(String tableName) {
        try {
            String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?";
            Integer cnt = jdbcTemplate.queryForObject(sql, new Object[]{tableName}, Integer.class);
            return cnt != null && cnt > 0;
        } catch (Exception ex) {
            return false;
        }
    }

    public void clearCache() {
        cache.clear();
    }
}
