SET NAMES utf8mb4;

-- 会员管理与原后台对齐：身份资料和会员专属合同。
-- 可重复执行。

DELIMITER $$
DROP PROCEDURE IF EXISTS add_member_management_alignment_columns$$
CREATE PROCEDURE add_member_management_alignment_columns()
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'order_user'
      AND column_name = 'phone_number'
      AND is_nullable = 'NO'
  ) THEN
    ALTER TABLE order_user MODIFY COLUMN phone_number VARCHAR(15) NULL COMMENT '手机号';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'order_user' AND column_name = 'identity_type') THEN
    ALTER TABLE order_user ADD COLUMN identity_type VARCHAR(20) NULL COMMENT '证件类型';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'order_user' AND column_name = 'identity_name') THEN
    ALTER TABLE order_user ADD COLUMN identity_name VARCHAR(100) NULL COMMENT '证件姓名';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'order_user' AND column_name = 'identity_number') THEN
    ALTER TABLE order_user ADD COLUMN identity_number VARCHAR(100) NULL COMMENT '证件号码';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'order_user' AND column_name = 'identity_front_image') THEN
    ALTER TABLE order_user ADD COLUMN identity_front_image VARCHAR(500) NULL COMMENT '证件正面图片';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'order_user' AND column_name = 'identity_back_image') THEN
    ALTER TABLE order_user ADD COLUMN identity_back_image VARCHAR(500) NULL COMMENT '证件反面图片';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'order_user' AND column_name = 'identity_handheld_image') THEN
    ALTER TABLE order_user ADD COLUMN identity_handheld_image VARCHAR(500) NULL COMMENT '手持证件图片';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'order_user' AND column_name = 'identity_status') THEN
    ALTER TABLE order_user ADD COLUMN identity_status CHAR(1) NOT NULL DEFAULT '0' COMMENT '身份审核状态';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'order_user' AND column_name = 'identity_remarks') THEN
    ALTER TABLE order_user ADD COLUMN identity_remarks VARCHAR(500) NULL COMMENT '身份审核备注';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'order_user' AND column_name = 'user_contract_content') THEN
    ALTER TABLE order_user ADD COLUMN user_contract_content LONGTEXT NULL COMMENT '用户合同内容';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'order_user' AND column_name = 'formal_contract_content') THEN
    ALTER TABLE order_user ADD COLUMN formal_contract_content LONGTEXT NULL COMMENT '正式合同内容';
  END IF;
END$$
CALL add_member_management_alignment_columns()$$
DROP PROCEDURE IF EXISTS add_member_management_alignment_columns$$
DELIMITER ;
