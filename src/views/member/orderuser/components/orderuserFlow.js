export function createFlowQueryParams() {
  return {
    pageNum: 1,
    pageSize: 10,
    serialCode: null,
    transactionType: null,
    amountMin: null,
    amountMax: null,
  };
}

function hasAmount(value) {
  return value !== null && value !== undefined && value !== "";
}

export function buildFlowListParams(queryParams, userId) {
  const {
    amountMin,
    amountMax,
    ...base
  } = queryParams;
  const params = {};

  if (hasAmount(amountMin)) params.amountMin = amountMin;
  if (hasAmount(amountMax)) params.amountMax = amountMax;

  return Object.keys(params).length
    ? { ...base, userId, params }
    : { ...base, userId };
}
