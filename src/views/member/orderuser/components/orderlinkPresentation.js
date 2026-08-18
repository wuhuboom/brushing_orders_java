export function formatOrderlinkAmount(value) {
  const text = String(value ?? "0").trim();
  if (!/^-?\d+(?:\.\d+)?$/.test(text)) return "0";
  const [integerPart, fractionPart = ""] = text.split(".");
  const trimmedFraction = fractionPart.replace(/0+$/, "");
  return trimmedFraction ? `${integerPart}.${trimmedFraction}` : integerPart;
}
