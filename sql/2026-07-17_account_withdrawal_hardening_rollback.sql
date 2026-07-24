-- Run the reverse plaintext backfill before this rollback if phase 2 already cleared plaintext.
DELETE srm
FROM sys_role_menu srm
JOIN sys_menu sm ON sm.menu_id = srm.menu_id
WHERE sm.perms = 'member:withdrawal:sensitive';
DELETE FROM sys_menu WHERE perms = 'member:withdrawal:sensitive';
DROP TABLE IF EXISTS withdrawal_daily_quota;

ALTER TABLE order_withdrawal
    DROP INDEX idx_withdrawal_account_status,
    DROP INDEX idx_withdrawal_user_status_time,
    DROP INDEX uk_withdrawal_user_request,
    DROP COLUMN business_date,
    DROP COLUMN account_mask,
    DROP COLUMN account_snapshot_enc,
    DROP COLUMN net_amount,
    DROP COLUMN request_id;

ALTER TABLE goods_withdrawal_account
    DROP INDEX idx_withdrawal_account_user_active,
    DROP COLUMN wallet_address_mask,
    DROP COLUMN account_name_mask,
    DROP COLUMN account_holder_mask,
    DROP COLUMN bank_account_mask,
    DROP COLUMN wallet_address_enc,
    DROP COLUMN account_name_enc,
    DROP COLUMN account_holder_enc,
    DROP COLUMN bank_account_enc,
    DROP COLUMN deleted_time,
    DROP COLUMN deleted,
    DROP COLUMN update_time;

DROP INDEX idx_recharge_user_time ON goods_recharge_record;
DROP INDEX idx_transaction_flow_user_time ON goods_transaction_flow;
