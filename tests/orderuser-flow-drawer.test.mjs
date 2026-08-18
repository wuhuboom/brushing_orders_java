import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

import {
  buildFlowListParams,
  createFlowQueryParams,
} from "../src/views/member/orderuser/components/orderuserFlow.js";

const drawer = readFileSync(
  new URL("../src/views/member/orderuser/components/OrderuserFlowDrawer.vue", import.meta.url),
  "utf8",
);
test("flow drawer matches the reference filters and visible columns", () => {
  for (const label of ["流水编号", "交易类型", "金额"]) {
    assert.match(drawer, new RegExp(`label="${label}"`));
  }
  assert.match(drawer, /v-model:value="queryParams\.amountMin"/);
  assert.match(drawer, /disabled value="~"/);
  assert.match(drawer, /v-model:value="queryParams\.amountMax"/);

  const columnBlock = drawer.slice(
    drawer.indexOf("const flowColumns"),
    drawer.indexOf("const data = reactive"),
  );
  assert.deepEqual(
    [...columnBlock.matchAll(/title: "([^"]+)"/g)].map((match) => match[1]),
    ["流水编号", "交易类型", "交易前余额", "金额", "交易后余额", "交易编号", "创建时间", "备注"],
  );
  assert.doesNotMatch(columnBlock, /title: "ID"/);
  assert.match(drawer, /:scroll="\{ x: 1310 \}"/);
});

test("flow amount range uses the backend nested params contract without leaking form-only fields", () => {
  const defaults = {
    pageNum: 1,
    pageSize: 10,
    serialCode: null,
    transactionType: null,
    amountMin: null,
    amountMax: null,
  };
  assert.deepEqual(createFlowQueryParams(), defaults);
  assert.deepEqual(buildFlowListParams(defaults, 7), {
    pageNum: 1,
    pageSize: 10,
    serialCode: null,
    transactionType: null,
    userId: 7,
  });

  const query = {
    ...createFlowQueryParams(),
    serialCode: "FLOW-1",
    amountMin: -10,
    amountMax: 0,
  };
  assert.deepEqual(buildFlowListParams(query, "9007199254740993"), {
    pageNum: 1,
    pageSize: 10,
    serialCode: "FLOW-1",
    transactionType: null,
    userId: "9007199254740993",
    params: { amountMin: -10, amountMax: 0 },
  });
  assert.equal(query.amountMin, -10, "building params must not mutate the reactive query");

});

test("only the transaction code cell offers the reference copy interaction", () => {
  assert.match(drawer, /column\.dataIndex === 'transactionCode'[\s\S]*?copyTransactionCode\(record\.transactionCode\)[\s\S]*?>复制<\/a-button>/);
  assert.match(drawer, /navigator\.clipboard\.writeText\(value\)/);
  assert.match(drawer, /msgSuccess\("交易编号已复制"\)/);
  assert.match(drawer, /msgError\("复制失败，请手动复制"\)/);
  assert.equal((drawer.match(/>复制<\/a-button>/g) || []).length, 1);

  const serialCell = drawer.slice(
    drawer.indexOf("const flowColumns"),
    drawer.indexOf("const data = reactive"),
  );
  assert.doesNotMatch(serialCell, /copy.*serialCode/i);
});

test("flow drawer keeps member-switch reset and stale-response protection", () => {
  assert.match(drawer, /function resetQuery\(\)[\s\S]*?queryParams\.value\.amountMin = null[\s\S]*?queryParams\.value\.amountMax = null[\s\S]*?handleQuery\(\)/);
  assert.match(drawer, /function reset\(\)[\s\S]*?Object\.assign\(queryParams\.value, createFlowQueryParams\(\)\)/);
  assert.match(drawer, /let listRequestToken = 0/);
  assert.match(drawer, /requestToken !== listRequestToken[\s\S]*?String\(props\.userId\) !== String\(userId\)/);
  assert.match(drawer, /buildFlowListParams\(queryParams\.value, userId\)/);
});
