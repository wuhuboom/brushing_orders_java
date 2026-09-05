-- Split the member page permission from its sensitive child drawers and mutations.
-- This migration is idempotent. It deliberately does not grant anything to roles
-- that only own member:orderuser:list.

START TRANSACTION;

SET @member_page_id = (
    SELECT menu_id
    FROM sys_menu
    WHERE perms = 'member:orderuser:list'
    LIMIT 1
);
SET @permission_base_id = (SELECT COALESCE(MAX(menu_id), 0) FROM sys_menu);

INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, update_by, update_time, remark
)
SELECT
    @permission_base_id + seed.seq,
    seed.menu_name,
    @member_page_id,
    seed.order_num,
    '#',
    '',
    NULL,
    '',
    1,
    0,
    'F',
    '0',
    '0',
    seed.perms,
    '#',
    'admin',
    NOW(),
    '',
    NULL,
    seed.remark
FROM (
    SELECT  1 seq, 20 order_num, '连单列表' menu_name, 'member:orderlink:list' perms, '会员页连单列表' remark
    UNION ALL SELECT  2, 21, '连单详情', 'member:orderlink:query', '会员页连单详情'
    UNION ALL SELECT  3, 22, '连单新增', 'member:orderlink:add', '会员页连单新增'
    UNION ALL SELECT  4, 23, '连单修改', 'member:orderlink:edit', '会员页连单修改'
    UNION ALL SELECT  5, 24, '连单删除', 'member:orderlink:remove', '会员页连单删除'
    UNION ALL SELECT  6, 25, '连单导出', 'member:orderlink:export', '会员页连单导出'
    UNION ALL SELECT  7, 30, '彩金列表', 'member:bonus:list', '会员页彩金列表'
    UNION ALL SELECT  8, 31, '彩金详情', 'member:bonus:query', '会员页彩金详情'
    UNION ALL SELECT  9, 32, '彩金新增', 'member:bonus:add', '会员页彩金新增'
    UNION ALL SELECT 10, 33, '彩金修改', 'member:bonus:edit', '会员页彩金修改'
    UNION ALL SELECT 11, 34, '彩金删除', 'member:bonus:remove', '会员页彩金删除'
    UNION ALL SELECT 12, 35, '彩金导出', 'member:bonus:export', '会员页彩金导出'
    UNION ALL SELECT 13, 36, '彩金领取', 'member:bonus:receive', '会改变余额的彩金领取动作'
    UNION ALL SELECT 14, 37, '彩金发放', 'member:bonus:give', '会改变余额的彩金发放动作'
    UNION ALL SELECT 15, 40, '额外佣金列表', 'member:extracommission:list', '会员页额外佣金列表'
    UNION ALL SELECT 16, 41, '额外佣金详情', 'member:extracommission:query', '会员页额外佣金详情'
    UNION ALL SELECT 17, 42, '额外佣金新增', 'member:extracommission:add', '会员页额外佣金新增'
    UNION ALL SELECT 18, 43, '额外佣金修改', 'member:extracommission:edit', '会员页额外佣金修改'
    UNION ALL SELECT 19, 44, '额外佣金删除', 'member:extracommission:remove', '会员页额外佣金删除'
    UNION ALL SELECT 20, 45, '额外佣金导出', 'member:extracommission:export', '会员页额外佣金导出'
    UNION ALL SELECT 21, 50, '提现账户列表', 'member:withdrawalAcc:list', '会员页提现账户列表'
    UNION ALL SELECT 22, 51, '提现账户详情', 'member:withdrawalAcc:query', '会员页提现账户详情'
    UNION ALL SELECT 23, 52, '提现账户新增', 'member:withdrawalAcc:add', '会员页提现账户新增'
    UNION ALL SELECT 24, 53, '提现账户修改', 'member:withdrawalAcc:edit', '会员页提现账户修改'
    UNION ALL SELECT 25, 54, '提现账户删除', 'member:withdrawalAcc:remove', '会员页提现账户删除'
    UNION ALL SELECT 26, 55, '提现账户导出', 'member:withdrawalAcc:export', '会员页提现账户导出'
) seed
LEFT JOIN sys_menu existing ON existing.perms = seed.perms
WHERE @member_page_id IS NOT NULL
  AND existing.menu_id IS NULL;

-- Preserve the access of existing non-admin member operators without granting
-- sensitive child data to a list-only role. Query/edit roles receive child reads.
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT source.role_id, target.menu_id
FROM sys_role_menu source
JOIN sys_menu source_menu ON source_menu.menu_id = source.menu_id
JOIN sys_menu target ON target.perms IN (
    'member:orderlink:list', 'member:orderlink:query',
    'member:bonus:list', 'member:bonus:query',
    'member:extracommission:list', 'member:extracommission:query',
    'member:withdrawalAcc:list', 'member:withdrawalAcc:query'
)
WHERE source_menu.perms IN ('member:orderuser:query', 'member:orderuser:edit')
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_menu current_permission
      WHERE current_permission.role_id = source.role_id
        AND current_permission.menu_id = target.menu_id
  );

-- Existing member editors receive the child write actions. This does not match
-- member:orderuser:list and therefore does not elevate a list-only role.
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT source.role_id, target.menu_id
FROM sys_role_menu source
JOIN sys_menu source_menu ON source_menu.menu_id = source.menu_id
JOIN sys_menu target ON target.perms IN (
    'member:orderlink:add', 'member:orderlink:edit', 'member:orderlink:remove',
    'member:bonus:add', 'member:bonus:edit', 'member:bonus:remove',
    'member:bonus:receive', 'member:bonus:give',
    'member:extracommission:add', 'member:extracommission:edit', 'member:extracommission:remove',
    'member:withdrawalAcc:add', 'member:withdrawalAcc:edit', 'member:withdrawalAcc:remove'
)
WHERE source_menu.perms = 'member:orderuser:edit'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_menu current_permission
      WHERE current_permission.role_id = source.role_id
        AND current_permission.menu_id = target.menu_id
  );

-- Existing member exporters retain export access for the embedded child data.
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT source.role_id, target.menu_id
FROM sys_role_menu source
JOIN sys_menu source_menu ON source_menu.menu_id = source.menu_id
JOIN sys_menu target ON target.perms IN (
    'member:orderlink:export',
    'member:bonus:export',
    'member:extracommission:export',
    'member:withdrawalAcc:export'
)
WHERE source_menu.perms = 'member:orderuser:export'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_menu current_permission
      WHERE current_permission.role_id = source.role_id
        AND current_permission.menu_id = target.menu_id
  );

COMMIT;
