SET NAMES utf8mb4;

-- 系统管理旧站对齐：仅创建结构，不调整菜单，不回填历史文件。
DELIMITER $$
DROP PROCEDURE IF EXISTS add_system_alignment_columns$$
CREATE PROCEDURE add_system_alignment_columns()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_dept' AND column_name = 'remark') THEN
    ALTER TABLE sys_dept ADD COLUMN remark VARCHAR(500) NULL COMMENT '备注' AFTER update_time;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_role' AND column_name = 'hide_phone') THEN
    ALTER TABLE sys_role ADD COLUMN hide_phone CHAR(1) NOT NULL DEFAULT 'N' COMMENT '是否隐藏手机号' AFTER status;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_role' AND column_name = 'is_builtin') THEN
    ALTER TABLE sys_role ADD COLUMN is_builtin CHAR(1) NOT NULL DEFAULT 'N' COMMENT '是否内置' AFTER hide_phone;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_oper_log' AND column_name = 'resource_code') THEN
    ALTER TABLE sys_oper_log ADD COLUMN resource_code VARCHAR(128) NULL COMMENT '资源代码' AFTER title;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_oper_log' AND column_name = 'request_headers') THEN
    ALTER TABLE sys_oper_log ADD COLUMN request_headers TEXT NULL COMMENT '请求头' AFTER oper_url;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_oper_log' AND column_name = 'query_string') THEN
    ALTER TABLE sys_oper_log ADD COLUMN query_string TEXT NULL COMMENT '查询字符串' AFTER request_headers;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_oper_log' AND column_name = 'request_params') THEN
    ALTER TABLE sys_oper_log ADD COLUMN request_params TEXT NULL COMMENT '请求参数' AFTER query_string;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_oper_log' AND column_name = 'request_body') THEN
    ALTER TABLE sys_oper_log ADD COLUMN request_body MEDIUMTEXT NULL COMMENT '请求体' AFTER request_params;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_oper_log' AND column_name = 'message') THEN
    ALTER TABLE sys_oper_log ADD COLUMN message TEXT NULL COMMENT '消息' AFTER error_msg;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_logininfor' AND column_name = 'request_headers') THEN
    ALTER TABLE sys_logininfor ADD COLUMN request_headers TEXT NULL COMMENT '请求头' AFTER msg;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_logininfor' AND column_name = 'request_params') THEN
    ALTER TABLE sys_logininfor ADD COLUMN request_params TEXT NULL COMMENT '登录参数' AFTER request_headers;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_role_data_rule' AND column_name = 'scope_type' AND character_maximum_length < 20) THEN
    ALTER TABLE sys_role_data_rule MODIFY COLUMN scope_type VARCHAR(20) NOT NULL COMMENT 'ALL/DEPT_AND_CHILD/DEPT/SELF';
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'is_locked') THEN
    UPDATE sys_user
       SET is_locked = CASE
         WHEN is_locked = 'Y' THEN '1'
         WHEN is_locked = 'N' OR is_locked IS NULL OR is_locked = '' THEN '0'
         ELSE is_locked
       END;
    ALTER TABLE sys_user MODIFY COLUMN is_locked CHAR(1) NOT NULL DEFAULT '1' COMMENT '是否冻结：0为冻结，1为未冻结';
  END IF;
END$$
CALL add_system_alignment_columns()$$
DROP PROCEDURE add_system_alignment_columns$$
DELIMITER ;

CREATE TABLE IF NOT EXISTS sys_permission_strategy (
  strategy_id BIGINT NOT NULL AUTO_INCREMENT,
  strategy_code VARCHAR(100) NOT NULL,
  strategy_name VARCHAR(100) NOT NULL,
  create_by VARCHAR(64) NULL,
  create_time DATETIME NULL,
  update_by VARCHAR(64) NULL,
  update_time DATETIME NULL,
  remark VARCHAR(500) NULL,
  PRIMARY KEY (strategy_id),
  UNIQUE KEY uk_sys_strategy_code (strategy_code),
  KEY idx_sys_strategy_name (strategy_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限策略';

CREATE TABLE IF NOT EXISTS sys_strategy_menu (
  strategy_id BIGINT NOT NULL,
  menu_id BIGINT NOT NULL,
  PRIMARY KEY (strategy_id, menu_id),
  KEY idx_sys_strategy_menu_menu (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='策略资源关联';

CREATE TABLE IF NOT EXISTS sys_permission_group (
  group_id BIGINT NOT NULL AUTO_INCREMENT,
  group_name VARCHAR(100) NOT NULL,
  create_by VARCHAR(64) NULL,
  create_time DATETIME NULL,
  update_by VARCHAR(64) NULL,
  update_time DATETIME NULL,
  remark VARCHAR(500) NULL,
  PRIMARY KEY (group_id),
  UNIQUE KEY uk_sys_permission_group_name (group_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限分组';

CREATE TABLE IF NOT EXISTS sys_group_strategy (
  group_id BIGINT NOT NULL,
  strategy_id BIGINT NOT NULL,
  PRIMARY KEY (group_id, strategy_id),
  KEY idx_sys_group_strategy_strategy (strategy_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分组策略关联';

CREATE TABLE IF NOT EXISTS sys_group_menu (
  group_id BIGINT NOT NULL,
  menu_id BIGINT NOT NULL,
  PRIMARY KEY (group_id, menu_id),
  KEY idx_sys_group_menu_menu (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分组直接资源';

CREATE TABLE IF NOT EXISTS sys_user_group (
  user_id BIGINT NOT NULL,
  group_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, group_id),
  KEY idx_sys_user_group_group (group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户权限分组';

CREATE TABLE IF NOT EXISTS sys_post_role (
  post_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (post_id, role_id),
  KEY idx_sys_post_role_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='职位角色关联';

CREATE TABLE IF NOT EXISTS sys_role_strategy (
  role_id BIGINT NOT NULL,
  strategy_id BIGINT NOT NULL,
  PRIMARY KEY (role_id, strategy_id),
  KEY idx_sys_role_strategy_strategy (strategy_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色策略关联';

CREATE TABLE IF NOT EXISTS sys_role_data_rule (
  role_id BIGINT NOT NULL,
  menu_id BIGINT NOT NULL,
  scope_type VARCHAR(20) NOT NULL COMMENT 'ALL/DEPT_AND_CHILD/DEPT/SELF',
  PRIMARY KEY (role_id, menu_id),
  KEY idx_sys_role_data_rule_menu (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色逐资源数据权限';

CREATE TABLE IF NOT EXISTS sys_file (
  file_id BIGINT NOT NULL AUTO_INCREMENT,
  bucket VARCHAR(64) NOT NULL DEFAULT 'local',
  file_type VARCHAR(32) NULL,
  file_hash CHAR(64) NOT NULL,
  content_type VARCHAR(128) NULL,
  storage_path VARCHAR(500) NOT NULL,
  file_url VARCHAR(1000) NOT NULL,
  file_size BIGINT NOT NULL DEFAULT 0,
  create_by VARCHAR(64) NULL,
  create_time DATETIME NULL,
  remark VARCHAR(500) NULL,
  PRIMARY KEY (file_id),
  UNIQUE KEY uk_sys_file_bucket_hash (bucket, file_hash),
  KEY idx_sys_file_type (file_type),
  KEY idx_sys_file_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统文件';

CREATE TABLE IF NOT EXISTS sys_file_reference (
  reference_id BIGINT NOT NULL AUTO_INCREMENT,
  file_id BIGINT NOT NULL,
  reference_name VARCHAR(255) NOT NULL,
  reference_type VARCHAR(64) NOT NULL DEFAULT 'ADMIN_UPLOAD',
  reference_target_id VARCHAR(128) NULL,
  create_by VARCHAR(64) NULL,
  create_time DATETIME NULL,
  remark VARCHAR(500) NULL,
  PRIMARY KEY (reference_id),
  KEY idx_sys_file_reference_file (file_id),
  KEY idx_sys_file_reference_type_target (reference_type, reference_target_id),
  KEY idx_sys_file_reference_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件引用';

UPDATE sys_file_reference SET reference_type='ADMIN_UPLOAD' WHERE reference_type='GENERAL';

-- 保护现有系统内置角色；不修改其他角色。
UPDATE sys_role SET is_builtin = 'Y'
WHERE role_key IN ('admin', 'common') AND COALESCE(is_builtin, 'N') <> 'Y';
