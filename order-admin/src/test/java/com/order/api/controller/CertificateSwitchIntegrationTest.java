package com.order.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.order.api.service.ApiLocaleService;
import com.order.api.service.ConfigQueryService;
import com.order.api.service.LocalizedMessageCatalogService;
import com.order.common.i18n.Translations;
import com.order.common.i18n.SupportedLocale;
import com.order.framework.config.ServerConfig;
import com.order.member.domain.OrderConfig;
import com.order.member.mapper.OrderConfigMapper;
import com.order.member.mapper.TranslationsMapper;
import com.order.member.service.IGoodsCustomerServiceService;
import com.order.member.service.IGoodsMemberLevelService;
import com.order.member.service.impl.OrderConfigServiceImpl;
import com.order.member.service.impl.TranslationsServiceImpl;
import com.order.system.service.ISysNoticeService;
import com.order.system.service.ISysTimeZoneService;
import com.order.web.controller.member.OrderConfigController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises real management/public controllers, JSON persistence, and translation services.
 * Only the mapper boundary uses in-memory rows; no database, Redis, or application is started.
 */
class CertificateSwitchIntegrationTest {
    private static final long WEBSITE_ID = 1L;
    private static final long CERTIFICATE_ID = 2L;
    private static final long CERTIFICATE_TRANSLATIONS_ID = 20L;
    private static final String DEFAULT_TIP = "非正式员工不可以查看证书";
    private static final String BASE_CERTIFICATE = "{\"certificateContent\":\"<p>Base certificate</p>\"}";
    private static final String ENGLISH_CERTIFICATE = "{\"certificateContent\":\"<p>English certificate</p>\"}";
    private static final String SPANISH_CERTIFICATE = "{\"certificateContent\":\"<p>Certificado</p>\"}";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Map<Long, OrderConfig> configRows;
    private Map<Long, Translations> translationRows;
    private TranslationsMapper translationsMapper;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        configRows = new LinkedHashMap<>();
        configRows.put(WEBSITE_ID, config(WEBSITE_ID, "website",
                "{\"name\":\"IRON\",\"currencyUnit\":\"USD\",\"privateOption\":\"preserved\"}", null));
        configRows.put(CERTIFICATE_ID, config(CERTIFICATE_ID, "certificate", BASE_CERTIFICATE,
                CERTIFICATE_TRANSLATIONS_ID));
        configRows.put(3L, config(3L, "about", "{\"aboutContent\":\"<p>About us</p>\"}", null));
        configRows.put(4L, config(4L, "register", "{\"protocolContent\":\"<p>Protocol</p>\"}", null));
        Translations certificateTranslations = new Translations();
        certificateTranslations.setId(CERTIFICATE_TRANSLATIONS_ID);
        certificateTranslations.setEnUs(ENGLISH_CERTIFICATE);
        certificateTranslations.setEsEs(SPANISH_CERTIFICATE);
        translationRows = new LinkedHashMap<>();
        translationRows.put(CERTIFICATE_TRANSLATIONS_ID, certificateTranslations);

        OrderConfigMapper configMapper = mock(OrderConfigMapper.class);
        when(configMapper.selectOrderConfigById(anyLong())).thenAnswer(invocation ->
                copy(configRows.get(invocation.getArgument(0))));
        when(configMapper.selectOrderConfigByType(anyString())).thenAnswer(invocation ->
                configRows.values().stream()
                        .filter(row -> row.getType().equals(invocation.getArgument(0)))
                        .findFirst().map(this::copy).orElse(null));
        when(configMapper.selectOrderConfigByTypes(anyList())).thenAnswer(invocation -> {
            List<String> types = invocation.getArgument(0);
            return configRows.values().stream()
                    .filter(row -> types.contains(row.getType()))
                    .map(this::copy).toList();
        });
        when(configMapper.updateOrderConfig(any())).thenAnswer(invocation -> {
            OrderConfig update = invocation.getArgument(0);
            OrderConfig row = configRows.get(update.getId());
            if (row == null) {
                return 0;
            }
            // Match the mapper XML's selective update behavior.
            if (update.getContent() != null) {
                row.setContent(update.getContent());
            }
            if (update.getTranslationsId() != null) {
                row.setTranslationsId(update.getTranslationsId());
            }
            row.setUpdateTime(update.getUpdateTime());
            return 1;
        });
        translationsMapper = mock(TranslationsMapper.class);
        when(translationsMapper.selectTranslationsById(anyLong())).thenAnswer(invocation ->
                translationRows.get(invocation.getArgument(0)));
        when(translationsMapper.selectTranslationsByIds(anyList())).thenAnswer(invocation -> {
            List<Long> ids = invocation.getArgument(0);
            return translationRows.values().stream().filter(row -> ids.contains(row.getId())).toList();
        });
        when(translationsMapper.updateTranslations(any())).thenAnswer(invocation -> {
            Translations update = invocation.getArgument(0);
            Translations row = translationRows.get(update.getId());
            if (row == null) {
                return 0;
            }
            if (update.getEnUs() != null) {
                row.setEnUs(update.getEnUs());
            }
            if (update.getEsEs() != null) {
                row.setEsEs(update.getEsEs());
            }
            return 1;
        });
        TranslationsServiceImpl translationsService = new TranslationsServiceImpl();
        ReflectionTestUtils.setField(translationsService, "translationsMapper", translationsMapper);
        OrderConfigServiceImpl configService =
                new OrderConfigServiceImpl(configMapper, objectMapper, translationsService);
        ConfigQueryService queryService = new ConfigQueryService(
                configService,
                mock(IGoodsCustomerServiceService.class),
                mock(IGoodsMemberLevelService.class),
                mock(ISysNoticeService.class),
                mock(ISysTimeZoneService.class),
                translationsService,
                objectMapper);
        OrderConfigController managementController = new OrderConfigController();
        ReflectionTestUtils.setField(managementController, "orderConfigService", configService);
        ApiLocaleService localeService = new ApiLocaleService();
        ConfigController publicController = new ConfigController(queryService, localeService,
                mock(LocalizedMessageCatalogService.class), mock(ServerConfig.class));
        mockMvc = MockMvcBuilders.standaloneSetup(managementController, publicController)
                .setControllerAdvice(new ConfigApiExceptionHandler(localeService),
                        new PublicApiFallbackExceptionHandler(), new ApiResponseLocalizationAdvice())
                .build();
    }

    @Test
    void certificateSettingsSaveTakesEffectImmediatelyAndDisablingRestoresStoredContent() throws Exception {
        expectContent("es-ES", "<p>Certificado</p>");

        saveCertificateSettings("1", "Base tip");
        mockMvc.perform(get("/member/orderconfig/{id}", CERTIFICATE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", containsString("\"certificateSwitch\":\"1\"")));
        expectWebsiteSwitch("1");
        expectContent("es-ES", "", "Base tip");
        expectContent("en-US", "", "Base tip");
        expectContent("ja-JP", "", "Base tip");

        saveCertificateSettings("0", "Base tip");
        expectWebsiteSwitch("0");
        expectContent("es-ES", "<p>Certificado</p>");
        expectContent("en-US", "<p>English certificate</p>");
        expectContent("ja-JP", "<p>English certificate</p>");
        assertEquals("<p>Base certificate</p>", objectMapper.readTree(configRows.get(CERTIFICATE_ID).getContent())
                .get("certificateContent").asText());
        assertEquals(ENGLISH_CERTIFICATE, translationRows.get(CERTIFICATE_TRANSLATIONS_ID).getEnUs());
        assertEquals(SPANISH_CERTIFICATE, translationRows.get(CERTIFICATE_TRANSLATIONS_ID).getEsEs());
        verify(translationsMapper, never()).updateTranslations(any());
        verify(translationsMapper, never()).insertTranslations(any());
        mockMvc.perform(get("/member/orderconfig/{id}", CERTIFICATE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", containsString("\"certificateTip\":\"Base tip\"")))
                .andExpect(jsonPath("$.data.translations.esEs").value(SPANISH_CERTIFICATE));
    }

    @Test
    void oldCertificateAndWebsiteWithoutSwitchPreserveContentAndDefaultToDisabled() throws Exception {
        expectWebsiteSwitch("0");
        expectContent("es-ES", "<p>Certificado</p>");
    }

    @Test
    void missingWebsitePreservesCertificateAndDefaultsToDisabled() throws Exception {
        configRows.remove(WEBSITE_ID);
        expectWebsiteSwitch("0");
        expectContent("es-ES", "<p>Certificado</p>");
    }

    @Test
    void legacyWebsiteSwitchWorksUntilCertificateOwnSettingOverridesIt() throws Exception {
        saveWebsiteSwitch("1");
        expectWebsiteSwitch("1");
        expectContent("es-ES", "", DEFAULT_TIP);

        saveCertificateSettings("0", "Saved tip");
        expectWebsiteSwitch("0");
        expectContent("es-ES", "<p>Certificado</p>");
        assertEquals("1", objectMapper.readTree(configRows.get(WEBSITE_ID).getContent())
                .get("certificateSwitch").asText());
    }

    @Test
    void legacyEnabledSwitchReturnsExplicitEmptyContentAndDefaultTipWithoutCertificateRow() throws Exception {
        configRows.remove(CERTIFICATE_ID);
        saveWebsiteSwitch("1");
        expectWebsiteSwitch("1");
        expectContent("es-ES", "", DEFAULT_TIP);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "1"})
    void translatedSwitchCannotOverrideCertificateOwnSetting(String ownedSwitch) throws Exception {
        String opposite = "1".equals(ownedSwitch) ? "0" : "1";
        saveWebsiteSwitch(opposite);
        saveCertificateSettings(ownedSwitch, "Base tip");
        translationRows.get(CERTIFICATE_TRANSLATIONS_ID).setEsEs(objectMapper.writeValueAsString(Map.of(
                "certificateContent", "<p>Certificado</p>", "certificateSwitch", opposite,
                "certificateTip", "Consejo")));
        expectWebsiteSwitch(ownedSwitch);
        if ("1".equals(ownedSwitch)) {
            expectContent("es-ES", "", "Consejo");
        } else {
            expectContent("es-ES", "<p>Certificado</p>");
        }
    }

    @Test
    void explicitLanguageWinsOverAcceptLanguageForCertificateTip() throws Exception {
        saveCertificateSettings("1", "Base tip");
        translationRows.get(CERTIFICATE_TRANSLATIONS_ID).setEnUs(objectMapper.writeValueAsString(Map.of(
                "certificateContent", "<p>English certificate</p>", "certificateTip", "English tip")));
        translationRows.get(CERTIFICATE_TRANSLATIONS_ID).setEsEs(objectMapper.writeValueAsString(Map.of(
                "certificateContent", "<p>Certificado</p>", "certificateTip", "Consejo")));

        mockMvc.perform(get("/api/config/content").queryParam("lang", "es_ES")
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "en-US"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_LANGUAGE, "es-ES"))
                .andExpect(jsonPath("$.data.lang").value("es_ES"))
                .andExpect(jsonPath("$.data.certificateContent").value(""))
                .andExpect(jsonPath("$.data.certificateTip").value("Consejo"));
        mockMvc.perform(get("/api/config/content").queryParam("lang", "en_US")
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "es-ES"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_LANGUAGE, "en-US"))
                .andExpect(jsonPath("$.data.lang").value("en_US"))
                .andExpect(jsonPath("$.data.certificateTip").value("English tip"));
    }

    @Test
    void savingOnlyTipTranslationPreservesBaseSwitchContentAndOtherLanguages() throws Exception {
        saveCertificateSettings("1", "Base tip");
        String savedBase = configRows.get(CERTIFICATE_ID).getContent();
        String savedWebsite = configRows.get(WEBSITE_ID).getContent();
        String spanish = objectMapper.writeValueAsString(Map.of(
                "certificateContent", "<p>Certificado</p>",
                "certificateTip", "Consejo actualizado", "certificateSwitch", "0"));
        // Match the translation drawer: update translations without replacing base content.
        String body = objectMapper.writeValueAsString(Map.of(
                "id", CERTIFICATE_ID, "translationsId", CERTIFICATE_TRANSLATIONS_ID,
                "translations", Map.of("id", CERTIFICATE_TRANSLATIONS_ID, "esEs", spanish)));
        mockMvc.perform(put("/member/orderconfig").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertEquals(savedBase, configRows.get(CERTIFICATE_ID).getContent());
        assertEquals(savedWebsite, configRows.get(WEBSITE_ID).getContent());
        assertEquals(ENGLISH_CERTIFICATE, translationRows.get(CERTIFICATE_TRANSLATIONS_ID).getEnUs());
        assertEquals(spanish, translationRows.get(CERTIFICATE_TRANSLATIONS_ID).getEsEs());
        expectWebsiteSwitch("1");
        expectContent("es-ES", "", "Consejo actualizado");
        expectContent("en-US", "", "Base tip");

        saveCertificateSettings("0", "Base tip");
        expectContent("es-ES", "<p>Certificado</p>");
        expectContent("en-US", "<p>English certificate</p>");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "{}", "null", "{broken", "[]", "\"legacy\"", "42"})
    void unusableWebsiteJsonDefaultsOffWithoutBreakingOtherPublicContent(String websiteContent) throws Exception {
        configRows.get(WEBSITE_ID).setContent(websiteContent);
        expectWebsiteSwitch("0");
        expectContent("es-ES", "<p>Certificado</p>");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "{}", "null", "{broken", "[]", "\"legacy\"", "42"})
    void unusableCertificateJsonUsesLegacySwitchAndSafeDefaultTip(String certificateContent) throws Exception {
        configRows.get(CERTIFICATE_ID).setContent(certificateContent);
        saveWebsiteSwitch("1");
        expectWebsiteSwitch("1");
        expectContent("es-ES", "", DEFAULT_TIP);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "1"})
    void certificateEndpointAlwaysReturnsLocalizedTipAndOnlyTheTwoConfigurationFields(String enabled) throws Exception {
        saveWebsiteSwitch("1".equals(enabled) ? "0" : "1");
        saveCertificateSettings(enabled, "Base tip");
        Translations translations = translationRows.get(CERTIFICATE_TRANSLATIONS_ID);
        translations.setEnUs(objectMapper.writeValueAsString(Map.of(
                "certificateContent", "<p>English certificate</p>", "certificateTip", "English tip")));
        translations.setEsEs(objectMapper.writeValueAsString(Map.of(
                "certificateContent", "<p>Certificado</p>", "certificateTip", "Consejo")));
        for (SupportedLocale locale : SupportedLocale.all()) {
            mockMvc.perform(get("/api/config/certificate").queryParam("lang", locale.getCode())
                            .header(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CONTENT_LANGUAGE, locale.toLanguageTag()))
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.size()").value(2))
                    .andExpect(jsonPath("$.data.certificateSwitch").value(enabled))
                    .andExpect(jsonPath("$.data.certificateTip").value(
                            locale == SupportedLocale.ES_ES ? "Consejo" : "English tip"))
                    .andExpect(jsonPath("$.data.certificateContent").doesNotExist());
        }
    }

    @Test
    void certificateEndpointUsesLegacySwitchAndDoesNotTreatLegacyHtmlAsATip() throws Exception {
        saveWebsiteSwitch("1");
        for (String lang : List.of("zh_CN", "en_US", "es_ES")) {
            mockMvc.perform(get("/api/config/certificate").queryParam("lang", lang))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.certificateSwitch").value("1"))
                    .andExpect(jsonPath("$.data.certificateTip").value(DEFAULT_TIP));
        }
        translationRows.get(CERTIFICATE_TRANSLATIONS_ID).setEnUs("<p>Raw English certificate</p>");
        translationRows.get(CERTIFICATE_TRANSLATIONS_ID).setEsEs("<p>Raw Spanish certificate</p>");
        saveCertificateSettings("0", "Base notice");
        mockMvc.perform(get("/api/config/certificate").header(HttpHeaders.ACCEPT_LANGUAGE, "es-ES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.certificateSwitch").value("0"))
                .andExpect(jsonPath("$.data.certificateTip").value("Base notice"));
        configRows.clear();
        mockMvc.perform(get("/api/config/certificate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.certificateSwitch").value("0"))
                .andExpect(jsonPath("$.data.certificateTip").value(DEFAULT_TIP));
    }

    private void saveCertificateSettings(String value, String tip) throws Exception {
        ObjectNode certificate = (ObjectNode) objectMapper.readTree(configRows.get(CERTIFICATE_ID).getContent());
        String existingContent = certificate.path("certificateContent").asText();
        certificate.put("certificateSwitch", value);
        certificate.put("certificateTip", tip);
        saveConfig(CERTIFICATE_ID, "certificate", certificate.toString());
        JsonNode stored = objectMapper.readTree(configRows.get(CERTIFICATE_ID).getContent());
        assertEquals(value, stored.get("certificateSwitch").asText());
        assertEquals(tip, stored.get("certificateTip").asText());
        assertEquals(existingContent, stored.get("certificateContent").asText());
    }

    private void saveWebsiteSwitch(String value) throws Exception {
        ObjectNode website = (ObjectNode) objectMapper.readTree(configRows.get(WEBSITE_ID).getContent());
        website.put("certificateSwitch", value);
        saveConfig(WEBSITE_ID, "website", website.toString());
        JsonNode stored = objectMapper.readTree(configRows.get(WEBSITE_ID).getContent());
        assertEquals(value, stored.get("certificateSwitch").asText());
        assertEquals("preserved", stored.get("privateOption").asText());
    }

    private void saveConfig(long id, String type, String content) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("id", id, "type", type, "content", content));
        mockMvc.perform(put("/member/orderconfig").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    private void expectWebsiteSwitch(String value) throws Exception {
        mockMvc.perform(get("/api/config/website"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.certificateSwitch").value(value))
                .andExpect(jsonPath("$.data.privateOption").doesNotExist());
    }

    private void expectContent(String language, String certificate) throws Exception {
        expectContent(language, certificate, "");
    }

    private void expectContent(String language, String certificate, String tip) throws Exception {
        mockMvc.perform(get("/api/config/content").header(HttpHeaders.ACCEPT_LANGUAGE, language))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_LANGUAGE, language))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.certificateContent").value(certificate))
                .andExpect(jsonPath("$.data.certificateTip").value(tip))
                .andExpect(jsonPath("$.data.aboutContent").value("<p>About us</p>"))
                .andExpect(jsonPath("$.data.protocolContent").value("<p>Protocol</p>"));
    }

    private OrderConfig config(long id, String type, String content, Long translationsId) {
        OrderConfig row = new OrderConfig();
        row.setId(id);
        row.setType(type);
        row.setContent(content);
        row.setTranslationsId(translationsId);
        return row;
    }

    private OrderConfig copy(OrderConfig row) {
        if (row == null) {
            return null;
        }
        OrderConfig copy = config(row.getId(), row.getType(), row.getContent(), row.getTranslationsId());
        copy.setCreateTime(row.getCreateTime());
        copy.setUpdateTime(row.getUpdateTime());
        return copy;
    }
}
