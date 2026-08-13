-- Align the recharge/withdrawal audit list fields with the legacy administration UI.
-- This migration is idempotent and safe to run more than once.

SET @schema_name = DATABASE();

SET @ddl = IF(
    EXISTS(
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = @schema_name
          AND table_name = 'goods_recharge_record'
          AND column_name = 'recharge_account'
    ),
    'SELECT 1',
    'ALTER TABLE goods_recharge_record ADD COLUMN recharge_account varchar(255) NULL AFTER is_hidden'
);
PREPARE migration_stmt FROM @ddl;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @ddl = IF(
    EXISTS(
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = @schema_name
          AND table_name = 'goods_recharge_record'
          AND column_name = 'update_by'
    ),
    'SELECT 1',
    'ALTER TABLE goods_recharge_record ADD COLUMN update_by varchar(64) NOT NULL DEFAULT ''system'' AFTER recharge_account'
);
PREPARE migration_stmt FROM @ddl;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @ddl = IF(
    EXISTS(
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = @schema_name
          AND table_name = 'goods_recharge_record'
          AND column_name = 'update_time'
    ),
    'SELECT 1',
    'ALTER TABLE goods_recharge_record ADD COLUMN update_time datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) AFTER update_by'
);
PREPARE migration_stmt FROM @ddl;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @ddl = IF(
    EXISTS(
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = @schema_name
          AND table_name = 'order_withdrawal'
          AND column_name = 'update_by'
    ),
    'SELECT 1',
    'ALTER TABLE order_withdrawal ADD COLUMN update_by varchar(64) NOT NULL DEFAULT ''system'' AFTER business_date'
);
PREPARE migration_stmt FROM @ddl;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @ddl = IF(
    EXISTS(
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = @schema_name
          AND table_name = 'order_withdrawal'
          AND column_name = 'update_time'
    ),
    'SELECT 1',
    'ALTER TABLE order_withdrawal ADD COLUMN update_time datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) AFTER update_by'
);
PREPARE migration_stmt FROM @ddl;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

UPDATE goods_recharge_record
SET update_by = COALESCE(NULLIF(update_by, ''), 'system'),
    update_time = COALESCE(create_time, update_time);

UPDATE order_withdrawal
SET update_by = COALESCE(NULLIF(update_by, ''), 'system'),
    update_time = COALESCE(create_time, update_time);
