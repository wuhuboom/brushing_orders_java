-- Align recharge review states with the current audit workflow:
-- 1 pending, 2 approved, 3 rejected.
-- Legacy recharge rows used 0 for approved; migrate them once and retire
-- the legacy dictionary entry so list filters do not show duplicate labels.

UPDATE goods_recharge_record
SET status = '2'
WHERE status = '0';

UPDATE sys_dict_data
SET dict_sort = 1,
    dict_label = '待审核',
    list_class = 'primary',
    status = '0',
    update_by = 'system',
    update_time = NOW()
WHERE dict_type = 'apply_status'
  AND dict_value = '1';

UPDATE sys_dict_data
SET dict_sort = 2,
    dict_label = '已通过',
    list_class = 'success',
    status = '0',
    update_by = 'system',
    update_time = NOW()
WHERE dict_type = 'apply_status'
  AND dict_value = '2';

INSERT INTO sys_dict_data (
    dict_sort, dict_label, dict_value, dict_type, css_class, list_class,
    is_default, status, create_by, create_time, update_by, update_time, remark
)
SELECT
    3, '已拒绝', '3', 'apply_status', NULL, 'danger',
    'N', '0', 'system', NOW(), 'system', NOW(), '审核驳回'
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_dict_data
    WHERE dict_type = 'apply_status'
      AND dict_value = '3'
);

UPDATE sys_dict_data
SET dict_sort = 3,
    dict_label = '已拒绝',
    list_class = 'danger',
    status = '0',
    update_by = 'system',
    update_time = NOW()
WHERE dict_type = 'apply_status'
  AND dict_value = '3';

UPDATE sys_dict_data
SET status = '1',
    update_by = 'system',
    update_time = NOW(),
    remark = '历史状态，已迁移为 2=已通过'
WHERE dict_type = 'apply_status'
  AND dict_value = '0';
