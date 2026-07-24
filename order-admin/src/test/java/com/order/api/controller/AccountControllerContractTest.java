package com.order.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.api.controller.dto.AccountApiDtos.WithdrawalRequest;
import com.order.api.controller.dto.AccountApiDtos.WithdrawalResponse;
import com.order.api.service.AccountApiException;
import com.order.api.service.WithdrawalAccountApplicationService;
import com.order.api.service.WithdrawalApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

import static com.order.api.service.AccountErrorCodes.IDEMPOTENCY_CONFLICT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AccountControllerContractTest {
    @Mock
    WithdrawalAccountApplicationService accountService;
    @Mock
    WithdrawalApplicationService withdrawalService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AccountController(accountService, withdrawalService))
                .setControllerAdvice(new AccountApiExceptionHandler())
                .build();
    }

    @Test
    void newWithdrawalUsesCreatedStatusAndDedicatedResponse() throws Exception {
        UUID requestId = UUID.randomUUID();
        WithdrawalResponse response = new WithdrawalResponse(
                12L, "W20260717001", new BigDecimal("100.00"),
                new BigDecimal("0.50"), new BigDecimal("99.50"), "1", new Date());
        when(withdrawalService.submit(eq(7L), any(WithdrawalRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/account/withdrawals")
                        .requestAttr("userId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WithdrawalRequest(
                                requestId, new BigDecimal("100.00"), "secret", 3L))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.withdrawalId").value(12))
                .andExpect(jsonPath("$.data.netAmount").value(99.5))
                .andExpect(jsonPath("$.data.userId").doesNotExist())
                .andExpect(jsonPath("$.data.tradePassword").doesNotExist());
    }

    @Test
    void newEndpointReturnsRestConflictStatus() throws Exception {
        when(withdrawalService.submit(eq(7L), any(WithdrawalRequest.class)))
                .thenThrow(AccountApiException.conflict(
                        IDEMPOTENCY_CONFLICT, "requestId conflict"));

        mockMvc.perform(post("/api/account/withdrawals")
                        .requestAttr("userId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WithdrawalRequest(
                                UUID.randomUUID(), new BigDecimal("10.00"), "secret", 3L))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(IDEMPOTENCY_CONFLICT));
    }

    @Test
    void legacyEndpointKeepsHttp200AndLegacyBusinessCode() throws Exception {
        when(withdrawalService.submitLegacy(7L, new BigDecimal("10.00"), "secret", 3L))
                .thenThrow(AccountApiException.conflict(
                        IDEMPOTENCY_CONFLICT, "requestId conflict"));

        mockMvc.perform(post("/api/account/withdrawal")
                        .requestAttr("userId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":10.00,"tradePassword":"secret","walletId":3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(IDEMPOTENCY_CONFLICT));
    }

    @Test
    void deleteUsesNoContentStatus() throws Exception {
        mockMvc.perform(delete("/api/account/withdrawal-accounts/3")
                        .requestAttr("userId", 7L))
                .andExpect(status().isNoContent());
    }

    @Test
    void invalidNewRequestReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/account/withdrawals")
                        .requestAttr("userId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":10.00,"tradePassword":"secret","withdrawalAccountId":3}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(515));
    }
}
