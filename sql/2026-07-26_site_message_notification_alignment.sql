-- Align member site messages and notification templates with the reference system.

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'order_site_message'
      AND column_name = 'translations_id'
);
SET @column_sql = IF(
    @column_exists = 0,
    'ALTER TABLE `order_site_message` ADD COLUMN `translations_id` bigint NULL COMMENT ''多语言翻译ID'' AFTER `content`',
    'SELECT 1'
);
PREPARE column_statement FROM @column_sql;
EXECUTE column_statement;
DEALLOCATE PREPARE column_statement;

SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'order_site_message'
      AND index_name = 'idx_order_site_message_translations_id'
);
SET @index_sql = IF(
    @index_exists = 0,
    'ALTER TABLE `order_site_message` ADD INDEX `idx_order_site_message_translations_id` (`translations_id`)',
    'SELECT 1'
);
PREPARE index_statement FROM @index_sql;
EXECUTE index_statement;
DEALLOCATE PREPARE index_statement;

SET @fk_exists = (
    SELECT COUNT(*)
    FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'order_site_message'
      AND constraint_name = 'fk_order_site_message_translations'
);
SET @fk_sql = IF(
    @fk_exists = 0,
    'ALTER TABLE `order_site_message` ADD CONSTRAINT `fk_order_site_message_translations` FOREIGN KEY (`translations_id`) REFERENCES `translations` (`id`) ON DELETE SET NULL',
    'SELECT 1'
);
PREPARE fk_statement FROM @fk_sql;
EXECUTE fk_statement;
DEALLOCATE PREPARE fk_statement;

UPDATE `order_config`
SET `content` = '{}'
WHERE `type` = 'notification'
  AND (`content` IS NULL OR `content` = '' OR JSON_VALID(`content`) = 0);

DROP PROCEDURE IF EXISTS align_notification_templates;
DELIMITER $$
CREATE PROCEDURE align_notification_templates()
BEGIN
    DECLARE index_value INT DEFAULT 0;
    DECLARE key_count INT DEFAULT 0;
    DECLARE template_key VARCHAR(64);
    DECLARE template_keys JSON;

    SET template_keys = JSON_ARRAY(
        'gift', 'deduction', 'recharge', 'withdrawing', 'withdrawalUnfreeze',
        'withdrawal', 'task', 'principalReturn', 'rebate', 'subRebate',
        'signIn', 'fee', 'deposit', 'bonus', 'baseSalary', 'aid',
        'registerBonus', 'productShare', 'taskReward', 'balanceOut',
        'balanceIn', 'workBonus', 'upgradeBonus', 'subsidy',
        'abnormalDeposit', 'activity', 'pointsExchange', 'loginSignIn',
        'taskSignIn', 'creditScore', 'other'
    );
    SET key_count = JSON_LENGTH(template_keys);

    WHILE index_value < key_count DO
        SET template_key = JSON_UNQUOTE(
            JSON_EXTRACT(template_keys, CONCAT('$[', index_value, ']'))
        );
        UPDATE `order_config`
        SET `content` = IF(
            JSON_CONTAINS_PATH(`content`, 'one', CONCAT('$.', template_key)) = 1,
            `content`,
            JSON_SET(
                `content`,
                CONCAT('$.', template_key),
                JSON_OBJECT(
                    'enabled', 0,
                    'formatAmount', 0,
                    'title', '',
                    'content', ''
                )
            )
        )
        WHERE `type` = 'notification';

        UPDATE `order_config`
        SET `content` = IF(
            JSON_CONTAINS_PATH(
                `content`, 'one', CONCAT('$.', template_key, '.enabled')
            ) = 1,
            `content`,
            JSON_SET(`content`, CONCAT('$.', template_key, '.enabled'), 0)
        )
        WHERE `type` = 'notification';

        UPDATE `order_config`
        SET `content` = IF(
            JSON_CONTAINS_PATH(
                `content`, 'one', CONCAT('$.', template_key, '.formatAmount')
            ) = 1,
            `content`,
            JSON_SET(`content`, CONCAT('$.', template_key, '.formatAmount'), 0)
        )
        WHERE `type` = 'notification';

        UPDATE `order_config`
        SET `content` = IF(
            JSON_CONTAINS_PATH(
                `content`, 'one', CONCAT('$.', template_key, '.title')
            ) = 1,
            `content`,
            JSON_SET(`content`, CONCAT('$.', template_key, '.title'), '')
        )
        WHERE `type` = 'notification';

        UPDATE `order_config`
        SET `content` = IF(
            JSON_CONTAINS_PATH(
                `content`, 'one', CONCAT('$.', template_key, '.content')
            ) = 1,
            `content`,
            JSON_SET(`content`, CONCAT('$.', template_key, '.content'), '')
        )
        WHERE `type` = 'notification';

        UPDATE `order_config`
        SET `content` = JSON_SET(
            `content`,
            CONCAT('$.', template_key, '.enabled'),
                CAST(COALESCE(
                    NULLIF(REPLACE(
                        JSON_UNQUOTE(JSON_UNQUOTE(JSON_UNQUOTE(JSON_EXTRACT(
                            `content`, CONCAT('$.', template_key, '.enabled')
                        )))), '"', ''),
                        ''
                    ),
                    '0'
                ) AS UNSIGNED),
            CONCAT('$.', template_key, '.formatAmount'),
                CAST(COALESCE(
                    NULLIF(REPLACE(
                        JSON_UNQUOTE(JSON_UNQUOTE(JSON_UNQUOTE(JSON_EXTRACT(
                            `content`, CONCAT('$.', template_key, '.formatAmount')
                        )))), '"', ''),
                        ''
                    ),
                    '0'
                ) AS UNSIGNED),
            CONCAT('$.', template_key, '.title'),
                JSON_UNQUOTE(JSON_UNQUOTE(JSON_UNQUOTE(JSON_EXTRACT(
                    `content`, CONCAT('$.', template_key, '.title')
                )))),
            CONCAT('$.', template_key, '.content'),
                JSON_UNQUOTE(JSON_UNQUOTE(JSON_UNQUOTE(JSON_EXTRACT(
                    `content`, CONCAT('$.', template_key, '.content')
                ))))
        )
        WHERE `type` = 'notification';

        UPDATE `order_config`
        SET `content` = JSON_SET(
            `content`,
            CONCAT('$.', template_key, '.enabled'),
            0
        )
        WHERE `type` = 'notification'
          AND (
              COALESCE(
                  NULLIF(TRIM(JSON_UNQUOTE(JSON_EXTRACT(
                      `content`, CONCAT('$.', template_key, '.title')
                  ))), ''),
                  ''
              ) = ''
              OR COALESCE(
                  NULLIF(TRIM(JSON_UNQUOTE(JSON_EXTRACT(
                      `content`, CONCAT('$.', template_key, '.content')
                  ))), ''),
                  ''
              ) = ''
          );
        SET index_value = index_value + 1;
    END WHILE;
END$$
DELIMITER ;

CALL align_notification_templates();
DROP PROCEDURE align_notification_templates;
