package com.order.api.controller;

import com.order.api.controller.dto.ConfigApiDtos;
import com.order.api.service.ApiLocaleService;
import com.order.api.service.ConfigQueryService;
import com.order.api.service.LocalizedMessageCatalogService;
import com.order.framework.config.ServerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
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
    private LocalizedMessageCatalogService messageCatalogService;
    @Mock
    private ServerConfig serverConfig;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ApiLocaleService localeService = new ApiLocaleService();
        ConfigController controller =
                new ConfigController(
                        queryService,
                        localeService,
                        messageCatalogService,
                        serverConfig);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(
                        new ConfigApiExceptionHandler(localeService),
                        new PublicApiFallbackExceptionHandler(),
                        new ApiResponseLocalizationAdvice())
                .build();
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
    void websiteResponseContainsOnlyThePresentationAllowlist() throws Exception {
        when(queryService.website()).thenReturn(new ConfigApiDtos.WebsiteConfigResponse(
                "IRON",
                "USD",
                "Copyright",
                "/profile/logo.png",
                2,
                "/profile/popup.png",
                "/profile/background.jpg",
                "/profile/h5.jpg",
                "1",
                "0",
                List.of("09:00", "22:00")));

        mockMvc.perform(get("/api/config/website"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("IRON"))
                .andExpect(jsonPath("$.data.popUpImage").value("/profile/popup.png"))
                .andExpect(jsonPath("$.data.redirectUrl").doesNotExist())
                .andExpect(jsonPath("$.data.customServiceScript").doesNotExist())
                .andExpect(jsonPath("$.data.domains").doesNotExist());
    }

    @Test
    void emptyCustomerListIsSuccessful() throws Exception {
        when(queryService.customerServices(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/config/customer-services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void uploadRejectsNonImagesWithBusinessCode703() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "payload.png", "image/png", "<html>payload</html>".getBytes());

        mockMvc.perform(multipart("/api/config/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Deprecation"))
                .andExpect(jsonPath("$.code").value(703))
                .andExpect(jsonPath("$.msg").value("Upload failed"));
    }
}
