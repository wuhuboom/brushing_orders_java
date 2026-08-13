-- Align the local funding workflow with the original system:
-- 1 = pending review, 2 = approved, 3 = rejected.
START TRANSACTION;

UPDATE goods_recharge_record
SET status = CASE status
    WHEN '0' THEN '2'
    WHEN '2' THEN '3'
    ELSE status
END
WHERE status IN ('0', '2');

UPDATE order_withdrawal
SET status = CASE status
    WHEN '0' THEN '2'
    WHEN '2' THEN '3'
    ELSE status
END
WHERE status IN ('0', '2');

UPDATE sys_dict_data
SET dict_sort = 1,
    dict_label = '待审核',
    dict_value = '1',
    list_class = 'processing',
    status = '0'
WHERE dict_type = 'apply_status' AND dict_code = 151;

UPDATE sys_dict_data
SET dict_sort = 2,
    dict_label = '已通过',
    dict_value = '2',
    list_class = 'success',
    status = '0'
WHERE dict_type = 'apply_status' AND dict_code = 150;

UPDATE sys_dict_data
SET dict_sort = 3,
    dict_label = '已拒绝',
    dict_value = '3',
    list_class = 'danger',
    status = '0'
WHERE dict_type = 'apply_status' AND dict_code = 152;

COMMIT;
