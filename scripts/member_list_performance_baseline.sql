-- Read-only performance reproduction for GET /member/member/list.
-- Target: MySQL 8.0.18+ (EXPLAIN ANALYZE and JSON_TABLE are required).
--
-- This script executes SELECT, SHOW and EXPLAIN ANALYZE only.  EXPLAIN ANALYZE
-- runs the underlying SELECT, so do not run the legacy count section against a
-- busy production database: it intentionally reproduces the expensive query.
--
-- Baseline request represented below:
--   pageNum=1, pageSize=10, admin/unrestricted scope, no filters.

SELECT VERSION() AS mysql_version, DATABASE() AS current_schema;

SELECT 'order_member_user' AS table_name, COUNT(*) AS exact_rows
FROM order_member_user
UNION ALL
SELECT 'order_topup', COUNT(*) FROM order_topup
UNION ALL
SELECT 'order_withdrawal', COUNT(*) FROM order_withdrawal
UNION ALL
SELECT 'order_member_level', COUNT(*) FROM order_member_level;

SELECT
    TABLE_NAME,
    INDEX_NAME,
    NON_UNIQUE,
    INDEX_TYPE,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') AS index_columns
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('order_member_user', 'order_topup', 'order_withdrawal')
GROUP BY TABLE_NAME, INDEX_NAME, NON_UNIQUE, INDEX_TYPE
ORDER BY TABLE_NAME, INDEX_NAME;

-- ---------------------------------------------------------------------------
-- Legacy PageHelper count shape.
-- This is the current list projection wrapped in SELECT COUNT(0) FROM (...).
-- ---------------------------------------------------------------------------
EXPLAIN ANALYZE
SELECT COUNT(0)
FROM (
    SELECT
        u.id,
        u.username,
        u.phone,
        u.password,
        u.trade_password,
        u.parent_id,
        u.avatar,
        u.ancestors,
        u.sex,
        u.email,
        u.credit_score,
        u.balance,
        u.frozen_balance,
        IFNULL((
            SELECT SUM(pending_withdrawal.amount)
            FROM order_withdrawal pending_withdrawal
            WHERE pending_withdrawal.user_id = u.id
              AND pending_withdrawal.status = '1'
        ), 0) AS withdraw_frozen_amount,
        (u.balance + u.frozen_balance) AS total_balance,
        u.invite_code,
        u.register_ip,
        u.last_login_time,
        u.account_status,
        u.trade_status,
        u.withdraw_status,
        u.real_name_status,
        u.real_name,
        u.id_card_number,
        u.id_card_front,
        u.id_card_back,
        u.withdraw_name,
        u.withdraw_address,
        u.withdraw_type,
        u.create_time,
        u.remark,
        u.is_real,
        u.level_id,
        u.commission,
        u.all_commission,
        u.deal_count,
        u.card_number,
        u.card_amount,
        u.task_status,
        u.version,
        (
            SELECT COUNT(1)
            FROM order_withdrawal w
            WHERE w.user_id = u.id
              AND w.application_time >= CURDATE()
              AND w.application_time < CURDATE() + INTERVAL 1 DAY
        ) AS today_withdraw_count,
        u.total_withdraw_count,
        u.today_reset_count,
        u.total_reset_count,
        u.withdraw_tip,
        parent_user.username AS parent_username,
        parent_user.phone AS parent_phone,
        (SELECT COUNT(1)
         FROM order_member_user direct_user
         WHERE direct_user.parent_id = u.id) AS direct_sub_count,
        (SELECT COUNT(1)
         FROM order_member_user descendant
         WHERE FIND_IN_SET(u.id, descendant.ancestors)) AS all_sub_count,
        level_info.id AS levelId,
        level_info.icon,
        level_info.name_zh,
        level_info.name_en,
        level_info.price,
        level_info.auto_upgrade_invite_count,
        level_info.commission_ratio,
        level_info.streak_commission_ratio,
        level_info.min_balance,
        level_info.order_count,
        level_info.withdraw_count,
        level_info.withdraw_limit,
        level_info.min_withdraw_amount,
        level_info.max_withdraw_amount,
        level_info.withdraw_fee,
        level_info.withdraw_order_per_day,
        level_info.description_zh,
        level_info.description_en,
        level_info.create_time AS levelTime,
        IFNULL((
            SELECT SUM(CASE
                WHEN topup.type = '0' THEN IFNULL(topup.amout, 0)
                ELSE IFNULL(topup.real_money, 0)
            END)
            FROM order_topup topup
            WHERE topup.user_id = u.id
              AND topup.status = '0'
        ), 0) AS total_recharge,
        IFNULL((
            SELECT SUM(successful_withdrawal.credited_amount)
            FROM order_withdrawal successful_withdrawal
            WHERE successful_withdrawal.user_id = u.id
              AND successful_withdrawal.status = '0'
        ), 0) AS total_withdraw,
        (
            IFNULL((
                SELECT SUM(CASE
                    WHEN duplicate_topup.type = '0' THEN IFNULL(duplicate_topup.amout, 0)
                    ELSE IFNULL(duplicate_topup.real_money, 0)
                END)
                FROM order_topup duplicate_topup
                WHERE duplicate_topup.user_id = u.id
                  AND duplicate_topup.status = '0'
            ), 0)
            -
            IFNULL((
                SELECT SUM(duplicate_withdrawal.credited_amount)
                FROM order_withdrawal duplicate_withdrawal
                WHERE duplicate_withdrawal.user_id = u.id
                  AND duplicate_withdrawal.status = '0'
            ), 0)
        ) AS diff_amount,
        (SELECT MAX(candidate_level.price)
         FROM order_member_level candidate_level
         WHERE candidate_level.price <= u.balance) AS max_level_price,
        (SELECT MIN(next_level.price)
         FROM order_member_level next_level
         WHERE next_level.price > COALESCE((
             SELECT MAX(current_level.price)
             FROM order_member_level current_level
             WHERE current_level.price <= u.balance
         ), -1)) AS next_level_price,
        COALESCE((
            SELECT MIN(next_level.price)
            FROM order_member_level next_level
            WHERE next_level.price > COALESCE((
                SELECT MAX(current_level.price)
                FROM order_member_level current_level
                WHERE current_level.price <= u.balance
            ), -1)
        ) - u.balance, 0) AS recharge_needed_for_next_level
    FROM order_member_user u
    LEFT JOIN order_member_level level_info ON u.level_id = level_info.id
    LEFT JOIN order_member_user parent_user ON u.parent_id = parent_user.id
) tmp_count;

-- ---------------------------------------------------------------------------
-- Candidate lightweight count.  Production code must append exactly the same
-- dynamic WHERE predicates as the page query; none of those predicates needs a
-- join because they all reference order_member_user u.
-- ---------------------------------------------------------------------------
EXPLAIN ANALYZE
SELECT COUNT(*)
FROM order_member_user u;

-- Candidate base page: no per-user financial or descendant scans.
EXPLAIN ANALYZE
SELECT
    u.id,
    u.username,
    u.phone,
    u.parent_id,
    u.ancestors,
    u.balance,
    u.frozen_balance,
    (u.balance + u.frozen_balance) AS total_balance,
    parent_user.username AS parent_username,
    parent_user.phone AS parent_phone,
    level_info.id AS levelId,
    level_info.name_zh,
    level_info.name_en
FROM order_member_user u
LEFT JOIN order_member_level level_info ON u.level_id = level_info.id
LEFT JOIN order_member_user parent_user ON u.parent_id = parent_user.id
ORDER BY u.create_time DESC
LIMIT 10;

-- Candidate one-pass financial aggregation for the page.  The production
-- mapper can pass the page IDs as IN values instead of repeating page_users.
EXPLAIN ANALYZE
WITH page_users AS (
    SELECT id
    FROM order_member_user
    ORDER BY create_time DESC
    LIMIT 10
),
financial_rows AS (
    SELECT
        topup.user_id,
        SUM(CASE
            WHEN topup.type = '0' THEN IFNULL(topup.amout, 0)
            ELSE IFNULL(topup.real_money, 0)
        END) AS total_recharge,
        0 AS total_withdraw,
        0 AS withdraw_frozen_amount,
        0 AS today_withdraw_count
    FROM order_topup topup
    INNER JOIN page_users page_user ON page_user.id = topup.user_id
    WHERE topup.status = '0'
    GROUP BY topup.user_id

    UNION ALL

    SELECT
        withdrawal.user_id,
        0 AS total_recharge,
        SUM(CASE
            WHEN withdrawal.status = '0'
                THEN IFNULL(withdrawal.credited_amount, 0)
            ELSE 0
        END) AS total_withdraw,
        SUM(CASE
            WHEN withdrawal.status = '1' THEN IFNULL(withdrawal.amount, 0)
            ELSE 0
        END) AS withdraw_frozen_amount,
        SUM(CASE
            WHEN withdrawal.application_time >= CURDATE()
             AND withdrawal.application_time < CURDATE() + INTERVAL 1 DAY
                THEN 1
            ELSE 0
        END) AS today_withdraw_count
    FROM order_withdrawal withdrawal
    INNER JOIN page_users page_user ON page_user.id = withdrawal.user_id
    GROUP BY withdrawal.user_id
)
SELECT
    page_user.id AS user_id,
    COALESCE(SUM(financial_row.total_recharge), 0) AS total_recharge,
    COALESCE(SUM(financial_row.total_withdraw), 0) AS total_withdraw,
    COALESCE(SUM(financial_row.withdraw_frozen_amount), 0) AS withdraw_frozen_amount,
    COALESCE(SUM(financial_row.today_withdraw_count), 0) AS today_withdraw_count
FROM page_users page_user
LEFT JOIN financial_rows financial_row ON financial_row.user_id = page_user.id
GROUP BY page_user.id;

-- Candidate batch all-descendant counts preserving the current FIND_IN_SET
-- semantics.  Each ancestors CSV is parsed once per request rather than once
-- for every returned user.  Use actual page IDs in production.
EXPLAIN ANALYZE
SELECT
    ancestor_token.ancestor_id AS user_id,
    COUNT(DISTINCT descendant.id) AS all_sub_count
FROM order_member_user descendant
JOIN JSON_TABLE(
    CONCAT(
        '[',
        REPLACE(
            JSON_QUOTE(COALESCE(descendant.ancestors, '')),
            ',',
            '","'
        ),
        ']'
    ),
    '$[*]' COLUMNS (ancestor_id VARCHAR(32) PATH '$')
) ancestor_token
WHERE ancestor_token.ancestor_id IN (
    SELECT CAST(page_user.id AS CHAR)
    FROM (
        SELECT id
        FROM order_member_user
        ORDER BY create_time DESC
        LIMIT 10
    ) page_user
)
GROUP BY ancestor_token.ancestor_id;

-- Semantic comparison: the batched financial formulas must match all four
-- legacy correlated expressions for every user.  Expected result: 0.
WITH topup_aggregate AS (
    SELECT
        topup.user_id,
        COALESCE(SUM(CASE
            WHEN topup.type = '0' THEN COALESCE(topup.amout, 0)
            ELSE COALESCE(topup.real_money, 0)
        END), 0) AS total_recharge
    FROM order_topup topup
    WHERE topup.status = '0'
    GROUP BY topup.user_id
),
withdrawal_aggregate AS (
    SELECT
        withdrawal.user_id,
        COALESCE(SUM(CASE
            WHEN withdrawal.status = '0'
                THEN COALESCE(withdrawal.credited_amount, 0)
            ELSE 0
        END), 0) AS total_withdraw,
        COALESCE(SUM(CASE
            WHEN withdrawal.status = '1'
                THEN COALESCE(withdrawal.amount, 0)
            ELSE 0
        END), 0) AS withdraw_frozen_amount,
        SUM(CASE
            WHEN withdrawal.application_time >= CURDATE()
             AND withdrawal.application_time < CURDATE() + INTERVAL 1 DAY
                THEN 1
            ELSE 0
        END) AS today_withdraw_count
    FROM order_withdrawal withdrawal
    GROUP BY withdrawal.user_id
)
SELECT COUNT(*) AS financial_mismatch_users
FROM order_member_user u
LEFT JOIN topup_aggregate batched_topup ON batched_topup.user_id = u.id
LEFT JOIN withdrawal_aggregate batched_withdrawal ON batched_withdrawal.user_id = u.id
WHERE COALESCE(batched_topup.total_recharge, 0) <> IFNULL((
          SELECT SUM(CASE
              WHEN old_topup.type = '0' THEN IFNULL(old_topup.amout, 0)
              ELSE IFNULL(old_topup.real_money, 0)
          END)
          FROM order_topup old_topup
          WHERE old_topup.user_id = u.id
            AND old_topup.status = '0'
      ), 0)
   OR COALESCE(batched_withdrawal.total_withdraw, 0) <> IFNULL((
          SELECT SUM(old_withdrawal.credited_amount)
          FROM order_withdrawal old_withdrawal
          WHERE old_withdrawal.user_id = u.id
            AND old_withdrawal.status = '0'
      ), 0)
   OR COALESCE(batched_withdrawal.withdraw_frozen_amount, 0) <> IFNULL((
          SELECT SUM(old_withdrawal.amount)
          FROM order_withdrawal old_withdrawal
          WHERE old_withdrawal.user_id = u.id
            AND old_withdrawal.status = '1'
      ), 0)
   OR COALESCE(batched_withdrawal.today_withdraw_count, 0) <> (
          SELECT COUNT(*)
          FROM order_withdrawal old_withdrawal
          WHERE old_withdrawal.user_id = u.id
            AND old_withdrawal.application_time >= CURDATE()
            AND old_withdrawal.application_time < CURDATE() + INTERVAL 1 DAY
      );

-- Semantic comparison: parsing ancestors in one pass must match the legacy
-- FIND_IN_SET expression for every user.  Expected result: 0.
WITH batched_descendant_count AS (
    SELECT
        ancestor_token.ancestor_id AS user_id,
        COUNT(DISTINCT descendant.id) AS all_sub_count
    FROM order_member_user descendant
    JOIN JSON_TABLE(
        CONCAT(
            '[',
            REPLACE(
                JSON_QUOTE(COALESCE(descendant.ancestors, '')),
                ',',
                '","'
            ),
            ']'
        ),
        '$[*]' COLUMNS (ancestor_id VARCHAR(32) PATH '$')
    ) ancestor_token
    GROUP BY ancestor_token.ancestor_id
)
SELECT COUNT(*) AS descendant_count_mismatch_users
FROM order_member_user u
LEFT JOIN batched_descendant_count batched
       ON batched.user_id = CAST(u.id AS CHAR)
WHERE COALESCE(batched.all_sub_count, 0) <> (
    SELECT COUNT(*)
    FROM order_member_user descendant
    WHERE FIND_IN_SET(u.id, descendant.ancestors)
);
