import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

import { flowTransactionTypeFallbackLabel } from "../src/views/member/flow/transactionTypeLabels.js";

const source = (path) => readFileSync(new URL(`../${path}`, import.meta.url), "utf8");

test("canonical reward flows have stable Chinese labels", () => {
  assert.equal(flowTransactionTypeFallbackLabel("jj"), "奖金");
  assert.equal(flowTransactionTypeFallbackLabel("rwjl"), "任务奖励");
  assert.equal(flowTransactionTypeFallbackLabel(" RWJL "), "任务奖励");
});

test("legacy bonus is not globally relabelled as task reward", () => {
  assert.equal(flowTransactionTypeFallbackLabel("bonus"), undefined);
  assert.equal(flowTransactionTypeFallbackLabel(null), undefined);
});

test("both flow tables use the canonical task-reward fallback", () => {
  const flowPage = source("src/views/member/flow/index.vue");
  const memberDrawer = source("src/views/member/orderuser/components/OrderuserFlowDrawer.vue");

  assert.match(flowPage, /flowTransactionTypeFallbackLabel\(value\)[\s\S]*?dictText\(transaction_type, value\)/);
  assert.match(memberDrawer, /:text="transactionTypeText\(record\.transactionType\)"/);
  assert.match(memberDrawer, /function transactionTypeText\(value\)[\s\S]*?flowTransactionTypeFallbackLabel\(value\)/);
});

test("the main flow filter exposes both canonical reward types", () => {
  const flowPage = source("src/views/member/flow/index.vue");
  assert.match(flowPage, /\{ value: "jj", label: "奖金" \}/);
  assert.match(flowPage, /\{ value: "rwjl", label: "任务奖励" \}/);
});
