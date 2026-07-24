-- /api/user consistency and profile-query indexes.
-- This migration deliberately aborts when duplicate business identifiers are
-- present. Resolve those records explicitly; do not auto-delete or auto-merge.

-- Preflight result sets are intentionally emitted before the migration starts,
-- so an aborted run still leaves an actionable duplicate-data report.
SELECT 'username' AS duplicate_field, username AS duplicate_value, COUNT(*) AS duplicate_count
FROM order_user
WHERE username IS NOT NULL
GROUP BY username
HAVING COUNT(*) > 1;

SELECT 'phone_number' AS duplicate_field, phone_number AS duplicate_value, COUNT(*) AS duplicate_count
FROM order_user
WHERE phone_number IS NOT NULL
GROUP BY phone_number
HAVING COUNT(*) > 1;

SELECT 'invite_code' AS duplicate_field, invite_code AS duplicate_value, COUNT(*) AS duplicate_count
FROM order_user
WHERE invite_code IS NOT NULL
GROUP BY invite_code
HAVING COUNT(*) > 1;

DELIMITER $$

DROP PROCEDURE IF EXISTS migrate_user_api_optimization$$
CREATE PROCEDURE migrate_user_api_optimization()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM order_user
        WHERE username IS NOT NULL
        GROUP BY username
        HAVING COUNT(*) > 1
        LIMIT 1
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Duplicate order_user.username values must be resolved';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM order_user
        WHERE phone_number IS NOT NULL
        GROUP BY phone_number
        HAVING COUNT(*) > 1
        LIMIT 1
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Duplicate order_user.phone_number values must be resolved';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM order_user
        WHERE invite_code IS NOT NULL
        GROUP BY invite_code
        HAVING COUNT(*) > 1
        LIMIT 1
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Duplicate order_user.invite_code values must be resolved';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'order_user'
          AND index_name = 'uk_order_user_username'
    ) THEN
        ALTER TABLE order_user
            ADD UNIQUE KEY uk_order_user_username (username);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'order_user'
          AND index_name = 'uk_order_user_phone_number'
    ) THEN
        ALTER TABLE order_user
            ADD UNIQUE KEY uk_order_user_phone_number (phone_number);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'order_user'
          AND index_name = 'uk_order_user_invite_code'
    ) THEN
        ALTER TABLE order_user
            ADD UNIQUE KEY uk_order_user_invite_code (invite_code);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'goods_transaction_flow'
          AND index_name = 'idx_gtf_user_type_created'
    ) THEN
        ALTER TABLE goods_transaction_flow
            ADD KEY idx_gtf_user_type_created (user_id, transaction_type, created_time);
    END IF;
END$$

CALL migrate_user_api_optimization()$$
DROP PROCEDURE migrate_user_api_optimization$$

DELIMITER ;

-- Staging verification:
-- EXPLAIN ANALYZE SELECT id FROM order_user WHERE username = 'sample_user';
-- EXPLAIN ANALYZE
-- SELECT SUM(transaction_amount)
-- FROM goods_transaction_flow
-- WHERE user_id = 1
--   AND transaction_type = 'fy'
--   AND created_time >= CURDATE()
--   AND created_time < CURDATE() + INTERVAL 1 DAY;
