# 连单首笔待提交时计入订单数

后台「交易设置」新增 `includePendingLinkedOrderInTaskProgress`：`"0"` 启用，`"1"` 禁用，默认禁用；旧配置缺少该字段时保持原逻辑，无需数据库迁移。

启用后，用户端、后台会员列表、会员操作抽屉和会员导出的订单进度按以下规则展示：

- 已完成进度为 1，后台在第 2 单配置 3 笔连单：首笔进入待提交时显示 2。
- 同组后续明细待提交或冻结期间仍显示 2，整组完成后仍显示 2。
- 第一笔取消且组内没有其他活动订单时恢复显示 1；存在冻结明细时保持显示 2。
- 普通待提交订单不受此开关影响。处理中切换开关在重新读取会员信息后生效。

已完成进度 `taskProgress` 的持久值保持原有结算规则，继续用于下一单匹配、彩金匹配、任务上限和提现条件。后台新增只读 `displayTaskProgress` 字段用于展示，编辑单数和新增连单默认序号仍使用原始完成进度；H5 现有 `taskProgress` 响应字段返回展示值，无需修改 H5。

实现由 `MemberTaskProgressService` 在会员查询完成分页后批量补充展示值。仅将 `order_info` 中 `type='1'`、`link_id` 非空且状态为待提交/冻结的订单纳入，按会员取最大的 `order_count`，不按明细数量累加。营销统计仍使用实际完成进度。

验证包括默认关闭、首笔待提交、同组不重复计数、冻结、取消、完成、开关切换、批量会员隔离、用户资料投影及订单匹配回归。真实 SQL 集成测试使用独立的本机专用库：

```bash
PENDING_LINKED_DB_USER=root PENDING_LINKED_DB_PASSWORD= \
mvn -o -B -pl order-admin -am \
  -Dtest=MemberTaskProgressMySqlTest \
  -Dmember.task.progress.jdbcUrl=jdbc:mysql://127.0.0.1:PORT/codex_member_task_progress_test \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

该测试会重建指定测试库中的 `order_info` 表，仅接受 `127.0.0.1` 和上述固定专用库名。
