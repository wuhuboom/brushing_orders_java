-- Align the website settings catalogue with the 27 setting kinds used by the legacy admin.
-- Idempotent: safe to execute more than once.

INSERT INTO order_config (`type`, `sort`, `name`, `content`, `create_time`, `update_time`)
SELECT 'error', 25, '错误代码', '{"codes":[]}', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM order_config WHERE `type` = 'error'
);

INSERT INTO order_config (`type`, `sort`, `name`, `content`, `create_time`, `update_time`)
SELECT 'backendRateLimit', 26, '后端限流设置', '{"value":[]}', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM order_config WHERE `type` = 'backendRateLimit'
);

INSERT INTO order_config (`type`, `sort`, `name`, `content`, `create_time`, `update_time`)
SELECT 'frontendRateLimit', 27, '前端限流设置', '{"value":[]}', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM order_config WHERE `type` = 'frontendRateLimit'
);
