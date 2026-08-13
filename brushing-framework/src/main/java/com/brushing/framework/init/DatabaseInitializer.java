package com.brushing.framework.init;

import com.brushing.common.utils.Arith;
import com.brushing.common.utils.StringUtils;
import com.brushing.system.domain.SysConfig;
import com.brushing.system.mapper.SysConfigMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DatabaseInitializer {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private GeoIpQueryQueryService queryService;

    @Autowired
    private SysConfigMapper configMapper;

    public static String version = "170";

    public void init() {
        scheduledUpdate();
        SysConfig sysConfig = configMapper.checkConfigKeyUnique("app-version");
        if (StringUtils.isNotNull(sysConfig)){
            String configValue = sysConfig.getConfigValue();
            if (StringUtils.isEmpty(configValue)){
                SysConfig config =new SysConfig();
                config.setConfigKey("app-version");
                config.setConfigValue(version);
                configMapper.insertConfig(config);
            }else{
                if (configValue.equals(version)){
                    System.out.println("版本一致");
                    return;
                }
            }
        }
        addColumnIfNotExists("order_topup", "type", "VARCHAR(50) NULL COMMENT '类型'");
        addColumnIfNotExists("order_topup", "pay_method", "VARCHAR(50) NULL COMMENT '充值方式'");
        addColumnIfNotExists("order_topup", "address", "VARCHAR(255) NULL COMMENT '地址'");
        addColumnIfNotExists("order_topup", "status", "CHAR(1) NOT NULL DEFAULT '0' COMMENT '状态 0成功 1待审核 2拒绝'");
        addColumnIfNotExists("order_topup", "audit_time", "DATETIME NULL COMMENT '审核时间'");
        addColumnIfNotExists("order_topup", "auditor", "VARCHAR(50) NULL COMMENT '审核人'");
        addColumnIfNotExists("order_topup", "remark", "VARCHAR(255) NULL COMMENT '备注'");

        addColumnIfNotExists("order_withdrawal", "ip", "VARCHAR(50) NULL COMMENT 'IP'");
        addColumnIfNotExists("order_withdrawal", "ip_address", "VARCHAR(255) NULL COMMENT 'IP地址'");

        addColumnIfNotExists("order_member_user", "task_status", "CHAR(1) NOT NULL DEFAULT '0' COMMENT '任务状态 0开启 1关闭'");

        addColumnIfNotExists("order_info", "balance_after_order", "DECIMAL(20, 2) NULL COMMENT '下单后用户余额'");
        addColumnIfNotExists("order_info", "frozen_balance_after_order", "DECIMAL(20, 2) NULL COMMENT '下单后用户冻结余额'");

        addColumnIfNotExists("order_site_config", "goods_table_type", "CHAR(1) NOT NULL DEFAULT '1' COMMENT '商品表类型 1:order_goods 2:order_goods_hotel'");
        addColumnIfNotExists("order_site_config", "new_user_can_task", "CHAR(1) NOT NULL DEFAULT '0' COMMENT '新用户能否任务 0能 1不能'");
        addColumnIfNotExists("order_site_config", "need_phone", "CHAR(1) NOT NULL DEFAULT '1' COMMENT '是否需要手机号 1不需要 0需要'");
        addColumnIfNotExists("order_site_config", "splash_ad_image", "VARCHAR(500) NULL COMMENT '前端开屏广告图片URL'");

        addColumnIfNotExists("sys_notice", "title_es", "VARCHAR(255) NULL COMMENT '西班牙语标题'");
        addColumnIfNotExists("sys_notice", "content_es", "LONGTEXT NULL COMMENT '西班牙语内容'");
        addColumnIfNotExists("order_global_config", "registration_agreement_es", "LONGTEXT NULL COMMENT '注册协议-西班牙语'");
        addColumnIfNotExists("order_global_config", "about_us_es", "LONGTEXT NULL COMMENT '关于我们-西班牙语'");
        addColumnIfNotExists("order_global_config", "certificate_es", "LONGTEXT NULL COMMENT '证书-西班牙语'");
        addColumnIfNotExists("order_global_config", "faq_es", "LONGTEXT NULL COMMENT '常见问题-西班牙语'");
        addColumnIfNotExists("order_global_config", "latest_events_es", "LONGTEXT NULL COMMENT '最新事件-西班牙语'");
        addColumnIfNotExists("order_global_config", "terms_conditions_es", "LONGTEXT NULL COMMENT '条款条规-西班牙语'");
        addColumnIfNotExists("order_global_config", "income_guide_es", "LONGTEXT NULL COMMENT '收入指南-西班牙语'");

        addColumnIfNotExists("order_member_level", "description_es", "LONGTEXT NULL COMMENT '描述-西班牙语'");

        addColumnIfNotExists("order_customer_service", "name_es", "VARCHAR(255) NULL COMMENT '西班牙语名称'");

        String createOrderFieldSetting = "CREATE TABLE `order_field_setting` (\n"
                + "  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',\n"
                + "  `table_name` VARCHAR(100) NOT NULL COMMENT '表名',\n"
                + "  `field_name` VARCHAR(100) NOT NULL COMMENT '字段名',\n"
                + "  `type` VARCHAR(100) NOT NULL COMMENT '类型（唯一）',\n"
                + "  `status` CHAR(1) NOT NULL DEFAULT '0' COMMENT '状态（char）',\n"
                + "  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',\n"
                + "  PRIMARY KEY (`id`),\n"
                + "  UNIQUE KEY `uk_table_field_type` (`type`)\n"
                + ") ENGINE=InnoDB\n"
                + "  DEFAULT CHARSET=utf8mb4\n"
                + "  COMMENT='字段设置表'";
        createTableIfNotExists("order_field_setting", createOrderFieldSetting);

        String createSysNoticeRead = "CREATE TABLE `sys_notice_read` (\n"
                + "  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',\n"
                + "  `notice_id` BIGINT NOT NULL COMMENT '公告ID',\n"
                + "  `user_id` BIGINT NOT NULL COMMENT '会员用户ID',\n"
                + "  `read_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',\n"
                + "  PRIMARY KEY (`id`),\n"
                + "  UNIQUE KEY `uk_user_notice` (`user_id`, `notice_id`),\n"
                + "  KEY `idx_notice_id` (`notice_id`)\n"
                + ") ENGINE=InnoDB\n"
                + "  DEFAULT CHARSET=utf8mb4\n"
                + "  COMMENT='前端会员通知已读记录表'";
        createTableIfNotExists("sys_notice_read", createSysNoticeRead);

        execSql("ALTER TABLE `order_recharge_address` \n" +
                "MODIFY COLUMN `qr_code` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '二维码' AFTER `name`;",sysConfig.getConfigValue(),140);
        execSql("ALTER TABLE `order_topup` \n" +
                "ADD COLUMN `is_real` char(1) NULL DEFAULT 'N' AFTER `remark`;",sysConfig.getConfigValue(),141);
        execSql("ALTER TABLE `order_topup` \n" +
                "ADD COLUMN `real_money` decimal(20, 2) NULL DEFAULT 0 AFTER `is_real`;",sysConfig.getConfigValue(),142);
        if (StringUtils.isNull(sysConfig)){
            SysConfig config =new SysConfig();
            config.setConfigKey("app-version");
            config.setConfigValue(version);
            configMapper.insertConfig(config);
        }else{
            sysConfig.setConfigValue(version);
            configMapper.updateConfig(sysConfig);
        }
    }

    private void execSql(String s,  String configValue, int currVerson) {
        int anInt = Arith.toInt(configValue, 139);
        if(anInt<currVerson){
            try {
                jdbcTemplate.execute(s);
            }catch (Exception e){
            }
        }
    }

    private void addColumnIfNotExists(String tableName, String columnName, String columnDefinition) {
        String checkColumnSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        Integer cnt = jdbcTemplate.queryForObject(checkColumnSql, Integer.class, tableName, columnName);
        if (cnt == null || cnt == 0) {
            String alterSql = "ALTER TABLE `" + tableName + "` ADD COLUMN `" + columnName + "` " + columnDefinition;
            try {
                jdbcTemplate.execute(alterSql);
                System.out.println("成功添加列: " + tableName + "." + columnName);
            } catch (DataAccessException e) {
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

    public void scheduledUpdate() {
        queryService.updateAllUsersGeoLocation();
    }

    private void createTableIfNotExists(String tableName, String createTableSql) {
        String checkTableSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?";
        Integer cnt = jdbcTemplate.queryForObject(checkTableSql, Integer.class, tableName);
        if (cnt == null || cnt == 0) {
            try {
                jdbcTemplate.execute(createTableSql);
                System.out.println("成功创建表: " + tableName);
            } catch (DataAccessException e) {
                String msg = e.getMessage();
                if (msg == null || !(msg.contains("already exists") || msg.contains("exists"))) {
                    throw e;
                }
                System.out.println("表已存在，忽略创建: " + tableName);
            }
        } else {
            System.out.println("表已存在，跳过: " + tableName);
        }
    }
}
