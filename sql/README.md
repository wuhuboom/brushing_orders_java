# 后台数据库升级包（2026-07-14）

适用数据库：`order`（MySQL 8.x）

本升级包整理了本轮后台对齐涉及的数据库变更，不包含旧站业务数据、H5 测试数据和历史上传文件回填。

## 文件说明

1. `01_customer_management_gap_fill.sql`
   - 客户管理兼容表、日志字段和客户模块菜单补齐。
2. `02_points_activity_website.sql`
   - 积分商城、活动管理、官网客户表、索引、权限和菜单。
3. `03_system_management_schema.sql`
   - 系统管理策略、分组、职位角色、数据权限、文件及文件引用结构。
   - 扩展组织、角色、操作日志、登录日志字段。
   - 将历史 `sys_user.is_locked` 的 `Y/N` 兼容值统一为 `1/0`。
4. `04_system_management_menu_activation.sql`
   - 激活与旧站一致的系统管理四组菜单和按钮权限；平台配置保留为隐藏兼容入口。
   - 修复重复操作日志菜单，并授权超级管理员角色。
5. `05_verify.sql`
   - 只读校验表、字段、菜单、权限和关联完整性。

## 推荐执行顺序

1. 备份当前 `order` 数据库。
2. 执行 `01_customer_management_gap_fill.sql`。
3. 执行 `02_points_activity_website.sql`。
4. 执行 `03_system_management_schema.sql`。
5. 部署后端和前端代码。
6. 执行 `04_system_management_menu_activation.sql`。
7. 清理 Redis 登录权限缓存，并让管理员重新登录。
8. 执行 `05_verify.sql`，确认不存在缺表、缺字段、重复菜单或孤立关联。

## 注意事项

- SQL 使用存储过程和 `DELIMITER`，请使用 MySQL Workbench、DataGrip 或 MySQL 命令行按“脚本”方式执行，不要逐条复制到只支持单语句的执行器。
- DDL 会隐式提交，不要依赖一个外层事务整体回滚；发布前必须先备份。
- 脚本支持在现有库重复执行，不会重复创建表、菜单或权限。
- `sys_file`、`sys_file_reference` 只记录升级后的新上传文件；不会扫描已有上传目录。
- 新业务表默认空数据，不复制旧站礼品、积分、活动、订单或官网客户数据。
- `04_system_management_menu_activation.sql` 应在新前后端部署完成后执行，避免菜单提前出现但页面尚未发布。

## 当前开发库执行状态

- 数据库：`order`
- 结构迁移：已重复执行验证
- 菜单激活：已重复执行验证
- 临时策略、分组、文件和文件引用测试数据：已清理
- 原有管理员、角色、组织和用户关系：保留
