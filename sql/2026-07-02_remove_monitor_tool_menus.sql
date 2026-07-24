CREATE TABLE IF NOT EXISTS sys_menu_backup_before_remove_monitor_tool AS
SELECT *
FROM sys_menu
WHERE 1 = 0;

CREATE TABLE IF NOT EXISTS sys_role_menu_backup_before_remove_monitor_tool AS
SELECT *
FROM sys_role_menu
WHERE 1 = 0;

INSERT INTO sys_menu_backup_before_remove_monitor_tool
SELECT m.*
FROM sys_menu m
JOIN (
    WITH RECURSIVE menu_tree AS (
        SELECT menu_id
        FROM sys_menu
        WHERE menu_id IN (2, 3)
        UNION ALL
        SELECT child.menu_id
        FROM sys_menu child
        JOIN menu_tree parent ON child.parent_id = parent.menu_id
    )
    SELECT menu_id FROM menu_tree
) removed ON removed.menu_id = m.menu_id
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_menu_backup_before_remove_monitor_tool backup
    WHERE backup.menu_id = m.menu_id
);

INSERT INTO sys_role_menu_backup_before_remove_monitor_tool
SELECT rm.*
FROM sys_role_menu rm
JOIN sys_menu_backup_before_remove_monitor_tool removed ON removed.menu_id = rm.menu_id
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_role_menu_backup_before_remove_monitor_tool backup
    WHERE backup.role_id = rm.role_id
      AND backup.menu_id = rm.menu_id
);

DELETE rm
FROM sys_role_menu rm
JOIN sys_menu_backup_before_remove_monitor_tool removed ON removed.menu_id = rm.menu_id;

DELETE m
FROM sys_menu m
JOIN sys_menu_backup_before_remove_monitor_tool removed ON removed.menu_id = m.menu_id;
