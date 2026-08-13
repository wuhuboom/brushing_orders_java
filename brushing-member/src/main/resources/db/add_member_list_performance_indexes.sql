-- Member-list performance indexes (MySQL 8.x)
--
-- This migration is safe to run repeatedly.  Each index is created only when
-- a visible index with the expected name, column order and non-unique BTREE
-- definition does not already exist in the current schema.
--
-- If an index name exists with a different definition or is INVISIBLE, the
-- attempted ADD INDEX deliberately fails with "Duplicate key name".  Do not
-- rename, replace or alter visibility automatically: inspect it first.
--
-- Production note: these three indexes have already been added online in the
-- reported production database.  Running this migration there should therefore
-- emit three "already present" rows and perform no ALTER TABLE.

SET @migration_schema := DATABASE();

-- order_topup(user_id, status)
SET @index_is_exact := (
    SELECT COUNT(*)
    FROM (
        SELECT
            INDEX_NAME,
            MIN(NON_UNIQUE) AS non_unique,
            MIN(INDEX_TYPE) AS index_type,
            MIN(IS_VISIBLE) AS is_visible,
            SUM(CASE WHEN SUB_PART IS NULL THEN 0 ELSE 1 END) AS prefix_column_count,
            GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') AS index_columns
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = @migration_schema
          AND TABLE_NAME = 'order_topup'
          AND INDEX_NAME = 'idx_topup_user_status'
        GROUP BY INDEX_NAME
    ) definition
    WHERE definition.non_unique = 1
      AND definition.index_type = 'BTREE'
      AND definition.is_visible = 'YES'
      AND definition.prefix_column_count = 0
      AND definition.index_columns = 'user_id,status'
);
SET @migration_sql := IF(
    @index_is_exact = 1,
    'SELECT ''idx_topup_user_status already present; skipped'' AS migration_status',
    'ALTER TABLE `order_topup` ADD INDEX `idx_topup_user_status` (`user_id`, `status`), ALGORITHM=INPLACE, LOCK=NONE'
);
PREPARE member_list_index_statement FROM @migration_sql;
EXECUTE member_list_index_statement;
DEALLOCATE PREPARE member_list_index_statement;

-- order_withdrawal(user_id, status)
SET @index_is_exact := (
    SELECT COUNT(*)
    FROM (
        SELECT
            INDEX_NAME,
            MIN(NON_UNIQUE) AS non_unique,
            MIN(INDEX_TYPE) AS index_type,
            MIN(IS_VISIBLE) AS is_visible,
            SUM(CASE WHEN SUB_PART IS NULL THEN 0 ELSE 1 END) AS prefix_column_count,
            GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') AS index_columns
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = @migration_schema
          AND TABLE_NAME = 'order_withdrawal'
          AND INDEX_NAME = 'idx_withdrawal_user_status'
        GROUP BY INDEX_NAME
    ) definition
    WHERE definition.non_unique = 1
      AND definition.index_type = 'BTREE'
      AND definition.is_visible = 'YES'
      AND definition.prefix_column_count = 0
      AND definition.index_columns = 'user_id,status'
);
SET @migration_sql := IF(
    @index_is_exact = 1,
    'SELECT ''idx_withdrawal_user_status already present; skipped'' AS migration_status',
    'ALTER TABLE `order_withdrawal` ADD INDEX `idx_withdrawal_user_status` (`user_id`, `status`), ALGORITHM=INPLACE, LOCK=NONE'
);
PREPARE member_list_index_statement FROM @migration_sql;
EXECUTE member_list_index_statement;
DEALLOCATE PREPARE member_list_index_statement;

-- order_withdrawal(user_id, application_time)
SET @index_is_exact := (
    SELECT COUNT(*)
    FROM (
        SELECT
            INDEX_NAME,
            MIN(NON_UNIQUE) AS non_unique,
            MIN(INDEX_TYPE) AS index_type,
            MIN(IS_VISIBLE) AS is_visible,
            SUM(CASE WHEN SUB_PART IS NULL THEN 0 ELSE 1 END) AS prefix_column_count,
            GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') AS index_columns
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = @migration_schema
          AND TABLE_NAME = 'order_withdrawal'
          AND INDEX_NAME = 'idx_withdrawal_user_application_time'
        GROUP BY INDEX_NAME
    ) definition
    WHERE definition.non_unique = 1
      AND definition.index_type = 'BTREE'
      AND definition.is_visible = 'YES'
      AND definition.prefix_column_count = 0
      AND definition.index_columns = 'user_id,application_time'
);
SET @migration_sql := IF(
    @index_is_exact = 1,
    'SELECT ''idx_withdrawal_user_application_time already present; skipped'' AS migration_status',
    'ALTER TABLE `order_withdrawal` ADD INDEX `idx_withdrawal_user_application_time` (`user_id`, `application_time`), ALGORITHM=INPLACE, LOCK=NONE'
);
PREPARE member_list_index_statement FROM @migration_sql;
EXECUTE member_list_index_statement;
DEALLOCATE PREPARE member_list_index_statement;

-- Post-migration verification.  The result must contain exactly the three
-- expected visible definitions (one row per index, in the listed column order).
SELECT
    TABLE_NAME,
    INDEX_NAME,
    NON_UNIQUE,
    INDEX_TYPE,
    MIN(IS_VISIBLE) AS IS_VISIBLE,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') AS index_columns
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND (
      (TABLE_NAME = 'order_topup'
       AND INDEX_NAME = 'idx_topup_user_status')
      OR
      (TABLE_NAME = 'order_withdrawal'
       AND INDEX_NAME IN (
           'idx_withdrawal_user_status',
           'idx_withdrawal_user_application_time'
       ))
  )
GROUP BY TABLE_NAME, INDEX_NAME, NON_UNIQUE, INDEX_TYPE
ORDER BY TABLE_NAME, INDEX_NAME;

-- Rollback guidance (intentionally not executed by this migration):
--
-- 1. Rolling application code back does not require dropping these indexes.
--    Keeping them is the lowest-risk rollback because they do not change query
--    results or table data.
-- 2. If storage/write overhead requires removal, first verify that each index
--    still has the exact definition shown above and that no other query uses it.
-- 3. During an approved maintenance window, remove only these named indexes:
--
--    ALTER TABLE `order_topup`
--      DROP INDEX `idx_topup_user_status`, ALGORITHM=INPLACE, LOCK=NONE;
--    ALTER TABLE `order_withdrawal`
--      DROP INDEX `idx_withdrawal_user_status`,
--      DROP INDEX `idx_withdrawal_user_application_time`,
--      ALGORITHM=INPLACE, LOCK=NONE;
--
-- Do not run the rollback statements merely because application deployment was
-- rolled back; index removal may itself acquire metadata locks.
