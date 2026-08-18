import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import {
  enabledBadgeStatus,
  fakeMemberTone,
  genderBadgeStatus,
  yesNoBadgeStatus,
} from "../src/views/member/orderuser/memberCellPresentation.js";
import { formatOrderlinkAmount } from "../src/views/member/orderuser/components/orderlinkPresentation.js";

const member = readFileSync(
  new URL("../src/views/member/orderuser/index.vue", import.meta.url),
  "utf8",
);
const orderlink = readFileSync(
  new URL("../src/views/member/orderuser/components/OrderlinkDrawer.vue", import.meta.url),
  "utf8",
);
const proTable = readFileSync(
  new URL("../src/components/AntProTable/index.vue", import.meta.url),
  "utf8",
);

test("member status cells follow the verified local dictionary semantics", () => {
  assert.equal(genderBadgeStatus("0"), "processing");
  assert.equal(genderBadgeStatus(1), "error");
  assert.equal(genderBadgeStatus("2"), "default");
  assert.equal(genderBadgeStatus(null), null);

  assert.equal(enabledBadgeStatus("0"), "success");
  assert.equal(enabledBadgeStatus(1), "error");
  assert.equal(enabledBadgeStatus("unexpected"), null);

  assert.equal(yesNoBadgeStatus(0), "success");
  assert.equal(yesNoBadgeStatus("1"), "error");
  assert.equal(yesNoBadgeStatus(""), null);

  assert.equal(fakeMemberTone("0"), "success");
  assert.equal(fakeMemberTone(1), "error");
  assert.equal(fakeMemberTone(undefined), null);
});

test("member list renders reference badge dots on status fields", () => {
  for (const field of ["gender", "isEnabled", "allowInvite", "isFrozen", "isBanned"]) {
    assert.match(member, new RegExp(`column\\.key === '${field}'[\\s\\S]*?<a-badge`));
  }

  const genericYesNoBlock = member.match(
    /<template v-else-if="column\.dict === 'yesNo'">([\s\S]*?)<\/template>/,
  )?.[1] || "";
  assert.match(genericYesNoBlock, /<a-badge/);
  assert.match(genericYesNoBlock, /yesNoBadgeStatus\(record\[column\.dataIndex\]\)/);

  for (const field of [
    "isWithdrawalNotification",
    "depositBlockWithdrawal",
    "web3AuthEnabled",
    "isInvalid",
    "isActivity",
    "verifyIdentityBeforeTask",
    "userContractEnabled",
    "userContractSigned",
    "formalContractEnabled",
    "formalContractSigned",
  ]) {
    assert.match(member, new RegExp(`key: "${field}"[^\\n]*dict: "yesNo"`));
  }

  const fakeBlock = member.match(
    /<template v-else-if="column\.key === 'isFake'">([\s\S]*?)<\/template>/,
  )?.[1] || "";
  assert.doesNotMatch(fakeBlock, /<a-badge/);
  assert.match(fakeBlock, /fake-member-link/);
  assert.match(member, /key: "isFake"[^\n]*align: "center"/);
  assert.match(member, /\.fake-member-success[\s\S]*?#52c41a/);
  assert.match(member, /\.fake-member-error[\s\S]*?#ff4d4f/);
});

test("continuous-order presentation matches the original summary and amount format", () => {
  assert.equal(formatOrderlinkAmount("1120.00"), "1120");
  assert.equal(formatOrderlinkAmount("1056.4600"), "1056.46");
  assert.equal(formatOrderlinkAmount(0), "0");
  assert.equal(formatOrderlinkAmount(null), "0");
  assert.equal(formatOrderlinkAmount("invalid"), "0");

  assert.match(orderlink, /<template #search>[\s\S]*?<template #title>/);
  assert.match(orderlink, /用户名: \{\{ user\.username/);
  assert.match(orderlink, /手机号码: \{\{ user\.phoneNumber/);
  assert.match(orderlink, /余额: \{\{ formatAmount\(user\.balance\)/);
  assert.match(orderlink, /任务进度: \{\{ taskProgressDisplay/);
  assert.match(orderlink, /最后登录时间: \{\{ parseTime\(user\.lastLoginTime\)/);
  assert.match(orderlink, /\.member-summary-tip[\s\S]*?color: #ff0000/);
  assert.match(orderlink, /\.member-summary[\s\S]*?font-size: 16px;[\s\S]*?font-weight: 700/);
});

test("continuous-order search, table widths, sorting and empty state mirror the original", () => {
  assert.match(orderlink, /label="连单ID"[\s\S]*?label="单数"[\s\S]*?label="商品标题"/);
  assert.match(orderlink, /productTitle: queryParams\.value\.productTitle/);
  assert.match(orderlink, /advancedSearchVisible/);
  assert.match(orderlink, /orderByColumn: "ol\.id"/);
  assert.match(orderlink, /@change="handleTableChange"/);
  assert.match(orderlink, /title: "ID"[^\n]*width: 120[^\n]*defaultSortOrder: "descend"/);
  assert.match(orderlink, /title: "连单ID"[^\n]*width: 100[^\n]*sorter: true/);
  assert.match(orderlink, /title: "单数"[^\n]*width: 100[^\n]*sorter: true/);
  assert.match(orderlink, /title: "返佣倍数"[^\n]*width: 120[^\n]*sorter: true/);
  assert.match(orderlink, /title: "商品图片"[^\n]*width: 100/);
  assert.match(orderlink, /title: "商品标题"[^\n]*width: 200/);
  assert.match(orderlink, /title: "价格类型"[^\n]*width: 120[^\n]*sorter: true/);
  assert.match(orderlink, /title: "价格"[^\n]*width: 160[^\n]*sorter: true/);
  assert.match(orderlink, /title: "创建时间"[^\n]*width: 170[^\n]*sorter: true/);
  assert.match(orderlink, /title: "最后修改时间"[^\n]*width: 170[^\n]*sorter: true/);
  assert.match(orderlink, /columnWidth: 32/);
  assert.match(orderlink, /<a-empty :image="simpleEmptyImage" description="暂无数据"/);
  assert.match(orderlink, />\s*创建\s*<\/a-button>/);
  assert.match(proTable, /<slot name="title">\{\{ title \}\}<\/slot>/);
  assert.match(proTable, /<slot name="emptyText" \/>/);
});
