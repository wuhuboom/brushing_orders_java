export const DEFAULT_CERTIFICATE_TIP = "非正式员工不可以查看证书";

export function normalizeCertificateSwitch(value) {
  return ["1", "true"].includes(String(value).trim().toLowerCase()) ? "1" : "0";
}

export function parseCertificateConfig(raw) {
  let parsed = raw;
  if (typeof raw === "string") {
    if (!raw.trim()) {
      parsed = {};
    } else {
      try {
        parsed = JSON.parse(raw);
      } catch {
        parsed = raw;
      }
    }
  }
  const values = parsed && typeof parsed === "object" && !Array.isArray(parsed)
    ? { ...parsed }
    : {};
  const wrapped = values.value && typeof values.value === "object" && !Array.isArray(values.value)
    ? values.value
    : {};
  const readText = keys => {
    for (const key of keys) {
      for (const source of [values, wrapped]) {
        if (typeof source[key] === "string" && source[key].trim()) return source[key];
      }
    }
    return undefined;
  };
  const blankTip = [values.certificateTip, wrapped.certificateTip]
    .find(value => typeof value === "string");
  return {
    ...values,
    certificateContent: String(
      typeof parsed === "string"
        ? parsed
        : readText(["certificateContent", "certificate", "content"]) ?? ""
    ),
    certificateTip: readText(["certificateTip"]) ?? blankTip ?? DEFAULT_CERTIFICATE_TIP,
  };
}

export function encodeCertificateConfig(source, model) {
  const certificateContent = model.certificateContent ?? "";
  const values = {
    ...source,
    certificateSwitch: normalizeCertificateSwitch(model.certificateSwitch),
    certificateTip: model.certificateTip ?? DEFAULT_CERTIFICATE_TIP,
    certificateContent,
  };
  for (const alias of ["certificate", "content"]) {
    if (Object.hasOwn(source, alias)) {
      values[alias] = certificateContent;
    }
  }
  if (source.value && typeof source.value === "object" && !Array.isArray(source.value)) {
    values.value = {
      ...source.value,
      certificateContent,
      certificateTip: values.certificateTip,
    };
    for (const alias of ["certificate", "content"]) {
      if (Object.hasOwn(source.value, alias)) values.value[alias] = certificateContent;
    }
  }
  return JSON.stringify(values);
}
