const FLOW_TRANSACTION_TYPE_FALLBACK_LABELS = Object.freeze({
  jj: "奖金",
  rwjl: "任务奖励",
});

/**
 * Provides stable labels for canonical flow types that must never leak as raw
 * codes while the asynchronously loaded system dictionary is unavailable.
 */
export function flowTransactionTypeFallbackLabel(value) {
  if (value === null || value === undefined) return undefined;
  return FLOW_TRANSACTION_TYPE_FALLBACK_LABELS[String(value).trim().toLowerCase()];
}
