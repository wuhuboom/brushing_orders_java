import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const source = (path) => readFileSync(new URL(`../${path}`, import.meta.url), "utf8");
const page = source("src/views/member/orderuser/index.vue");
const form = source("src/views/member/orderuser/components/OrderuserForm.vue");

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

test("reference-sized operation dialogs expose loading guards and reset handlers", () => {
  for (const loading of [
    "modifySubmitting", "loginSubmitting", "tradeSubmitting", "parentSubmitting",
    "vipSubmitting", "reputationSubmitting", "signDaysSubmitting",
  ]) {
    assert.match(page, new RegExp(`:confirm-loading="${loading}"`));
    assert.match(page, new RegExp(`:closable="!${loading}"`));
    assert.match(page, new RegExp(`:keyboard="!${loading}"`));
    assert.match(page, new RegExp(`:cancel-button-props="\\{ disabled: ${loading} \\}"`));
  }
  assert.equal((page.match(/width="416px"/g) || []).length, 7);
  for (const close of [
    "closeModifyCount", "closeLoginPassword", "closeTradePassword", "closeModifyParent",
    "closeModifyVip", "closeModifyReputation", "closeModifySignDays",
  ]) {
    assert.match(page, new RegExp(`@cancel="${close}"`));
  }
});

test("dangerous row actions identify their target and cannot be submitted twice", () => {
  assert.match(page, /pendingMemberActions = reactive\(new Set\(\)\)/);
  assert.match(page, /pendingMemberActions\.has\(key\)/);
  assert.match(page, /会员“\$\{memberTarget\(row\)\}”/);
  assert.match(page, /:loading="isMemberActionPending\(record, 'resetOrder'\)"/);
  assert.match(page, /payload: \{ id: row\.id, version: row\.version, accountStatus: newValue \}/);
  assert.match(page, /:trigger="\['hover'\]"/);
  assert.match(page, /\{\{ buildFakeMemberToggle\(record\.isFake\)\.action \}\}/);
  assert.match(page, /const \{ value: newValue, action \} = buildFakeMemberToggle\(row\.isFake\);/);
});

test("operation payloads preserve zero reputation and exclude display-only or hidden fields", () => {
  assert.match(page, /row\.reputationScore \?\? 100/g);
  assert.match(page, /updateOrderuser\(buildModifyCountPayload\(modifyForm\)\)/);
  assert.match(page, /buildCopyMemberForm\(defaults, row \|\| \{\}/);
  assert.doesNotMatch(page, /function handleCopyMember\(row\)[\s\S]{0,320}?getOrderuser\(/);
  assert.match(form, /buildMemberSubmitPayload\(props\.formData/);
  assert.doesNotMatch(form, /\.\.\.props\.formData/);
  assert.match(form, /v-if="!isEditMode"/);
  assert.match(form, /:disabled="isEditMode"/);
});

test("operation forms expose required state and reference validation messages", () => {
  assert.match(page, /label="单数" name="taskProgress"/);
  assert.match(page, /taskProgress:\s*\[\s*\{ required: true, message: "单数是必填项！"/);
  assert.match(page, /label="登录密码" name="password"/);
  assert.match(page, /label="交易密码" name="tradePassword"/);
  assert.match(page, /password: \[requiredTrimmedRule\("登录密码"\), trimmedPasswordLengthRule\("登录密码"\)\]/);
  assert.match(page, /tradePassword: \[requiredTrimmedRule\("交易密码"\), trimmedPasswordLengthRule\("交易密码"\)\]/);
  assert.match(page, /message: `\$\{label\}是必填项！`/);
  assert.match(page, /label="上级邀请码" name="parentInviteCode" :required="!parentTopLevel"/);
  assert.match(page, /label="信誉分" name="newReputation"/);
  assert.match(page, /\{ required: true, message: "信誉分是必填项！", trigger: "change" \}/);
  assert.match(page, /label="VIP等级" name="vipId"/);
  assert.doesNotMatch(page, /label="会员等级" name="vipId"/);

  assert.match(form, /label="用户名" name="username"/);
  assert.match(form, /label="登录密码" name="password"/);
  assert.match(form, /label="交易密码" name="tradePassword"/);
  assert.match(form, /username: isEditMode\.value \? \[\] : \[requiredTrimmedRule\("用户名"\)\]/);
  assert.match(form, /tradePassword: isEditMode\.value[\s\S]*?\[requiredTrimmedRule\("交易密码"\), passwordLengthRule\("交易密码"\)\]/);
  assert.match(form, /password: isEditMode\.value[\s\S]*?\[requiredTrimmedRule\("登录密码"\), passwordLengthRule\("登录密码"\)\]/);
});

test("member form locks before delayed validation and an old request cannot close a reopened session", async () => {
  const validation = deferred();
  const request = deferred();
  const submitting = { value: false };
  const visible = { value: true };
  let validateCalls = 0;
  let requestCalls = 0;
  const events = [];
  const { run, setSession } = compileHandler(form, "handleSubmit", {
    submitting,
    props: { readonly: false, modelValue: true, formData: { id: null } },
    visible,
    formRef: { value: { validate: () => {
      validateCalls += 1;
      return validation.promise;
    } } },
    buildMemberSubmitPayload: () => ({ username: "member" }),
    birthdayToTimestamp: (value) => value,
    isEditMode: { value: false },
    updateOrderuser: async () => {},
    addOrderuser: () => {
      requestCalls += 1;
      return request.promise;
    },
    proxy: { $modal: { msgSuccess: () => {}, msgError: () => {} } },
    emit: (...event) => events.push(event),
    formSession: 1,
  }, "formSession");

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
  assert.equal(visible.value, true, "the completed old request must not close the reopened drawer");
  assert.deepEqual(events, [["success"]]);
});

test("member form releases its pre-validation lock after validation fails", async () => {
  const submitting = { value: false };
  let validateCalls = 0;
  let requestCalls = 0;
  const formRef = { value: { validate: async () => {
    validateCalls += 1;
    if (validateCalls === 1) throw { errorFields: [{ name: "username" }] };
  } } };
  const { run } = compileHandler(form, "handleSubmit", {
    submitting,
    props: { readonly: false, modelValue: true, formData: { id: null } },
    visible: { value: true },
    formRef,
    buildMemberSubmitPayload: () => ({}),
    birthdayToTimestamp: (value) => value,
    isEditMode: { value: false },
    updateOrderuser: async () => {},
    addOrderuser: async () => { requestCalls += 1; },
    proxy: { $modal: { msgSuccess: () => {}, msgError: () => {} } },
    emit: () => {},
    formSession: 1,
  }, "formSession");

  await run();
  assert.equal(submitting.value, false);
  assert.equal(requestCalls, 0);
  await run();
  assert.equal(validateCalls, 2);
  assert.equal(requestCalls, 1);
});
