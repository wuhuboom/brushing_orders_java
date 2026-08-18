export const OPERATION_ADD = 0;
export const OPERATION_SUBTRACT = 1;
export const GIFT_BY_RATIO = 1;
export const GIFT_BY_AMOUNT = 2;

const BACKEND_GIFT_BY_RATIO = 0;
const BACKEND_GIFT_BY_AMOUNT = 1;

export const transactionTypeOptions = Object.freeze([
  { value: "sxf", label: "手续费" },
  { value: "ck", label: "存款" },
  { value: "bonus", label: "奖金" },
  { value: "dx", label: "底薪" },
  { value: "yzj", label: "援助金" },
  { value: "spfr", label: "商品分润" },
  { value: "bt", label: "补贴" },
  { value: "zjyc", label: "资金异常存款" },
  { value: "xyd", label: "信誉分" },
  { value: "qt", label: "其他" },
]);

export function createTransactionForm() {
  return {
    operationType: OPERATION_ADD,
    transactionType: "ck",
    amount: null,
    giftType: GIFT_BY_RATIO,
    giftRatio: 0,
    giftAmount: 0,
    remark: null,
  };
}

export function isGiftTransaction(form) {
  return Number(form?.operationType) === OPERATION_ADD && form?.transactionType === "ck";
}

export function calculateGiftAmount(amount, ratio) {
  const numericAmount = Number(amount);
  const numericRatio = Number(ratio);
  if (!Number.isFinite(numericAmount) || !Number.isFinite(numericRatio)) {
    return 0;
  }
  return Number((numericAmount * numericRatio / 100).toFixed(2));
}

export function isPositiveTransactionAmount(value) {
  return value !== null && value !== undefined && value !== "" && Number.isFinite(Number(value)) && Number(value) > 0;
}

export function toBackendGiftType(giftType) {
  return Number(giftType) === GIFT_BY_AMOUNT ? BACKEND_GIFT_BY_AMOUNT : BACKEND_GIFT_BY_RATIO;
}

export function buildTransactionPayload(userId, form) {
  const includeGift = isGiftTransaction(form);
  return {
    userId,
    operationType: form.operationType,
    transactionType: form.transactionType,
    amount: form.amount,
    giftType: includeGift ? toBackendGiftType(form.giftType) : null,
    giftRatio: includeGift ? Number(form.giftRatio ?? 0) : 0,
    giftAmount: includeGift ? Number(form.giftAmount ?? 0) : 0,
    remark: form.remark || null,
  };
}
