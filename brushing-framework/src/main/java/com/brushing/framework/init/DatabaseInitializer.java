package com.brushing.framework.init;


import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Country;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.xml.stream.Location;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;


@Service
public class DatabaseInitializer {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private GeoIpQueryQueryService queryService;

    public void init() {
        // 检查表结构
        checkAndAddTotpFields();
        addGlobalConfigColumns();
        addMemberLevelColumns();
        scheduledUpdate();
    }


    public void scheduledUpdate() {
        queryService.updateAllUsersGeoLocation();
    }





























    public void addMemberLevelColumns() {
        String tableName = "order_member_level";  // 表名

        // 定义所有需要添加的列：主字段名 + 语言后缀 + 类型 + 注释
        // 使用Map组织；value为"类型 NOT NULL DEFAULT ('') COMMENT '注释'"
        java.util.Map<String, String> columnsToAdd = new java.util.HashMap<>();

        // 描述 (Description)
        columnsToAdd.put("description_ja", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '描述 - 日文'");
        columnsToAdd.put("description_th", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '描述 - 泰文'");
        columnsToAdd.put("description_ko", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '描述 - 韩文'");
        columnsToAdd.put("description_zh_tw", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '描述 - 中文繁体'");

        // 1) 查询列是否存在并添加
        for (java.util.Map.Entry<String, String> entry : columnsToAdd.entrySet()) {
            String columnName = entry.getKey();
            String columnDefinition = entry.getValue();  // 如 "LONGTEXT NOT NULL DEFAULT ('') COMMENT '...'"

            String checkColumnSql =
                    "SELECT COUNT(*) " +
                            "FROM INFORMATION_SCHEMA.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";

            Integer cnt = jdbcTemplate.queryForObject(checkColumnSql, Integer.class, tableName, columnName);

            // 2) 不存在则添加
            if (cnt == null || cnt == 0) {
                String alterSql = "ALTER TABLE `" + tableName + "` " +
                        "ADD COLUMN `" + columnName + "` " + columnDefinition;
                try {
                    jdbcTemplate.execute(alterSql);
                    System.out.println("成功添加列: " + columnName);  // 可选：日志输出
                } catch (DataAccessException e) {
                    // 并发下可能已被其它实例先添加，忽略“Duplicate column name”错误
                    // 额外捕获默认值相关错误（如果版本不支持），记录日志
                    String msg = e.getMessage();
                    if (msg != null && msg.contains("Duplicate column name")) {
                        System.out.println("列 " + columnName + " 已存在或被其他实例添加，忽略。");
                    } else if (msg != null && msg.contains("can't have a default value")) {
                        System.out.println("警告: 列 " + columnName + " 默认值设置失败（MySQL版本限制），已回退为 NULLABLE。尝试修复...");
                        // 可选回退：添加为 NULLABLE
                        String fallbackSql = "ALTER TABLE `" + tableName + "` " +
                                "ADD COLUMN `" + columnName + "` LONGTEXT NULL DEFAULT NULL COMMENT '" +
                                columnDefinition.split("COMMENT '")[1].split("'")[0] + "'";  // 提取注释
                        jdbcTemplate.execute(fallbackSql);
                        System.out.println("回退成功: " + columnName + " 使用 NULL DEFAULT。");
                    } else {
                        throw e;
                    }
                }
            } else {
                System.out.println("列 " + columnName + " 已存在，跳过。");  // 可选：日志输出
            }
        }
    }

    public void addGlobalConfigColumns() {
        String tableName = "order_global_config";  // 表名

        // 使用Map组织；value为"类型 NOT NULL DEFAULT ('') COMMENT '注释'"
        java.util.Map<String, String> columnsToAdd = new java.util.HashMap<>();

        // 注册协议 (Registration Agreement)
        columnsToAdd.put("registration_agreement_ja", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '注册协议 - 日文'");
        columnsToAdd.put("registration_agreement_th", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '注册协议 - 泰文'");
        columnsToAdd.put("registration_agreement_ko", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '注册协议 - 韩文'");
        columnsToAdd.put("registration_agreement_zh_tw", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '注册协议 - 中文繁体'");

        // 关于我们 (About Us)
        columnsToAdd.put("about_us_ja", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '关于我们 - 日文'");
        columnsToAdd.put("about_us_th", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '关于我们 - 泰文'");
        columnsToAdd.put("about_us_ko", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '关于我们 - 韩文'");
        columnsToAdd.put("about_us_zh_tw", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '关于我们 - 中文繁体'");

        // 证书 (Certificate)
        columnsToAdd.put("certificate_ja", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '证书 - 日文'");
        columnsToAdd.put("certificate_th", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '证书 - 泰文'");
        columnsToAdd.put("certificate_ko", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '证书 - 韩文'");
        columnsToAdd.put("certificate_zh_tw", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '证书 - 中文繁体'");

        // 常见问题 (FAQ)
        columnsToAdd.put("faq_ja", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '常见问题 - 日文'");
        columnsToAdd.put("faq_th", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '常见问题 - 泰文'");
        columnsToAdd.put("faq_ko", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '常见问题 - 韩文'");
        columnsToAdd.put("faq_zh_tw", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '常见问题 - 中文繁体'");

        // 最新事件 (Latest Events)
        columnsToAdd.put("latest_events_ja", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '最新事件 - 日文'");
        columnsToAdd.put("latest_events_th", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '最新事件 - 泰文'");
        columnsToAdd.put("latest_events_ko", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '最新事件 - 韩文'");
        columnsToAdd.put("latest_events_zh_tw", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '最新事件 - 中文繁体'");

        // 条款条规 (Terms and Conditions)
        columnsToAdd.put("terms_conditions_ja", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '条款条规 - 日文'");
        columnsToAdd.put("terms_conditions_th", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '条款条规 - 泰文'");
        columnsToAdd.put("terms_conditions_ko", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '条款条规 - 韩文'");
        columnsToAdd.put("terms_conditions_zh_tw", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '条款条规 - 中文繁体'");

        // 收入指南 (Income Guide)
        columnsToAdd.put("income_guide_ja", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '收入指南 - 日文'");
        columnsToAdd.put("income_guide_th", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '收入指南 - 泰文'");
        columnsToAdd.put("income_guide_ko", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '收入指南 - 韩文'");
        columnsToAdd.put("income_guide_zh_tw", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '收入指南 - 中文繁体'");

        // 1) 查询列是否存在并添加
        for (java.util.Map.Entry<String, String> entry : columnsToAdd.entrySet()) {
            String columnName = entry.getKey();
            String columnDefinition = entry.getValue();  // 如 "LONGTEXT NOT NULL DEFAULT ('') COMMENT '...'"

            String checkColumnSql =
                    "SELECT COUNT(*) " +
                            "FROM INFORMATION_SCHEMA.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";

            Integer cnt = jdbcTemplate.queryForObject(checkColumnSql, Integer.class, tableName, columnName);

            // 2) 不存在则添加
            if (cnt == null || cnt == 0) {
                String alterSql = "ALTER TABLE `" + tableName + "` " +
                        "ADD COLUMN `" + columnName + "` " + columnDefinition;
                try {
                    jdbcTemplate.execute(alterSql);
                    System.out.println("成功添加列: " + columnName);  // 可选：日志输出
                } catch (DataAccessException e) {
                    // 并发下可能已被其它实例先添加，忽略“Duplicate column name”错误
                    // 额外捕获默认值相关错误（如果版本不支持），记录日志
                    String msg = e.getMessage();
                    if (msg != null && msg.contains("Duplicate column name")) {
                        System.out.println("列 " + columnName + " 已存在或被其他实例添加，忽略。");
                    } else if (msg != null && msg.contains("can't have a default value")) {
                        System.out.println("警告: 列 " + columnName + " 默认值设置失败（MySQL版本限制），已回退为 NULLABLE。尝试修复...");
                        // 可选回退：添加为 NULLABLE
                        String fallbackSql = "ALTER TABLE `" + tableName + "` " +
                                "ADD COLUMN `" + columnName + "` LONGTEXT NULL DEFAULT NULL COMMENT '" +
                                columnDefinition.split("COMMENT '")[1].split("'")[0] + "'";  // 提取注释
                        jdbcTemplate.execute(fallbackSql);
                        System.out.println("回退成功: " + columnName + " 使用 NULL DEFAULT。");
                    } else {
                        throw e;
                    }
                }
            } else {
                System.out.println("列 " + columnName + " 已存在，跳过。");  // 可选：日志输出
            }
        }
    }

    private void checkAndAddTotpFields() {
        String tableName = "order_member_user";
        String columnName = "version";

        // 1) 查询列是否存在
        String checkColumnSql =
                "SELECT COUNT(*) " +
                        "FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";

        Integer cnt = jdbcTemplate.queryForObject(checkColumnSql, Integer.class, tableName, columnName);

        // 2) 不存在则添加
        if (cnt == null || cnt == 0) {
            String alterSql = "ALTER TABLE `" + tableName + "` " +
                    "ADD COLUMN `" + columnName + "` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '版本号'";
            try {
                jdbcTemplate.execute(alterSql);
            } catch (org.springframework.dao.DataAccessException e) {
                // 并发下可能已被其它实例先添加，忽略“Duplicate column name”错误
                String msg = e.getMessage();
                if (msg == null || !msg.contains("Duplicate column name")) {
                    throw e;
                }
            }
        }
        String tableNames = "order_site_config";  // 表名
        String[] columnNames = {"level_status", "series_status"};  // 新增的字段名数组

        // 1) 查询列是否存在
        for (String column : columnNames) {
            String ColumnSql =
                    "SELECT COUNT(*) " +
                            "FROM INFORMATION_SCHEMA.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";

            Integer cnts = jdbcTemplate.queryForObject(ColumnSql, Integer.class, tableNames, column);

            // 2) 不存在则添加
            if (cnts == null || cnts == 0) {
                String alterSql = "ALTER TABLE `" + tableNames + "` " +
                        "ADD COLUMN `" + column + "` CHAR(1) NOT NULL DEFAULT '0' COMMENT '" + column + " 字段'";

                try {
                    jdbcTemplate.execute(alterSql);
                } catch (org.springframework.dao.DataAccessException e) {
                    // 并发下可能已被其它实例先添加，忽略“Duplicate column name”错误
                    String msg = e.getMessage();
                    if (msg == null || !msg.contains("Duplicate column name")) {
                        throw e;
                    }
                }
            }
        }


        // 3) 查询所有 DECIMAL 类型字段并修改其长度为 20，并设置默认值为 0
    /**    String checkDecimalFieldsSql =
                "SELECT TABLE_NAME, COLUMN_NAME " +
                        "FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND DATA_TYPE = 'decimal'";

// 获取所有 DECIMAL 类型字段
        List<Map<String, Object>> decimalFields = jdbcTemplate.queryForList(checkDecimalFieldsSql);

        for (Map<String, Object> field : decimalFields) {
            String tableNameInDb = (String) field.get("TABLE_NAME");
            String columnNameInDb = (String) field.get("COLUMN_NAME");

            // 生成 ALTER TABLE 语句来修改 DECIMAL 字段的长度为 20，并设置默认值为 0
            String alterDecimalColumnSql = "ALTER TABLE `" + tableNameInDb + "` " +
                    "MODIFY COLUMN `" + columnNameInDb + "` DECIMAL(20,2) DEFAULT 0";

            try {
                jdbcTemplate.execute(alterDecimalColumnSql); // 执行修改操作
            } catch (org.springframework.dao.DataAccessException e) {
                // 可以在此处理并发冲突，或者忽略已修改的字段
                String msg = e.getMessage();
                if (msg == null || !msg.contains("Duplicate column name")) {
                    throw e;
                }
            }

            // 生成 UPDATE 语句将 DECIMAL 字段中的 NULL 值替换为 0
            String updateNullValuesSql = "UPDATE `" + tableNameInDb + "` " +
                    "SET `" + columnNameInDb + "` = 0 " +
                    "WHERE `" + columnNameInDb + "` IS NULL";

            try {
                jdbcTemplate.execute(updateNullValuesSql); // 执行替换 NULL 为 0 的操作
            } catch (org.springframework.dao.DataAccessException e) {
                // 可以根据需求处理异常
                String msg = e.getMessage();
                if (msg == null || !msg.contains("Duplicate column name")) {
                    throw e;
                }
            }
        }*/
    }

}

