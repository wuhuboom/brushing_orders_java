import test from "node:test"
import assert from "node:assert/strict"
import { readFileSync } from "node:fs"

const trade = readFileSync(
  new URL("../src/views/member/orderconfig/components/trade-config.vue", import.meta.url),
  "utf8"
)

test("trade settings mirror the reference four-column field layout", () => {
  assert.match(trade, /:gutter="\[24, 0\]"/)
  assert.match(trade, /:span="item\.span \|\| 6"/)
  assert.match(trade, /size="large"/)
  assert.match(trade, /label: "提现手续费率"[^\n]*suffix: "%"/)
  assert.match(trade, /label: "上级返佣百分比"[^\n]*suffix: "%"/)
  assert.match(trade, /<span>~<\/span>/)
  assert.match(trade, /mode="multiple"[\s\S]*?placeholder="请选择服务时间范围"/)
})

test("trade settings keep the reference order and complete field set", () => {
  const orderedLabels = [
    "注册赠送金额",
    "交易最低余额",
    "会员提现状态",
    "会员提现最低信誉分",
    "会员最低提现金额",
    "会员最高提现金额",
    "平台单日最高提现金额",
    "提现手续费率",
    "上级返佣百分比",
    "匹配范围(%)",
    "服务时间范围",
    "交易时间范围",
    "充值后禁止提现",
    "提现是否限制等级最低余额",
    "提现时间范围",
    "是否自动提交任务",
    "开始任务延迟毫秒",
    "提交任务延迟毫秒",
    "订单过期时间（单位秒），为0则表示不开启",
    "提交任务是否锁定额外佣金",
    "是否允许修改提现地址",
    "提现需要完成的任务组数",
    "充值赠送交易类型",
    "任务进度是否计算连单明细",
    "任务进度是否包含待提交任务",
    "禁止客户提现所需交易密码失败次数(0-不限制)",
    "余额为负数时禁止下级用户返佣",
    "任务是否验证彩金",
    "开始任务是否验证可用余额",
    "扣除注册赠送金额所在任务组数(0-不扣除)",
  ]

  let previousIndex = -1
  for (const label of orderedLabels) {
    const index = trade.indexOf(`label: "${label}"`)
    assert.ok(index > previousIndex, `${label} should keep the reference order`)
    previousIndex = index
  }

  assert.match(trade, /\{ label: "否", value: "1" \}[\s\S]*?\{ label: "是", value: "0" \}/)
  assert.match(trade, /\{ label: "禁用", value: "1" \}[\s\S]*?\{ label: "启用", value: "0" \}/)
  assert.match(trade, /disabledChildCommissions: "1"[\s\S]*?validAward: "1"[\s\S]*?validBalance: "1"[\s\S]*?deductRegisterGiveAmountTaskGroup: 0/)
  assert.match(trade, /Object\.prototype\.hasOwnProperty\.call\(parsed, key\)/)
})
