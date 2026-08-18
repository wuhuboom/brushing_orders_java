import assert from "node:assert/strict"
import { readFileSync } from "node:fs"
import test from "node:test"

import {
  normalizeRechargeBonusTradeType,
  tradeTypeOptions
} from "../src/views/member/orderconfig/components/tradeTransactionTypes.js"

const tradeConfigSource = readFileSync(
  new URL("../src/views/member/orderconfig/components/trade-config.vue", import.meta.url),
  "utf8"
)

test("recharge gift transaction setting uses real transaction_type dictionary values", () => {
  assert.deepEqual(tradeTypeOptions.slice(0, 3), [
    { label: "赠送", value: "zs" },
    { label: "扣款", value: "kk" },
    { label: "充值", value: "cz" }
  ])
  assert.deepEqual(
    tradeTypeOptions.find(option => option.label === "奖金"),
    { label: "奖金", value: "jj" }
  )
  assert.deepEqual(
    tradeTypeOptions.find(option => option.label === "提现驳回"),
    { label: "提现驳回", value: "txbh" }
  )
  assert.deepEqual(
    tradeTypeOptions.filter(option => ["gzjl", "sjjl"].includes(option.value)),
    [
      { label: "工作奖励", value: "gzjl" },
      { label: "升级奖励", value: "sjjl" }
    ]
  )
  assert.equal(new Set(tradeTypeOptions.map(option => option.value)).size, tradeTypeOptions.length)
})

test("legacy trade setting values are normalized without changing the selected meaning", () => {
  assert.equal(normalizeRechargeBonusTradeType("bonus"), "jj")
  assert.equal(normalizeRechargeBonusTradeType("taskReward"), "rwjl")
  assert.equal(normalizeRechargeBonusTradeType("zs"), "zs")
  assert.equal(normalizeRechargeBonusTradeType("txbh"), "txbh")
  assert.equal(normalizeRechargeBonusTradeType("invalid"), "zs")
  assert.equal(normalizeRechargeBonusTradeType(null), "zs")
})

test("trade settings normalize rechargeBonusTradeType while loading JSON content", () => {
  assert.match(
    tradeConfigSource,
    /key === "rechargeBonusTradeType"[\s\S]*?normalizeRechargeBonusTradeType\(parsed\[key\]\)/
  )
  assert.match(
    tradeConfigSource,
    /rangeParts\.length > 1 \? Number\(rangeParts\[1\]\) : minimum/
  )
})
