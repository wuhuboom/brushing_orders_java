const normalizeValue = (value) => value === null || value === undefined || value === ""
  ? null
  : String(value);

export function genderBadgeStatus(value) {
  const normalized = normalizeValue(value);
  if (normalized === "0") return "processing";
  if (normalized === "1") return "error";
  if (normalized === "2") return "default";
  return null;
}

export function enabledBadgeStatus(value) {
  const normalized = normalizeValue(value);
  if (normalized === "0") return "success";
  if (normalized === "1") return "error";
  return null;
}

export function yesNoBadgeStatus(value) {
  const normalized = normalizeValue(value);
  if (normalized === "0") return "success";
  if (normalized === "1") return "error";
  return null;
}

export function fakeMemberTone(value) {
  const normalized = normalizeValue(value);
  if (normalized === "0") return "success";
  if (normalized === "1") return "error";
  return null;
}
