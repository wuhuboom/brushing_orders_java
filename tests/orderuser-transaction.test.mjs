import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

import {
  GIFT_BY_AMOUNT,
  GIFT_BY_RATIO,
  OPERATION_ADD,
  OPERATION_SUBTRACT,
  buildTransactionPayload,
  calculateGiftAmount,
  createTransactionForm,
  isGiftTransaction,
  isPositiveTransactionAmount,
  toBackendGiftType,
  transactionTypeOptions,
} from "../src/views/member/orderuser/components/orderuserTransaction.js";

const transactionModalSource = readFileSync(
  new URL("../src/views/member/orderuser/components/OrderuserTransactionModal.vue", import.meta.url),
  "utf8",
);

test("transaction types and defaults match the reference member adjustment form", () => {
  assert.deepEqual(transactionTypeOptions.map(({ label }) => label), [
    "手续费", "存款", "奖金", "底薪", "援助金", "商品分润", "补贴", "资金异常存款", "信誉分", "其他",
  ]);
  assert.deepEqual(createTransactionForm(), {
    operationType: OPERATION_ADD,
    transactionType: "ck",
    amount: null,
    giftType: GIFT_BY_RATIO,
    giftRatio: 0,
    giftAmount: 0,
    remark: null,
  });
});

test("gift controls only apply to deposits and preserve reference percentage math", () => {
  assert.equal(isGiftTransaction({ operationType: OPERATION_ADD, transactionType: "ck" }), true);
  assert.equal(isGiftTransaction({ operationType: OPERATION_SUBTRACT, transactionType: "ck" }), false);
  assert.equal(isGiftTransaction({ operationType: OPERATION_ADD, transactionType: "bonus" }), false);
  assert.equal(calculateGiftAmount(100, 10), 10);
  assert.equal(calculateGiftAmount(12.345, 10), 1.23);
  assert.equal(calculateGiftAmount(null, 10), 0);
});

test("transaction payload neutralizes hidden gifts and keeps eligible fixed gifts", () => {
  const form = { ...createTransactionForm(), amount: 100, giftRatio: 10, giftAmount: 10 };
  assert.deepEqual(buildTransactionPayload(7, form), {
    userId: 7,
    operationType: OPERATION_ADD,
    transactionType: "ck",
    amount: 100,
    giftType: 0,
    giftRatio: 10,
    giftAmount: 10,
    remark: null,
  });

  assert.deepEqual(buildTransactionPayload(7, { ...form, operationType: OPERATION_SUBTRACT }), {
    userId: 7,
    operationType: OPERATION_SUBTRACT,
    transactionType: "ck",
    amount: 100,
    giftType: null,
    giftRatio: 0,
    giftAmount: 0,
    remark: null,
  });

  const fixedGift = buildTransactionPayload(7, { ...form, giftType: GIFT_BY_AMOUNT, giftAmount: 25 });
  assert.equal(fixedGift.giftType, 1);
  assert.equal(fixedGift.giftAmount, 25);
  assert.equal(toBackendGiftType(GIFT_BY_RATIO), 0);
  assert.equal(toBackendGiftType(GIFT_BY_AMOUNT), 1);
});

test("transaction amount must be a finite positive number", () => {
  assert.equal(isPositiveTransactionAmount(0), false);
  assert.equal(isPositiveTransactionAmount(-1), false);
  assert.equal(isPositiveTransactionAmount(null), false);
  assert.equal(isPositiveTransactionAmount("bad"), false);
  assert.equal(isPositiveTransactionAmount("0.01"), true);
});

test("gift mode changes trigger recalculation and only the active gift field is validated", () => {
  assert.match(
    transactionModalSource,
    /\[\(\) => form\.amount, \(\) => form\.giftRatio, \(\) => form\.giftType\]/,
  );
  assert.match(transactionModalSource, /form\.giftType !== GIFT_BY_RATIO/);
  assert.match(transactionModalSource, /form\.giftType !== GIFT_BY_AMOUNT/);
});
