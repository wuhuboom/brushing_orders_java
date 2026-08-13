package com.order.api.controller;

import com.order.api.service.ApiLocaleService;
import com.order.api.service.UserApiService;
import com.order.api.service.WithdrawalAccountAccessService;
import com.order.common.i18n.SupportedLocale;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerI18nContractTest {
    @Mock
    private UserApiService userApiService;
    @Mock
    private ServerConfig serverConfig;
    @Mock
    private WithdrawalAccountAccessService withdrawalAccountAccessService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ApiLocaleService localeService = new ApiLocaleService();
        AuthController controller =
                new AuthController(
                        userApiService,
                        serverConfig,
                        localeService,
                        withdrawalAccountAccessService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(
                        new UserApiExceptionHandler(localeService),
                        new PublicApiFallbackExceptionHandler(),
                        new ApiResponseLocalizationAdvice())
                .build();
    }

    @Test
    void localizedUserProfileExposesTheResolvedLanguageHeaders() throws Exception {
        mockMvc.perform(get("/api/user/getInfo")
                        .requestAttr("userId", 7L)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "fr-FR"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_LANGUAGE, "fr-FR"))
                .andExpect(header().string(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE))
                .andExpect(jsonPath("$.code").value(200));

        verify(userApiService).userInfo(7L, SupportedLocale.FR_FR);
    }

    @Test
    void tradePasswordCheckReturnsAccessTokenAsData() throws Exception {
        when(withdrawalAccountAccessService.issue(7L)).thenReturn("trade-access");

        mockMvc.perform(post("/api/user/checkTradePassword")
                        .requestAttr("userId", 7L)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"tradePassword\":\"Trade123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("trade-access"))
                .andExpect(jsonPath("$.msg").value("Success"));
    }

    @Test
    void invalidLoginBodyKeepsTheEstablishedBusinessCodeWithEnglishMessage() throws Exception {
        mockMvc.perform(post("/api/user/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(601))
                .andExpect(jsonPath("$.msg").value("Invalid username or password"));
    }

    @Test
    void avatarRejectsNonImageMimeWithCanonicalEnglish703Message() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile("file", "avatar.png", "text/plain", "not-an-image".getBytes());

        mockMvc.perform(multipart("/api/user/avatar")
                        .file(file)
                        .requestAttr("userId", 7L)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "es-ES"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_LANGUAGE, "es-ES"))
                .andExpect(jsonPath("$.code").value(703))
                .andExpect(jsonPath("$.msg").value("Upload failed"));

        verify(userApiService, never()).updateAvatar(eq(7L), org.mockito.ArgumentMatchers.any());
    }
}
