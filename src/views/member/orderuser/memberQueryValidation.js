const JAVA_LONG_MAX = "9223372036854775807";

export function normalizeMemberIdQuery(value) {
  if (value === null || value === undefined) return null;
  if (typeof value === "number" && !Number.isSafeInteger(value)) return null;

  const normalized = String(value).trim();
  if (!/^[1-9]\d*$/.test(normalized)) return null;
  if (normalized.length > JAVA_LONG_MAX.length) return null;
  if (normalized.length === JAVA_LONG_MAX.length && normalized > JAVA_LONG_MAX) return null;

  return normalized;
}
