-- Align lucky-bonus values with the original system:
-- 1 = distribute immediately, 2 = distribute after completing the task group.

ALTER TABLE order_bonus_table
    MODIFY COLUMN expiry_time DATETIME NULL COMMENT '过期时间';

DELIMITER $$
CREATE PROCEDURE migrate_bonus_claimable_key()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'order_bonus_table'
          AND column_name = 'claimable_order_bonus_user_id'
          AND generation_expression LIKE '%distribution_type%0%'
    ) OR EXISTS (
        SELECT 1
        FROM order_bonus_table
        WHERE distribution_type = '0'
    ) THEN
        UPDATE order_bonus_table
        SET distribution_type = CASE distribution_type
            WHEN '0' THEN '1'
            WHEN '1' THEN '2'
            ELSE distribution_type
        END
        WHERE distribution_type IN ('0', '1');
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'order_bonus_table'
          AND index_name = 'uk_order_bonus_claimable_exact'
    ) THEN
        ALTER TABLE order_bonus_table
            DROP INDEX uk_order_bonus_claimable_exact;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'order_bonus_table'
          AND column_name = 'claimable_order_bonus_user_id'
    ) THEN
        ALTER TABLE order_bonus_table
            DROP COLUMN claimable_order_bonus_user_id;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'order_bonus_table'
          AND column_name = 'claimable_order_bonus_order_num'
    ) THEN
        ALTER TABLE order_bonus_table
            DROP COLUMN claimable_order_bonus_order_num;
    END IF;

    ALTER TABLE order_bonus_table
        ADD COLUMN claimable_order_bonus_user_id BIGINT
            GENERATED ALWAYS AS (
                CASE
                    WHEN distribution_type IN ('1', '2') AND is_received = '1'
                    THEN user_id
                    ELSE NULL
                END
            ) STORED,
        ADD COLUMN claimable_order_bonus_order_num BIGINT
            GENERATED ALWAYS AS (
                CASE
                    WHEN distribution_type IN ('1', '2') AND is_received = '1'
                    THEN COALESCE(order_num, -1)
                    ELSE NULL
                END
            ) STORED,
        ADD UNIQUE KEY uk_order_bonus_claimable_exact (
            claimable_order_bonus_user_id,
            claimable_order_bonus_order_num
        );
END$$

CALL migrate_bonus_claimable_key()$$
DROP PROCEDURE migrate_bonus_claimable_key$$
DELIMITER ;
