CREATE TABLE IF NOT EXISTS translations (
    id bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
    zh_CN text COMMENT '中文(简体)',
    zh_TW text COMMENT '中文(繁体)',
    ko_KR text COMMENT '韩文',
    th_TH text COMMENT '泰文',
    ja_JP text COMMENT '日文',
    pt_PT text COMMENT '葡萄牙语',
    en_US text COMMENT '英文',
    ar_SA text COMMENT '阿拉伯语',
    es_ES text COMMENT '西班牙语',
    sv_SE text COMMENT '瑞典语',
    it_IT text COMMENT '意大利语',
    de_DE text COMMENT '德语',
    no_NO text COMMENT '挪威语',
    ru_RU text COMMENT '俄语',
    hu_HU text COMMENT '匈牙利语',
    pl_PL text COMMENT '波兰语',
    sk_SK text COMMENT '斯洛伐克语',
    fr_FR text COMMENT '法语',
    cs_CZ text COMMENT '捷克语',
    pt_BR text COMMENT '巴西葡萄牙语',
    hi_IN text COMMENT '印地语',
    PRIMARY KEY (id)
) COMMENT='多语言翻译';

DROP PROCEDURE IF EXISTS add_column_if_missing;

DELIMITER //
CREATE PROCEDURE add_column_if_missing(
    IN table_name_value varchar(64),
    IN column_name_value varchar(64),
    IN column_definition text
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = table_name_value
          AND COLUMN_NAME = column_name_value
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', table_name_value, '` ADD COLUMN ', column_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

CALL add_column_if_missing('translations', 'ar_SA', '`ar_SA` text COMMENT ''阿拉伯语''');
CALL add_column_if_missing('translations', 'es_ES', '`es_ES` text COMMENT ''西班牙语''');
CALL add_column_if_missing('translations', 'sv_SE', '`sv_SE` text COMMENT ''瑞典语''');
CALL add_column_if_missing('translations', 'it_IT', '`it_IT` text COMMENT ''意大利语''');
CALL add_column_if_missing('translations', 'de_DE', '`de_DE` text COMMENT ''德语''');
CALL add_column_if_missing('translations', 'no_NO', '`no_NO` text COMMENT ''挪威语''');
CALL add_column_if_missing('translations', 'ru_RU', '`ru_RU` text COMMENT ''俄语''');
CALL add_column_if_missing('translations', 'hu_HU', '`hu_HU` text COMMENT ''匈牙利语''');
CALL add_column_if_missing('translations', 'pl_PL', '`pl_PL` text COMMENT ''波兰语''');
CALL add_column_if_missing('translations', 'sk_SK', '`sk_SK` text COMMENT ''斯洛伐克语''');
CALL add_column_if_missing('translations', 'fr_FR', '`fr_FR` text COMMENT ''法语''');
CALL add_column_if_missing('translations', 'cs_CZ', '`cs_CZ` text COMMENT ''捷克语''');
CALL add_column_if_missing('translations', 'pt_BR', '`pt_BR` text COMMENT ''巴西葡萄牙语''');
CALL add_column_if_missing('translations', 'hi_IN', '`hi_IN` text COMMENT ''印地语''');
CALL add_column_if_missing('order_config', 'translations_id', '`translations_id` bigint NULL COMMENT ''多语言翻译ID''');
CALL add_column_if_missing('goods_member_level', 'translations_id', '`translations_id` bigint NULL COMMENT ''多语言翻译ID''');
CALL add_column_if_missing('goods_customer_service', 'translations_id', '`translations_id` bigint NULL COMMENT ''多语言翻译ID''');
CALL add_column_if_missing('sys_notice', 'translations_id', '`translations_id` bigint NULL COMMENT ''多语言翻译ID''');

DROP PROCEDURE IF EXISTS add_column_if_missing;

DROP PROCEDURE IF EXISTS add_index_if_missing;

DELIMITER //
CREATE PROCEDURE add_index_if_missing(
    IN table_name_value varchar(64),
    IN index_name_value varchar(64),
    IN index_definition text
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = table_name_value
          AND INDEX_NAME = index_name_value
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', table_name_value, '` ADD ', index_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

CALL add_index_if_missing('order_config', 'idx_order_config_translations_id', 'INDEX `idx_order_config_translations_id` (`translations_id`)');
CALL add_index_if_missing('goods_member_level', 'idx_member_level_translations_id', 'INDEX `idx_member_level_translations_id` (`translations_id`)');
CALL add_index_if_missing('goods_customer_service', 'idx_customer_service_translations_id', 'INDEX `idx_customer_service_translations_id` (`translations_id`)');
CALL add_index_if_missing('sys_notice', 'idx_sys_notice_translations_id', 'INDEX `idx_sys_notice_translations_id` (`translations_id`)');

DROP PROCEDURE IF EXISTS add_index_if_missing;

DROP PROCEDURE IF EXISTS add_foreign_key_if_missing;

DELIMITER //
CREATE PROCEDURE add_foreign_key_if_missing(
    IN table_name_value varchar(64),
    IN constraint_name_value varchar(64),
    IN constraint_definition text
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.REFERENTIAL_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = table_name_value
          AND CONSTRAINT_NAME = constraint_name_value
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', table_name_value, '` ADD ', constraint_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

CALL add_foreign_key_if_missing('order_config', 'fk_order_config_translations', 'CONSTRAINT `fk_order_config_translations` FOREIGN KEY (`translations_id`) REFERENCES `translations` (`id`) ON DELETE SET NULL');
CALL add_foreign_key_if_missing('goods_member_level', 'fk_member_level_translations', 'CONSTRAINT `fk_member_level_translations` FOREIGN KEY (`translations_id`) REFERENCES `translations` (`id`) ON DELETE SET NULL');
CALL add_foreign_key_if_missing('goods_customer_service', 'fk_customer_service_translations', 'CONSTRAINT `fk_customer_service_translations` FOREIGN KEY (`translations_id`) REFERENCES `translations` (`id`) ON DELETE SET NULL');
CALL add_foreign_key_if_missing('sys_notice', 'fk_sys_notice_translations', 'CONSTRAINT `fk_sys_notice_translations` FOREIGN KEY (`translations_id`) REFERENCES `translations` (`id`) ON DELETE SET NULL');

DROP PROCEDURE IF EXISTS add_foreign_key_if_missing;
