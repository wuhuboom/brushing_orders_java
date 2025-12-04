import i18n from "@/locales";

/**
 * Resolve a menu/title label based on current locale and meta definition.
 * Prefers translated `titleKey`, then locale-specific name fallbacks.
 *
 * @param {object} meta - Route/menu meta definition.
 * @param {Function} translate - Translator function (e.g. i18n `t`).
 * @param {string|object} currentLocale - Locale string or ref/computed containing locale.
 * @returns {string} Localized title string.
 */
export function resolveMenuTitle(meta, translate, currentLocale) {
  if (!meta) return "";
  const t = typeof translate === "function" ? translate : i18n.global.t;
  const localeValue =
    typeof currentLocale === "string"
      ? currentLocale
      : currentLocale?.value ?? i18n.global.locale;

  if (meta.titleKey) {
    const translated = t(meta.titleKey);
    if (translated && translated !== meta.titleKey) {
      return translated;
    }
  }

  const normalizedLocale = String(localeValue || "").toLowerCase();
  const titleValue = meta.title ?? meta.rawTitle ?? "";
  const enValue = meta.enName ?? meta.rawEnName ?? "";

  if (normalizedLocale.startsWith("en")) {
    return enValue || titleValue || "";
  }
  return titleValue || enValue || "";
}
