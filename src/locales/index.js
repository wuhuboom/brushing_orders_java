import Cookies from "js-cookie";
import { createI18n } from "vue-i18n";
import zhCNMessages from "./zh-CN";
import enUSMessages from "./en-US";
import zhCNElement from "element-plus/es/locale/lang/zh-cn";
import enUSElement from "element-plus/es/locale/lang/en";

export const LANGUAGE_COOKIE_KEY = "app-locale";
const fallbackLocale = "zh_CN";

const messages = {
  zh_CN: zhCNMessages,
  en_US: enUSMessages,
};

const elementLocaleMap = {
  zh_CN: zhCNElement,
  en_US: enUSElement,
};

// Normalize saved locale to use underscore variant (e.g. zh-CN -> zh_CN)
const rawSaved = Cookies.get(LANGUAGE_COOKIE_KEY) || fallbackLocale;
const savedLocale = (rawSaved || "").replace(/-/g, "_");

const i18n = createI18n({
  legacy: false,
  globalInjection: true,
  locale: savedLocale,
  fallbackLocale,
  messages,
});

export function setupI18n(app) {
  app.use(i18n);
}

export function getCurrentLocale() {
  return i18n.global.locale.value;
}

export function resolveElementLocale(locale) {
  return elementLocaleMap[locale] || elementLocaleMap[fallbackLocale];
}

export function changeLocale(locale) {
  if (!locale) return getCurrentLocale();
  const norm = String(locale).replace(/-/g, "_");
  if (!messages[norm]) {
    return getCurrentLocale();
  }
  i18n.global.locale.value = norm;
  Cookies.set(LANGUAGE_COOKIE_KEY, norm);
  return norm;
}

export const localeOptions = [
  { label: zhCNMessages.navbar.chinese, value: "zh_CN", shortLabel: "中文" },
  { label: enUSMessages.navbar.english, value: "en_US", shortLabel: "EN" },
];

export default i18n;
