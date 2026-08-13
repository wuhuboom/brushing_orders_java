SET NAMES utf8mb4;

DELIMITER $$
DROP PROCEDURE IF EXISTS align_order_link_audit_columns$$
CREATE PROCEDURE align_order_link_audit_columns()
BEGIN
  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'order_link'
       AND column_name = 'create_by'
  ) THEN
    ALTER TABLE order_link
      ADD COLUMN create_by VARCHAR(64) NULL AFTER status;
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'order_link'
       AND column_name = 'update_by'
  ) THEN
    ALTER TABLE order_link
      ADD COLUMN update_by VARCHAR(64) NULL AFTER create_time;
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'order_link'
       AND column_name = 'update_time'
  ) THEN
    ALTER TABLE order_link
      ADD COLUMN update_time DATETIME(3) NULL AFTER update_by;
  END IF;
END$$
CALL align_order_link_audit_columns()$$
DROP PROCEDURE align_order_link_audit_columns$$
DELIMITER ;

UPDATE order_link
   SET create_by = COALESCE(NULLIF(create_by, ''), 'system'),
       update_by = COALESCE(NULLIF(update_by, ''), NULLIF(create_by, ''), 'system'),
       update_time = COALESCE(update_time, create_time);
