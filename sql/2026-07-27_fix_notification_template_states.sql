-- Repair legacy notification settings that were marked enabled without content.
-- The reference system uses the "deposit" template for approved H5 deposits.

UPDATE `order_config`
SET `content` = '{}'
WHERE `type` = 'notification'
  AND (`content` IS NULL OR `content` = '' OR JSON_VALID(`content`) = 0);

DROP PROCEDURE IF EXISTS repair_notification_template_states;
DELIMITER $$
CREATE PROCEDURE repair_notification_template_states()
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
        SET `content` = JSON_SET(
            `content`,
            CONCAT('$.', template_key, '.enabled'),
            0
        )
        WHERE `type` = 'notification'
          AND JSON_CONTAINS_PATH(
              `content`, 'one', CONCAT('$.', template_key)
          ) = 1
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

CALL repair_notification_template_states();
DROP PROCEDURE repair_notification_template_states;

UPDATE `order_config`
SET `content` = IF(
    JSON_CONTAINS_PATH(`content`, 'one', '$.deposit') = 1,
    `content`,
    JSON_SET(`content`, '$.deposit', JSON_OBJECT())
)
WHERE `type` = 'notification';

UPDATE `order_config`
SET `content` = JSON_SET(
    `content`,
    '$.deposit.enabled',
    1,
    '$.deposit.formatAmount',
    COALESCE(
        CAST(JSON_UNQUOTE(JSON_EXTRACT(
            `content`, '$.deposit.formatAmount'
        )) AS UNSIGNED),
        0
    ),
    '$.deposit.title',
    COALESCE(
        NULLIF(JSON_UNQUOTE(JSON_EXTRACT(
            `content`, '$.deposit.title'
        )), ''),
        '📣Balance Change Notification'
    ),
    '$.deposit.content',
    COALESCE(
        NULLIF(JSON_UNQUOTE(JSON_EXTRACT(
            `content`, '$.deposit.content'
        )), ''),
        '<p><strong>Dear {username}, 👋</strong></p><p>Your account balance has changed.</p><p><strong>Deposit Amount:</strong> ${amount}</p><p>If you have any questions or need assistance, please feel free to contact our support team.</p>'
    )
)
WHERE `type` = 'notification';
