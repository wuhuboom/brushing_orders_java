function missing(value) {
  return value == null || String(value).trim() === "";
}

export function productMatchValidationErrors(form) {
  const errors = {};
  const enabled = String(form.productMatching) === "0";
  if (!["0", "1"].includes(String(form.productMatching))) {
    errors.productMatching = "请选择产品匹配状态";
  }
  for (const [field, label] of [
    ["productMatchMin", "最小匹配比例"],
    ["productMatchMax", "最大匹配比例"],
  ]) {
    const value = form[field];
    if (missing(value)) {
      if (enabled) errors[field] = `请输入${label}`;
      continue;
    }
    const number = Number(value);
    if (!Number.isFinite(number) || number < 0 || number > 100 || (enabled && number === 0)) {
      errors[field] = enabled
        ? `${label}必须大于 0 且不超过 100%`
        : `${label}必须在 0～100% 之间`;
    } else if (Math.abs(number * 100 - Math.round(number * 100)) > 1e-8) {
      errors[field] = `${label}最多保留两位小数`;
    }
  }
  if (!missing(form.productMatchMin) && !missing(form.productMatchMax)
    && !errors.productMatchMin && !errors.productMatchMax
    && Number(form.productMatchMin) > Number(form.productMatchMax)) {
    errors.productMatchMax = "最大匹配比例不能小于最小匹配比例";
  }
  return errors;
}

export function buildProductMatchPayload(form) {
  const error = Object.values(productMatchValidationErrors(form))[0];
  if (error) throw new Error(error);
  return {
    id: form.id,
    version: form.version,
    productMatching: String(form.productMatching),
    productMatchMin: missing(form.productMatchMin) ? null : Number(form.productMatchMin),
    productMatchMax: missing(form.productMatchMax) ? null : Number(form.productMatchMax),
  };
}
