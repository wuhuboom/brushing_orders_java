package com.order.common.i18n;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Canonical locale registry for persisted translations and API negotiation.
 */
public enum SupportedLocale {
    ZH_CN("zh_CN", "简体中文", false, Translations::getZhCn),
    ZH_TW("zh_TW", "繁體中文", false, Translations::getZhTw),
    KO_KR("ko_KR", "한국어", false, Translations::getKoKr),
    TH_TH("th_TH", "ไทย", false, Translations::getThTh),
    JA_JP("ja_JP", "日本語", true, Translations::getJaJp),
    PT_PT("pt_PT", "Português", false, Translations::getPtPt),
    EN_US("en_US", "English", true, Translations::getEnUs),
    AR_SA("ar_SA", "العربية", false, Translations::getArSa),
    ES_ES("es_ES", "Español", true, Translations::getEsEs),
    SV_SE("sv_SE", "Svenska", true, Translations::getSvSe),
    IT_IT("it_IT", "Italiano", true, Translations::getItIt),
    DE_DE("de_DE", "Deutsch", true, Translations::getDeDe),
    NO_NO("no_NO", "Norsk", true, Translations::getNoNo),
    RU_RU("ru_RU", "Русский", true, Translations::getRuRu),
    HU_HU("hu_HU", "Magyar", true, Translations::getHuHu),
    PL_PL("pl_PL", "Polski", true, Translations::getPlPl),
    SK_SK("sk_SK", "Slovenčina", true, Translations::getSkSk),
    FR_FR("fr_FR", "Français", true, Translations::getFrFr),
    CS_CZ("cs_CZ", "Čeština", false, Translations::getCsCz),
    PT_BR("pt_BR", "Português (Brasil)", false, Translations::getPtBr),
    HI_IN("hi_IN", "हिन्दी", false, Translations::getHiIn);

    private static final Map<String, SupportedLocale> EXACT = new LinkedHashMap<>();
    private static final Map<String, SupportedLocale> LANGUAGE_DEFAULTS = new LinkedHashMap<>();

    static {
        for (SupportedLocale locale : values()) {
            EXACT.put(normalizeKey(locale.code), locale);
            EXACT.put(normalizeKey(locale.toLanguageTag()), locale);
        }
        LANGUAGE_DEFAULTS.put("zh", ZH_CN);
        LANGUAGE_DEFAULTS.put("ko", KO_KR);
        LANGUAGE_DEFAULTS.put("th", TH_TH);
        LANGUAGE_DEFAULTS.put("ja", JA_JP);
        LANGUAGE_DEFAULTS.put("pt", PT_PT);
        LANGUAGE_DEFAULTS.put("en", EN_US);
        LANGUAGE_DEFAULTS.put("ar", AR_SA);
        LANGUAGE_DEFAULTS.put("es", ES_ES);
        LANGUAGE_DEFAULTS.put("sv", SV_SE);
        LANGUAGE_DEFAULTS.put("it", IT_IT);
        LANGUAGE_DEFAULTS.put("de", DE_DE);
        LANGUAGE_DEFAULTS.put("no", NO_NO);
        LANGUAGE_DEFAULTS.put("nb", NO_NO);
        LANGUAGE_DEFAULTS.put("ru", RU_RU);
        LANGUAGE_DEFAULTS.put("hu", HU_HU);
        LANGUAGE_DEFAULTS.put("pl", PL_PL);
        LANGUAGE_DEFAULTS.put("sk", SK_SK);
        LANGUAGE_DEFAULTS.put("fr", FR_FR);
        LANGUAGE_DEFAULTS.put("cs", CS_CZ);
        LANGUAGE_DEFAULTS.put("hi", HI_IN);
    }

    private final String code;
    private final String label;
    private final boolean h5Enabled;
    private final Function<Translations, String> reader;

    SupportedLocale(String code, String label, boolean h5Enabled, Function<Translations, String> reader) {
        this.code = code;
        this.label = label;
        this.h5Enabled = h5Enabled;
        this.reader = reader;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public boolean isH5Enabled() {
        return h5Enabled;
    }

    public String toLanguageTag() {
        return code.replace('_', '-');
    }

    public String read(Translations translations) {
        return translations == null ? null : reader.apply(translations);
    }

    public static SupportedLocale from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = normalizeKey(value);
        SupportedLocale exact = EXACT.get(normalized);
        if (exact != null) {
            return exact;
        }
        if (normalized.startsWith("zh-hant")) {
            return ZH_TW;
        }
        if (normalized.startsWith("zh-hans")) {
            return ZH_CN;
        }
        String language = normalized.split("-", 2)[0];
        return LANGUAGE_DEFAULTS.get(language);
    }

    /**
     * Explicit lang wins. Unsupported explicit values deliberately fall back to English.
     * Without an explicit lang, the first supported Accept-Language range is used.
     */
    public static SupportedLocale resolve(String explicitLang, String acceptLanguage) {
        if (explicitLang != null && !explicitLang.isBlank()) {
            SupportedLocale explicit = from(explicitLang);
            return explicit == null ? EN_US : explicit;
        }
        if (acceptLanguage != null && !acceptLanguage.isBlank()) {
            try {
                for (Locale.LanguageRange range : Locale.LanguageRange.parse(acceptLanguage)) {
                    if (range.getWeight() <= 0) {
                        continue;
                    }
                    SupportedLocale locale = from(range.getRange());
                    if (locale != null) {
                        return locale;
                    }
                }
            } catch (IllegalArgumentException ignored) {
                // Malformed browser headers use the stable default below.
            }
        }
        return EN_US;
    }

    public static List<SupportedLocale> all() {
        return Arrays.asList(values());
    }

    private static String normalizeKey(String value) {
        return value.trim().replace('_', '-').toLowerCase(Locale.ROOT);
    }
}
