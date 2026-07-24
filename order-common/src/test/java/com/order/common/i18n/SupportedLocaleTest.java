package com.order.common.i18n;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SupportedLocaleTest {

    @Test
    void exposesAndResolvesAllTwentyOnePersistedLanguages() {
        assertEquals(21, SupportedLocale.all().size());
        for (SupportedLocale locale : SupportedLocale.values()) {
            assertEquals(locale, SupportedLocale.from(locale.getCode()));
            assertEquals(locale, SupportedLocale.from(locale.toLanguageTag()));
        }
        assertEquals(12, SupportedLocale.all().stream().filter(SupportedLocale::isH5Enabled).count());
    }

    @Test
    void acceptsLanguageAliasesAndChineseScripts() {
        assertEquals(SupportedLocale.EN_US, SupportedLocale.from("en"));
        assertEquals(SupportedLocale.NO_NO, SupportedLocale.from("nb-NO"));
        assertEquals(SupportedLocale.ZH_CN, SupportedLocale.from("zh-Hans"));
        assertEquals(SupportedLocale.ZH_TW, SupportedLocale.from("zh-Hant-HK"));
        assertEquals(SupportedLocale.PT_BR, SupportedLocale.from("pt-BR"));
        assertNull(SupportedLocale.from("xx-ZZ"));
    }

    @Test
    void explicitLanguageWinsAndUnknownExplicitLanguageUsesEnglish() {
        assertEquals(
                SupportedLocale.DE_DE,
                SupportedLocale.resolve("de-DE", "fr-FR,es-ES;q=0.8"));
        assertEquals(
                SupportedLocale.EN_US,
                SupportedLocale.resolve("xx-ZZ", "fr-FR,es-ES;q=0.8"));
    }

    @Test
    void acceptLanguageUsesQualityWeightsAndSkipsUnsupportedRanges() {
        assertEquals(
                SupportedLocale.FR_FR,
                SupportedLocale.resolve(null, "xx-ZZ;q=1, de-DE;q=0.4, fr-FR;q=0.9"));
        assertEquals(SupportedLocale.EN_US, SupportedLocale.resolve(null, "fr-FR;q=0"));
        assertEquals(SupportedLocale.EN_US, SupportedLocale.resolve(null, "malformed;q=abc"));
        assertEquals(SupportedLocale.EN_US, SupportedLocale.resolve(null, null));
    }

    @Test
    void valueFallbackIsRequestedThenEnglishThenBase() {
        Translations translations = new Translations();
        translations.setEsEs("Español");
        translations.setEnUs("English");

        assertEquals("Español", TranslationResolver.resolve(translations, SupportedLocale.ES_ES, "Base"));
        assertEquals("English", TranslationResolver.resolve(translations, SupportedLocale.FR_FR, "Base"));

        translations.setEnUs(" ");
        assertEquals("Base", TranslationResolver.resolve(translations, SupportedLocale.FR_FR, "Base"));
        assertNull(TranslationResolver.resolve(translations, SupportedLocale.FR_FR, " "));
        assertEquals(
                List.of("Español", " ", "Base"),
                TranslationResolver.candidates(translations, SupportedLocale.ES_ES, "Base"));
    }
}
