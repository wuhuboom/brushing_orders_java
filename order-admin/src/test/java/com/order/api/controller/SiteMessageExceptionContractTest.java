package com.order.api.controller;

import com.order.api.service.ApiLocaleService;
import com.order.api.service.ConfigApiException;
import com.order.api.service.SiteMessageQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SiteMessageExceptionContractTest {
    @Mock
    private SiteMessageQueryService queryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ApiLocaleService localeService = new ApiLocaleService();
        SiteMessageController controller = new SiteMessageController(queryService, localeService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(
                        new ConfigApiExceptionHandler(localeService),
                        new PublicApiFallbackExceptionHandler(),
                        new ApiResponseLocalizationAdvice())
                .build();
    }

    @Test
    void missingOwnedMessageKeepsItsConfiguredHttpStatusAndCode() throws Exception {
        when(queryService.get(eq(7L), eq(99L), any()))
                .thenThrow(new ConfigApiException(404, HttpStatus.NOT_FOUND, "站内信不存在"));

        mockMvc.perform(get("/api/messages/99").requestAttr("userId", 7L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.msg").value("Resource not found"));
    }
}
