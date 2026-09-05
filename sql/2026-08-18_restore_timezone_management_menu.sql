SET NAMES utf8mb4;

START TRANSACTION;

-- 恢复“客户管理 > 网站管理 > 时区管理”。原始代码生成配置的 parentMenuId 即为 2067。
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  2093, '时区管理', 2067, 9, 'zone', 'system/zone/index', NULL, 'Zone',
  1, 0, 'C', '0', '0', 'system:zone:list', 'time',
  'admin', NOW(), '', NULL, '时区管理菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2093);

UPDATE sys_menu
SET menu_name = '时区管理',
    parent_id = 2067,
    order_num = 9,
    path = 'zone',
    component = 'system/zone/index',
    route_name = 'Zone',
    is_frame = 1,
    menu_type = 'C',
    visible = '0',
    status = '0',
    perms = 'system:zone:list',
    icon = CASE WHEN icon IS NULL OR icon = '' OR icon = '#' THEN 'time' ELSE icon END,
    update_by = 'admin',
    update_time = NOW(),
    remark = '时区管理菜单'
WHERE menu_id = 2093;

-- 页面“启用”按钮与接口使用同一权限，避免按钮权限存在但数据库没有对应权限项。
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT
  ids.menu_id, '时区启用', 2093, 6, '#', NULL, NULL, '',
  1, 0, 'F', '0', '0', 'system:zone:active', '#',
  'admin', NOW(), '', NULL, '时区启用权限'
FROM (
  SELECT GREATEST(5152, COALESCE(MAX(menu_id), 0) + 1) AS menu_id
  FROM sys_menu
) ids
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'system:zone:active');

-- 菜单改挂到网站管理后，为原先拥有时区菜单的角色补齐新的父菜单链。
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT time_zone_grant.role_id, parent_menu.menu_id
FROM sys_role_menu time_zone_grant
JOIN sys_menu parent_menu ON parent_menu.menu_id IN (2002, 2067)
WHERE time_zone_grant.menu_id = 2093
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_menu existed
    WHERE existed.role_id = time_zone_grant.role_id
      AND existed.menu_id = parent_menu.menu_id
  );

-- 已经拥有时区管理菜单的角色同步获得“启用”权限。
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT parent_grant.role_id, active_menu.menu_id
FROM sys_role_menu parent_grant
JOIN sys_menu active_menu ON active_menu.perms = 'system:zone:active'
WHERE parent_grant.menu_id = 2093
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_menu existed
    WHERE existed.role_id = parent_grant.role_id
      AND existed.menu_id = active_menu.menu_id
  );

-- 超级管理员的菜单关联保持完整（运行时仍保留其通配权限）。
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT admin_role.role_id, menu.menu_id
FROM sys_role admin_role
JOIN sys_menu menu ON menu.menu_id IN (2002, 2067, 2093) OR menu.perms = 'system:zone:active'
WHERE admin_role.role_key = 'admin'
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_menu existed
    WHERE existed.role_id = admin_role.role_id
      AND existed.menu_id = menu.menu_id
  );

COMMIT;
