package com.brushing.framework.init;


import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;



@Service
public class DatabaseInitializer {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void init() {
        // 检查表结构
        checkAndAddTotpFields();
    }

    private void checkAndAddTotpFields() {
        // 查询表中是否存在totp_secret字段
        String checkTotpSecretSql = "SHOW COLUMNS FROM sys_user LIKE 'totp_secret'";
        boolean hasTotpSecret = jdbcTemplate.queryForList(checkTotpSecretSql).size() > 0;

        // 如果不存在 totp_secret 则添加该字段
        if (!hasTotpSecret) {
            String addTotpSecretSql = "ALTER TABLE sys_user ADD COLUMN totp_secret VARCHAR(64)";
            jdbcTemplate.execute(addTotpSecretSql);
            System.out.println("Added column: totp_secret");
        }

        // 查询表中是否存在totp_enabled字段
        String checkTotpEnabledSql = "SHOW COLUMNS FROM sys_user LIKE 'totp_enabled'";
        boolean hasTotpEnabled = jdbcTemplate.queryForList(checkTotpEnabledSql).size() > 0;

        // 如果不存在 totp_enabled 则添加该字段，并设置默认值为 '1'
        if (!hasTotpEnabled) {
            String addTotpEnabledSql = "ALTER TABLE sys_user ADD COLUMN totp_enabled CHAR DEFAULT '1'";
            jdbcTemplate.execute(addTotpEnabledSql);
            System.out.println("Added column: totp_enabled");
        }

        String checkTotpEnabled = "SHOW COLUMNS FROM order_site_config LIKE 'totp_enabled'";
        boolean hasTotpEnabledSite = jdbcTemplate.queryForList(checkTotpEnabled).size() > 0;
        if (!hasTotpEnabledSite) {
            String addTotpEnabledSql = "ALTER TABLE order_site_config ADD COLUMN totp_enabled CHAR DEFAULT '0'";
            jdbcTemplate.execute(addTotpEnabledSql);
        }

        //设置交易密码为非必填
        String setTradPasswrod="ALTER TABLE order_member_user MODIFY COLUMN trade_password VARCHAR(255) NULL";
        jdbcTemplate.execute(setTradPasswrod);
    }
}

