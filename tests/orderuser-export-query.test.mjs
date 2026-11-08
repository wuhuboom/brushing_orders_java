import assert from "node:assert/strict";
import test from "node:test";
import { buildMemberQueryParams } from "../src/views/member/orderuser/memberQueryParams.js";

test("member export keeps all list filters and date bounds while removing pagination", () => {
  const query = {
    pageNum: 7,
    pageSize: 20,
    id: " 9007199254740993 ",
    keyword: "member-a",
    isOnline: "0",
    usernameList: "member-a,member-b",
    balanceMin: 0,
    balanceMax: "9999.99",
    isFake: "1",
    orderByColumn: "ou.balance",
    isAsc: "desc",
    createTimeRange: ["2026-11-01", "2026-11-06"],
    params: { scope: "direct" },
  };
  const snapshot = structuredClone(query);
  const list = buildMemberQueryParams(query);
  const exported = buildMemberQueryParams(query, { paginated: false });
  const { pageNum, pageSize, ...listFilters } = list;

  assert.equal(pageNum, 7);
  assert.equal(pageSize, 20);
  assert.deepEqual(exported, listFilters);
  assert.equal(exported.id, "9007199254740993");
  assert.equal(exported.isOnline, "0");
  assert.equal(exported.balanceMin, 0);
  assert.deepEqual(exported.params, {
    scope: "direct",
    beginTime: "2026-11-01",
    endTime: "2026-11-06",
  });
  assert.equal(Object.hasOwn(exported, "createTimeRange"), false);
  assert.equal(Object.hasOwn(exported, "pageNum"), false);
  assert.equal(Object.hasOwn(exported, "pageSize"), false);
  assert.deepEqual(query, snapshot);
});

test("reset date filters and invalid IDs cannot broaden an export unexpectedly", () => {
  const request = buildMemberQueryParams({ id: null, createTimeRange: null });
  assert.deepEqual(request.params, { beginTime: null, endTime: null });
  for (const id of ["0", "-1", "abc", "9223372036854775808", 9007199254740992]) {
    assert.throws(
      () => buildMemberQueryParams({ id }, { paginated: false }),
      /ID 请输入有效的正整数/,
    );
  }
});
