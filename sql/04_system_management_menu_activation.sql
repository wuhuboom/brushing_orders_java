SET NAMES utf8mb4;

-- 系统管理菜单激活脚本。依赖结构脚本和对应前后端版本，允许重复执行。
UPDATE sys_menu SET menu_name='用户管理', parent_id=1, order_num=1, path='user', component=NULL, menu_type='M', visible='0', status='0' WHERE menu_id=2000;
UPDATE sys_menu SET menu_name='权限管理', parent_id=1, order_num=2, path='permission', component=NULL, menu_type='M', visible='0', status='0' WHERE menu_id=2001;

INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 5000,'文件管理',1,3,'file',NULL,NULL,'',1,0,'M','0','0',NULL,'folder','admin',NOW(),'',NULL,'系统文件管理'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=5000 OR (parent_id=1 AND path='file'));
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 5001,'操作日志',1,4,'operationLog',NULL,NULL,'',1,0,'M','0','0',NULL,'log','admin',NOW(),'',NULL,'系统操作日志'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=5001 OR (parent_id=1 AND path='operationLog'));
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 5002,'平台配置',1,5,'platform',NULL,NULL,'',1,0,'M','0','0',NULL,'tool','admin',NOW(),'',NULL,'旧站外平台配置功能'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=5002 OR (parent_id=1 AND path='platform'));
-- 旧站系统管理只有四组；平台配置保留为隐藏兼容入口。
UPDATE sys_menu SET visible='1',status='0' WHERE menu_id=5002 OR (parent_id=1 AND path='platform');

UPDATE sys_menu SET menu_name='组织管理',parent_id=2000,order_num=1,path='orgs',component='system/dept/index',perms='system:dept:list' WHERE menu_id=103;
UPDATE sys_menu SET menu_name='职位管理',parent_id=2000,order_num=2,path='positions',component='system/post/index',perms='system:post:list' WHERE menu_id=104;
UPDATE sys_menu SET menu_name='用户管理',parent_id=2000,order_num=3,path='users',component='system/user/index',perms='system:user:list' WHERE menu_id=100;
UPDATE sys_menu SET menu_name='角色管理',parent_id=2001,order_num=2,path='roles',component='system/role/index',perms='system:role:list' WHERE menu_id=101;

INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 5010,'策略管理',2001,1,'strategies','system/strategy/index',NULL,'',1,0,'C','0','0','system:strategy:list','skill','admin',NOW(),'',NULL,'策略管理'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=5010 OR (parent_id=2001 AND path='strategies'));
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 5011,'分组管理',2001,3,'groups','system/group/index',NULL,'',1,0,'C','0','0','system:group:list','people','admin',NOW(),'',NULL,'分组管理'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=5011 OR (parent_id=2001 AND path='groups'));
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 5012,'文件引用',5000,1,'fileReferences','system/file/reference',NULL,'',1,0,'C','0','0','system:file-reference:list','link','admin',NOW(),'',NULL,'文件引用'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=5012 OR (parent_id=5000 AND path='fileReferences'));
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT 5013,'文件管理',5000,2,'files','system/file/index',NULL,'',1,0,'C','0','0','system:file:list','folder','admin',NOW(),'',NULL,'文件管理'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=5013 OR (parent_id=5000 AND path='files'));

UPDATE sys_menu SET menu_name='操作日志',parent_id=5001,order_num=1,path='operationLogs',component='monitor/operlog/index',perms='system:operation-log:list' WHERE menu_id=500;
UPDATE sys_menu SET menu_name='登录日志',parent_id=5001,order_num=2,path='userLoginLogs',component='monitor/logininfor/index',perms='system:login-log:list' WHERE menu_id=501;
UPDATE sys_menu SET parent_id=5002,order_num=1,path='menu' WHERE menu_id=102;
UPDATE sys_menu SET parent_id=5002,order_num=2,path='dict' WHERE menu_id=105;
UPDATE sys_menu SET parent_id=5002,order_num=3,path='notice' WHERE menu_id=107;
-- 时区管理沿用生成配置中的原始归属：客户管理 > 网站管理。
UPDATE sys_menu SET parent_id=2067,order_num=9,path='zone' WHERE menu_id=2093;
UPDATE sys_menu SET parent_id=5002,order_num=5,path='config' WHERE menu_id=106;

-- 清除旧的空日志父菜单，避免侧栏出现重复“操作日志”。
DELETE FROM sys_role_menu WHERE menu_id=108;
DELETE FROM sys_menu WHERE menu_id=108;

-- 新页面按钮权限。
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,query,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,update_by,update_time,remark)
SELECT v.menu_id,v.menu_name,v.parent_id,v.order_num,'#',NULL,NULL,'',1,0,'F','0','0',v.perms,'#','admin',NOW(),'',NULL,v.menu_name
FROM (
  SELECT 5100 menu_id,'策略查询' menu_name,5010 parent_id,1 order_num,'system:strategy:query' perms UNION ALL
  SELECT 5101,'策略新增',5010,2,'system:strategy:add' UNION ALL SELECT 5102,'策略修改',5010,3,'system:strategy:edit' UNION ALL SELECT 5103,'策略删除',5010,4,'system:strategy:remove' UNION ALL
  SELECT 5110,'分组查询',5011,1,'system:group:query' UNION ALL SELECT 5111,'分组新增',5011,2,'system:group:add' UNION ALL SELECT 5112,'分组修改',5011,3,'system:group:edit' UNION ALL SELECT 5113,'分组删除',5011,4,'system:group:remove' UNION ALL
  SELECT 5120,'文件引用查询',5012,1,'system:file-reference:query' UNION ALL SELECT 5121,'文件引用上传',5012,2,'system:file-reference:add' UNION ALL SELECT 5122,'文件引用删除',5012,3,'system:file-reference:remove' UNION ALL
  SELECT 5130,'文件查询',5013,1,'system:file:query' UNION ALL SELECT 5131,'文件删除',5013,2,'system:file:remove' UNION ALL
  SELECT 5140,'操作日志删除',500,1,'system:operation-log:remove' UNION ALL SELECT 5150,'登录日志删除',501,1,'system:login-log:remove'
) v
WHERE NOT EXISTS (SELECT 1 FROM sys_menu m WHERE m.menu_id=v.menu_id OR m.perms=v.perms);

-- 将新增菜单及按钮授予当前超级管理员角色。
INSERT INTO sys_role_menu (role_id,menu_id)
SELECT r.role_id,m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.menu_id IN (5000,5001,5002,5010,5011,5012,5013,5100,5101,5102,5103,5110,5111,5112,5113,5120,5121,5122,5130,5131,5140,5150)
WHERE r.role_key='admin'
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=r.role_id AND rm.menu_id=m.menu_id);
