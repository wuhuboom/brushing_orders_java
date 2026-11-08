package com.order.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.api.controller.dto.ConfigApiDtos;
import com.order.common.i18n.ITranslationsService;
import com.order.common.i18n.SupportedLocale;
import com.order.common.i18n.Translations;
import com.order.member.domain.GoodsMemberLevel;
import com.order.member.domain.GoodsCustomerService;
import com.order.member.domain.OrderConfig;
import com.order.member.service.IGoodsCustomerServiceService;
import com.order.member.service.IGoodsMemberLevelService;
import com.order.member.service.IOrderConfigService;
import com.order.system.service.ISysNoticeService;
import com.order.system.service.ISysTimeZoneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;


@ExtendWith(MockitoExtension.class)
class ConfigQueryServiceTest {
    @Mock
    private IOrderConfigService orderConfigService;
    @Mock
    private IGoodsCustomerServiceService customerServiceService;
    @Mock
    private IGoodsMemberLevelService levelService;
    @Mock
    private ISysNoticeService noticeService;
    @Mock
    private ISysTimeZoneService timeZoneService;
    @Mock
    private ITranslationsService translationsService;

    private ConfigQueryService service;

    @BeforeEach
    void setUp() {
        service = new ConfigQueryService(
                orderConfigService,
                customerServiceService,
                levelService,
                noticeService,
                timeZoneService,
                translationsService,
                new ObjectMapper());
    }

    @Test
    void contentUsesPerFieldFallbackWithOneBusinessAndOneTranslationQuery() {
        OrderConfig register = config(
                1L,
                11L,
                "register",
                "{\"protocolContent\":\"Base protocol\"}");
        OrderConfig about = config(
                2L,
                12L,
                "about",
                "{\"aboutContent\":\"Base about\"}");

        Translations registerTranslations = translations(
                11L,
                "{\"protocolContent\":\"English protocol\"}",
                "{\"unrelated\":\"Sin valor\"}");
        Translations aboutTranslations = translations(
                12L,
                null,
                "{\"aboutContent\":\"Acerca de\"}");

        when(orderConfigService.selectOrderConfigByTypes(anyList())).thenReturn(List.of(register, about));
        when(translationsService.selectTranslationsByIds(anyCollection()))
                .thenReturn(Map.of(11L, registerTranslations, 12L, aboutTranslations));

        ConfigApiDtos.GlobalContentResponse result = service.content(SupportedLocale.ES_ES);

        assertEquals("English protocol", result.protocolContent());
        assertEquals("Acerca de", result.aboutContent());
        assertNull(result.helpContent());
        verify(orderConfigService).selectOrderConfigByTypes(argThat(types -> types.contains("website")));
        verify(translationsService).selectTranslationsByIds(anyCollection());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"{}", "{\"certificateSwitch\":\"0\"}", "{\"certificateSwitch\":false}"})
    void disabledOrMissingLegacyWebsiteSwitchPreservesLocalizedContent(String websiteContent) {
        OrderConfig certificate = config(3L, 13L, "certificate",
                "{\"certificateContent\":\"Base certificate\"}");
        List<OrderConfig> configs = websiteContent == null
                ? List.of(certificate)
                : List.of(certificate, config(4L, null, "website", websiteContent));
        when(orderConfigService.selectOrderConfigByTypes(anyList())).thenReturn(configs);
        when(translationsService.selectTranslationsByIds(anyCollection())).thenReturn(Map.of(
                13L, translations(13L,
                        "{\"certificateContent\":\"English certificate\"}",
                        "{\"certificateContent\":\"Certificado\"}")));

        assertEquals("Certificado", service.content(SupportedLocale.ES_ES).certificateContent());
        assertEquals("English certificate", service.content(SupportedLocale.EN_US).certificateContent());
        assertEquals("", service.content(SupportedLocale.ES_ES).certificateTip());
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"1\"", "1", "true", "\"true\"", "\"TRUE\""})
    void certificateOwnedSwitchSuppressesEveryLanguageRegardlessOfTranslatedSwitch(String enabledValue) {
        OrderConfig certificate = config(3L, 13L, "certificate",
                "{\"certificateContent\":\"Base certificate\",\"certificateSwitch\":" + enabledValue + "}");
        OrderConfig website = config(4L, null, "website", "{\"certificateSwitch\":\"0\"}");
        OrderConfig about = config(2L, 12L, "about", "{\"aboutContent\":\"About us\"}");
        when(orderConfigService.selectOrderConfigByTypes(anyList()))
                .thenReturn(List.of(certificate, website, about));
        when(translationsService.selectTranslationsByIds(anyCollection())).thenReturn(Map.of(
                13L, translations(13L,
                        "{\"certificateContent\":\"English certificate\",\"certificateSwitch\":\"0\"}",
                        "{\"certificateContent\":\"Certificado\",\"certificateSwitch\":\"0\"}")));

        for (SupportedLocale locale : SupportedLocale.all()) {
            ConfigApiDtos.GlobalContentResponse result = service.content(locale);
            assertEquals("", result.certificateContent(), "locale " + locale.getCode());
            assertEquals("非正式员工不可以查看证书", result.certificateTip());
            assertEquals("About us", result.aboutContent());
        }
    }

    @Test
    void turningCertificateOwnedSwitchOffRestoresTranslationFallbackAndClearsTip() {
        OrderConfig certificate = config(3L, 13L, "certificate",
                "{\"certificateContent\":\"Base certificate\",\"certificateSwitch\":\"1\",\"certificateTip\":\"Base tip\"}");
        OrderConfig website = config(4L, null, "website", "{\"certificateSwitch\":\"1\"}");
        when(orderConfigService.selectOrderConfigByTypes(anyList())).thenReturn(List.of(certificate, website));
        Translations localized = translations(13L,
                "{\"certificateContent\":\"English certificate\"}", "{\"unrelated\":\"Sin valor\"}");
        when(translationsService.selectTranslationsByIds(anyCollection())).thenReturn(Map.of(13L, localized));

        assertEquals("", service.content(SupportedLocale.ES_ES).certificateContent());
        assertEquals("Base tip", service.content(SupportedLocale.ES_ES).certificateTip());
        certificate.setContent("{\"certificateContent\":\"Base certificate\",\"certificateSwitch\":\"0\",\"certificateTip\":\"Base tip\"}");
        assertEquals("English certificate", service.content(SupportedLocale.ES_ES).certificateContent());
        assertEquals("", service.content(SupportedLocale.ES_ES).certificateTip());
        assertEquals("{\"certificateContent\":\"English certificate\"}", localized.getEnUs());
    }

    @ParameterizedTest
    @CsvSource(value = {
            "\"1\"|1", "1|1", "true|1", "\"true\"|1", "\"TRUE\"|1",
            "\" 1 \"|1", "\" TRUE \"|1",
            "\"0\"|0", "0|0", "false|0", "null|0", "\"\"|0", "\"unexpected\"|0"
    }, delimiter = '|')
    void certificateOwnKeyIsAuthoritativeOverLegacyWebsiteAndNormalizedInBothResponses(
            String jsonValue, String expected) {
        OrderConfig certificate = config(3L, 13L, "certificate",
                "{\"certificateContent\":\"Base certificate\",\"certificateTip\":\"Base tip\",\"certificateSwitch\":" + jsonValue + "}");
        OrderConfig website = config(4L, null, "website", "{\"certificateSwitch\":\"1\"}");
        when(orderConfigService.selectOrderConfigByTypes(anyList())).thenReturn(List.of(certificate, website));
        when(translationsService.selectTranslationsByIds(anyCollection())).thenReturn(Map.of());

        assertEquals(expected, service.website().certificateSwitch());
        ConfigApiDtos.GlobalContentResponse result = service.content(SupportedLocale.ES_ES);
        assertEquals("1".equals(expected) ? "" : "Base certificate", result.certificateContent());
        assertEquals("1".equals(expected) ? "Base tip" : "", result.certificateTip());
    }

    @Test
    void legacyWebsiteSwitchRemainsEffectiveUntilCertificateHasItsOwnKey() {
        OrderConfig website = config(4L, null, "website", "{\"certificateSwitch\":\"1\"}");
        OrderConfig certificate = config(3L, 13L, "certificate",
                "{\"certificateContent\":\"Base certificate\"}");
        when(orderConfigService.selectOrderConfigByTypes(anyList())).thenReturn(List.of(certificate, website));
        when(translationsService.selectTranslationsByIds(anyCollection())).thenReturn(Map.of());

        assertEquals("1", service.website().certificateSwitch());
        assertEquals("", service.content(SupportedLocale.ES_ES).certificateContent());
        assertEquals("非正式员工不可以查看证书", service.content(SupportedLocale.ES_ES).certificateTip());
    }

    @ParameterizedTest
    @MethodSource("certificateTipFallbackCases")
    void enabledTipUsesStrictRequestedThenEnglishThenBaseThenDefault(
            String base, String english, String spanish, String expected) {
        OrderConfig certificate = config(3L, 13L, "certificate", base);
        OrderConfig website = config(4L, null, "website", "{\"certificateSwitch\":\"1\"}");
        when(orderConfigService.selectOrderConfigByTypes(anyList())).thenReturn(List.of(certificate, website));
        when(translationsService.selectTranslationsByIds(anyCollection())).thenReturn(Map.of(
                13L, translations(13L, english, spanish)));

        ConfigApiDtos.GlobalContentResponse result = service.content(SupportedLocale.ES_ES);
        assertEquals("", result.certificateContent());
        assertEquals(expected, result.certificateTip());
    }

    private static Stream<Arguments> certificateTipFallbackCases() {
        String base = "{\"certificateSwitch\":\"1\",\"certificateContent\":\"Base certificate\",\"certificateTip\":\"Base tip\"}";
        String english = "{\"certificateTip\":\"English tip\"}";
        return Stream.of(
                Arguments.of(base, english, "{\"certificateTip\":\"Consejo\"}", "Consejo"),
                Arguments.of(base, english, "{\"certificateTip\":\" \"}", "English tip"),
                Arguments.of(base, english, "<p>Raw certificate</p>", "English tip"),
                Arguments.of(base, english, "\"Raw certificate\"", "English tip"),
                Arguments.of(base, english, "[\"Raw certificate\"]", "English tip"),
                Arguments.of(base, english, "42", "English tip"),
                Arguments.of(base, english, "null", "English tip"),
                Arguments.of(base, english, "{broken", "English tip"),
                Arguments.of(base, english, "{\"certificateTip\":{\"nested\":\"Wrong\"}}", "English tip"),
                Arguments.of(base, english, "{\"certificateTip\":[\"Wrong\"]}", "English tip"),
                Arguments.of(base, english, "{\"certificateTip\":123}", "English tip"),
                Arguments.of(base, english, "{\"certificateTip\":false}", "English tip"),
                Arguments.of(base, english, "{\"value\":{\"certificateTip\":\"Wrapped Spanish tip\"}}", "Wrapped Spanish tip"),
                Arguments.of(base, english, "{\"certificateTip\":\"Root tip\",\"value\":{\"certificateTip\":\"Wrapped tip\"}}", "Root tip"),
                Arguments.of(base, "{}", "{}", "Base tip"),
                Arguments.of(base, "<p>Raw English certificate</p>", "<p>Raw Spanish certificate</p>", "Base tip"),
                Arguments.of("{\"certificateSwitch\":\"1\",\"certificateTip\":\" \"}", "{}", "{}", "非正式员工不可以查看证书"),
                Arguments.of("<p>Raw base certificate</p>", "<p>Raw English certificate</p>", "<p>Raw Spanish certificate</p>", "非正式员工不可以查看证书"));
    }

    @ParameterizedTest
    @MethodSource("certificateContentFallbackCases")
    void disabledContentSupportsLegacyAliasesWithinEachLanguageBeforeFallingBack(
            String base, String english, String spanish, String expected) {
        OrderConfig certificate = config(3L, 13L, "certificate", base);
        when(orderConfigService.selectOrderConfigByTypes(anyList())).thenReturn(List.of(certificate));
        when(translationsService.selectTranslationsByIds(anyCollection())).thenReturn(Map.of(
                13L, translations(13L, english, spanish)));

        ConfigApiDtos.GlobalContentResponse result = service.content(SupportedLocale.ES_ES);
        assertEquals(expected, result.certificateContent());
        assertEquals("", result.certificateTip());
    }

    private static Stream<Arguments> certificateContentFallbackCases() {
        String base = "{\"certificateContent\":\"Base certificate\",\"certificateSwitch\":\"0\"}";
        String english = "{\"certificateContent\":\"English certificate\"}";
        return Stream.of(
                Arguments.of(base, english, "{\"certificateContent\":\"Canonical Spanish\",\"certificate\":\"Legacy Spanish\"}", "Canonical Spanish"),
                Arguments.of(base, english, "{\"certificate\":\"Legacy Spanish\"}", "Legacy Spanish"),
                Arguments.of(base, english, "{\"content\":\"Content alias Spanish\"}", "Content alias Spanish"),
                Arguments.of(base, english, "{\"value\":{\"certificate\":\"Wrapped Spanish\"}}", "Wrapped Spanish"),
                Arguments.of(base, english, "{\"certificateContent\":\"Root Spanish\",\"value\":{\"certificateContent\":\"Wrapped Spanish\"}}", "Root Spanish"),
                Arguments.of(base, english, "{\"certificateContent\":\"Root Spanish\",\"value\":{\"certificateSwitch\":\"1\"}}", "Root Spanish"),
                Arguments.of(base, english, "{\"certificateContent\":\" \",\"certificate\":\"Legacy Spanish\"}", "Legacy Spanish"),
                Arguments.of(base, english, "<p>Raw Spanish certificate</p>", "<p>Raw Spanish certificate</p>"),
                Arguments.of(base, "{\"certificate\":\"Legacy English\"}", "{}", "Legacy English"),
                Arguments.of("{\"certificate\":\"Legacy base\"}", "{}", "{}", "Legacy base"),
                Arguments.of("<p>Raw base certificate</p>", "{}", "{}", "<p>Raw base certificate</p>"));
    }

    @Test
    void missingWebsiteAndCertificateDefaultSwitchToDisabled() {
        assertEquals("0", service.website().certificateSwitch());
    }

    @Test
    void memberLevelSupportsLegacyRawDescriptionAndJsonNameFallback() {
        GoodsMemberLevel level = new GoodsMemberLevel();
        level.setId(8L);
        level.setTranslationsId(80L);
        level.setName("Base name");
        level.setDescription("Base description");

        Translations translations = translations(
                80L,
                "{\"name\":\"English VIP\",\"description\":\"English description\"}",
                "Descripción heredada");

        when(levelService.selectGoodsMemberLevelList(null)).thenReturn(List.of(level));
        when(translationsService.selectTranslationsByIds(anyCollection())).thenReturn(Map.of(80L, translations));

        ConfigApiDtos.MemberLevelResponse result =
                service.memberLevels(SupportedLocale.ES_ES).get(0);

        assertEquals("English VIP", result.name());
        assertEquals("Descripción heredada", result.description());
    }

    @Test
    void damagedStrictJsonFallsBackInsteadOfLeakingBrokenTranslation() {
        GoodsMemberLevel level = new GoodsMemberLevel();
        level.setId(9L);
        level.setTranslationsId(90L);
        level.setName("Base name");
        level.setDescription("Base description");

        Translations translations = translations(
                90L,
                "{\"name\":\"English name\",\"description\":\"English description\"}",
                "{\"name\":");

        when(levelService.selectGoodsMemberLevelList(null)).thenReturn(List.of(level));
        when(translationsService.selectTranslationsByIds(anyCollection())).thenReturn(Map.of(90L, translations));

        ConfigApiDtos.MemberLevelResponse result =
                service.memberLevels(SupportedLocale.ES_ES).get(0);

        assertEquals("English name", result.name());
        assertEquals("English description", result.description());
    }

    @Test
    void tradeReturnsOnlyThePublicAllowlist() {
        OrderConfig trade = config(
                3L,
                null,
                "trade",
                """
                {
                  "registerBonusAmount": 12,
                  "minTradeBalance": 100,
                  "startTaskDelayMs": 1500,
                  "submitTaskDelayMs": 2500,
                  "matchDelaySeconds": 999,
                  "passwordFailureThreshold": 3
                }
                """);
        when(orderConfigService.selectOrderConfigByType("trade")).thenReturn(trade);

        ConfigApiDtos.TradeConfigResponse result = service.trade();

        assertEquals(12, result.registerBonusAmount());
        assertEquals(100, result.minTradeBalance());
        assertEquals(1500, result.startTaskDelayMs());
        assertEquals(2500, result.submitTaskDelayMs());
    }

    @Test
    void websiteReturnsOnlyPresentationFieldsAndSupportsLegacyAliases() {
        OrderConfig website = config(
                4L,
                null,
                "website",
                """
                {
                  "siteName": "IRON",
                  "currencyUnit": "EUR",
                  "siteLogo": "/profile/logo.png",
                  "splashAdImage": "/profile/popup.png",
                  "siteBackground": "/profile/background.jpg",
                  "h5BackgroundImage": "/profile/h5.jpg",
                  "popUpLimit": 3,
                  "showLogo": "1",
                  "enableImageHide": "0",
                  "imageDisplayTimeRange": ["09:00", "22:00"],
                  "redirectUrl": "https://internal.example",
                  "customServiceScript": "secret-script"
                }
                """);

        when(orderConfigService.selectOrderConfigByTypes(anyList())).thenReturn(List.of(website));
        ConfigApiDtos.WebsiteConfigResponse result = service.website();

        verify(orderConfigService).selectOrderConfigByTypes(argThat(types ->
                types.contains("website") && types.contains("certificate")));
        org.mockito.Mockito.verifyNoInteractions(translationsService);
        assertEquals("IRON", result.name());
        assertEquals("EUR", result.currencyUnit());
        assertEquals("/profile/logo.png", result.logo());
        assertEquals("/profile/popup.png", result.popUpImage());
        assertEquals("/profile/background.jpg", result.backgroundImage());
        assertEquals("/profile/h5.jpg", result.h5BackgroundImage());
        assertEquals(3, result.popUpLimit());
        assertEquals("1", result.hideImage());
        assertEquals("0", result.certificateSwitch());
        assertEquals(List.of("09:00", "22:00"), result.imageShowTimeRange());
    }

    @Test
    void customerServicesFilterEnabledRowsAndUseStrictJsonFallback() {
        GoodsCustomerService customer = new GoodsCustomerService();
        customer.setId("customer-1");
        customer.setName("Base support");
        customer.setSortOrder(10L);
        customer.setTranslationsId(101L);
        when(customerServiceService.selectGoodsCustomerServiceList(any()))
                .thenReturn(List.of(customer));
        Translations translations = translations(
                101L,
                "{\"name\":\"English support\"}",
                "{\"name\":");
        when(translationsService.selectTranslationsByIds(anyCollection()))
                .thenReturn(Map.of(101L, translations));

        ConfigApiDtos.CustomerServiceResponse result =
                service.customerServices(SupportedLocale.ES_ES).get(0);

        assertEquals("English support", result.name());
        ArgumentCaptor<GoodsCustomerService> criteria =
                ArgumentCaptor.forClass(GoodsCustomerService.class);
        verify(customerServiceService).selectGoodsCustomerServiceList(criteria.capture());
        assertEquals("1", criteria.getValue().getIsEnabled());
    }

    private OrderConfig config(Long id, Long translationsId, String type, String content) {
        OrderConfig config = new OrderConfig();
        config.setId(id);
        config.setTranslationsId(translationsId);
        config.setType(type);
        config.setContent(content);
        return config;
    }

    private Translations translations(Long id, String english, String spanish) {
        Translations translations = new Translations();
        translations.setId(id);
        translations.setEnUs(english);
        translations.setEsEs(spanish);
        return translations;
    }
}
