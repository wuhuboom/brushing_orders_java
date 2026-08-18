export const translationLanguages = [
  { field: "zhCn", column: "zh_CN", flag: "🇨🇳", label: "简体中文" },
  { field: "zhTw", column: "zh_TW", flag: "🇹🇼", label: "繁體中文" },
  { field: "enUs", column: "en_US", flag: "🇺🇸", label: "English" },
  { field: "jaJp", column: "ja_JP", flag: "🇯🇵", label: "日本語" },
  { field: "idId", column: "id_ID", flag: "🇮🇩", label: "Bahasa Indonesia" },
  { field: "thTh", column: "th_TH", flag: "🇹🇭", label: "ไทย" },
  { field: "ptPt", column: "pt_PT", flag: "🇵🇹", label: "Português" },
  { field: "arSa", column: "ar_SA", flag: "🇸🇦", label: "العربية" },
  { field: "esEs", column: "es_ES", flag: "🇪🇸", label: "Español" },
  { field: "svSe", column: "sv_SE", flag: "🇸🇪", label: "Svenska" },
  { field: "itIt", column: "it_IT", flag: "🇮🇹", label: "Italiano" },
  { field: "deDe", column: "de_DE", flag: "🇩🇪", label: "Deutsch" },
  { field: "noNo", column: "no_NO", flag: "🇳🇴", label: "Norsk" },
  { field: "ruRu", column: "ru_RU", flag: "🇷🇺", label: "Русский" },
  { field: "huHu", column: "hu_HU", flag: "🇭🇺", label: "Magyar" },
  { field: "plPl", column: "pl_PL", flag: "🇵🇱", label: "Polski" },
  { field: "skSk", column: "sk_SK", flag: "🇸🇰", label: "Slovenčina" },
  { field: "frFr", column: "fr_FR", flag: "🇫🇷", label: "Français" },
  { field: "csCz", column: "cs_CZ", flag: "🇨🇿", label: "Čeština" },
  { field: "ptBr", column: "pt_BR", flag: "🇧🇷", label: "Português" },
  { field: "hiIn", column: "hi_IN", flag: "🇮🇳", label: "हिन्दी, हिंदी" },
  { field: "koKr", column: "ko_KR", flag: "🇰🇷", label: "한국어" },
];

// Shared by level management and all website-setting translation drawers.
// Keep existing languages and append newly requested languages only once.
export const adminTranslationLanguageFields = [
  "enUs",
  "esEs",
  "frFr",
  "zhCn",
  "deDe",
  "itIt",
  "koKr",
  "jaJp",
  "idId",
  "svSe",
  "noNo",
  "ruRu",
  "huHu",
  "plPl",
  "skSk",
];

export const createEmptyTranslations = () =>
  translationLanguages.reduce((translations, language) => {
    translations[language.field] = "";
    return translations;
  }, {});
