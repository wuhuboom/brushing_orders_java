import assert from "node:assert/strict";
import test from "node:test";
import { deferred, giftHarness, memoryStorage, settle } from "./helpers/gift-modal-harness.mjs";

test("gift locks before validation and persists the exact request before posting", async () => {
  const validation = deferred();
  const response = deferred();
  let validates = 0;
  const posted = [];
  const h = giftHarness({
    validate: () => { validates++; return validation.promise; },
    giftAmount: intent => {
      posted.push(intent);
      assert.deepEqual(JSON.parse(h.storage.getItem(h.state.journalKey)), intent);
      return response.promise;
    },
  });
  await settle();
  h.state.form.amount = 8.5;
  h.state.form.remark = "gift";
  const first = h.state.submit();
  const second = h.state.submit();
  assert.equal(h.state.submitting.value, true);
  assert.equal(validates, 1);
  assert.equal(posted.length, 0);
  validation.resolve();
  await settle();
  assert.equal(posted.length, 1);
  assert.match(posted[0].requestId, /^[a-zA-Z0-9_-]{16,64}$/);
  response.resolve({ data: { requestId: posted[0].requestId, state: "SUCCEEDED" } });
  await Promise.all([first, second]);
  assert.equal(h.messages.success.length, 1);
  assert.equal(h.state.pending.value, null);
  assert.equal(h.storage.getItem(h.state.journalKey), null);
});

test("FAILED gift results never display success or close the modal", async () => {
  const h = giftHarness({ giftAmount: async intent => ({
    data: { requestId: intent.requestId, state: "FAILED", message: "余额更新失败，未入账" },
  }) });
  await settle();
  h.state.form.amount = 1;
  await h.state.submit();
  assert.deepEqual(h.events, []);
  assert.deepEqual(h.messages.success, []);
  assert.deepEqual(h.messages.errors, ["余额更新失败，未入账"]);
  assert.equal(h.state.pending.value, null);
  assert.equal(h.storage.getItem(h.state.journalKey), null);
});

test("invalid form validation releases the lock and never creates or sends a gift", async () => {
  let validations = 0;
  let posts = 0;
  const h = giftHarness({
    validate: async () => {
      if (++validations === 1) throw { errorFields: [{ name: "amount" }] };
    },
    giftAmount: async intent => {
      posts++;
      return { data: { requestId: intent.requestId, state: "SUCCEEDED" } };
    },
  });
  await settle();
  await h.state.submit();
  assert.equal(h.state.submitting.value, false);
  assert.equal(h.state.pending.value, null);
  assert.equal(posts, 0);
  assert.equal(h.storage.getItem(h.state.journalKey), null);
  h.state.form.amount = 1;
  await h.state.submit();
  assert.equal(posts, 1);
});

test("timeout and page refresh restore and replay the original gift even if fields change", async () => {
  const storage = memoryStorage();
  const first = giftHarness({ storage, giftAmount: async () => { throw Error("timeout"); } });
  await settle();
  first.state.form.amount = 12;
  first.state.form.remark = "original";
  await first.state.submit();
  const saved = JSON.parse(storage.getItem(first.state.journalKey));
  assert.equal(first.state.pending.value.requestId, saved.requestId);
  assert.equal(first.messages.success.length, 0);
  first.unmount();
  let replayed;
  const refreshed = giftHarness({ storage, giftAmount: async intent => {
    replayed = intent;
    return { data: { requestId: intent.requestId, state: "SUCCEEDED" } };
  } });
  await settle();
  assert.equal(refreshed.state.form.amount, 12);
  assert.equal(refreshed.state.form.remark, "original");
  refreshed.state.form.amount = 99;
  await refreshed.state.submit();
  assert.deepEqual(replayed, saved);
  assert.equal(refreshed.messages.success.length, 1);
  assert.equal(storage.getItem(refreshed.state.journalKey), null);
});

test("reopening a completed gift confirms it by status without issuing another credit", async () => {
  const storage = memoryStorage();
  const first = giftHarness({ storage, giftAmount: async () => { throw Error("timeout"); } });
  await settle();
  first.state.form.amount = 12;
  await first.state.submit();
  first.unmount();
  let posts = 0;
  const restored = giftHarness({ storage,
    giftAmount: async () => { posts++; },
    transactionStatus: async requestId => ({ data: { requestId, state: "SUCCEEDED" } }),
  });
  await settle();
  assert.equal(posts, 0);
  assert.equal(restored.messages.success.length, 1);
  assert.equal(restored.state.pending.value, null);
});

test("unknown or mismatched status preserves the gift and cannot report success", async () => {
  for (const result of [{ state: "UNKNOWN" }, { state: "SUCCEEDED", requestId: "different-request" }]) {
    const h = giftHarness({ giftAmount: async intent => ({
      data: { requestId: intent.requestId, ...result },
    }) });
    await settle();
    h.state.form.amount = 2;
    await h.state.submit();
    assert.ok(h.state.pending.value);
    assert.ok(h.storage.getItem(h.state.journalKey));
    assert.equal(h.messages.success.length, 0);
    assert.deepEqual(h.events, []);
    h.unmount();
  }
});

test("a saved request can be safely retried when status lookup fails", async () => {
  const h = giftHarness({ giftAmount: async () => { throw Error("timeout"); } });
  await settle();
  h.state.form.amount = 3;
  await h.state.submit();
  const original = JSON.parse(h.storage.getItem(h.state.journalKey));
  h.context.transactionStatus = async () => { throw Error("status unavailable"); };
  let replayed;
  h.context.giftAmount = async intent => {
    replayed = intent;
    return { data: { requestId: intent.requestId, state: "SUCCEEDED" } };
  };
  await h.state.submit();
  assert.deepEqual(replayed, original);
  assert.equal(h.messages.success.length, 1);
});

test("storage failure stops a new gift before it is sent", async () => {
  let posts = 0;
  const h = giftHarness({
    storage: { getItem: () => null, setItem: () => { throw Error("storage full"); } },
    giftAmount: async () => { posts++; },
  });
  await settle();
  h.state.form.amount = 5;
  await h.state.submit();
  assert.equal(posts, 0);
  assert.equal(h.state.submitting.value, false);
  assert.equal(h.state.pending.value, null);
  assert.deepEqual(h.messages.errors, ["storage full"]);
});

test("switching users during validation cannot send the previous user's gift", async () => {
  const validation = deferred();
  let posts = 0;
  const h = giftHarness({ validate: () => validation.promise, giftAmount: async () => { posts++; } });
  await settle();
  h.state.form.amount = 2;
  const submission = h.state.submit();
  h.setProps({ userId: 8 });
  validation.resolve();
  await submission;
  assert.equal(posts, 0);
  assert.equal(h.state.pending.value, null);
  assert.equal(h.state.form.amount, null);
});

test("late gift responses cannot close or unlock a newer user's active submission", async () => {
  const oldResponse = deferred();
  const newResponse = deferred();
  const posted = [];
  const h = giftHarness({ giftAmount: intent => {
    posted.push(intent);
    return posted.length === 1 ? oldResponse.promise : newResponse.promise;
  } });
  await settle();
  h.state.form.amount = 2;
  const oldSubmission = h.state.submit();
  await settle();
  h.setProps({ userId: 8 });
  await settle();
  h.state.form.amount = 3;
  const newSubmission = h.state.submit();
  await settle();
  oldResponse.resolve({ data: { requestId: posted[0].requestId, state: "SUCCEEDED" } });
  await oldSubmission;
  assert.equal(h.state.submitting.value, true);
  assert.equal(h.state.pending.value.requestId, posted[1].requestId);
  assert.equal(h.messages.success.length, 0);
  assert.deepEqual(h.events, []);
  newResponse.resolve({ data: { requestId: posted[1].requestId, state: "SUCCEEDED" } });
  await newSubmission;
  assert.equal(h.messages.success.length, 1);
});

test("late terminal responses preserve a newer journal and pending gift", async () => {
  const h = giftHarness({ giftAmount: async () => { throw Error("timeout"); } });
  await settle();
  h.state.form.amount = 2;
  await h.state.submit();
  const old = h.state.pending.value;
  const newer = { ...old, requestId: "newer-request-123456", amount: 3 };
  h.storage.setItem(h.state.journalKey, JSON.stringify(newer));
  h.state.pending.value = newer;
  const messageCount = h.messages.success.length;
  h.state.acceptResult({ requestId: old.requestId, state: "SUCCEEDED" }, old, h.state.journalKey, 1);
  assert.equal(JSON.parse(h.storage.getItem(h.state.journalKey)).requestId, newer.requestId);
  assert.equal(h.state.pending.value.requestId, newer.requestId);
  assert.equal(h.messages.success.length, messageCount);
});

test("an old polling response cannot restart polling after the modal switches users", async () => {
  const response = deferred();
  const h = giftHarness({
    giftAmount: async () => { throw Error("timeout"); },
    transactionStatus: () => response.promise,
  });
  await settle();
  h.state.form.amount = 2;
  await h.state.submit();
  const requestId = h.state.pending.value.requestId;
  h.setProps({ userId: 8 });
  response.resolve({ data: { requestId, state: "UNKNOWN" } });
  await settle();
  assert.equal(h.timers.size, 0);
  assert.equal(h.state.pending.value, null);
  assert.equal(h.messages.success.length, 0);
});
