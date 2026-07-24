-- Phase 1: additive schema for account encryption, idempotency and daily quotas.
ALTER TABLE goods_withdrawal_account
    ADD COLUMN update_time DATETIME(3) NULL AFTER create_time,
    ADD COLUMN deleted CHAR(1) NOT NULL DEFAULT '0' AFTER update_time,
    ADD COLUMN deleted_time DATETIME(3) NULL AFTER deleted,
    ADD COLUMN bank_account_enc TEXT NULL AFTER deleted_time,
    ADD COLUMN account_holder_enc TEXT NULL AFTER bank_account_enc,
    ADD COLUMN account_name_enc TEXT NULL AFTER account_holder_enc,
    ADD COLUMN wallet_address_enc TEXT NULL AFTER account_name_enc,
    ADD COLUMN bank_account_mask VARCHAR(64) NULL AFTER wallet_address_enc,
    ADD COLUMN account_holder_mask VARCHAR(128) NULL AFTER bank_account_mask,
    ADD COLUMN account_name_mask VARCHAR(128) NULL AFTER account_holder_mask,
    ADD COLUMN wallet_address_mask VARCHAR(128) NULL AFTER account_name_mask,
    ADD INDEX idx_withdrawal_account_user_active (user_id, deleted, is_default, create_time);

ALTER TABLE order_withdrawal
    ADD COLUMN request_id VARCHAR(36) NULL AFTER order_number,
    ADD COLUMN net_amount DECIMAL(20,2) NULL AFTER fee,
    ADD COLUMN account_snapshot_enc TEXT NULL AFTER withdrawal_account_id,
    ADD COLUMN account_mask VARCHAR(255) NULL AFTER account_snapshot_enc,
    ADD COLUMN business_date DATE NULL AFTER account_mask,
    ADD UNIQUE INDEX uk_withdrawal_user_request (user_id, request_id),
    ADD INDEX idx_withdrawal_user_status_time (user_id, status, create_time),
    ADD INDEX idx_withdrawal_account_status (withdrawal_account_id, status);

UPDATE order_withdrawal
SET net_amount = ROUND(amount - COALESCE(fee, 0), 2)
WHERE net_amount IS NULL;

UPDATE order_withdrawal
SET business_date = DATE(create_time)
WHERE business_date IS NULL;

UPDATE order_withdrawal ow
JOIN goods_withdrawal_account gwa ON gwa.id = ow.withdrawal_account_id
SET ow.account_mask = CASE
    WHEN gwa.type = '1' THEN CONCAT(
        COALESCE(NULLIF(gwa.wallet_name, ''), 'Wallet'), ' ',
        CASE
            WHEN gwa.wallet_address IS NULL THEN ''
            WHEN CHAR_LENGTH(gwa.wallet_address) <= 10
                THEN CONCAT('****', RIGHT(gwa.wallet_address, 4))
            ELSE CONCAT(LEFT(gwa.wallet_address, 6), '****', RIGHT(gwa.wallet_address, 4))
        END
    )
    ELSE CONCAT(
        COALESCE(NULLIF(gwa.bank_name, ''), 'Bank'), ' ',
        CASE
            WHEN gwa.bank_account IS NULL THEN ''
            ELSE CONCAT('****', RIGHT(REPLACE(gwa.bank_account, ' ', ''), 4))
        END
    )
END
WHERE ow.account_mask IS NULL;

CREATE TABLE withdrawal_daily_quota (
    business_date DATE NOT NULL,
    reserved_amount DECIMAL(20,2) NOT NULL DEFAULT 0,
    update_time DATETIME(3) NOT NULL,
    PRIMARY KEY (business_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Atomic platform withdrawal quota reservation';

CREATE INDEX idx_recharge_user_time ON goods_recharge_record (user_id, create_time);
CREATE INDEX idx_transaction_flow_user_time ON goods_transaction_flow (user_id, created_time);

-- Full payout details are protected by a separate button permission.
-- Super administrators inherit it; other roles must be granted it explicitly.
SET @withdrawal_permission_parent = (
    SELECT parent_id
    FROM sys_menu
    WHERE perms = 'member:withdrawal:query'
    LIMIT 1
);
SET @sensitive_permission_id = (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, update_by, update_time, remark
)
SELECT @sensitive_permission_id, '查看完整付款账户', @withdrawal_permission_parent, 90,
       '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       'member:withdrawal:sensitive', '#', 'admin', NOW(), '', NULL,
       '提现付款账户敏感信息权限'
WHERE @withdrawal_permission_parent IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM sys_menu WHERE perms = 'member:withdrawal:sensitive'
  );
