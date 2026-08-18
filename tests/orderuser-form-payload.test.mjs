import assert from "node:assert/strict";
import test from "node:test";

import {
  buildFakeMemberToggle,
  buildCopyMemberForm,
  buildMemberSubmitPayload,
  buildModifyCountPayload,
} from "../src/views/member/orderuser/components/orderuserFormPayload.js";

const sensitiveSnapshot = {
  id: 3151,
  version: 8,
  username: "member-a",
  phoneNumber: "123",
  password: "server-placeholder",
  tradePassword: "server-placeholder",
  vipId: 2,
  reputationScore: 0,
  parentInviteCode: "  PARENT  ",
  parentId: 9,
  balance: 9999,
  frozenBalance: 8888,
  baseSalary: 7777,
  taskProgress: 40,
  signDays: 22,
  todaySignCount: 1,
  totalSignDays: 300,
  identityName: "secret",
  identityNumber: "secret-number",
  userContractEnabled: "0",
  formalContractSigned: "0",
};

test("member edit payload only contains fields that the generic edit drawer exposes", () => {
  const payload = buildMemberSubmitPayload(sensitiveSnapshot);

  assert.deepEqual(payload, {
    phoneNumber: "123",
    vipId: 2,
    reputationScore: 0,
    parentId: 9,
    parentInviteCode: "PARENT",
    id: 3151,
    version: 8,
  });
  for (const forbidden of [
    "username", "password", "tradePassword", "balance", "frozenBalance", "baseSalary",
    "taskProgress", "signDays", "todaySignCount", "totalSignDays", "identityName",
    "identityNumber", "userContractEnabled", "formalContractSigned",
  ]) {
    assert.equal(Object.hasOwn(payload, forbidden), false, `${forbidden} must not leak into edit payload`);
  }
});

test("copy keeps visible source defaults but resets credentials and all hidden business state", () => {
  const copied = buildCopyMemberForm(
    { isEnabled: "0", accountStatus: "0", balance: null, taskProgress: null },
    sensitiveSnapshot,
  );

  assert.equal(copied.username, "member-a");
  assert.equal(copied.phoneNumber, "123");
  assert.equal(copied.reputationScore, 0);
  assert.equal(copied.password, null);
  assert.equal(copied.tradePassword, null);
  assert.equal(copied.id, null);
  assert.equal(copied.version, null);
  assert.equal(copied.balance, null);
  assert.equal(copied.taskProgress, null);
  for (const forbidden of [
    "frozenBalance", "baseSalary", "signDays", "todaySignCount", "totalSignDays",
    "identityName", "identityNumber", "userContractEnabled", "formalContractSigned",
  ]) {
    assert.equal(Object.hasOwn(copied, forbidden), false, `${forbidden} must not be cloned`);
  }
});

test("create trims credentials and modify-count never sends its display-only progress label", () => {
  const createPayload = buildMemberSubmitPayload({
    username: "  new-user ",
    password: " 123456 ",
    tradePassword: " 654321 ",
    parentInviteCode: "   ",
  });
  assert.equal(createPayload.username, "new-user");
  assert.equal(createPayload.password, "123456");
  assert.equal(createPayload.tradePassword, "654321");
  assert.equal(createPayload.parentId, 0);

  const countPayload = buildModifyCountPayload({ id: 7, orderCount: "3 / 40", taskProgress: 3 });
  assert.deepEqual(countPayload, { id: 7, taskProgress: 3 });
  assert.equal(Object.hasOwn(countPayload, "orderCount"), false);
});

test("fake-member action follows the legacy 0=yes and 1=no dictionary contract", () => {
  assert.deepEqual(buildFakeMemberToggle("0"), { value: "1", action: "设为真人" });
  assert.deepEqual(buildFakeMemberToggle(0), { value: "1", action: "设为真人" });
  assert.deepEqual(buildFakeMemberToggle("1"), { value: "0", action: "设为假人" });
  assert.deepEqual(buildFakeMemberToggle(1), { value: "0", action: "设为假人" });
});
