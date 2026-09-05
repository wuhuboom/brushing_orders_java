CREATE TABLE IF NOT EXISTS sys_user_table_column_config (
    config_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    user_id BIGINT NOT NULL COMMENT '后台系统用户ID',
    table_key VARCHAR(128) NOT NULL COMMENT '前端表格稳定标识',
    config_content TEXT NOT NULL COMMENT '列显隐、顺序及固定状态JSON',
    create_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (config_id),
    UNIQUE KEY uk_sys_user_table_column_config_user_table (user_id, table_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台用户表格列配置';
