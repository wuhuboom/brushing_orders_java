SET NAMES utf8mb4;

-- 1. 新增业务表是否全部存在；missing_count 应为 0。
SELECT COUNT(*) AS missing_count
FROM (
  SELECT 'legacy_auth_record' table_name UNION ALL
  SELECT 'legacy_yuebao_account' UNION ALL
  SELECT 'legacy_yuebao_flow' UNION ALL
  SELECT 'legacy_bulletin' UNION ALL
  SELECT 'legacy_wallet' UNION ALL
  SELECT 'legacy_recruitment' UNION ALL
  SELECT 'points_gift' UNION ALL
  SELECT 'points_account' UNION ALL
  SELECT 'points_flow' UNION ALL
  SELECT 'points_gift_order' UNION ALL
  SELECT 'activity_info' UNION ALL
  SELECT 'activity_prize' UNION ALL
  SELECT 'activity_account' UNION ALL
  SELECT 'activity_account_prize' UNION ALL
  SELECT 'activity_partner' UNION ALL
  SELECT 'website_customer' UNION ALL
  SELECT 'sys_permission_strategy' UNION ALL
  SELECT 'sys_strategy_menu' UNION ALL
  SELECT 'sys_permission_group' UNION ALL
  SELECT 'sys_group_strategy' UNION ALL
  SELECT 'sys_group_menu' UNION ALL
  SELECT 'sys_user_group' UNION ALL
  SELECT 'sys_post_role' UNION ALL
  SELECT 'sys_role_strategy' UNION ALL
  SELECT 'sys_role_data_rule' UNION ALL
  SELECT 'sys_file' UNION ALL
  SELECT 'sys_file_reference'
) expected
LEFT JOIN information_schema.tables actual
  ON actual.table_schema = DATABASE()
 AND actual.table_name = expected.table_name
WHERE actual.table_name IS NULL;

-- 2. 如需定位缺失表，结果集应为空。
SELECT expected.table_name AS missing_table
FROM (
  SELECT 'legacy_auth_record' table_name UNION ALL SELECT 'legacy_yuebao_account' UNION ALL
  SELECT 'legacy_yuebao_flow' UNION ALL SELECT 'legacy_bulletin' UNION ALL
  SELECT 'legacy_wallet' UNION ALL SELECT 'legacy_recruitment' UNION ALL
  SELECT 'points_gift' UNION ALL SELECT 'points_account' UNION ALL SELECT 'points_flow' UNION ALL
  SELECT 'points_gift_order' UNION ALL SELECT 'activity_info' UNION ALL SELECT 'activity_prize' UNION ALL
  SELECT 'activity_account' UNION ALL SELECT 'activity_account_prize' UNION ALL SELECT 'activity_partner' UNION ALL
  SELECT 'website_customer' UNION ALL SELECT 'sys_permission_strategy' UNION ALL SELECT 'sys_strategy_menu' UNION ALL
  SELECT 'sys_permission_group' UNION ALL SELECT 'sys_group_strategy' UNION ALL SELECT 'sys_group_menu' UNION ALL
  SELECT 'sys_user_group' UNION ALL SELECT 'sys_post_role' UNION ALL SELECT 'sys_role_strategy' UNION ALL
  SELECT 'sys_role_data_rule' UNION ALL SELECT 'sys_file' UNION ALL SELECT 'sys_file_reference'
) expected
LEFT JOIN information_schema.tables actual
  ON actual.table_schema = DATABASE()
 AND actual.table_name = expected.table_name
WHERE actual.table_name IS NULL;

-- 3. 系统管理扩展字段；返回数量应为 14。
SELECT table_name, column_name, column_type, column_default
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND (table_name, column_name) IN (
    ('sys_dept', 'remark'),
    ('sys_role', 'hide_phone'),
    ('sys_role', 'is_builtin'),
    ('sys_oper_log', 'resource_code'),
    ('sys_oper_log', 'request_headers'),
    ('sys_oper_log', 'query_string'),
    ('sys_oper_log', 'request_params'),
    ('sys_oper_log', 'request_body'),
    ('sys_oper_log', 'message'),
    ('sys_logininfor', 'request_headers'),
    ('sys_logininfor', 'request_params'),
    ('sys_user', 'is_locked'),
    ('order_login_log', 'success'),
    ('order_login_log', 'request_headers')
  )
ORDER BY table_name, column_name;

-- 4. 系统管理四组菜单和页面；平台配置记录应为 visible='1'（隐藏兼容）。
SELECT menu_id, menu_name, parent_id, order_num, path, component, menu_type, perms, visible, status
FROM sys_menu
WHERE menu_id IN (100, 2000, 2001, 5000, 5001, 5002, 2010, 2011, 2012, 5010, 5011, 5012, 5013, 5014, 5015)
ORDER BY parent_id, order_num, menu_id;

-- 5. 同一父级下不应出现重复 path；结果集应为空。
SELECT parent_id, path, COUNT(*) AS duplicate_count
FROM sys_menu
WHERE menu_type IN ('M', 'C')
  AND path IS NOT NULL
  AND path NOT IN ('', '#')
GROUP BY parent_id, path
HAVING COUNT(*) > 1;

-- 6. 菜单主键、角色菜单关系不应重复；两个 duplicate_count 都应为 0。
SELECT 'sys_menu' AS relation_name, COUNT(*) - COUNT(DISTINCT menu_id) AS duplicate_count FROM sys_menu
UNION ALL
SELECT 'sys_role_menu', COUNT(*) - COUNT(DISTINCT CONCAT(role_id, ':', menu_id)) FROM sys_role_menu;

-- 7. 新权限关联表不应有孤立数据；所有 orphan_count 应为 0。
SELECT 'sys_strategy_menu' relation_name, COUNT(*) orphan_count
FROM sys_strategy_menu r
LEFT JOIN sys_permission_strategy p ON p.strategy_id = r.strategy_id
LEFT JOIN sys_menu m ON m.menu_id = r.menu_id
WHERE p.strategy_id IS NULL OR m.menu_id IS NULL
UNION ALL
SELECT 'sys_group_strategy', COUNT(*)
FROM sys_group_strategy r
LEFT JOIN sys_permission_group g ON g.group_id = r.group_id
LEFT JOIN sys_permission_strategy s ON s.strategy_id = r.strategy_id
WHERE g.group_id IS NULL OR s.strategy_id IS NULL
UNION ALL
SELECT 'sys_group_menu', COUNT(*)
FROM sys_group_menu r
LEFT JOIN sys_permission_group g ON g.group_id = r.group_id
LEFT JOIN sys_menu m ON m.menu_id = r.menu_id
WHERE g.group_id IS NULL OR m.menu_id IS NULL
UNION ALL
SELECT 'sys_user_group', COUNT(*)
FROM sys_user_group r
LEFT JOIN sys_user u ON u.user_id = r.user_id
LEFT JOIN sys_permission_group g ON g.group_id = r.group_id
WHERE u.user_id IS NULL OR g.group_id IS NULL
UNION ALL
SELECT 'sys_post_role', COUNT(*)
FROM sys_post_role r
LEFT JOIN sys_post p ON p.post_id = r.post_id
LEFT JOIN sys_role role ON role.role_id = r.role_id
WHERE p.post_id IS NULL OR role.role_id IS NULL
UNION ALL
SELECT 'sys_role_strategy', COUNT(*)
FROM sys_role_strategy r
LEFT JOIN sys_role role ON role.role_id = r.role_id
LEFT JOIN sys_permission_strategy s ON s.strategy_id = r.strategy_id
WHERE role.role_id IS NULL OR s.strategy_id IS NULL
UNION ALL
SELECT 'sys_role_data_rule', COUNT(*)
FROM sys_role_data_rule r
LEFT JOIN sys_role role ON role.role_id = r.role_id
LEFT JOIN sys_menu m ON m.menu_id = r.menu_id
WHERE role.role_id IS NULL OR m.menu_id IS NULL
UNION ALL
SELECT 'sys_file_reference', COUNT(*)
FROM sys_file_reference r
LEFT JOIN sys_file f ON f.file_id = r.file_id
WHERE f.file_id IS NULL;

-- 8. 文件和权限业务数据量，仅用于确认环境状态。
SELECT 'sys_permission_strategy' table_name, COUNT(*) row_count FROM sys_permission_strategy
UNION ALL SELECT 'sys_permission_group', COUNT(*) FROM sys_permission_group
UNION ALL SELECT 'sys_file', COUNT(*) FROM sys_file
UNION ALL SELECT 'sys_file_reference', COUNT(*) FROM sys_file_reference;

-- 9. 超级管理员应拥有新系统管理菜单权限；缺失菜单结果集应为空。
SELECT m.menu_id, m.menu_name, m.perms
FROM sys_menu m
LEFT JOIN sys_role_menu rm ON rm.menu_id = m.menu_id AND rm.role_id = 1
WHERE m.menu_id BETWEEN 5000 AND 5099
  AND rm.menu_id IS NULL;
