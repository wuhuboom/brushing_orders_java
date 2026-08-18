import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const source = (name) => readFileSync(
  new URL(`../src/views/member/orderuser/components/${name}`, import.meta.url),
  "utf8",
);

const gift = source("OrderuserGiftModal.vue");
const bonus = source("OrderuserBonusDrawer.vue");
const extra = source("OrderuserExtracommissionDrawer.vue");

function deferred() {
  let resolve;
  let reject;
  const promise = new Promise((resolvePromise, rejectPromise) => {
    resolve = resolvePromise;
    reject = rejectPromise;
  });
  return { promise, resolve, reject };
}

function compileHandler(component, functionName, bindings, sessionName) {
  const marker = `async function ${functionName}()`;
  const start = component.indexOf(marker);
  assert.notEqual(start, -1, `${functionName} must exist`);
  const braceStart = component.indexOf("{", start);
  let depth = 0;
  let end = -1;
  for (let index = braceStart; index < component.length; index += 1) {
    if (component[index] === "{") depth += 1;
    if (component[index] === "}") depth -= 1;
    if (depth === 0) {
      end = index + 1;
      break;
    }
  }
  assert.notEqual(end, -1, `${functionName} must have a complete body`);
  const functionSource = component.slice(start, end);
  const names = Object.keys(bindings);
  const factory = Function(
    ...names,
    `"use strict"; ${functionSource}; return { run: ${functionName}, setSession(value) { ${sessionName} = value; } };`,
  );
  return factory(...Object.values(bindings));
}

test("gift modal keeps the reference fields and rejects non-finite or non-positive amounts", () => {
  assert.match(gift, /title="赠送"[\s\S]*?width="800px"/);
  for (const label of ["用户名", "手机号", "余额", "冻结", "总余额", "金额", "备注"]) {
    assert.match(gift, new RegExp(`label="${label}"`));
  }
  assert.match(gift, /amount:\s*null/);
  assert.match(gift, /:min="0\.01"/);
  assert.match(gift, /Number\.isFinite\(amount\)\s*&&\s*amount\s*>\s*0/);
  assert.match(gift, /amount:\s*Number\(form\.amount\)/);
  assert.match(gift, /finiteNumber\(data\.totalBalance, balance \+ frozenBalance\)/);
});

test("gift submission is single-flight and closing clears values and validation", () => {
  assert.match(gift, /:confirm-loading="submitting"/);
  assert.match(gift, /:closable="!submitting"/);
  assert.match(gift, /:keyboard="!submitting"/);
  assert.match(gift, /if \(submitting\.value\) return;/);
  assert.match(gift, /const submitSession = modalSession;[\s\S]*?submitting\.value = true;[\s\S]*?await formRef\.value\?\.validate/);
  assert.match(gift, /finally\s*\{\s*submitting\.value = false;/);
  assert.match(gift, /@after-close="handleAfterClose"/);
  assert.match(gift, /function handleAfterClose\(\)\s*\{\s*if \(props\.modelValue\) return;[\s\S]*?resetForm\(\)[\s\S]*?resetUserInfo\(\)[\s\S]*?clearValidate/);
});

test("gift modal locks before delayed validation and an old request cannot close a reopened session", async () => {
  const validation = deferred();
  const request = deferred();
  const submitting = { value: false };
  let validateCalls = 0;
  let requestCalls = 0;
  const events = [];
  const props = { modelValue: true, userId: 7 };
  const { run, setSession } = compileHandler(gift, "submit", {
    submitting,
    props,
    formRef: { value: { validate: () => {
      validateCalls += 1;
      return validation.promise;
    } } },
    modalSession: 1,
    giftAmount: () => {
      requestCalls += 1;
      return request.promise;
    },
    form: { amount: 8.5, remark: "test" },
    emit: (...event) => events.push(event),
    proxy: { $modal: { msgSuccess: () => {}, msgError: () => {} } },
  }, "modalSession");

  const first = run();
  const duplicateDuringValidation = run();
  assert.equal(submitting.value, true);
  assert.equal(validateCalls, 1);
  assert.equal(requestCalls, 0);

  validation.resolve();
  await Promise.resolve();
  assert.equal(requestCalls, 1);
  setSession(2);
  const duplicateDuringRequest = run();
  assert.equal(validateCalls, 1);
  assert.equal(submitting.value, true);

  request.resolve();
  await Promise.all([first, duplicateDuringValidation, duplicateDuringRequest]);
  assert.equal(submitting.value, false);
  assert.deepEqual(events, [["success"]], "the completed old request must not close the reopened modal");
});

test("gift modal releases its pre-validation lock after validation fails", async () => {
  const submitting = { value: false };
  let validateCalls = 0;
  let requestCalls = 0;
  const { run } = compileHandler(gift, "submit", {
    submitting,
    props: { modelValue: true, userId: 7 },
    formRef: { value: { validate: async () => {
      validateCalls += 1;
      if (validateCalls === 1) throw { errorFields: [{ name: "amount" }] };
    } } },
    modalSession: 1,
    giftAmount: async () => { requestCalls += 1; },
    form: { amount: 1, remark: "" },
    emit: () => {},
    proxy: { $modal: { msgSuccess: () => {}, msgError: () => {} } },
  }, "modalSession");

  await run();
  assert.equal(submitting.value, false);
  assert.equal(requestCalls, 0);
  await run();
  assert.equal(validateCalls, 2);
  assert.equal(requestCalls, 1);
});

test("bonus uses 85/65 percent drawers and preserves reference defaults", () => {
  assert.match(bonus, /title="彩金设置"[\s\S]{0,160}?width="85%"/);
  assert.match(bonus, /v-model:open="dialogOpen"[\s\S]*?width="65%"/);
  assert.match(bonus, /dialogTitle\.value = "创建"/);
  assert.doesNotMatch(bonus, /<a-modal\b/);
  assert.match(bonus, /displayDuration:\s*0/);
  assert.match(bonus, /distributionType:\s*"2"/);
  assert.match(bonus, /pushType:\s*"0"/);
});

test("bonus member summary matches the reference table header title", () => {
  const searchIndex = bonus.indexOf('<template #search>');
  const titleIndex = bonus.indexOf('<template #title>');
  const toolbarIndex = bonus.indexOf('<template #toolbar>');
  const titleBlock = bonus.slice(titleIndex, toolbarIndex);

  assert.ok(searchIndex >= 0 && searchIndex < titleIndex && titleIndex < toolbarIndex);
  assert.doesNotMatch(bonus, /title="彩金列表"/);
  assert.equal((titleBlock.match(/class="member-summary-field"/g) || []).length, 4);

  let previousIndex = -1;
  for (const label of ["用户名: ", "手机号码: ", "余额: ", "任务进度: ", "最后登录时间: "]) {
    const currentIndex = titleBlock.indexOf(label);
    assert.ok(currentIndex > previousIndex, `${label} should keep the reference order`);
    previousIndex = currentIndex;
  }

  assert.match(titleBlock, /余额: \{\{ user\.balance \?\? 0 \}\}/);
  assert.match(titleBlock, /任务进度: \{\{ taskProgressDisplay \}\}/);
  assert.match(titleBlock, /最后登录时间: \{\{ parseTime\(user\.lastLoginTime\) \|\| "-" \}\}/);
  assert.match(titleBlock, /type="link"[\s\S]*?@click="refreshAll"[\s\S]*?<ReloadOutlined/);
  assert.doesNotMatch(titleBlock, /<strong\b|>刷新</);

  assert.match(bonus, /\.member-summary\s*\{[\s\S]*?font-size:\s*16px;[\s\S]*?font-weight:\s*700;[\s\S]*?line-height:\s*16px;/);
  assert.match(bonus, /\.member-summary-field\s*\{\s*margin-right:\s*10px;/);
  assert.match(bonus, /ant-pro-search-card \.ant-card-body\)\s*\{\s*padding:\s*24px 0;/);
  assert.match(bonus, /ant-pro-table-card \.ant-card-body\)\s*\{\s*padding:\s*0;/);
  assert.match(bonus, /ant-pro-table-toolbar\)\s*\{\s*padding:\s*16px 0;/);
  assert.doesNotMatch(bonus, /\.user-summary\b|gap:\s*24px/);
});

test("bonus mutations are guarded, report failures, and refresh their owner", () => {
  assert.match(bonus, /:closable="!mutationPending"/);
  assert.match(bonus, /:keyboard="!mutationPending"/);
  assert.match(bonus, /submitting\.value[\s\S]*?deleting\.value[\s\S]*?receivingIds\.value\.size[\s\S]*?givingIds\.value\.size/);
  assert.match(bonus, /async function submitForm\(\)\s*\{\s*if \(submitting\.value\) return;\s*submitting\.value = true;\s*try \{\s*await bonusRef\.value\?\.validate/);
  assert.match(bonus, /receivingIds\.value\.has\(key\)\) return/);
  assert.match(bonus, /givingIds\.value\.has\(key\)\) return/);
  assert.match(bonus, /if \(deleting\.value \|\| !ids\.value\.length\) return/);
  assert.match(bonus, /reportMutationError\(error, "领取彩金失败", confirmed\)/);
  assert.match(bonus, /reportMutationError\(error, "发放彩金失败", confirmed\)/);
  const giveBlock = bonus.slice(
    bonus.indexOf("async function handleGive"),
    bonus.indexOf("function isReceiving"),
  );
  assert.match(giveBlock, /emit\("success"\)/);
  assert.match(bonus, /getCheckboxProps: \(record\) => \(\{ disabled: record\.isReceived !== "1" \}\)/);
  assert.match(bonus, /record\.isReceived === '1'[\s\S]*?@click="handlePushEdit\(record\)"/);
  assert.match(bonus, /:disabled="record\.isDistributed === '0' \|\| record\.isReceived === '0'"/);
});

test("bonus identifiers stay strings and form resets cannot leak response-only fields", () => {
  assert.match(bonus, /getOrderuserOperationSummary/);
  assert.doesNotMatch(bonus, /\bgetOrderuser\(/);
  assert.match(bonus, /id:\s*normalizeIdentifier\(row\.id\)/);
  assert.match(bonus, /value:\s*normalizeIdentifier\(item\.id\)/);
  assert.match(bonus, /return values\.map\(normalizeIdentifier\)\.filter\(Boolean\)/);
  assert.doesNotMatch(bonus, /map\(Number\)/);
  assert.doesNotMatch(bonus, /Number\(item\.id\)/);
  assert.match(bonus, /Object\.keys\(form\)[\s\S]*?delete form\[key\]/);
  const payloadBlock = bonus.slice(
    bonus.indexOf("function buildBonusPayload"),
    bonus.indexOf("function handlePushTypeChange"),
  );
  assert.doesNotMatch(payloadBlock, /\.\.\.form/);
  assert.match(payloadBlock, /userId:\s*normalizeIdentifier\(props\.userId\)/);
  assert.match(payloadBlock, /expiryTime:\s*toExpiryTimestamp\(form\.expiryTime\)/);
  assert.match(bonus, /form\.expiryTime = formatExpiryInput\(form\.expiryTime\)/);
  assert.match(bonus, /function toExpiryTimestamp\(value\)[\s\S]*?Number\.isFinite\(timestamp\)/);
  assert.match(bonus, /requestSequence !== listRequestSequence[\s\S]*?normalizeIdentifier\(props\.userId\) !== targetId/);
});

test("extra commission uses nested drawers and numeric positive fields", () => {
  assert.match(extra, /title="额外佣金设置"[\s\S]*?width="85%"/);
  assert.match(extra, /v-model:open="formVisible"[\s\S]*?width="65%"/);
  assert.match(extra, /formTitle\.value = "创建"/);
  assert.doesNotMatch(extra, /<a-modal\b/);
  assert.match(extra, /v-model:value="form\.orderCount"[\s\S]*?:min="1"[\s\S]*?:precision="0"/);
  assert.match(extra, /v-model:value="form\.productPrice"[\s\S]*?:min="0\.01"/);
  assert.match(extra, /v-model:value="form\.amount"[\s\S]*?:min="0\.01"/);
  assert.match(extra, /Number\.isFinite\(number\) && number > 0 && Number\.isInteger\(number\)/);
  assert.match(extra, /isLocked:\s*"1"/);
});

test("extra commission follows backend lifecycle boundaries and uses an allowlisted payload", () => {
  assert.match(extra, /getOrderuserOperationSummary/);
  assert.doesNotMatch(extra, /\bgetOrderuser\(/);
  assert.match(extra, /:closable="!mutationPending"/);
  assert.match(extra, /:keyboard="!mutationPending"/);
  assert.match(extra, /const mutationPending = computed\(\(\) => submitting\.value \|\| deletingIds\.value\.size > 0\)/);
  assert.match(extra, /String\(row\?\.status\) === "1" && String\(row\?\.isLocked\) === "1"/);
  assert.match(extra, /:disabled="!canMutate\(record\)"/);
  assert.match(extra, /:disabled="!canMutate\(record\) \|\| isDeleting\(record\)"/);
  const payloadBlock = extra.slice(
    extra.indexOf("function buildExtraCommissionPayload"),
    extra.indexOf("function closeFormDrawer"),
  );
  assert.match(payloadBlock, /orderCount:\s*Number\(form\.orderCount\)/);
  assert.match(payloadBlock, /productPrice:\s*Number\(form\.productPrice\)/);
  assert.match(payloadBlock, /amount:\s*Number\(form\.amount\)/);
  assert.doesNotMatch(payloadBlock, /isLocked|status|createTime|\.\.\.form/);
  assert.doesNotMatch(extra, /updateExtracommission\(form\)|addExtracommission\(form\)/);
  assert.match(extra, /if \(submitting\.value\) return;\s*submitting\.value = true;\s*try \{\s*await formRef\.value\?\.validate/);
  assert.match(extra, /function closeFormDrawer\(\)[\s\S]*?resetFormData\(\)/);
  assert.match(extra, /Object\.keys\(form\)[\s\S]*?delete form\[key\]/);
});
