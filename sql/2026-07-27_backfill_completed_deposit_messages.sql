-- Backfill the deposit notification for completed recharge records.
-- This migration is idempotent by member, recharge time, and rendered title.

SET @deposit_title = (
    SELECT JSON_UNQUOTE(JSON_EXTRACT(`content`, '$.deposit.title'))
    FROM `order_config`
    WHERE `type` = 'notification'
    LIMIT 1
);
SET @deposit_content = (
    SELECT JSON_UNQUOTE(JSON_EXTRACT(`content`, '$.deposit.content'))
    FROM `order_config`
    WHERE `type` = 'notification'
    LIMIT 1
);

UPDATE `order_site_message` message
JOIN `goods_recharge_record` recharge
  ON message.`create_time` = recharge.`create_time`
JOIN `order_user` member
  ON member.`id` = recharge.`user_id`
SET message.`content` = REPLACE(
    REPLACE(
        REPLACE(
            REPLACE(
                REPLACE(
                    REPLACE(
                        @deposit_content,
                        '${username}', COALESCE(member.`username`, '')
                    ),
                    '{username}', COALESCE(member.`username`, '')
                ),
                '${phone}', COALESCE(member.`phone_number`, '')
            ),
            '{phone}', COALESCE(member.`phone_number`, '')
        ),
        '${amount}', CAST(recharge.`amount` AS CHAR)
    ),
    '{amount}', CAST(recharge.`amount` AS CHAR)
)
WHERE recharge.`status` = '2'
  AND recharge.`amount` > 0
  AND FIND_IN_SET(
      CAST(recharge.`user_id` AS CHAR),
      REPLACE(message.`member_list`, ' ', '')
  ) > 0
  AND message.`title` = REPLACE(
      REPLACE(
          REPLACE(
              REPLACE(
                  @deposit_title,
                  '${username}', COALESCE(member.`username`, '')
              ),
              '{username}', COALESCE(member.`username`, '')
          ),
          '${phone}', COALESCE(member.`phone_number`, '')
      ),
      '{phone}', COALESCE(member.`phone_number`, '')
  );

INSERT INTO `order_site_message` (
    `title`,
    `member_list`,
    `is_enabled`,
    `create_time`,
    `content`
)
SELECT
    REPLACE(
        REPLACE(
            REPLACE(
                REPLACE(
                    @deposit_title,
                    '${username}', COALESCE(member.`username`, '')
                ),
                '{username}', COALESCE(member.`username`, '')
            ),
            '${phone}', COALESCE(member.`phone_number`, '')
        ),
        '{phone}', COALESCE(member.`phone_number`, '')
    ),
    CAST(recharge.`user_id` AS CHAR),
    1,
    recharge.`create_time`,
    REPLACE(
        REPLACE(
            REPLACE(
                REPLACE(
                    REPLACE(
                        REPLACE(
                            @deposit_content,
                            '${username}', COALESCE(member.`username`, '')
                        ),
                        '{username}', COALESCE(member.`username`, '')
                    ),
                    '${phone}', COALESCE(member.`phone_number`, '')
                ),
                '{phone}', COALESCE(member.`phone_number`, '')
            ),
            '${amount}', CAST(recharge.`amount` AS CHAR)
        ),
        '{amount}', CAST(recharge.`amount` AS CHAR)
    )
FROM `goods_recharge_record` recharge
JOIN `order_user` member
  ON member.`id` = recharge.`user_id`
WHERE recharge.`status` = '2'
  AND recharge.`amount` > 0
  AND @deposit_title IS NOT NULL
  AND @deposit_title <> ''
  AND @deposit_content IS NOT NULL
  AND @deposit_content <> ''
  AND NOT EXISTS (
      SELECT 1
      FROM `order_site_message` message
      WHERE message.`create_time` = recharge.`create_time`
        AND FIND_IN_SET(
            CAST(recharge.`user_id` AS CHAR),
            REPLACE(message.`member_list`, ' ', '')
        ) > 0
        AND message.`title` = REPLACE(
            REPLACE(
                REPLACE(
                    REPLACE(
                        @deposit_title,
                        '${username}', COALESCE(member.`username`, '')
                    ),
                    '{username}', COALESCE(member.`username`, '')
                ),
                '${phone}', COALESCE(member.`phone_number`, '')
            ),
            '{phone}', COALESCE(member.`phone_number`, '')
        )
  );
