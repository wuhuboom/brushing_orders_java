package com.order.api.controller;

import com.order.api.controller.dto.ConfigApiDtos;
import com.order.api.service.ApiLocaleService;
import com.order.api.service.ConfigApiException;
import com.order.api.service.ConfigQueryService;
import com.order.api.service.LocalizedApiMessageService;
import com.order.api.service.LocalizedMessageCatalogService;
import com.order.framework.config.ServerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ConfigControllerContractTest {
    @Mock
    private ConfigQueryService queryService;
    @Mock
    private LocalizedApiMessageService messageService;
    @Mock
    private LocalizedMessageCatalogService messageCatalogService;
    @Mock
    private ServerConfig serverConfig;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ApiLocaleService localeService = new ApiLocaleService();
        when(messageService.message(anyInt(), any(), anyString(), any(Object[].class)))
                .thenAnswer(invocation -> invocation.getArgument(2));
        ConfigController controller =
                new ConfigController(
                        queryService,
                        localeService,
                        messageService,
                        messageCatalogService,
                        serverConfig);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(
                        new ConfigApiExceptionHandler(localeService, messageService))
                .build();
    }

    @Test
    void languageParameterWinsAndCanonicalResponsesExposeLanguageHeaders() throws Exception {
        when(queryService.languages()).thenReturn(List.of(
                new ConfigApiDtos.LanguageOption("en_US", "en-US", "English", true),
                new ConfigApiDtos.LanguageOption("zh_CN", "zh-CN", "简体中文", false)));

        mockMvc.perform(get("/api/config/languages")
                        .param("lang", "de_DE")
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "fr-FR;q=1"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_LANGUAGE, "de-DE"))
                .andExpect(header().string(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].h5Enabled").value(true));
    }

    @Test
    void unknownExplicitLanguageUsesStableEnglishFallback() throws Exception {
        when(queryService.languages()).thenReturn(List.of());

        mockMvc.perform(get("/api/config/languages")
                        .param("lang", "xx-ZZ")
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "fr-FR"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_LANGUAGE, "en-US"));
    }

    @Test
    void errorMessageCatalogUsesRequestedLocaleAndBusinessCodeKeys() throws Exception {
        when(messageCatalogService.catalog(any()))
                .thenReturn(Map.of("504", "Contraseña de transacción incorrecta"));

        mockMvc.perform(get("/api/config/error-messages")
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "es-ES"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_LANGUAGE, "es-ES"))
                .andExpect(jsonPath("$.data.504")
                        .value("Contraseña de transacción incorrecta"));
    }

    @Test
    void canonicalNoticeNotFoundIs404WhileLegacyAliasKeepsHttp200() throws Exception {
        when(queryService.notice(any(), any()))
                .thenThrow(new ConfigApiException(701, HttpStatus.NOT_FOUND, "No data"));

        mockMvc.perform(get("/api/config/notices/99").header(HttpHeaders.ACCEPT_LANGUAGE, "es-ES"))
                .andExpect(status().isNotFound())
                .andExpect(header().string(HttpHeaders.CONTENT_LANGUAGE, "es-ES"))
                .andExpect(jsonPath("$.code").value(701));

        mockMvc.perform(get("/api/config/getNotice/99").header(HttpHeaders.ACCEPT_LANGUAGE, "es-ES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(701));
    }

    @Test
    void invalidCanonicalPaginationIs400WhileLegacyAliasKeepsHttp200() throws Exception {
        mockMvc.perform(get("/api/config/notices").param("pageSize", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(get("/api/config/getNoticeList").param("pageSize", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void tradeResponseContainsOnlyTheClientAllowlist() throws Exception {
        when(queryService.trade()).thenReturn(new ConfigApiDtos.TradeConfigResponse(
                5,
                100,
                "1",
                80,
                10,
                1000,
                1.5,
                List.of("00:00", "23:59"),
                List.of("00:00", "23:59"),
                List.of("00:00", "23:59"),
                300,
                2,
                true));

        mockMvc.perform(get("/api/config/trade"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.registerBonusAmount").value(5))
                .andExpect(jsonPath("$.data.orderExpireSeconds").value(300))
                .andExpect(jsonPath("$.data.matchDelaySeconds").doesNotExist())
                .andExpect(jsonPath("$.data.passwordFailureThreshold").doesNotExist())
                .andExpect(jsonPath("$.data.translations").doesNotExist())
                .andExpect(jsonPath("$.data.remark").doesNotExist());
    }

    @Test
    void canonicalEmptyCustomerListIsSuccessWhileLegacyKeepsNoDataCode() throws Exception {
        when(queryService.customerServices(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/config/customer-services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(0)));

        mockMvc.perform(get("/api/config/getCustomerService"))
                .andExpect(status().isOk())
                .andExpect(header().string("Deprecation", "true"))
                .andExpect(jsonPath("$.code").value(701));
    }

    @Test
    void legacyUploadRejectsNonImagesWithHttp200AndBusinessCode703() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "payload.png", "image/png", "<html>payload</html>".getBytes());

        mockMvc.perform(multipart("/api/config/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(header().string("Deprecation", "true"))
                .andExpect(jsonPath("$.code").value(703))
                .andExpect(jsonPath("$.msg").value("Upload failed"));
    }
}
