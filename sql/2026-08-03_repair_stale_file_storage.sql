-- Repair sys_file paths after moving local uploads from Windows to Linux.
--
-- IMPORTANT:
-- 1. Copy the physical files first and keep their relative paths unchanged.
-- 2. Verify the files exist below /www/wwwroot/order_java/uploadPath.
-- 3. Change @files_copied_and_verified to 1 before executing the UPDATE.
--
-- If the physical files are already missing, do not run the UPDATE. The
-- application will repair each stale record when the same file is uploaded
-- again.

SET @old_storage_root = 'E:/order/uploadPath/';
SET @new_storage_root = '/www/wwwroot/order_java/uploadPath/';
SET @files_copied_and_verified = 0;

SELECT file_id, file_hash, storage_path, file_url, file_size
FROM sys_file
WHERE storage_path LIKE CONCAT(@old_storage_root, '%')
ORDER BY file_id;

CREATE TABLE IF NOT EXISTS sys_file_storage_path_backup_20260803 LIKE sys_file;

INSERT IGNORE INTO sys_file_storage_path_backup_20260803
SELECT *
FROM sys_file
WHERE storage_path LIKE CONCAT(@old_storage_root, '%');

UPDATE sys_file
SET storage_path = CONCAT(
        @new_storage_root,
        SUBSTRING(storage_path, CHAR_LENGTH(@old_storage_root) + 1)
    )
WHERE @files_copied_and_verified = 1
  AND storage_path LIKE CONCAT(@old_storage_root, '%');

SELECT ROW_COUNT() AS repaired_file_rows;
