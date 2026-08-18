import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";

const source = (name) => readFileSync(
  new URL(`../src/views/member/orderuser/components/${name}`, import.meta.url),
  "utf8"
);

const withdrawal = source("OrderuserWithdrawalDrawer.vue");
const sub = source("OrderuserSubDrawer.vue");
const orderinfo = source("OrderuserOrderinfoDrawer.vue");
const flow = source("OrderuserFlowDrawer.vue");

test("member secondary drawers keep the legacy 85 percent outer width", () => {
  for (const component of [withdrawal, sub, orderinfo, flow]) {
    assert.match(component, /<a-drawer[\s\S]{0,220}?width="85%"/);
  }
  assert.equal((withdrawal.match(/width="85%"/g) || []).length, 2,
    "withdrawal list and editor drawers should both use the reference width");
  assert.match(sub, /title="查看下级会员"/);
  assert.match(orderinfo, /title="查看订单明细"/);
  assert.match(flow, /title="查看交易流水"/);
  assert.match(withdrawal, /title\.value = "创建"/);
});

test("withdrawal account loading is scoped to the open member and drops stale requests", () => {
  assert.match(withdrawal, /getType\(\{ type: value \}\)/);
  assert.doesNotMatch(withdrawal, /getType\(value\)/);
  assert.match(withdrawal, /if \(!visible\.value \|\| userId === null \|\| userId === undefined \|\| userId === ""\)/);
  assert.match(withdrawal, /const requestToken = \+\+listRequestToken/);
  assert.match(withdrawal, /requestToken !== listRequestToken[\s\S]*?String\(props\.userId\) !== String\(userId\)/);
  assert.match(withdrawal, /function clearSelection\(\)[\s\S]*?ids\.value = \[\]/);
  assert.doesNotMatch(withdrawal, /\nreset\(\);\s*\ngetList\(\);\s*\n<\/script>/);
});

test("withdrawal editor resets hidden values and submits only the active account shape once", () => {
  assert.match(withdrawal, /form\.value\.withdrawalTypeId = null/);
  assert.match(withdrawal, /value === "0"[\s\S]*?form\.value\.walletAddress = null[\s\S]*?form\.value\.attachment = null/);
  assert.match(withdrawal, /value === "1"[\s\S]*?form\.value\.accountHolder = null/);
  assert.match(withdrawal, /@close="handleFormClose"/);
  assert.match(withdrawal, /const submitting = ref\(false\)/);
  assert.match(withdrawal, /:loading="submitting"/);
  assert.equal((withdrawal.match(/:keyboard="!submitting/g) || []).length, 2);
  assert.match(withdrawal, /if \(submitting\.value \|\| !withdrawalAccRef\.value\) return/);
  assert.match(withdrawal, /submitting\.value = true;\s*try \{\s*await withdrawalAccRef\.value\.validate\(\)/);
  assert.match(withdrawal, /if \(deleting\.value\) return/);
  assert.match(withdrawal, /deleting\.value = true;[\s\S]*?\.finally\(\(\) => \{\s*deleting\.value = false/);
  assert.match(withdrawal, /function buildWithdrawalPayload\(\)/);

  const payloadBuilder = withdrawal.slice(
    withdrawal.indexOf("function buildWithdrawalPayload"),
    withdrawal.indexOf("async function submitForm")
  );
  const activeBranches = payloadBuilder.match(
    /if \(form\.value\.type === "0"\) \{([\s\S]*?)\} else if \(form\.value\.type === "1"\) \{([\s\S]*?)\n  \}/
  );
  assert.ok(activeBranches, "active withdrawal payload branches should remain explicit");
  assert.doesNotMatch(activeBranches[1], /accountName|walletName|walletAddress/);
  assert.doesNotMatch(activeBranches[2], /bankName|depositType|branchCode|branchName|bankAccount|accountHolder/);
  assert.match(activeBranches[2], /attachment: form\.value\.attachment/);
  assert.match(withdrawal, /:value="String\(dict\.id\)"/);
  assert.doesNotMatch(withdrawal, /walletName:\s*\[\{ required: true/);
});

test("withdrawal account UI exposes only list data and validation supported by the backend contract", () => {
  const columns = withdrawal.slice(
    withdrawal.indexOf("const withdrawalColumns"),
    withdrawal.indexOf("const formWithdrawalTypes")
  );
  assert.doesNotMatch(columns, /title: "类型"/);
  assert.match(columns, /title: "出金类型"/);
  assert.match(columns, /title: "参数"/);
  assert.match(columns, /title: "附件"/);
  assert.match(withdrawal, /column\.dataIndex === 'attachment'[\s\S]*?<a-image[\s\S]*?record\.attachment/);

  const queryState = withdrawal.slice(
    withdrawal.indexOf("queryParams: {"),
    withdrawal.indexOf("rules: {}")
  );
  for (const supportedField of ["userId", "type", "isDefault"]) {
    assert.match(queryState, new RegExp(`${supportedField}: null`));
  }
  for (const unsupportedField of ["withdrawalType", "walletAddress", "bankName", "accountName"]) {
    assert.doesNotMatch(queryState, new RegExp(`${unsupportedField}: null`));
  }
});

test("withdrawal create starts without an account type and validates the actual service requirements", () => {
  const defaults = withdrawal.slice(
    withdrawal.indexOf("function createDefaultForm"),
    withdrawal.indexOf("function reset()")
  );
  assert.match(defaults, /type: null/);
  assert.match(defaults, /attachment: null/);
  assert.doesNotMatch(defaults, /type: "0"/);

  const ruleBuilder = withdrawal.slice(
    withdrawal.indexOf("function updateRules"),
    withdrawal.indexOf("function cancel()")
  );
  for (const requiredField of ["type", "isDefault", "withdrawalTypeId", "bankName", "bankAccount", "accountHolder", "walletAddress"]) {
    assert.match(ruleBuilder, new RegExp(`${requiredField}: \\[\\{ required: true`));
  }
  for (const optionalField of ["depositType", "branchCode", "branchName", "accountName", "walletName", "attachment"]) {
    assert.doesNotMatch(ruleBuilder, new RegExp(`${optionalField}: \\[\\{ required: true`));
  }

  const walletFieldsStart = withdrawal.indexOf('<template v-if="form.type === \'1\'">');
  const walletFields = withdrawal.slice(
    walletFieldsStart,
    withdrawal.indexOf("</a-row>", walletFieldsStart)
  );
  assert.match(walletFields, /label="附件" name="attachment"/);
  assert.match(walletFields, /<image-upload v-model="form\.attachment" :limit="1"/);
  assert.match(withdrawal, /attachment: record\.attachment \?\? null/);
});

test("sub-member drawer uses backend statistic fields, status dictionary and flow permission", () => {
  for (const field of [
    "totalWithdrawalAmount",
    "totalRechargeAmount",
    "todayRest",
    "totalRest",
  ]) {
    assert.match(sub, new RegExp(`dataIndex: "${field}"`));
  }
  for (const staleField of [
    "withdrawalAmount",
    "rechargeAmount",
    "todayResetCount",
    "totalResetCount",
  ]) {
    assert.doesNotMatch(sub, new RegExp(`dataIndex: "${staleField}"`));
  }
  assert.match(sub, /column\.dataIndex === 'isEnabled'[\s\S]*?:options="sys_enabled"/);
  assert.match(sub, /@click="openFlow\(record\)"[\s\S]*?v-hasPermi="\['member:flow:list'\]"/);
  assert.match(sub, /let listRequestToken = 0/);
  assert.match(sub, /requestToken !== listRequestToken[\s\S]*?String\(props\.userId\) !== String\(userId\)/);
  assert.match(sub, /function reset\(\)[\s\S]*?showAll\.value = false/);
});

test("order and flow drawers reset between members and ignore stale list responses", () => {
  assert.match(orderinfo, /const rows = Array\.isArray\(res\.rows\)/);
  assert.match(orderinfo, /total\.value = res\.total \?\? res\.data\?\.total \?\? rows\.length/);
  assert.doesNotMatch(orderinfo, /const data = res\.rows \|\| res\.data \|\| res/);

  for (const component of [orderinfo, flow]) {
    assert.match(component, /let listRequestToken = 0/);
    assert.match(component, /function reset\(/);
    assert.match(component, /listRequestToken \+= 1/);
    assert.match(component, /requestToken !== listRequestToken[\s\S]*?String\(props\.userId\) !== String\(userId\)/);
    assert.match(component, /\{ immediate: true \}/);
  }
});
