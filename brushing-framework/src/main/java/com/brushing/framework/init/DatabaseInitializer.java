package com.brushing.framework.init;


import com.brushing.common.utils.StringUtils;
import com.brushing.system.domain.SysConfig;
import com.brushing.system.mapper.SysConfigMapper;
import com.brushing.system.mapper.SysUserMapper;
import com.brushing.system.service.ISysConfigService;
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
import org.springframework.transaction.annotation.Transactional;

import javax.xml.stream.Location;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;


@Service
@Transactional
public class DatabaseInitializer {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private GeoIpQueryQueryService queryService;

    @Autowired
    private SysConfigMapper configMapper;

    public void init() {
        scheduledUpdate();
        SysConfig sysConfig = configMapper.checkConfigKeyUnique("app-version");
          if (StringUtils.isNotNull(sysConfig)){
              String configValue = sysConfig.getConfigValue();
              if (StringUtils.isEmpty(configValue)){
                  SysConfig config =new SysConfig();
                  config.setConfigKey("app-version");
                  config.setConfigValue("1.3.6");
                  configMapper.insertConfig(config);
              }else{
                  if (configValue.equals("1.3.6")){
                      System.out.println("版本一致");
                      return;
                  }
              }
          }

        // 检查表结构
        checkAndAddTotpFields();
        addGlobalConfigColumns();
        addMemberLevelColumns();

        createOrderShopTable();

        // ensure sys_notice i18n title columns exist
        addSysNoticeColumns();

        addColumnIfNotExists("sys_user", "agent_user", "VARCHAR(255) NULL COMMENT '代理用户'");
        addColumnIfNotExists("sys_menu", "en_name", "VARCHAR(255) NULL COMMENT '英文名称'");
        // 为 sys_user 表添加代理开关字段
        addColumnIfNotExists("sys_user", "agent_switch", "CHAR(1) DEFAULT '0' COMMENT '代理开关'");

        addColumnIfNotExists("order_customer_service", "name_zh", "VARCHAR(200) NULL COMMENT '中文名称'");

        // 为 order_customer_service 表添加日文名称字段
        addColumnIfNotExists("order_customer_service", "name_jp", "VARCHAR(200) NULL COMMENT '日文名称'");

        // 为 order_customer_service 表添加韩文名称字段
        addColumnIfNotExists("order_customer_service", "name_ko", "VARCHAR(200) NULL COMMENT '韩文名称'");

        // 为 order_customer_service 表添加泰文名称字段
        addColumnIfNotExists("order_customer_service", "name_th", "VARCHAR(200) NULL COMMENT '泰文名称'");
        addColumnIfNotExists("order_customer_service", "name_por", "VARCHAR(200) NULL COMMENT '葡萄牙'");

        // 为 order_customer_service 表添加中文繁体名称字段
        addColumnIfNotExists("order_customer_service", "name_zh_tw", "VARCHAR(200) NULL COMMENT '中文繁体名称'");

        addColumnIfNotExists("order_withdrawal", "wallet_id", "bigint NULL COMMENT '钱包或者银行卡id'");

        addColumnIfNotExists("order_site_config", "auto_reset", "CHAR(1) DEFAULT '0' COMMENT '自动重置'");

        addColumnIfNotExists("order_site_config", "reset_order_count", "CHAR(1) DEFAULT '1' COMMENT '重置订单数'");

        addColumnIfNotExists("order_site_config", "min_balance", "CHAR(1) DEFAULT '1' COMMENT '是否开启订单最小金额'");

        addColumnIfNotExists("order_site_config", "vip_auto_shop", "CHAR(1) DEFAULT '1' COMMENT '是否通过订单数自动升级VIP'");

        addColumnIfNotExists("order_site_config", "enable_full_order", "CHAR(1) DEFAULT '0' COMMENT '是否开启满单提示'");

        addColumnIfNotExists("order_series", "type", "CHAR(1) DEFAULT '1' COMMENT '价格类型'");

        String createTableSql = """
        CREATE TABLE `order_bank_wallet` (
          `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
          `user_id` bigint NOT NULL COMMENT '用户ID',
          `type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '银行卡还是钱包',
          `bank_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '银行账户类型',
          `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '姓名',
          `bank_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '银行编码',
          `bank_card` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '银行卡号',
          `wallet_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '钱包类型',
          `wallet_address` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '钱包地址',
          `create_time` datetime(3) NULL DEFAULT NULL COMMENT '创建时间',
          PRIMARY KEY (`id`) USING BTREE
        ) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;
        """;
        createTableIfNotExists("order_bank_wallet", createTableSql);

       if (StringUtils.isNull(sysConfig)){
           SysConfig config =new SysConfig();
           config.setConfigKey("app-version");
           config.setConfigValue("1.3.6");
           configMapper.insertConfig(config);
       }else{
           sysConfig.setConfigValue("1.3.6");
           configMapper.updateConfig(sysConfig);
       }
    }


    public void scheduledUpdate() {
        queryService.updateAllUsersGeoLocation();
    }




    private void createOrderShopTable() {
            String createTableSql = """
        CREATE TABLE `order_shop` (
          `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
          `name` varchar(255) NOT NULL COMMENT '商店名称',
          `icon` varchar(255) NULL COMMENT '商店图标',
          `vip_level` int(11) NOT NULL DEFAULT 0 COMMENT 'VIP 等级',
          `auto_vip` int(11) NOT NULL DEFAULT 0 COMMENT '自动升级最小订单数',
          `max_auto_vip` int(11) NOT NULL DEFAULT 0 COMMENT '自动升级最大订单数',
          `min_money` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '最小交易金额',
          `max_money` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '最大交易金额',
          `commission_percentage` decimal(5, 2) NOT NULL DEFAULT 0.00 COMMENT '佣金百分比',
          `remark` varchar(500) NULL COMMENT '备注',
          `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
          `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
          PRIMARY KEY (`id`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
        """;
            // 创建表，如果不存在
            createTableIfNotExists("order_shop", createTableSql);
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
        columnsToAdd.put("description_por", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '描述 - 葡萄牙'");
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
        columnsToAdd.put("registration_agreement_por", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '注册协议 - 葡萄牙'");

        // 关于我们 (About Us)
        columnsToAdd.put("about_us_ja", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '关于我们 - 日文'");
        columnsToAdd.put("about_us_th", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '关于我们 - 泰文'");
        columnsToAdd.put("about_us_ko", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '关于我们 - 韩文'");
        columnsToAdd.put("about_us_zh_tw", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '关于我们 - 中文繁体'");
        columnsToAdd.put("about_us_por", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '关于我们 - 葡萄牙'");

        // 证书 (Certificate)
        columnsToAdd.put("certificate_ja", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '证书 - 日文'");
        columnsToAdd.put("certificate_th", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '证书 - 泰文'");
        columnsToAdd.put("certificate_ko", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '证书 - 韩文'");
        columnsToAdd.put("certificate_por", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '证书 - 葡萄牙'");
        columnsToAdd.put("certificate_zh_tw", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '证书 - 中文繁体'");

        // 常见问题 (FAQ)
        columnsToAdd.put("faq_ja", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '常见问题 - 日文'");
        columnsToAdd.put("faq_th", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '常见问题 - 泰文'");
        columnsToAdd.put("faq_ko", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '常见问题 - 韩文'");
        columnsToAdd.put("faq_por", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '常见问题 - 葡萄牙'");
        columnsToAdd.put("faq_zh_tw", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '常见问题 - 中文繁体'");

        // 最新事件 (Latest Events)
        columnsToAdd.put("latest_events_ja", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '最新事件 - 日文'");
        columnsToAdd.put("latest_events_th", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '最新事件 - 泰文'");
        columnsToAdd.put("latest_events_ko", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '最新事件 - 韩文'");
        columnsToAdd.put("latest_events_por", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '最新事件 - 葡萄牙'");
        columnsToAdd.put("latest_events_zh_tw", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '最新事件 - 中文繁体'");

        // 条款条规 (Terms and Conditions)
        columnsToAdd.put("terms_conditions_ja", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '条款条规 - 日文'");
        columnsToAdd.put("terms_conditions_th", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '条款条规 - 泰文'");
        columnsToAdd.put("terms_conditions_ko", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '条款条规 - 韩文'");
        columnsToAdd.put("terms_conditions_por", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '条款条规 - 葡萄牙'");
        columnsToAdd.put("terms_conditions_zh_tw", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '条款条规 - 中文繁体'");

        // 收入指南 (Income Guide)
        columnsToAdd.put("income_guide_ja", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '收入指南 - 日文'");
        columnsToAdd.put("income_guide_th", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '收入指南 - 泰文'");
        columnsToAdd.put("income_guide_ko", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '收入指南 - 韩文'");
        columnsToAdd.put("income_guide_por", "LONGTEXT NOT NULL DEFAULT ('') COMMENT '收入指南 - 葡萄牙'");
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

    }


    private void addColumnIfNotExists(String tableName, String columnName, String columnDefinition) {
        // 1) 查询列是否存在
        String checkColumnSql = "SELECT COUNT(*) " +
                "FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";

        Integer cnt = jdbcTemplate.queryForObject(checkColumnSql, Integer.class, tableName, columnName);

        // 2) 不存在则添加
        if (cnt == null || cnt == 0) {
            String alterSql = "ALTER TABLE `" + tableName + "` " +
                    "ADD COLUMN `" + columnName + "` " + columnDefinition;
            try {
                jdbcTemplate.execute(alterSql);
                System.out.println("成功添加列: " + tableName + "." + columnName);
            } catch (DataAccessException e) {
                // 并发下可能已被其它实例先添加，忽略“Duplicate column name”错误
                String msg = e.getMessage();
                if (msg == null || !msg.contains("Duplicate column name")) {
                    throw e;
                }
                System.out.println("列已存在，忽略添加: " + tableName + "." + columnName);
            }
        } else {
            System.out.println("列已存在，跳过: " + tableName + "." + columnName);
        }
    }

    private void createTableIfNotExists(String tableName, String createTableSql) {
        // 1) 查询表是否存在
        String checkTableSql = "SELECT COUNT(*) " +
                "FROM INFORMATION_SCHEMA.TABLES " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?";

        Integer cnt = jdbcTemplate.queryForObject(checkTableSql, Integer.class, tableName);

        // 2) 不存在则创建
        if (cnt == null || cnt == 0) {
            try {
                jdbcTemplate.execute(createTableSql);
                System.out.println("成功创建表: " + tableName);
            } catch (DataAccessException e) {
                // 并发下可能已被其它实例先创建，忽略“Table already exists”错误
                String msg = e.getMessage();
                if (msg == null || !msg.contains("Table") || !msg.contains("already exists")) {
                    throw e;
                }
                System.out.println("表已存在，忽略创建: " + tableName);
            }
        } else {
            addColumnIfNotExists("order_bank_wallet", "bank_type", "VARCHAR(100) NULL COMMENT '代理用户'");
            System.out.println("表已存在，跳过: " + tableName);
        }
    }

    /**
     * 为 sys_notice 表添加国际化标题字段
     */
    public void addSysNoticeColumns() {
        String tableName = "sys_notice";
        java.util.Map<String, String> columnsToAdd = new java.util.HashMap<>();

        columnsToAdd.put("title_zh", "VARCHAR(255) NULL COMMENT '中文标题'");
        columnsToAdd.put("title_en", "VARCHAR(255) NULL COMMENT '英文标题'");
        columnsToAdd.put("title_ja", "VARCHAR(255) NULL COMMENT '日文标题'");
        columnsToAdd.put("title_th", "VARCHAR(255) NULL COMMENT '泰文标题'");
        columnsToAdd.put("title_ko", "VARCHAR(255) NULL COMMENT '韩文标题'");
        columnsToAdd.put("title_por", "VARCHAR(255) NULL COMMENT '葡萄牙文标题'");
        columnsToAdd.put("title_zh_tw", "VARCHAR(255) NULL COMMENT '繁体中文标题'");

        // 内容字段使用 LONGTEXT（支持较长内容）
        columnsToAdd.put("content_zh", "LONGTEXT NULL COMMENT '内容 - 中文'");
        columnsToAdd.put("content_en", "LONGTEXT NULL COMMENT '内容 - 英文'");
        columnsToAdd.put("content_ja", "LONGTEXT NULL COMMENT '内容 - 日文'");
        columnsToAdd.put("content_th", "LONGTEXT NULL COMMENT '内容 - 泰文'");
        columnsToAdd.put("content_ko", "LONGTEXT NULL COMMENT '内容 - 韩文'");
        columnsToAdd.put("content_por", "LONGTEXT NULL COMMENT '内容 - 葡萄牙'");
        columnsToAdd.put("content_zh_tw", "LONGTEXT NULL COMMENT '内容 - 繁体中文'");

        String checkColumnSql =
                "SELECT COUNT(*) " +
                        "FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";

        for (java.util.Map.Entry<String, String> entry : columnsToAdd.entrySet()) {
            String columnName = entry.getKey();
            String columnDefinition = entry.getValue();
            Integer cnt = jdbcTemplate.queryForObject(checkColumnSql, Integer.class, tableName, columnName);
            if (cnt == null || cnt == 0) {
                String alterSql = "ALTER TABLE `" + tableName + "` ADD COLUMN `" + columnName + "` " + columnDefinition;
                try {
                    jdbcTemplate.execute(alterSql);
                    System.out.println("成功添加列: " + tableName + "." + columnName);
                } catch (DataAccessException e) {
                    String msg = e.getMessage();
                    if (msg != null && msg.contains("Duplicate column name")) {
                        System.out.println("列 " + columnName + " 已存在或被其他实例添加，忽略。");
                    } else {
                        throw e;
                    }
                }
            }
        }
    }

}
