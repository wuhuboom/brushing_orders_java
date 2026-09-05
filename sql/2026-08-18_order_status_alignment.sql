-- Keep the order status dictionary aligned with the cancel workflow.
-- This migration is idempotent and does not mutate order rows.

START TRANSACTION;

INSERT INTO sys_dict_data (
    dict_sort, dict_label, dict_value, dict_type, css_class, list_class,
    is_default, status, create_by, create_time, update_by, update_time, remark
)
SELECT
    3, '已取消', '3', 'order_status', NULL, 'default',
    'N', '0', 'system', NOW(), 'system', NOW(), '订单取消终态'
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_dict_data
    WHERE dict_type = 'order_status'
      AND dict_value = '3'
);

UPDATE sys_dict_data
SET dict_sort = 3,
    dict_label = '已取消',
    list_class = 'default',
    status = '0',
    update_by = 'system',
    update_time = NOW(),
    remark = '订单取消终态'
WHERE dict_type = 'order_status'
  AND dict_value = '3';

COMMIT;
