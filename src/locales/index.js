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

const savedLocale = Cookies.get(LANGUAGE_COOKIE_KEY) || fallbackLocale;

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
  if (!messages[locale]) {
    return getCurrentLocale();
  }
  i18n.global.locale.value = locale;
  Cookies.set(LANGUAGE_COOKIE_KEY, locale);
  return locale;
}

export const localeOptions = [
  { label: zhCNMessages.navbar.chinese, value: "zh_CN", shortLabel: "中文" },
  { label: enUSMessages.navbar.english, value: "en_US", shortLabel: "EN" },
];

export default i18n;
