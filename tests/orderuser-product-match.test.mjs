import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";
import {
  buildProductMatchPayload,
  productMatchValidationErrors,
} from "../src/views/member/orderuser/components/orderuserProductMatch.js";

const source = readFileSync(new URL(
  "../src/views/member/orderuser/components/OrderuserProductMatchModal.vue", import.meta.url,
), "utf8");

const personal = { id: 7, version: 9, productMatching: "0", productMatchMin: 70, productMatchMax: 90 };

test("personal matching requires a positive ordered range within 100% with two-decimal precision", () => {
  assert.deepEqual(productMatchValidationErrors(personal), {});
  assert.deepEqual(productMatchValidationErrors({ ...personal, productMatchMin: 0.01, productMatchMax: 100 }), {});
  assert.deepEqual(productMatchValidationErrors({ ...personal, productMatchMin: 80, productMatchMax: 80 }), {});
  for (const [field, value] of [
    ["productMatchMin", null], ["productMatchMax", ""], ["productMatchMin", 0],
    ["productMatchMin", -1], ["productMatchMax", 101], ["productMatchMax", Infinity],
    ["productMatchMax", "invalid"], ["productMatchMin", 70.001], ["productMatchMax", 60],
  ]) {
    assert.ok(productMatchValidationErrors({ ...personal, [field]: value })[field], `${field}=${value} must fail`);
  }
  assert.throws(() => buildProductMatchPayload({ ...personal, productMatchMax: 20 }), /不能小于/);
});

test("disabling the personal override allows empty or zero bounds and still rejects invalid numbers", () => {
  for (const bounds of [{ productMatchMin: null, productMatchMax: null }, { productMatchMin: 0, productMatchMax: 0 }]) {
    assert.deepEqual(productMatchValidationErrors({ ...personal, productMatching: "1", ...bounds }), {});
  }
  assert.ok(productMatchValidationErrors({ ...personal, productMatching: "1", productMatchMin: -1 }).productMatchMin);
  assert.ok(productMatchValidationErrors({ ...personal, productMatching: "1", productMatchMax: 60 }).productMatchMax);
});

test("personal matching payload contains only the intended member and expected version", () => {
  const payload = buildProductMatchPayload({ ...personal, balance: 999, vipId: 1, taskProgress: 30, password: "hidden" });
  assert.deepEqual(payload, personal);
});

function extractFunction(name) {
  const start = source.search(new RegExp(`(?:async )?function ${name}\\(`));
  assert.notEqual(start, -1);
  const openingBrace = source.indexOf("{", start);
  let depth = 0;
  for (let i = openingBrace; i < source.length; i += 1) {
    if (source[i] === "{") depth += 1;
    if (source[i] === "}") depth -= 1;
    if (depth === 0) return source.slice(start, i + 1);
  }
  assert.fail(`Unclosed function: ${name}`);
}

function deferred() {
  let resolve;
  const promise = new Promise((done) => { resolve = done; });
  return { promise, resolve };
}

function createHarness({ get = async (id) => ({ data: { ...personal, id } }), validate = async () => {}, update = async () => {} } = {}) {
  const context = {
    props: { modelValue: true, userId: 7 },
    form: { ...personal },
    storedRange: { productMatchMin: personal.productMatchMin, productMatchMax: personal.productMatchMax },
    defaults: { id: null, version: null, productMatching: "1", productMatchMin: null, productMatchMax: null },
    loading: { value: false },
    submitting: { value: false },
    isLoadedUser: { value: true },
    visible: { value: true },
    formRef: { value: { clearValidate() {}, validate } },
    getOrderuser: get,
    updateOrderuser: update,
    buildProductMatchPayload,
    nextTick: async () => {},
    proxy: { $modal: { msgError() {}, msgSuccess() {} } },
    emit() {},
  };
  return { context, ...new Function("context", `
    const { props, form, storedRange, defaults, loading, submitting, isLoadedUser, visible, formRef,
      getOrderuser, updateOrderuser, buildProductMatchPayload, nextTick, proxy, emit } = context;
    let detailRequestSequence = 0;
    ${extractFunction("isCurrentEditSession")}
    ${extractFunction("loadProductMatch")}
    ${extractFunction("revalidateRange")}
    ${extractFunction("handleSubmit")}
    return { loadProductMatch, revalidateRange, handleSubmit };
  `)(context) };
}

test("a late detail response cannot replace the next member's personal matching settings", async () => {
  const first = deferred();
  const second = deferred();
  const harness = createHarness({ get: (id) => id === 7 ? first.promise : second.promise });
  const firstLoad = harness.loadProductMatch(true, 7);
  await Promise.resolve();
  harness.context.props.userId = 8;
  const secondLoad = harness.loadProductMatch(true, 8);
  await Promise.resolve();
  second.resolve({ data: { ...personal, id: 8, version: 22, productMatchMin: 20 } });
  await secondLoad;
  first.resolve({ data: personal });
  await firstLoad;
  assert.equal(harness.context.form.id, 8);
  assert.equal(harness.context.form.version, 22);
  assert.equal(harness.context.form.productMatchMin, 20);
});

test("disabling personal matching retains the saved bounds instead of pretending to clear them", async () => {
  const harness = createHarness();
  await harness.loadProductMatch(true, 7);
  Object.assign(harness.context.form, { productMatchMin: 10, productMatchMax: null, productMatching: "1" });
  harness.revalidateRange();
  assert.deepEqual(buildProductMatchPayload(harness.context.form), { ...personal, productMatching: "1" });
});

test("delayed validation accepts one save and a member change prevents a stale save", async () => {
  const validation = deferred();
  const writes = [];
  const harness = createHarness({ validate: () => validation.promise, update: async (data) => { writes.push(data); } });
  const first = harness.handleSubmit();
  const duplicate = harness.handleSubmit();
  assert.equal(harness.context.submitting.value, true);
  validation.resolve();
  await Promise.all([first, duplicate]);
  assert.deepEqual(writes, [personal]);

  const staleValidation = deferred();
  const stale = createHarness({ validate: () => staleValidation.promise, update: async (data) => { writes.push(data); } });
  const pending = stale.handleSubmit();
  stale.context.props.userId = 8;
  staleValidation.resolve();
  await pending;
  assert.equal(writes.length, 1);
});
