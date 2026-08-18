import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const identitySource = readFileSync(
  new URL(
    "../src/views/member/orderuser/components/OrderuserIdentityModal.vue",
    import.meta.url,
  ),
  "utf8",
).replace(/\r\n/g, "\n");
const contractSource = readFileSync(
  new URL(
    "../src/views/member/orderuser/components/OrderuserContractModal.vue",
    import.meta.url,
  ),
  "utf8",
).replace(/\r\n/g, "\n");

function payloadKeys(source, functionName) {
  const match = source.match(
    new RegExp(`function ${functionName}\\(\\) \\{\\s*return \\{([\\s\\S]*?)\\n  \\};\\n\\}`),
  );
  assert.ok(match, `${functionName} should return an explicit object`);
  return [...match[1].matchAll(/^\s{4}(\w+):/gm)].map((item) => item[1]);
}

function extractFunction(source, functionName) {
  const asyncStart = source.indexOf(`async function ${functionName}(`);
  const start = asyncStart >= 0 ? asyncStart : source.indexOf(`function ${functionName}(`);
  assert.notEqual(start, -1, `${functionName} should exist`);
  const openingBrace = source.indexOf("{", start);
  let depth = 0;
  for (let index = openingBrace; index < source.length; index += 1) {
    if (source[index] === "{") depth += 1;
    if (source[index] === "}") depth -= 1;
    if (depth === 0) return source.slice(start, index + 1);
  }
  assert.fail(`${functionName} should have a closing brace`);
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

function createSubmitHarness(source, payloadBuilderName, { validation, update } = {}) {
  const context = {
    submitting: { value: false },
    loading: { value: false },
    isLoadedUser: { value: true },
    props: { modelValue: true, userId: 7 },
    form: { id: 7 },
    formRef: {
      value: {
        validate: validation || (() => Promise.resolve()),
      },
    },
    proxy: { $modal: { msgError() {}, msgSuccess() {} } },
    visible: { value: true },
    emit() {},
    updateOrderuser: update || (() => Promise.resolve()),
    buildPayload: () => ({ id: 7 }),
    sameIdentifier: (left, right) => String(left) === String(right),
    detailRequestSequence: 3,
  };
  const createHarness = new Function(
    "context",
    `
      const {
        submitting, loading, isLoadedUser, props, form, formRef, proxy, visible,
        emit, updateOrderuser, sameIdentifier
      } = context;
      const ${payloadBuilderName} = context.buildPayload;
      let detailRequestSequence = context.detailRequestSequence;
      ${extractFunction(source, "isCurrentEditSession")}
      ${extractFunction(source, "handleSubmit")}
      return {
        handleSubmit,
        setSessionSequence(value) { detailRequestSequence = value; },
      };
    `,
  );
  return { context, ...createHarness(context) };
}

test("identity and contract editors use non-dismissable 70% drawers", () => {
  for (const source of [identitySource, contractSource]) {
    assert.match(source, /<a-drawer/);
    assert.doesNotMatch(source, /<a-modal/);
    assert.match(source, /width="70%"/);
    assert.match(source, /:mask-closable="false"/);
    assert.match(source, /:closable="!submitting"/);
    assert.match(source, /:keyboard="!submitting"/);
    assert.match(source, /<template #footer>/);
    assert.match(source, /:loading="submitting"/);
    assert.match(source, /@click="handleCancel"/);
    assert.match(source, /@click="handleSubmit"/);
  }
  assert.match(identitySource, /title="编辑身份信息"/);
  assert.match(contractSource, /title="编辑合同"/);
});

test("identity required fields and stored value mappings are preserved", () => {
  for (const field of ["identityType", "identityName", "identityNumber", "identityStatus"]) {
    assert.match(identitySource, new RegExp(`${field}: \\[\\{ required: true`));
  }
  assert.match(identitySource, /value="id_card">身份证/);
  assert.match(identitySource, /value="driver_license">驾驶证/);
  assert.match(identitySource, /value="passport">护照/);
  assert.match(identitySource, /value="0">待审核/);
  assert.match(identitySource, /value="1">通过/);
  assert.match(identitySource, /value="2">拒绝/);
});

test("all four contract switches are named and required", () => {
  assert.match(contractSource, /:name="field.key"/);
  assert.match(contractSource, /const rules = Object\.fromEntries/);
  for (const field of [
    "userContractEnabled",
    "userContractSigned",
    "formalContractEnabled",
    "formalContractSigned",
  ]) {
    assert.match(contractSource, new RegExp(`key: "${field}"`));
  }
  assert.match(contractSource, /\[\{ required: true, message: `请选择\$\{label\}`/);
});

test("detail loading is resettable, sequenced, and bound to the current member", () => {
  for (const source of [identitySource, contractSource]) {
    assert.match(source, /let detailRequestSequence = 0/);
    assert.match(source, /const requestSequence = \+\+detailRequestSequence/);
    assert.match(source, /requestSequence !== detailRequestSequence/);
    assert.match(source, /sameIdentifier\(props\.userId, userId\)/);
    assert.match(source, /sameIdentifier\(form\.id, props\.userId\)/);
    assert.match(source, /function resetForm\(\)/);
    assert.match(source, /detailRequestSequence \+= 1/);
    assert.match(source, /formRef\.value\?\.clearValidate\(\)/);
    assert.match(source, /function handleClose\(\)[\s\S]*?visible\.value = false/);
    assert.match(source, /if \(submitting\.value \|\| loading\.value\) return/);
    assert.match(source, /尚未加载完成，请稍后重试/);
    assert.match(source, /sessionSequence === detailRequestSequence/);
    assert.match(source, /isCurrentEditSession\(submittedUserId, submittedSessionSequence\)/);
    assert.equal(
      (source.match(/submitting\.value = false;/g) || []).length,
      1,
      "only the active submit finally block may release the write guard",
    );
  }
});

test("delayed validation locks identity and contract submit before a double click", async () => {
  for (const [source, payloadBuilderName] of [
    [identitySource, "buildIdentityPayload"],
    [contractSource, "buildContractPayload"],
  ]) {
    const validation = deferred();
    let validationCalls = 0;
    let updateCalls = 0;
    const harness = createSubmitHarness(source, payloadBuilderName, {
      validation: () => {
        validationCalls += 1;
        return validation.promise;
      },
      update: async () => {
        updateCalls += 1;
      },
    });

    const firstSubmit = harness.handleSubmit();
    const secondSubmit = harness.handleSubmit();
    assert.equal(harness.context.submitting.value, true);
    assert.equal(validationCalls, 1);
    assert.equal(updateCalls, 0);

    validation.resolve();
    await Promise.all([firstSubmit, secondSubmit]);
    assert.equal(updateCalls, 1);
    assert.equal(harness.context.submitting.value, false);
  }
});

test("validation failure unlocks while an old write cannot close a reopened session", async () => {
  for (const [source, payloadBuilderName] of [
    [identitySource, "buildIdentityPayload"],
    [contractSource, "buildContractPayload"],
  ]) {
    const invalidHarness = createSubmitHarness(source, payloadBuilderName, {
      validation: () => Promise.reject(new Error("invalid")),
    });
    await invalidHarness.handleSubmit();
    assert.equal(invalidHarness.context.submitting.value, false);

    const write = deferred();
    let updateCalls = 0;
    const activeHarness = createSubmitHarness(source, payloadBuilderName, {
      update: () => {
        updateCalls += 1;
        return write.promise;
      },
    });
    const oldSubmit = activeHarness.handleSubmit();
    await Promise.resolve();
    assert.equal(updateCalls, 1);
    assert.equal(activeHarness.context.submitting.value, true);

    activeHarness.context.props.modelValue = false;
    activeHarness.setSessionSequence(4);
    activeHarness.context.props.modelValue = true;
    activeHarness.context.visible.value = true;
    await activeHarness.handleSubmit();
    assert.equal(updateCalls, 1, "the reopened session stays locked until the old write settles");

    write.resolve();
    await oldSubmit;
    assert.equal(activeHarness.context.visible.value, true, "the old write must not close the new session");
    assert.equal(activeHarness.context.submitting.value, false);
  }
});

test("identity and contract submissions use field whitelists and report outcomes", () => {
  assert.deepEqual(payloadKeys(identitySource, "buildIdentityPayload"), [
    "id",
    "identityType",
    "identityName",
    "identityNumber",
    "identityFrontImage",
    "identityBackImage",
    "identityHandheldImage",
    "identityStatus",
    "identityRemarks",
  ]);
  assert.deepEqual(payloadKeys(contractSource, "buildContractPayload"), [
    "id",
    "userContractEnabled",
    "userContractSigned",
    "formalContractEnabled",
    "formalContractSigned",
    "userContractContent",
    "formalContractContent",
  ]);
  for (const source of [identitySource, contractSource]) {
    assert.match(source, /await updateOrderuser\(payload\)/);
    assert.doesNotMatch(source, /updateOrderuser\(\{ \.\.\.form \}\)/);
    assert.match(source, /emit\("success"\)/);
    assert.match(source, /保存成功/);
    assert.match(source, /保存失败，请重试/);
  }
});
