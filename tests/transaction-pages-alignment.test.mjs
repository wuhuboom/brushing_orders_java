import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const source = (path) => readFileSync(new URL(`../${path}`, import.meta.url), "utf8");

const recharge = source("src/views/member/recharge/index.vue");
const flow = source("src/views/member/flow/index.vue");
const withdrawal = source("src/views/member/withdrawal/index.vue");

function searchMarkup(page) {
  const start = page.indexOf("<template #search>");
  const end = page.indexOf("<template #toolbar>", start);
  assert.notEqual(start, -1, "missing search slot");
  assert.notEqual(end, -1, "missing search slot end");
  return page.slice(start, end);
}

function rowLabels(page) {
  return [...searchMarkup(page).matchAll(/<a-form-item label="([^"]+)"/g)]
    .map((match) => match[1]);
}

function cssRuleBody(page, selector) {
  const start = page.indexOf(selector);
  const bodyStart = page.indexOf("{", start);
  const end = page.indexOf("}", bodyStart);
  assert.notEqual(start, -1, `missing CSS selector: ${selector}`);
  assert.notEqual(bodyStart, -1, `missing CSS body: ${selector}`);
  assert.notEqual(end, -1, `missing CSS body end: ${selector}`);
  return page.slice(bodyStart + 1, end);
}

function operationMarkup(page) {
  const marker = '<template v-else-if="column.key === \'operation\'">';
  const start = page.indexOf(marker);
  const end = page.indexOf("</template>", start);
  assert.notEqual(start, -1, "missing operation cell");
  assert.notEqual(end, -1, "missing operation cell end");
  return page.slice(start, end);
}

function columnSpecs(page, declaration, nextDeclaration) {
  const start = page.indexOf(`const ${declaration} = [`);
  const end = page.indexOf(`const ${nextDeclaration}`, start);
  assert.notEqual(start, -1, `missing columns: ${declaration}`);
  assert.notEqual(end, -1, `missing columns end: ${declaration}`);
  return [...page.slice(start, end).matchAll(/title:\s*"([^"]+)"[^\n]*?width:\s*(\d+)/g)]
    .map((match) => ({ title: match[1], width: Number.parseInt(match[2], 10) }));
}

function frozenObject(page, declaration) {
  const marker = `const ${declaration} = Object.freeze({`;
  const start = page.indexOf(marker);
  const end = page.indexOf("});", start);
  assert.notEqual(start, -1, `missing object: ${declaration}`);
  assert.notEqual(end, -1, `missing object end: ${declaration}`);
  const objectStart = page.indexOf("{", start);
  const literal = page.slice(objectStart, end + 1);
  return Function(`"use strict"; return (${literal});`)();
}

function computedOptionDefinitions(page, declaration) {
  const marker = `const ${declaration} = computed(() => [`;
  const start = page.indexOf(marker);
  const arrayStart = page.indexOf("[", start);
  const end = page.indexOf("].map(", arrayStart);
  assert.notEqual(start, -1, `missing computed options: ${declaration}`);
  assert.notEqual(end, -1, `missing computed options end: ${declaration}`);
  return Function(`"use strict"; return (${page.slice(arrayStart, end + 1)});`)();
}

function functionDeclaration(page, name) {
  const functionStart = page.indexOf(`function ${name}(`);
  assert.notEqual(functionStart, -1, `missing function: ${name}`);
  const start = page.slice(functionStart - 6, functionStart) === "async "
    ? functionStart - 6
    : functionStart;
  const bodyStart = page.indexOf("{", functionStart);
  let depth = 0;
  for (let index = bodyStart; index < page.length; index += 1) {
    if (page[index] === "{") depth += 1;
    if (page[index] === "}") depth -= 1;
    if (depth === 0) return page.slice(start, index + 1);
  }
  assert.fail(`unterminated function: ${name}`);
}

function deferred() {
  let resolve;
  let reject;
  const promise = new Promise((resolvePromise, rejectPromise) => {
    resolve = resolvePromise;
    reject = rejectPromise;
  });
  return { promise, resolve, reject };
}

function listHarness(page, { apiName, listName, queryIsRef, includeRequestParams = false }, api, enrich) {
  const requestCounter = page.match(/let listRequestId = 0;/)?.[0];
  assert.ok(requestCounter, "missing latest-request counter");
  const queryState = "{ pageNum: 1, pageSize: 20, amountMin: null, amountMax: null }";
  const requestParams = includeRequestParams ? functionDeclaration(page, "requestParams") : "";
  const getList = functionDeclaration(page, "getList");
  const factory = Function(apiName, "enrichSensitiveAccounts", `
    "use strict";
    const loading = { value: false };
    const total = { value: 0 };
    const ${listName} = { value: [] };
    const dateRange = { value: [] };
    const queryParams = ${queryIsRef ? `{ value: ${queryState} }` : queryState};
    ${requestCounter}
    ${requestParams}
    ${getList}
    return { getList, loading, rows: ${listName}, total };
  `);
  return factory(api, enrich || (async (rows) => rows));
}

test("transaction searches use one three-column flow with two collapsed fields and trailing actions", () => {
  const cases = [
    {
      page: recharge,
      labels: ["用户名", "手机号码", "上级用户名", "用户钱包地址", "金额", "出金类型", "状态", "是否假人", "创建时间", "交易类型", "是否隐藏"],
    },
    {
      page: flow,
      labels: ["流水编号", "用户名", "交易类型", "金额", "是否隐藏", "交易编号", "创建时间"],
    },
    {
      page: withdrawal,
      labels: ["用户名", "手机号码", "上级用户名", "用户钱包地址", "金额", "出金类型", "状态", "是否假人", "创建时间", "交易类型", "是否隐藏"],
    },
  ];

  for (const { page, labels } of cases) {
    const search = searchMarkup(page);
    const columns = [...search.matchAll(/<a-col\b[^>]*>/g)].map((match) => match[0]);

    assert.equal((search.match(/<a-row\b/g) || []).length, 1);
    assert.match(search, /<a-row :gutter="\[24, 24\]" align="middle">/);
    assert.deepEqual(rowLabels(page), labels);
    assert.equal(columns.length, labels.length + 1);
    assert.ok(columns.every((column) => column.includes(':lg="8"')));
    assert.ok(columns.slice(0, 2).every((column) => !column.includes("advancedSearchVisible")));
    assert.ok(columns.slice(2, -1).every((column) => column.includes('v-if="advancedSearchVisible"')));
    assert.match(columns.at(-1), /class="ant-pro-query-actions"/);
    assert.doesNotMatch(columns.at(-1), /v-if=/);
    assert.equal((search.match(/v-if="advancedSearchVisible"/g) || []).length, labels.length - 2);
    assert.doesNotMatch(search, /advanced-query-row|flex="auto"|:lg="[67]"/);
  }
});

test("search selects expose the verified live option order without changing backend values", () => {
  const expectedYesNo = [
    { value: "1", label: "否" },
    { value: "0", label: "是" },
  ];
  const expectedTransactions = [
    { value: "zs", label: "赠送" },
    { value: "kk", label: "扣款" },
    { value: "cz", label: "充值" },
    { value: "txz", label: "提现中" },
    { value: "txjd", label: "提现解冻" },
    { value: "tx", label: "提现" },
    { value: "rw", label: "任务" },
    { value: "bjfh", label: "本金返回" },
    { value: "fy", label: "返佣" },
    { value: "xjfy", label: "下级返佣" },
  ];

  for (const page of [recharge, withdrawal]) {
    assert.deepEqual(computedOptionDefinitions(page, "liveYesNoOptions"), expectedYesNo);
    assert.deepEqual(computedOptionDefinitions(page, "liveTransactionTypeOptions"), expectedTransactions);
    assert.match(page, /v-for="dict in liveYesNoOptions"/);
    assert.match(page, /v-for="dict in liveTransactionTypeOptions"/);
  }

  assert.deepEqual(computedOptionDefinitions(flow, "liveYesNoOptions"), expectedYesNo);
  assert.deepEqual(computedOptionDefinitions(flow, "liveTransactionTypeOptions"), [
    ...expectedTransactions,
    { value: "jj", label: "奖金" },
    { value: "rwjl", label: "任务奖励" },
  ]);
  assert.match(flow, /v-for="dict in liveYesNoOptions"/);
  assert.match(flow, /v-for="dict in liveTransactionTypeOptions"/);

  for (const page of [recharge, withdrawal]) {
    assert.match(page, /v-for="dict in order_zhlx"/);
    assert.doesNotMatch(page, /网络-ERC-USDT|网络-TRC-USDT/);
  }
});

test("recharge columns match the live order and widths without a fake conversion field", () => {
  assert.deepEqual(columnSpecs(recharge, "rechargeColumns", "rowSelection"), [
    { title: "用户名", width: 140 },
    { title: "手机号码", width: 140 },
    { title: "上级用户名", width: 140 },
    { title: "充值账户", width: 300 },
    { title: "金额", width: 100 },
    { title: "出金类型", width: 120 },
    { title: "赠送金额", width: 100 },
    { title: "到账金额", width: 100 },
    { title: "状态", width: 100 },
    { title: "创建时间", width: 170 },
    { title: "备注", width: 200 },
    { title: "交易类型", width: 100 },
    { title: "订单号", width: 200 },
    { title: "是否隐藏", width: 100 },
    { title: "最后修改人", width: 120 },
    { title: "最后修改时间", width: 170 },
    { title: "操作", width: 150 },
  ]);
  assert.doesNotMatch(recharge, /convertedAmount|转换后金额/);
  assert.match(recharge, /:scroll="\{ x: 2482, y: 'calc\(100vh - 440px\)' \}"/);
});

test("flow columns match the live order and widths without an operation column", () => {
  assert.deepEqual(columnSpecs(flow, "flowColumns", "rowSelection"), [
    { title: "流水编号", width: 200 },
    { title: "用户名", width: 140 },
    { title: "交易类型", width: 140 },
    { title: "交易前余额", width: 160 },
    { title: "金额", width: 160 },
    { title: "交易后余额", width: 160 },
    { title: "是否隐藏", width: 100 },
    { title: "交易编号", width: 240 },
    { title: "创建时间", width: 170 },
    { title: "备注", width: 200 },
  ]);
  assert.doesNotMatch(flow.slice(flow.indexOf("const flowColumns"), flow.indexOf("const rowSelection")), /title:\s*"操作"/);
  assert.match(flow, /key: "serialCode"[^\n]*width: 200[^\n]*fixed: "left"/);
  assert.match(flow, /:scroll="\{ x: 1702, y: 'calc\(100vh - 440px\)' \}"/);
});

test("withdrawal columns match the live order and widths without a fake conversion field", () => {
  assert.deepEqual(columnSpecs(withdrawal, "withdrawalColumns", "rowSelection"), [
    { title: "用户名", width: 140 },
    { title: "手机号码", width: 140 },
    { title: "上级用户名", width: 140 },
    { title: "提现账户", width: 300 },
    { title: "余额信息", width: 180 },
    { title: "金额", width: 120 },
    { title: "附件", width: 100 },
    { title: "出金类型", width: 120 },
    { title: "状态", width: 100 },
    { title: "创建时间", width: 170 },
    { title: "备注", width: 200 },
    { title: "交易类型", width: 100 },
    { title: "订单号", width: 200 },
    { title: "是否隐藏", width: 100 },
    { title: "手续费", width: 80 },
    { title: "最后修改人", width: 120 },
    { title: "最后修改时间", width: 170 },
    { title: "操作", width: 190 },
  ]);
  assert.doesNotMatch(withdrawal, /convertedAmount|转换后金额/);
  assert.match(withdrawal, /:scroll="\{ x: 2702, y: 'calc\(100vh - 440px\)' \}"/);
});

test("live status semantics render as badge dots rather than tags", () => {
  for (const page of [recharge, flow, withdrawal]) {
    assert.doesNotMatch(page, /<dict-tag/);
    assert.match(page, /<a-badge status="processing" :text="transactionTypeText\(record\.transactionType\)"/);
    assert.match(page, /function transactionTypeText\(value\)[\s\S]*?liveTransactionTypeOptions\.value\.find[\s\S]*?dictText\(transaction_type, value\)/);
    assert.match(page, /"0": Object\.freeze\(\{ status: "error", text: "是" \}\)/);
    assert.match(page, /"1": Object\.freeze\(\{ status: "error", text: "否" \}\)/);
  }

  for (const page of [recharge, withdrawal]) {
    assert.match(page, /"1": Object\.freeze\(\{ status: "processing", text: "待审核" \}\)/);
    assert.match(page, /"2": Object\.freeze\(\{ status: "success", text: "已通过" \}\)/);
    assert.match(page, /"3": Object\.freeze\(\{ status: "error", text: "已拒绝" \}\)/);
  }

  assert.match(recharge, /column\.dataIndex === 'withdrawalType'[\s\S]{0,160}dictText\(order_zhlx, record\.withdrawalType\)/);
  assert.match(withdrawal, /column\.key === 'withdrawalType'[\s\S]{0,120}withdrawalTypeText\(record\)/);
});

test("server sorting is constrained to qualified page-local whitelist columns", () => {
  assert.deepEqual(frozenObject(recharge, "sortColumnMap"), {
    amount: "gr.amount",
    withdrawalType: "gr.withdrawal_type",
    receivedAmount: "gr.received_amount",
    status: "gr.status",
    createTime: "gr.create_time",
    transactionType: "gr.transaction_type",
    orderNumber: "gr.order_number",
    isHidden: "gr.is_hidden",
  });
  assert.deepEqual(frozenObject(flow, "sortColumnMap"), {
    serialCode: "gtf.serial_code",
    transactionType: "gtf.transaction_type",
    transactionAmount: "gtf.transaction_amount",
    isHidden: "gtf.is_hidden",
    createdTime: "gtf.created_time",
  });
  assert.deepEqual(frozenObject(withdrawal, "sortColumnMap"), {
    amount: "ow.amount",
    withdrawalType: "gwa.withdrawal_type_id",
    status: "ow.status",
    createTime: "ow.create_time",
    transactionType: "ow.transaction_type",
    orderNumber: "ow.order_number",
    isHidden: "ow.is_hidden",
  });

  for (const page of [recharge, flow, withdrawal]) {
    assert.match(page, /@change="handleTableChange"/);
    assert.match(page, /sorter\?\.order \? sortColumnMap\[columnKey\] \|\| null : null/);
    assert.match(page, /sorter\?\.order === "ascend"[\s\S]*?"asc"[\s\S]*?"desc"/);
    assert.match(page, /defaultSortOrder: "descend"/);
  }
  assert.match(recharge, /orderByColumn: "gr\.order_number"/);
  assert.match(flow, /orderByColumn: "gtf\.serial_code"/);
  assert.match(withdrawal, /orderByColumn: "ow\.order_number"/);
});

test("transaction lists statically guard rows, totals, and loading with the latest request id", () => {
  for (const page of [recharge, flow, withdrawal]) {
    const getList = functionDeclaration(page, "getList");
    assert.match(page, /let listRequestId = 0;/);
    assert.match(getList, /const requestId = \+\+listRequestId;/);
    assert.match(getList, /if \(requestId !== listRequestId\) return;/);
    assert.match(getList, /finally[\s\S]*?if \(requestId === listRequestId\) loading\.value = false;/);
  }

  const withdrawalGetList = functionDeclaration(withdrawal, "getList");
  assert.match(
    withdrawalGetList,
    /const rows = await enrichSensitiveAccounts\(response\.rows \|\| \[\]\);\s*if \(requestId !== listRequestId\) return;\s*withdrawalList\.value = rows;/,
  );
}
);

test("recharge and flow keep the newest out-of-order response and loading owner", async () => {
  const cases = [
    {
      name: "recharge",
      page: recharge,
      options: { apiName: "listRecharge", listName: "rechargeList", queryIsRef: true },
    },
    {
      name: "flow",
      page: flow,
      options: {
        apiName: "listFlow",
        listName: "flowList",
        queryIsRef: false,
        includeRequestParams: true,
      },
    },
  ];

  for (const { name, page, options } of cases) {
    const lateCalls = [];
    const lateHarness = listHarness(page, options, () => {
      const call = deferred();
      lateCalls.push(call);
      return call.promise;
    });
    const staleRequest = lateHarness.getList();
    const latestRequest = lateHarness.getList();
    lateCalls[1].resolve({ rows: [{ id: `${name}-latest` }], total: 2 });
    await latestRequest;
    lateCalls[0].resolve({ rows: [{ id: `${name}-stale` }], total: 1 });
    await staleRequest;
    assert.deepEqual(lateHarness.rows.value, [{ id: `${name}-latest` }]);
    assert.equal(lateHarness.total.value, 2);

    const loadingCalls = [];
    const loadingHarness = listHarness(page, options, () => {
      const call = deferred();
      loadingCalls.push(call);
      return call.promise;
    });
    const firstRequest = loadingHarness.getList();
    const pendingLatestRequest = loadingHarness.getList();
    loadingCalls[0].resolve({ rows: [{ id: `${name}-stale-first` }], total: 1 });
    await firstRequest;
    assert.equal(loadingHarness.loading.value, true, `${name} stale request released loading`);
    assert.deepEqual(loadingHarness.rows.value, []);
    loadingCalls[1].resolve({ rows: [{ id: `${name}-latest-last` }], total: 3 });
    await pendingLatestRequest;
    assert.equal(loadingHarness.loading.value, false);
    assert.deepEqual(loadingHarness.rows.value, [{ id: `${name}-latest-last` }]);
    assert.equal(loadingHarness.total.value, 3);
  }
});

test("withdrawal latest-request guard spans the sensitive-account enrichment batch", async () => {
  const options = {
    apiName: "listWithdrawal",
    listName: "withdrawalList",
    queryIsRef: true,
  };

  const staleEnrichment = deferred();
  const latestEnrichment = deferred();
  const responses = [
    { rows: [{ id: "stale" }], total: 1 },
    { rows: [{ id: "latest" }], total: 2 },
  ];
  let responseIndex = 0;
  const harness = listHarness(
    withdrawal,
    options,
    async () => responses[responseIndex++],
    (rows) => rows[0].id === "stale" ? staleEnrichment.promise : latestEnrichment.promise,
  );
  const staleRequest = harness.getList();
  const latestRequest = harness.getList();
  latestEnrichment.resolve([{ id: "latest-enriched" }]);
  await latestRequest;
  staleEnrichment.resolve([{ id: "stale-enriched" }]);
  await staleRequest;
  assert.deepEqual(harness.rows.value, [{ id: "latest-enriched" }]);
  assert.equal(harness.total.value, 2);
  assert.equal(harness.loading.value, false);

  const staleFirstEnrichment = deferred();
  const pendingLatestEnrichment = deferred();
  let secondResponseIndex = 0;
  const loadingHarness = listHarness(
    withdrawal,
    options,
    async () => responses[secondResponseIndex++],
    (rows) => rows[0].id === "stale"
      ? staleFirstEnrichment.promise
      : pendingLatestEnrichment.promise,
  );
  const staleFirstRequest = loadingHarness.getList();
  const pendingLatestRequest = loadingHarness.getList();
  staleFirstEnrichment.resolve([{ id: "stale-first-enriched" }]);
  await staleFirstRequest;
  assert.equal(loadingHarness.loading.value, true);
  assert.deepEqual(loadingHarness.rows.value, []);
  pendingLatestEnrichment.resolve([{ id: "latest-last-enriched" }]);
  await pendingLatestRequest;
  assert.equal(loadingHarness.loading.value, false);
  assert.deepEqual(loadingHarness.rows.value, [{ id: "latest-last-enriched" }]);
  assert.equal(loadingHarness.total.value, 2);
});

test("list lifecycle always releases loading and clears stale row selection", () => {
  for (const page of [recharge, flow, withdrawal]) {
    assert.match(page, /columnWidth: 32/);
    assert.match(page, /function clearSelection\(\)[\s\S]*?ids\.value = \[\]/);
    assert.match(page, /function handleQuery\(\)[\s\S]*?clearSelection\(\)[\s\S]*?pageNum/);
    assert.match(page, /function handleAntPageChange\([^)]*\)[\s\S]*?clearSelection\(\)[\s\S]*?pageSize/);
    assert.match(page, /function handleHidden\([^)]*\)[\s\S]*?clearSelection\(\)/);
    assert.match(page, /finally\s*\(?(?:\(\)\s*=>)?\s*\{[\s\S]*?loading\.value = false/);
    assert.match(page, /size: 'small', showSizeChanger: true/);
    assert.match(page, /:key="tableResetKey"/);
    assert.match(page, /function resetQuery\(\)[\s\S]*?tableResetKey\.value \+= 1/);
    assert.doesNotMatch(page, /function resetQuery\(\)[\s\S]*?advancedSearchVisible\.value = false/);
  }
});

test("dictionary fallbacks unwrap refs and ignore empty entries before formatting", () => {
  for (const page of [recharge, flow, withdrawal]) {
    assert.match(page, /const values = Array\.isArray\(options\) \? options : options\?\.value/);
    assert.match(page, /selectDictLabel\(\(values \|\| \[\]\)\.filter\(Boolean\), value\)/);
  }
});

test("row operation links use the live default button height while flow keeps its compact copy icon", () => {
  assert.doesNotMatch(operationMarkup(recharge), /size="small"/);
  assert.doesNotMatch(operationMarkup(withdrawal), /size="small"/);
  assert.match(flow, /column\.key === 'transactionCode'[\s\S]{0,400}size="small"/);
});

test("review and sensitive-account overlays match the live modal and drawer geometry", () => {
  for (const page of [recharge, withdrawal]) {
    assert.match(page, /v-if="dialogMode === 'reject'"[\s\S]*?title="拒绝"[\s\S]*?width="450px"/);
    assert.match(page, /<a-drawer[\s\S]*?title="备注"[\s\S]*?width="85%"/);
  }
  assert.match(withdrawal, /<a-drawer[\s\S]*?title="提现地址"[\s\S]*?width="85%"/);
});

test("amount inputs and withdrawal totals preserve decimal strings", () => {
  for (const page of [recharge, flow, withdrawal]) {
    assert.equal((page.match(/string-mode/g) || []).length, 2);
    assert.doesNotMatch(page, /\bNumber\s*\(/);
  }

  const start = withdrawal.indexOf("function plainAmount");
  const end = withdrawal.indexOf("async function copyAccountValue", start);
  const monetaryFunctions = withdrawal.slice(start, end);
  const helpers = Function(
    `"use strict"; ${monetaryFunctions}; return { plainAmount, addDecimalStrings, isNegativeAmount };`,
  )();

  assert.equal(helpers.plainAmount("1.2300"), "1.2300");
  assert.equal(helpers.addDecimalStrings("9007199254740993.10", "0.20"), "9007199254740993.30");
  assert.equal(helpers.addDecimalStrings("-1.25", "0.20"), "-1.05");
  assert.equal(helpers.isNegativeAmount("-0.00"), false);
  assert.equal(helpers.isNegativeAmount("-0.01"), true);
});

test("transaction table typography is page-scoped to the live computed style", () => {
  const headerSelector = ".transaction-alignment-page :deep(.ant-pro-table .ant-table-thead > tr > th)";
  const sorterSelector = ".transaction-alignment-page :deep(.ant-pro-table .ant-table-column-sorters)";
  const labelSelector = ".transaction-alignment-page :deep(.ant-pro-query-form .ant-form-item-label)";
  const pickerSelector = ".transaction-alignment-page :deep(.ant-pro-query-form .ant-picker)";
  const scrollbarSelector = ":global(body:has(.transaction-alignment-page)::-webkit-scrollbar)";

  for (const page of [recharge, flow, withdrawal]) {
    const pageRule = cssRuleBody(page, ".transaction-alignment-page {");
    const headerRule = cssRuleBody(page, headerSelector);
    const sorterRule = cssRuleBody(page, sorterSelector);
    const labelRule = cssRuleBody(page, labelSelector);
    const pickerRule = cssRuleBody(page, pickerSelector);
    const scrollbarRule = cssRuleBody(page, scrollbarSelector);

    assert.match(pageRule, /padding-top: 28px;/);
    assert.match(pageRule, /margin-bottom: 0;/);
    assert.doesNotMatch(pageRule, /margin-right:/);
    assert.doesNotMatch(headerRule, /(?:^|\n)\s*height:/);
    assert.match(headerRule, /padding: 12px 8px;/);
    assert.match(headerRule, /font-size: 15px;/);
    assert.match(headerRule, /font-weight: 600;/);
    assert.match(headerRule, /line-height: 23\.57px;/);
    assert.match(sorterRule, /height: 23\.57px;/);
    assert.match(labelRule, /flex: 0 0 80px;/);
    assert.match(labelRule, /max-width: 80px;/);
    assert.match(pickerRule, /height: 32px;/);
    assert.match(pickerRule, /padding-block: 4px;/);
    assert.match(scrollbarRule, /width: 15px;/);
    assert.match(page, /:global\(body:has\(\.transaction-alignment-page\) \.copyright\) \{[\s\S]*?display: none;/);
    assert.match(page, /:global\(body:has\(\.transaction-alignment-page\) \.app-main\) \{[\s\S]*?padding-bottom: 0 !important;/);
    assert.match(page, /ant-badge-status-text/);
  }
});
