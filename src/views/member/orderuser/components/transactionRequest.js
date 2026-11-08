// Persist the exact intent before sending. A missing status never means it is safe to create a new intent.
export function transactionStorageKey(api, operator, userId) {
  if (!operator || userId == null) throw new Error("请重新登录后再操作");
  return `admin-transaction:v1:${api}:${operator}:${userId}`;
}

export function loadTransaction(storage, key) {
  const raw = storage.getItem(key);
  if (!raw) return null;
  const value = JSON.parse(raw);
  if (!value.requestId || value.userId == null) throw new Error("待确认交易记录异常，请联系管理员核对流水");
  return value;
}

export function prepareTransaction(storage, key, payload, cryptoApi = globalThis.crypto) {
  const existing = loadTransaction(storage, key);
  if (existing) return existing;
  const bytes = cryptoApi.getRandomValues(new Uint8Array(16));
  const requestId = Array.from(bytes, value => value.toString(16).padStart(2, "0")).join("");
  const intent = { ...payload, requestId };
  storage.setItem(key, JSON.stringify(intent)); // Fail closed when storage is unavailable.
  return intent;
}

export function clearTransaction(storage, key, requestId) {
  if (loadTransaction(storage, key)?.requestId === requestId) storage.removeItem(key);
}

export function terminalTransaction(result, requestId) {
  return result?.requestId === requestId && ["SUCCEEDED", "FAILED"].includes(result.state);
}
