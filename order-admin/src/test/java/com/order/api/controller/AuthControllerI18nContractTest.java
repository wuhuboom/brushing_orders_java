package com.order.api.controller;

import com.order.api.service.ApiLocaleService;
import com.order.api.service.LocalizedApiMessageService;
import com.order.api.service.UserApiService;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
    private LocalizedApiMessageService messageService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ApiLocaleService localeService = new ApiLocaleService();
        AuthController controller =
                new AuthController(userApiService, serverConfig, localeService, messageService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new UserApiExceptionHandler(localeService, messageService))
                .build();
    }

    @Test
    void localizedUserProfileExposesTheResolvedLanguageHeaders() throws Exception {
        when(messageService.message(
                eq(200), eq(SupportedLocale.FR_FR), anyString(), any(Object[].class)))
                .thenReturn("Succès");

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
    void avatarRejectsNonImageMimeWithOnlyTheLocalizedGeneric703Message() throws Exception {
        when(messageService.message(
                eq(703), eq(SupportedLocale.ES_ES), eq("Upload failed"), any(Object[].class)))
                .thenReturn("Error al subir");
        MockMultipartFile file =
                new MockMultipartFile("file", "avatar.png", "text/plain", "not-an-image".getBytes());

        mockMvc.perform(multipart("/api/user/avatar")
                        .file(file)
                        .requestAttr("userId", 7L)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "es-ES"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_LANGUAGE, "es-ES"))
                .andExpect(jsonPath("$.code").value(703))
                .andExpect(jsonPath("$.msg").value("Error al subir"));

        verify(userApiService, never()).updateAvatar(eq(7L), org.mockito.ArgumentMatchers.any());
    }
}
