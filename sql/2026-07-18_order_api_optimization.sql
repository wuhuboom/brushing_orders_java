-- /api/order integrity, history snapshots and query indexes.
-- This migration is repeatable. It reports and aborts on ambiguous duplicates
-- instead of deleting or merging financial records automatically.

SELECT user_id, COUNT(*) AS pending_order_count
FROM order_info
WHERE status = '1'
GROUP BY user_id
HAVING COUNT(*) > 1;

SELECT order_number, COUNT(*) AS duplicate_count
FROM order_info
WHERE order_number IS NOT NULL
GROUP BY order_number
HAVING COUNT(*) > 1;

SELECT user_id, order_num, COUNT(*) AS duplicate_claimable_bonus_count
FROM order_bonus_table
WHERE distribution_type = '0'
  AND is_received = '1'
GROUP BY user_id, order_num
HAVING COUNT(*) > 1;

DELIMITER $$

DROP PROCEDURE IF EXISTS migrate_order_api_optimization$$
CREATE PROCEDURE migrate_order_api_optimization()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM order_info
        WHERE status = '1'
        GROUP BY user_id
        HAVING COUNT(*) > 1
        LIMIT 1
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Multiple pending orders exist for one or more users';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM order_info
        WHERE order_number IS NOT NULL
        GROUP BY order_number
        HAVING COUNT(*) > 1
        LIMIT 1
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Duplicate order_info.order_number values must be resolved';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM order_bonus_table
        WHERE distribution_type = '0'
          AND is_received = '1'
        GROUP BY user_id, order_num
        HAVING COUNT(*) > 1
        LIMIT 1
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Duplicate claimable order bonuses must be resolved';
    END IF;

    CREATE TABLE IF NOT EXISTS order_api_request (
        id BIGINT NOT NULL AUTO_INCREMENT,
        user_id BIGINT NOT NULL,
        operation_type VARCHAR(32)
            CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
        request_id VARCHAR(64)
            CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
        result_type VARCHAR(10)
            CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
        result_id BIGINT NOT NULL,
        create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
        PRIMARY KEY (id),
        UNIQUE KEY uk_order_api_request_key (
            user_id, operation_type, request_id
        ),
        KEY idx_order_api_request_result (result_type, result_id)
    ) ENGINE=InnoDB;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'order_info'
          AND column_name = 'product_title'
    ) THEN
        ALTER TABLE order_info
            ADD COLUMN product_title VARCHAR(300) NULL AFTER link_id;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'order_info'
          AND column_name = 'product_image'
    ) THEN
        ALTER TABLE order_info
            ADD COLUMN product_image VARCHAR(255) NULL AFTER product_title;
    END IF;

    UPDATE order_info oi
    LEFT JOIN goods g ON g.id = oi.product_id
    SET oi.product_title = COALESCE(oi.product_title, g.title),
        oi.product_image = COALESCE(oi.product_image, g.image)
    WHERE oi.product_title IS NULL OR oi.product_image IS NULL;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'order_info'
          AND column_name = 'pending_user_id'
    ) THEN
        ALTER TABLE order_info
            ADD COLUMN pending_user_id BIGINT
                GENERATED ALWAYS AS (
                    CASE WHEN status = '1' THEN user_id ELSE NULL END
                ) STORED;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'order_info'
          AND index_name = 'uk_order_info_order_number'
    ) THEN
        ALTER TABLE order_info
            ADD UNIQUE KEY uk_order_info_order_number (order_number);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'order_info'
          AND index_name = 'uk_order_info_pending_user'
    ) THEN
        ALTER TABLE order_info
            ADD UNIQUE KEY uk_order_info_pending_user (pending_user_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'order_bonus_table'
          AND column_name = 'claimable_order_bonus_user_id'
    ) THEN
        ALTER TABLE order_bonus_table
            ADD COLUMN claimable_order_bonus_user_id BIGINT
                GENERATED ALWAYS AS (
                    CASE
                        WHEN distribution_type = '0' AND is_received = '1'
                        THEN user_id
                        ELSE NULL
                    END
                ) STORED;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'order_bonus_table'
          AND column_name = 'claimable_order_bonus_order_num'
    ) THEN
        ALTER TABLE order_bonus_table
            ADD COLUMN claimable_order_bonus_order_num BIGINT
                GENERATED ALWAYS AS (
                    CASE
                        WHEN distribution_type = '0' AND is_received = '1'
                        THEN COALESCE(order_num, -1)
                        ELSE NULL
                    END
                ) STORED;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'order_bonus_table'
          AND index_name = 'uk_order_bonus_claimable_exact'
    ) THEN
        ALTER TABLE order_bonus_table
            ADD UNIQUE KEY uk_order_bonus_claimable_exact (
                claimable_order_bonus_user_id,
                claimable_order_bonus_order_num
            );
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'order_info'
          AND index_name = 'idx_order_info_user_status_created'
    ) THEN
        ALTER TABLE order_info
            ADD KEY idx_order_info_user_status_created (user_id, status, create_time, id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'order_link'
          AND index_name = 'idx_order_link_user_count_status_created'
    ) THEN
        ALTER TABLE order_link
            ADD KEY idx_order_link_user_count_status_created
                (user_id, order_count, status, create_time, id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'order_bonus_table'
          AND index_name = 'idx_order_bonus_match'
    ) THEN
        ALTER TABLE order_bonus_table
            ADD KEY idx_order_bonus_match
                (user_id, is_received, is_distributed, distribution_type, order_num, expiry_time, id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'goods'
          AND index_name = 'idx_goods_enabled_price'
    ) THEN
        ALTER TABLE goods
            ADD KEY idx_goods_enabled_price (is_enabled, price, id);
    END IF;
END$$

CALL migrate_order_api_optimization()$$
DROP PROCEDURE migrate_order_api_optimization$$

DELIMITER ;

-- Staging verification:
-- EXPLAIN ANALYZE
-- SELECT id
-- FROM order_info
-- WHERE user_id = 1 AND status = '1'
-- ORDER BY id DESC
-- LIMIT 1;
--
-- EXPLAIN ANALYZE
-- SELECT id, price
-- FROM goods
-- WHERE is_enabled = '0' AND price <= 100
-- ORDER BY price DESC, id ASC
-- LIMIT 1;
