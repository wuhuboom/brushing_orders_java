package com.order.api.controller;

import com.order.api.service.WithdrawalApplicationService;
import com.order.common.exception.ServiceException;
import com.order.framework.web.exception.GlobalExceptionHandler;
import com.order.member.service.IOrderWithdrawalService;
import com.order.web.controller.member.OrderWithdrawalController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminWithdrawalAdviceIsolationTest {
    @Mock
    private IOrderWithdrawalService withdrawalQueryService;
    @Mock
    private WithdrawalApplicationService withdrawalApplicationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        OrderWithdrawalController controller = new OrderWithdrawalController(
                withdrawalQueryService, withdrawalApplicationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(
                        new AccountApiExceptionHandler(),
                        new PublicApiFallbackExceptionHandler(),
                        new GlobalExceptionHandler())
                .build();
    }

    @Test
    void adminAccessDeniedStillUsesTheManagementGlobalHandler() throws Exception {
        when(withdrawalApplicationService.sensitiveAccount(12L))
                .thenThrow(new AccessDeniedException("denied"));

        mockMvc.perform(get("/member/withdrawal/12/sensitive-account"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.msg").value("没有权限，请联系管理员授权"));
    }

    @Test
    void adminServiceExceptionKeepsItsManagementCodeAndMessage() throws Exception {
        when(withdrawalApplicationService.sensitiveAccount(12L))
                .thenThrow(new ServiceException("后台业务错误", 777));

        mockMvc.perform(get("/member/withdrawal/12/sensitive-account"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(777))
                .andExpect(jsonPath("$.msg").value("后台业务错误"));
    }
}
