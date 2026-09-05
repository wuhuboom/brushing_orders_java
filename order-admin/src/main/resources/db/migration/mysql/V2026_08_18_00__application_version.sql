CREATE TABLE IF NOT EXISTS sys_application_version (
    id TINYINT UNSIGNED NOT NULL COMMENT '固定为1',
    application_version VARCHAR(64) NOT NULL COMMENT '应用发布版本',
    schema_version VARCHAR(64) NULL COMMENT '最新Flyway结构版本',
    last_migrated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后成功迁移时间',
    PRIMARY KEY (id),
    CONSTRAINT chk_sys_application_version_singleton CHECK (id = 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用与数据库版本';
