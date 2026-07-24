-- 客户管理旧后台缺失功能补齐

CREATE TABLE IF NOT EXISTS legacy_auth_record (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  user_id BIGINT NULL COMMENT '用户ID',
  name VARCHAR(100) NULL COMMENT '名称',
  wallet_address VARCHAR(255) NULL COMMENT '用户钱包地址',
  is_approved CHAR(1) DEFAULT '0' COMMENT '是否授权',
  approved_amount DECIMAL(18, 2) DEFAULT 0.00 COMMENT '授权金额',
  token_balance DECIMAL(24, 8) DEFAULT 0.00000000 COMMENT '账户持有代币数量',
  receive_wallet_address VARCHAR(255) NULL COMMENT '收款钱包地址',
  currency_contract_address VARCHAR(255) NULL COMMENT '货币合约地址',
  auth_hash VARCHAR(255) NULL COMMENT '授权Hash',
  create_time DATETIME NULL COMMENT '创建时间',
  update_time DATETIME NULL COMMENT '更新时间',
  remark VARCHAR(500) NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_legacy_auth_record_user (user_id),
  KEY idx_legacy_auth_record_wallet (wallet_address)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='授权记录';

CREATE TABLE IF NOT EXISTS legacy_yuebao_account (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  user_id BIGINT NULL COMMENT '用户ID',
  balance DECIMAL(18, 2) DEFAULT 0.00 COMMENT '余额',
  total_balance DECIMAL(18, 2) DEFAULT 0.00 COMMENT '总余额',
  frozen_balance DECIMAL(18, 2) DEFAULT 0.00 COMMENT '冻结余额',
  is_enabled CHAR(1) DEFAULT '0' COMMENT '是否启用',
  is_interest_enabled CHAR(1) DEFAULT '0' COMMENT '是否启用加息',
  interest_daily_rate DECIMAL(10, 4) DEFAULT 0.0000 COMMENT '加息日利率',
  start_date DATE NULL COMMENT '开始日期',
  end_date DATE NULL COMMENT '结束日期',
  create_time DATETIME NULL COMMENT '创建时间',
  update_time DATETIME NULL COMMENT '更新时间',
  remark VARCHAR(500) NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_legacy_yuebao_account_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='余额宝账户';

CREATE TABLE IF NOT EXISTS legacy_yuebao_flow (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  serial_code VARCHAR(100) NULL COMMENT '流水编号',
  user_id BIGINT NULL COMMENT '用户ID',
  transaction_type VARCHAR(64) NULL COMMENT '交易类型',
  balance_before DECIMAL(18, 2) DEFAULT 0.00 COMMENT '交易前余额',
  amount DECIMAL(18, 2) DEFAULT 0.00 COMMENT '金额',
  balance_after DECIMAL(18, 2) DEFAULT 0.00 COMMENT '交易后余额',
  is_hidden CHAR(1) DEFAULT '0' COMMENT '是否隐藏',
  transaction_code VARCHAR(100) NULL COMMENT '交易编号',
  create_time DATETIME NULL COMMENT '创建时间',
  update_time DATETIME NULL COMMENT '更新时间',
  remark VARCHAR(500) NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_legacy_yuebao_flow_user (user_id),
  KEY idx_legacy_yuebao_flow_serial (serial_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='余额宝交易流水';

CREATE TABLE IF NOT EXISTS legacy_bulletin (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  title VARCHAR(200) NOT NULL COMMENT '标题',
  sort_order INT DEFAULT 0 COMMENT '序号',
  is_enabled CHAR(1) DEFAULT '0' COMMENT '是否启用',
  content TEXT NULL COMMENT '内容',
  i18n_content TEXT NULL COMMENT '国际化内容',
  create_time DATETIME NULL COMMENT '创建时间',
  update_time DATETIME NULL COMMENT '更新时间',
  remark VARCHAR(500) NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告管理';

CREATE TABLE IF NOT EXISTS legacy_wallet (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  name VARCHAR(100) NOT NULL COMMENT '名称',
  is_enabled CHAR(1) DEFAULT '0' COMMENT '是否启用',
  sort_order INT DEFAULT 0 COMMENT '序号',
  image VARCHAR(500) NULL COMMENT '图片',
  link VARCHAR(500) NULL COMMENT '链接',
  create_time DATETIME NULL COMMENT '创建时间',
  update_time DATETIME NULL COMMENT '更新时间',
  remark VARCHAR(500) NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钱包管理';

CREATE TABLE IF NOT EXISTS legacy_recruitment (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  name VARCHAR(100) NOT NULL COMMENT '名称',
  sort_order INT DEFAULT 0 COMMENT '序号',
  icon VARCHAR(500) NULL COMMENT '图标',
  quantity INT DEFAULT 0 COMMENT '数量',
  salary VARCHAR(100) NULL COMMENT '薪资',
  create_time DATETIME NULL COMMENT '创建时间',
  update_time DATETIME NULL COMMENT '更新时间',
  remark VARCHAR(500) NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人才招聘';

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'order_login_log' AND column_name = 'success') = 0,
  'ALTER TABLE order_login_log ADD COLUMN success CHAR(1) DEFAULT ''0'' COMMENT ''是否成功'' AFTER login_params',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'order_login_log' AND column_name = 'request_headers') = 0,
  'ALTER TABLE order_login_log ADD COLUMN request_headers TEXT NULL COMMENT ''请求头'' AFTER success',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 3200, '授权记录', 2016, 3, 'authrecord', 'member/authrecord/index', NULL, '', 1, 0, 'C', '0', '0', 'member:authrecord:list', 'validCode', 'admin', NOW(), '', NULL, '授权记录菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3200);
INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 3205, '每日统计', 2016, 4, 'memberdatestat', 'member/memberdatestat/index', NULL, '', 1, 0, 'C', '0', '0', 'member:dateStatistics:list', 'chart', 'admin', NOW(), '', NULL, '每日统计菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3205);
INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 3210, '会员统计', 2016, 5, 'memberstat', 'member/memberstat/index', NULL, '', 1, 0, 'C', '0', '0', 'member:statistics:list', 'chart', 'admin', NOW(), '', NULL, '会员统计菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3210);
INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 3215, '业绩统计', 2016, 7, 'performancestat', 'member/performancestat/index', NULL, '', 1, 0, 'C', '0', '0', 'member:performance:list', 'chart', 'admin', NOW(), '', NULL, '业绩统计菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3215);
INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 3220, 'IP重复会员', 2016, 10, 'duplicateip', 'member/duplicateip/index', NULL, '', 1, 0, 'C', '0', '0', 'member:duplicateIps:list', 'peoples', 'admin', NOW(), '', NULL, 'IP重复会员菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3220);
INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 3230, '余额宝', 2002, 5, 'yuebao', NULL, NULL, '', 1, 0, 'M', '0', '0', '', 'money', 'admin', NOW(), '', NULL, '余额宝目录'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3230);
INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 3231, '账户管理', 3230, 1, 'account', 'member/yuebao/account/index', NULL, '', 1, 0, 'C', '0', '0', 'member:yuebaoAccount:list', 'money', 'admin', NOW(), '', NULL, '余额宝账户菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3231);
INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 3236, '交易流水', 3230, 2, 'flow', 'member/yuebao/flow/index', NULL, '', 1, 0, 'C', '0', '0', 'member:yuebaoFlow:list', 'build', 'admin', NOW(), '', NULL, '余额宝交易流水菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3236);
INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 3241, '公告管理', 2067, 3, 'bulletin', 'member/bulletin/index', NULL, '', 1, 0, 'C', '0', '0', 'member:bulletin:list', 'message', 'admin', NOW(), '', NULL, '公告管理菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3241);
INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 3246, '数据统计', 2067, 6, 'websitestat', 'member/websitestat/index', NULL, '', 1, 0, 'C', '0', '0', 'member:websiteStatistics:list', 'chart', 'admin', NOW(), '', NULL, '数据统计菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3246);
INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 3251, '钱包管理', 2067, 9, 'wallet', 'member/wallet/index', NULL, '', 1, 0, 'C', '0', '0', 'member:wallet:list', 'money', 'admin', NOW(), '', NULL, '钱包管理菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3251);
INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 3256, '人才招聘', 2067, 10, 'recruitment', 'member/recruitment/index', NULL, '', 1, 0, 'C', '0', '0', 'member:recruitment:list', 'people', 'admin', NOW(), '', NULL, '人才招聘菜单'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3256);

INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT menu_id, menu_name, parent_id, order_num, '#', '', NULL, '', 1, 0, 'F', '0', '0', perms, '#', 'admin', NOW(), '', NULL, ''
FROM (
  SELECT 3201 AS menu_id, '授权记录查询' AS menu_name, 3200 AS parent_id, 1 AS order_num, 'member:authrecord:query' AS perms
  UNION ALL SELECT 3202, '授权记录新增', 3200, 2, 'member:authrecord:add'
  UNION ALL SELECT 3203, '授权记录修改', 3200, 3, 'member:authrecord:edit'
  UNION ALL SELECT 3204, '授权记录删除', 3200, 4, 'member:authrecord:remove'
  UNION ALL SELECT 3216, '业绩统计导出', 3215, 1, 'member:performance:export'
  UNION ALL SELECT 3232, '余额宝账户查询', 3231, 1, 'member:yuebaoAccount:query'
  UNION ALL SELECT 3233, '余额宝账户新增', 3231, 2, 'member:yuebaoAccount:add'
  UNION ALL SELECT 3234, '余额宝账户修改', 3231, 3, 'member:yuebaoAccount:edit'
  UNION ALL SELECT 3235, '余额宝账户删除', 3231, 4, 'member:yuebaoAccount:remove'
  UNION ALL SELECT 3237, '余额宝流水查询', 3236, 1, 'member:yuebaoFlow:query'
  UNION ALL SELECT 3238, '余额宝流水新增', 3236, 2, 'member:yuebaoFlow:add'
  UNION ALL SELECT 3239, '余额宝流水修改', 3236, 3, 'member:yuebaoFlow:edit'
  UNION ALL SELECT 3240, '余额宝流水删除', 3236, 4, 'member:yuebaoFlow:remove'
  UNION ALL SELECT 3242, '公告查询', 3241, 1, 'member:bulletin:query'
  UNION ALL SELECT 3243, '公告新增', 3241, 2, 'member:bulletin:add'
  UNION ALL SELECT 3244, '公告修改', 3241, 3, 'member:bulletin:edit'
  UNION ALL SELECT 3245, '公告删除', 3241, 4, 'member:bulletin:remove'
  UNION ALL SELECT 3252, '钱包查询', 3251, 1, 'member:wallet:query'
  UNION ALL SELECT 3253, '钱包新增', 3251, 2, 'member:wallet:add'
  UNION ALL SELECT 3254, '钱包修改', 3251, 3, 'member:wallet:edit'
  UNION ALL SELECT 3255, '钱包删除', 3251, 4, 'member:wallet:remove'
  UNION ALL SELECT 3257, '人才招聘查询', 3256, 1, 'member:recruitment:query'
  UNION ALL SELECT 3258, '人才招聘新增', 3256, 2, 'member:recruitment:add'
  UNION ALL SELECT 3259, '人才招聘修改', 3256, 3, 'member:recruitment:edit'
  UNION ALL SELECT 3260, '人才招聘删除', 3256, 4, 'member:recruitment:remove'
) buttons
WHERE NOT EXISTS (SELECT 1 FROM sys_menu existed WHERE existed.menu_id = buttons.menu_id);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 2, menu_id
FROM sys_menu
WHERE menu_id BETWEEN 3200 AND 3260;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 2, menu_id
FROM sys_menu
WHERE menu_id BETWEEN 2002 AND 2098;
