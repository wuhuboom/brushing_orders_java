-- Failed login attempts for an unknown account do not have a user record yet.
-- Keep those security audit records instead of inventing a sentinel user ID.
ALTER TABLE order_login_log
    MODIFY COLUMN user_id BIGINT NULL COMMENT '用户ID（未知账号登录失败时为空）';
