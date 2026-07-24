SET NAMES utf8mb4;

-- 积分商城、活动管理、官网管理旧后台功能补齐。
-- 本脚本可重复执行，不导入旧站业务数据。

-- 会员管理旧站字段。使用信息架构判断，保证在历史库重复执行安全。
DELIMITER $$
DROP PROCEDURE IF EXISTS add_order_user_alignment_columns$$
CREATE PROCEDURE add_order_user_alignment_columns()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'order_user' AND column_name = 'web3_auth_enabled') THEN
    ALTER TABLE order_user ADD COLUMN web3_auth_enabled CHAR(1) NOT NULL DEFAULT '1' COMMENT '启用Web3授权';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'order_user' AND column_name = 'is_invalid') THEN
    ALTER TABLE order_user ADD COLUMN is_invalid CHAR(1) NOT NULL DEFAULT '1' COMMENT '是否无效';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'order_user' AND column_name = 'is_activity') THEN
    ALTER TABLE order_user ADD COLUMN is_activity CHAR(1) NOT NULL DEFAULT '0' COMMENT '是否活动';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'order_user' AND column_name = 'withdrawal_password_fail_limit') THEN
    ALTER TABLE order_user ADD COLUMN withdrawal_password_fail_limit INT NOT NULL DEFAULT 0 COMMENT '禁止提现所需交易密码失败次数';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'order_user' AND column_name = 'withdrawal_password_fail_count') THEN
    ALTER TABLE order_user ADD COLUMN withdrawal_password_fail_count INT NOT NULL DEFAULT 0 COMMENT '提现交易密码连续失败次数';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'order_user' AND column_name = 'max_single_withdrawal') THEN
    ALTER TABLE order_user ADD COLUMN max_single_withdrawal DECIMAL(20,2) NOT NULL DEFAULT 0 COMMENT '单次最大提现金额';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'order_user' AND column_name = 'verify_identity_before_task') THEN
    ALTER TABLE order_user ADD COLUMN verify_identity_before_task CHAR(1) NOT NULL DEFAULT '1' COMMENT '任务前验证身份';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'order_user' AND column_name = 'user_contract_enabled') THEN
    ALTER TABLE order_user ADD COLUMN user_contract_enabled CHAR(1) NOT NULL DEFAULT '1' COMMENT '启用用户合同';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'order_user' AND column_name = 'user_contract_signed') THEN
    ALTER TABLE order_user ADD COLUMN user_contract_signed CHAR(1) NOT NULL DEFAULT '1' COMMENT '签署用户合同';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'order_user' AND column_name = 'formal_contract_enabled') THEN
    ALTER TABLE order_user ADD COLUMN formal_contract_enabled CHAR(1) NOT NULL DEFAULT '1' COMMENT '启用正式合同';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'order_user' AND column_name = 'formal_contract_signed') THEN
    ALTER TABLE order_user ADD COLUMN formal_contract_signed CHAR(1) NOT NULL DEFAULT '1' COMMENT '签署正式合同';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'order_user' AND column_name = 'withdrawal_block_remark') THEN
    ALTER TABLE order_user ADD COLUMN withdrawal_block_remark VARCHAR(500) NULL COMMENT '禁止提现备注';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'order_user' AND column_name = 'sign_days') THEN
    ALTER TABLE order_user ADD COLUMN sign_days INT NOT NULL DEFAULT 0 COMMENT '签到天数';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'order_user' AND column_name = 'today_sign_count') THEN
    ALTER TABLE order_user ADD COLUMN today_sign_count INT NOT NULL DEFAULT 0 COMMENT '今日签到次数';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'order_user' AND column_name = 'total_sign_days') THEN
    ALTER TABLE order_user ADD COLUMN total_sign_days INT NOT NULL DEFAULT 0 COMMENT '累计签到天数';
  END IF;
END$$
CALL add_order_user_alignment_columns()$$
DROP PROCEDURE add_order_user_alignment_columns$$
DELIMITER ;

CREATE TABLE IF NOT EXISTS points_gift (
  id BIGINT NOT NULL AUTO_INCREMENT,
  title VARCHAR(255) NOT NULL,
  sub_title VARCHAR(255) NULL,
  kind CHAR(1) NOT NULL DEFAULT '1' COMMENT '1礼品 2现金',
  is_enabled CHAR(1) NOT NULL DEFAULT '1',
  points DECIMAL(18,2) NOT NULL DEFAULT 0,
  price DECIMAL(18,2) NOT NULL DEFAULT 0,
  image VARCHAR(500) NULL,
  stock BIGINT NOT NULL DEFAULT 0,
  available_stock BIGINT NOT NULL DEFAULT 0,
  frozen_stock BIGINT NOT NULL DEFAULT 0,
  sale_volume BIGINT NOT NULL DEFAULT 0,
  version INT NOT NULL DEFAULT 0,
  create_by VARCHAR(64) NULL,
  create_time DATETIME NULL,
  update_by VARCHAR(64) NULL,
  update_time DATETIME NULL,
  remark VARCHAR(500) NULL,
  PRIMARY KEY (id),
  KEY idx_points_gift_title (title),
  KEY idx_points_gift_enabled (is_enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分礼品';

CREATE TABLE IF NOT EXISTS points_account (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  points DECIMAL(18,2) NOT NULL DEFAULT 0,
  available_points DECIMAL(18,2) NOT NULL DEFAULT 0,
  frozen_points DECIMAL(18,2) NOT NULL DEFAULT 0,
  version INT NOT NULL DEFAULT 0,
  create_time DATETIME NULL,
  update_time DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_points_account_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分账户';

CREATE TABLE IF NOT EXISTS points_flow (
  id BIGINT NOT NULL AUTO_INCREMENT,
  flow_no VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL,
  username_snapshot VARCHAR(100) NULL,
  phone_snapshot VARCHAR(64) NULL,
  before_points DECIMAL(18,2) NOT NULL DEFAULT 0,
  change_points DECIMAL(18,2) NOT NULL DEFAULT 0,
  after_points DECIMAL(18,2) NOT NULL DEFAULT 0,
  transaction_no VARCHAR(64) NULL,
  create_by VARCHAR(64) NULL,
  create_time DATETIME NULL,
  remark VARCHAR(500) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_points_flow_no (flow_no),
  KEY idx_points_flow_user (user_id),
  KEY idx_points_flow_transaction (transaction_no),
  KEY idx_points_flow_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分流水';

CREATE TABLE IF NOT EXISTS points_gift_order (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_no VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL,
  username_snapshot VARCHAR(100) NULL,
  gift_id BIGINT NULL,
  gift_image VARCHAR(500) NULL,
  gift_title VARCHAR(255) NULL,
  points DECIMAL(18,2) NOT NULL DEFAULT 0,
  status CHAR(1) NOT NULL DEFAULT '1' COMMENT '1待处理 2已发货 3已收货 4已取消',
  consignee VARCHAR(100) NULL,
  telephone VARCHAR(64) NULL,
  delivery_address VARCHAR(500) NULL,
  version INT NOT NULL DEFAULT 0,
  create_time DATETIME NULL,
  update_time DATETIME NULL,
  remark VARCHAR(500) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_points_gift_order_no (order_no),
  KEY idx_points_gift_order_user (user_id),
  KEY idx_points_gift_order_status (status),
  KEY idx_points_gift_order_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分礼品订单';

CREATE TABLE IF NOT EXISTS activity_info (
  id BIGINT NOT NULL AUTO_INCREMENT,
  title VARCHAR(255) NOT NULL,
  sub_title VARCHAR(255) NULL,
  is_enabled CHAR(1) NOT NULL DEFAULT '1',
  sort_order INT NOT NULL DEFAULT 0,
  image VARCHAR(500) NULL,
  no_winning_tips VARCHAR(500) NULL,
  rule_content LONGTEXT NULL,
  description LONGTEXT NULL,
  create_by VARCHAR(64) NULL,
  create_time DATETIME NULL,
  update_by VARCHAR(64) NULL,
  update_time DATETIME NULL,
  remark VARCHAR(500) NULL,
  PRIMARY KEY (id),
  KEY idx_activity_info_title (title),
  KEY idx_activity_info_enabled (is_enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动';

CREATE TABLE IF NOT EXISTS activity_prize (
  id BIGINT NOT NULL AUTO_INCREMENT,
  activity_id BIGINT NOT NULL,
  probability DECIMAL(8,4) NOT NULL DEFAULT 0,
  title VARCHAR(255) NOT NULL,
  sub_title VARCHAR(255) NULL,
  kind CHAR(1) NOT NULL DEFAULT '1' COMMENT '1礼品 2现金 3抽奖次数',
  price DECIMAL(18,2) NOT NULL DEFAULT 0,
  is_enabled CHAR(1) NOT NULL DEFAULT '1',
  sort_order INT NOT NULL DEFAULT 0,
  image VARCHAR(500) NULL,
  create_by VARCHAR(64) NULL,
  create_time DATETIME NULL,
  update_by VARCHAR(64) NULL,
  update_time DATETIME NULL,
  remark VARCHAR(500) NULL,
  PRIMARY KEY (id),
  KEY idx_activity_prize_activity (activity_id),
  KEY idx_activity_prize_enabled (activity_id, is_enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动奖品';

CREATE TABLE IF NOT EXISTS activity_account (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  available_times BIGINT NOT NULL DEFAULT 0,
  used_times BIGINT NOT NULL DEFAULT 0,
  version INT NOT NULL DEFAULT 0,
  create_time DATETIME NULL,
  update_time DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_activity_account_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动账户';

CREATE TABLE IF NOT EXISTS activity_account_prize (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  activity_id BIGINT NOT NULL,
  prize_id BIGINT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  is_winning CHAR(1) NOT NULL DEFAULT '0',
  create_time DATETIME NULL,
  update_time DATETIME NULL,
  remark VARCHAR(500) NULL,
  PRIMARY KEY (id),
  KEY idx_activity_account_prize_user (user_id),
  KEY idx_activity_account_prize_activity (activity_id),
  UNIQUE KEY uk_activity_account_prize_sequence (user_id, activity_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户活动奖品设置';

CREATE TABLE IF NOT EXISTS activity_partner (
  id BIGINT NOT NULL AUTO_INCREMENT,
  activity_id BIGINT NOT NULL,
  activity_title VARCHAR(255) NULL,
  user_id BIGINT NOT NULL,
  username_snapshot VARCHAR(100) NULL,
  phone_snapshot VARCHAR(64) NULL,
  is_winning CHAR(1) NOT NULL DEFAULT '0',
  prize_id BIGINT NULL,
  prize_image VARCHAR(500) NULL,
  prize_title VARCHAR(255) NULL,
  prize_kind CHAR(1) NULL,
  prize_price DECIMAL(18,2) NOT NULL DEFAULT 0,
  is_hidden CHAR(1) NOT NULL DEFAULT '0',
  is_verified CHAR(1) NOT NULL DEFAULT '0',
  create_time DATETIME NULL,
  update_time DATETIME NULL,
  remark VARCHAR(500) NULL,
  PRIMARY KEY (id),
  KEY idx_activity_partner_activity (activity_id),
  KEY idx_activity_partner_user (user_id),
  KEY idx_activity_partner_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动参与记录';

DELIMITER $$
DROP PROCEDURE IF EXISTS add_commerce_alignment_indexes$$
CREATE PROCEDURE add_commerce_alignment_indexes()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'activity_account_prize' AND index_name = 'uk_activity_account_prize_sequence') THEN
    ALTER TABLE activity_account_prize ADD UNIQUE KEY uk_activity_account_prize_sequence (user_id, activity_id, sort_order);
  END IF;
END$$
CALL add_commerce_alignment_indexes()$$
DROP PROCEDURE add_commerce_alignment_indexes$$
DELIMITER ;

CREATE TABLE IF NOT EXISTS website_customer (
  id BIGINT NOT NULL AUTO_INCREMENT,
  first_name VARCHAR(100) NOT NULL,
  last_name VARCHAR(100) NOT NULL,
  company VARCHAR(255) NULL,
  email VARCHAR(255) NOT NULL,
  country VARCHAR(100) NOT NULL,
  content TEXT NULL,
  create_time DATETIME NULL,
  PRIMARY KEY (id),
  KEY idx_website_customer_name (first_name, last_name),
  KEY idx_website_customer_company (company),
  KEY idx_website_customer_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='官网客户';

-- 顶部模块菜单。
INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 4000, '积分商城', 0, 3, 'points', NULL, NULL, '', 1, 0, 'M', '0', '0', '', 'shopping', 'admin', NOW(), '', NULL, '积分商城'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 4000);
INSERT INTO sys_menu SELECT 4001, '礼品管理', 4000, 1, 'gift', NULL, NULL, '', 1, 0, 'M', '0', '0', '', 'gift', 'admin', NOW(), '', NULL, '礼品管理目录'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 4001);
INSERT INTO sys_menu SELECT 4002, '礼品管理', 4001, 1, 'gifts', 'points/gifts/index', NULL, '', 1, 0, 'C', '0', '0', 'points:gift:list', 'gift', 'admin', NOW(), '', NULL, '礼品管理页面'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 4002);
INSERT INTO sys_menu SELECT 4003, '积分账户', 4000, 2, 'account', NULL, NULL, '', 1, 0, 'M', '0', '0', '', 'user', 'admin', NOW(), '', NULL, '积分账户目录'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 4003);
INSERT INTO sys_menu SELECT 4004, '积分账户', 4003, 1, 'accounts', 'points/accounts/index', NULL, '', 1, 0, 'C', '0', '0', 'points:account:list', 'user', 'admin', NOW(), '', NULL, '积分账户页面'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 4004);
INSERT INTO sys_menu SELECT 4005, '积分流水', 4003, 2, 'flows', 'points/flows/index', NULL, '', 1, 0, 'C', '0', '0', 'points:flow:list', 'list', 'admin', NOW(), '', NULL, '积分流水页面'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 4005);
INSERT INTO sys_menu SELECT 4006, '积分订单', 4000, 3, 'giftOrder', NULL, NULL, '', 1, 0, 'M', '0', '0', '', 'ordered-list', 'admin', NOW(), '', NULL, '积分订单目录'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 4006);
INSERT INTO sys_menu SELECT 4007, '积分订单', 4006, 1, 'orders', 'points/orders/index', NULL, '', 1, 0, 'C', '0', '0', 'points:giftOrder:list', 'ordered-list', 'admin', NOW(), '', NULL, '积分订单页面'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 4007);

INSERT INTO sys_menu SELECT 4100, '活动管理', 0, 4, 'activities', NULL, NULL, '', 1, 0, 'M', '0', '0', '', 'guide', 'admin', NOW(), '', NULL, '活动管理'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 4100);
INSERT INTO sys_menu SELECT 4101, '活动管理', 4100, 1, 'activity', NULL, NULL, '', 1, 0, 'M', '0', '0', '', 'guide', 'admin', NOW(), '', NULL, '活动管理目录'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 4101);
INSERT INTO sys_menu SELECT 4102, '活动管理', 4101, 1, 'activities', 'activities/activities/index', NULL, '', 1, 0, 'C', '0', '0', 'activity:activity:list', 'guide', 'admin', NOW(), '', NULL, '活动管理页面'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 4102);
INSERT INTO sys_menu SELECT 4103, '奖品管理', 4101, 2, 'prizes', 'activities/prizes/index', NULL, '', 1, 0, 'C', '0', '0', 'activity:prize:list', 'star', 'admin', NOW(), '', NULL, '活动奖品页面'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 4103);
INSERT INTO sys_menu SELECT 4104, '活动账户', 4100, 2, 'account', NULL, NULL, '', 1, 0, 'M', '0', '0', '', 'user', 'admin', NOW(), '', NULL, '活动账户目录'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 4104);
INSERT INTO sys_menu SELECT 4105, '活动账户', 4104, 1, 'accounts', 'activities/accounts/index', NULL, '', 1, 0, 'C', '0', '0', 'activity:account:list', 'user', 'admin', NOW(), '', NULL, '活动账户页面'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 4105);
INSERT INTO sys_menu SELECT 4106, '参与记录', 4104, 2, 'partners', 'activities/partners/index', NULL, '', 1, 0, 'C', '0', '0', 'activity:partner:list', 'list', 'admin', NOW(), '', NULL, '活动参与记录页面'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 4106);

INSERT INTO sys_menu SELECT 4200, '官网管理', 0, 5, 'websites', NULL, NULL, '', 1, 0, 'M', '0', '0', '', 'international', 'admin', NOW(), '', NULL, '官网管理'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 4200);
INSERT INTO sys_menu SELECT 4201, '官网管理', 4200, 1, 'website', NULL, NULL, '', 1, 0, 'M', '0', '0', '', 'international', 'admin', NOW(), '', NULL, '官网管理目录'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 4201);
INSERT INTO sys_menu SELECT 4202, '客户管理', 4201, 1, 'customers', 'websites/customers/index', NULL, '', 1, 0, 'C', '0', '0', 'website:customer:list', 'peoples', 'admin', NOW(), '', NULL, '官网客户页面'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 4202);

-- 按钮权限。
INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT menu_id, menu_name, parent_id, order_num, '#', '', NULL, '', 1, 0, 'F', '0', '0', perms, '#', 'admin', NOW(), '', NULL, ''
FROM (
  SELECT 4010 menu_id, '礼品查询' menu_name, 4002 parent_id, 1 order_num, 'points:gift:query' perms
  UNION ALL SELECT 4011, '礼品新增', 4002, 2, 'points:gift:add'
  UNION ALL SELECT 4012, '礼品修改', 4002, 3, 'points:gift:edit'
  UNION ALL SELECT 4013, '礼品删除', 4002, 4, 'points:gift:remove'
  UNION ALL SELECT 4014, '礼品库存调整', 4002, 5, 'points:gift:adjust'
  UNION ALL SELECT 4015, '积分调整', 4004, 1, 'points:account:adjust'
  UNION ALL SELECT 4016, '订单修改', 4007, 1, 'points:giftOrder:update'
  UNION ALL SELECT 4017, '订单发货', 4007, 2, 'points:giftOrder:ship'
  UNION ALL SELECT 4018, '订单收货', 4007, 3, 'points:giftOrder:receive'
  UNION ALL SELECT 4019, '订单取消', 4007, 4, 'points:giftOrder:cancel'
  UNION ALL SELECT 4110, '活动查询', 4102, 1, 'activity:activity:query'
  UNION ALL SELECT 4111, '活动新增', 4102, 2, 'activity:activity:add'
  UNION ALL SELECT 4112, '活动修改', 4102, 3, 'activity:activity:edit'
  UNION ALL SELECT 4113, '活动删除', 4102, 4, 'activity:activity:remove'
  UNION ALL SELECT 4114, '奖品查询', 4103, 1, 'activity:prize:query'
  UNION ALL SELECT 4115, '奖品新增', 4103, 2, 'activity:prize:add'
  UNION ALL SELECT 4116, '奖品修改', 4103, 3, 'activity:prize:edit'
  UNION ALL SELECT 4117, '奖品删除', 4103, 4, 'activity:prize:remove'
  UNION ALL SELECT 4118, '活动次数调整', 4105, 1, 'activity:account:adjust'
  UNION ALL SELECT 4119, '用户奖品设置', 4105, 2, 'activity:account:prize'
  UNION ALL SELECT 4120, '参与记录显示隐藏', 4106, 1, 'activity:partner:hidden'
  UNION ALL SELECT 4210, '官网客户查询', 4202, 1, 'website:customer:query'
  UNION ALL SELECT 4211, '官网客户新增', 4202, 2, 'website:customer:add'
  UNION ALL SELECT 4212, '官网客户删除', 4202, 3, 'website:customer:remove'
) buttons
WHERE NOT EXISTS (SELECT 1 FROM sys_menu existed WHERE existed.menu_id = buttons.menu_id);

-- 调整顶部和客户菜单层级；系统功能不再挂在客户网站管理下。
UPDATE sys_menu SET order_num = 2 WHERE menu_id = 2002;
UPDATE sys_menu SET parent_id = 1, order_num = 6 WHERE menu_id = 107;
UPDATE sys_menu SET parent_id = 1, order_num = 7 WHERE menu_id = 2093;
UPDATE sys_menu SET order_num = 1 WHERE menu_id IN (2004, 2023, 2036, 2043, 2068);
UPDATE sys_menu SET order_num = 2 WHERE menu_id IN (2010, 2017, 2030, 2074);
UPDATE sys_menu SET order_num = 3 WHERE menu_id IN (3200, 2061, 3241);
UPDATE sys_menu SET order_num = 4 WHERE menu_id IN (3205, 2087);
UPDATE sys_menu SET order_num = 5 WHERE menu_id IN (3210, 3246);
UPDATE sys_menu SET order_num = 6 WHERE menu_id IN (2049, 2080);
UPDATE sys_menu SET order_num = 7 WHERE menu_id IN (3215, 3251);
UPDATE sys_menu SET order_num = 8 WHERE menu_id IN (2055, 3256);
UPDATE sys_menu SET order_num = 9 WHERE menu_id = 3220;
UPDATE sys_menu SET order_num = 4 WHERE menu_id = 2042;
UPDATE sys_menu SET order_num = 6 WHERE menu_id = 2067;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 2, menu_id FROM sys_menu WHERE menu_id IN (
  4000, 4001, 4002, 4003, 4004, 4005, 4006, 4007,
  4010, 4011, 4012, 4013, 4014, 4015, 4016, 4017, 4018, 4019,
  4100, 4101, 4102, 4103, 4104, 4105, 4106,
  4110, 4111, 4112, 4113, 4114, 4115, 4116, 4117, 4118, 4119, 4120,
  4200, 4201, 4202, 4210, 4211, 4212
);

INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 3999, '会员登录解冻', 2023, 7, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'member:orderuser:unlock', '#', 'admin', NOW(), '', NULL, '会员批量登录解冻'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 3999);
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (2, 3999);

-- 已存在记录也强制校正名称，保证脚本可修复历史乱码或旧临时菜单。
UPDATE sys_menu SET menu_name = CASE menu_id
  WHEN 4000 THEN '积分商城' WHEN 4001 THEN '礼品管理' WHEN 4002 THEN '礼品管理'
  WHEN 4003 THEN '积分账户' WHEN 4004 THEN '积分账户' WHEN 4005 THEN '积分流水'
  WHEN 4006 THEN '积分订单' WHEN 4007 THEN '积分订单'
  WHEN 4010 THEN '礼品查询' WHEN 4011 THEN '礼品新增' WHEN 4012 THEN '礼品修改'
  WHEN 4013 THEN '礼品删除' WHEN 4014 THEN '礼品库存调整' WHEN 4015 THEN '积分调整'
  WHEN 4016 THEN '订单修改' WHEN 4017 THEN '订单发货' WHEN 4018 THEN '订单收货' WHEN 4019 THEN '订单取消'
  WHEN 4100 THEN '活动管理' WHEN 4101 THEN '活动管理' WHEN 4102 THEN '活动管理'
  WHEN 4103 THEN '奖品管理' WHEN 4104 THEN '活动账户' WHEN 4105 THEN '活动账户' WHEN 4106 THEN '参与记录'
  WHEN 4110 THEN '活动查询' WHEN 4111 THEN '活动新增' WHEN 4112 THEN '活动修改' WHEN 4113 THEN '活动删除'
  WHEN 4114 THEN '奖品查询' WHEN 4115 THEN '奖品新增' WHEN 4116 THEN '奖品修改' WHEN 4117 THEN '奖品删除'
  WHEN 4118 THEN '活动次数调整' WHEN 4119 THEN '用户奖品设置' WHEN 4120 THEN '参与记录显示隐藏'
  WHEN 4200 THEN '官网管理' WHEN 4201 THEN '官网管理' WHEN 4202 THEN '客户管理'
  WHEN 4210 THEN '官网客户查询' WHEN 4211 THEN '官网客户新增' WHEN 4212 THEN '官网客户删除'
  ELSE menu_name END
WHERE menu_id IN (
  4000, 4001, 4002, 4003, 4004, 4005, 4006, 4007,
  4010, 4011, 4012, 4013, 4014, 4015, 4016, 4017, 4018, 4019,
  4100, 4101, 4102, 4103, 4104, 4105, 4106,
  4110, 4111, 4112, 4113, 4114, 4115, 4116, 4117, 4118, 4119, 4120,
  4200, 4201, 4202, 4210, 4211, 4212
);

UPDATE sys_menu SET route_name = CASE menu_id
  WHEN 4000 THEN 'PointsRoot' WHEN 4001 THEN 'PointsGiftGroup' WHEN 4002 THEN 'PointsGiftList'
  WHEN 4003 THEN 'PointsAccountGroup' WHEN 4004 THEN 'PointsAccountList' WHEN 4005 THEN 'PointsFlowList'
  WHEN 4006 THEN 'PointsOrderGroup' WHEN 4007 THEN 'PointsOrderList'
  WHEN 4100 THEN 'ActivitiesRoot' WHEN 4101 THEN 'ActivityGroup' WHEN 4102 THEN 'ActivityList'
  WHEN 4103 THEN 'ActivityPrizeList' WHEN 4104 THEN 'ActivityAccountGroup'
  WHEN 4105 THEN 'ActivityAccountList' WHEN 4106 THEN 'ActivityPartnerList'
  WHEN 4200 THEN 'WebsitesRoot' WHEN 4201 THEN 'WebsiteGroup' WHEN 4202 THEN 'WebsiteCustomerList'
  ELSE route_name END
WHERE menu_id IN (
  4000, 4001, 4002, 4003, 4004, 4005, 4006, 4007,
  4010, 4011, 4012, 4013, 4014, 4015, 4016, 4017, 4018, 4019,
  4100, 4101, 4102, 4103, 4104, 4105, 4106,
  4110, 4111, 4112, 4113, 4114, 4115, 4116, 4117, 4118, 4119, 4120,
  4200, 4201, 4202, 4210, 4211, 4212
);
