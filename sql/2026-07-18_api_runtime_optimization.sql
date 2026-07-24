-- Runtime query support for /api/account. Repeatable on MySQL 8.
DELIMITER $$

DROP PROCEDURE IF EXISTS migrate_api_runtime_optimization$$
CREATE PROCEDURE migrate_api_runtime_optimization()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'order_withdrawal'
          AND index_name = 'idx_withdrawal_business_status_time'
    ) THEN
        ALTER TABLE order_withdrawal
            ADD KEY idx_withdrawal_business_status_time
                (business_date, status, create_time);
    END IF;
END$$

CALL migrate_api_runtime_optimization()$$
DROP PROCEDURE migrate_api_runtime_optimization$$

DELIMITER ;
