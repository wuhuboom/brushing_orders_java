package com.order.api.controller;

import com.order.api.controller.dto.OrderApiDtos.CreationResult;
import com.order.api.controller.dto.OrderApiDtos.SubmitResponse;
import com.order.api.service.OrderApiException;
import com.order.api.service.OrderApplicationService;
import com.order.common.core.page.TableDataInfo;
import com.order.framework.web.exception.GlobalExceptionHandler;
import com.order.member.domain.OrderBonusTable;
import com.order.member.domain.OrderInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static com.order.api.service.OrderErrorCodes.ORDER_STATE_CONFLICT;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderControllerContractTest {
    @Mock OrderApplicationService orderService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new OrderController(orderService))
                .setControllerAdvice(
                        new OrderApiExceptionHandler(),
                        new PublicApiFallbackExceptionHandler(),
                        new GlobalExceptionHandler(),
                        new ApiResponseLocalizationAdvice())
                .build();
    }

    @Test
    void createReturnsOrderDiscriminatorAndNoSensitiveFields() throws Exception {
        OrderInfo order = order();
        order.setUserId(7L);
        order.setUpperRebate(new BigDecimal("9.99"));
        order.setExtraCommissionId(12L);
        order.setLinkId(13L);
        order.setRemarks("internal");
        when(orderService.create(7L, "request-12345678"))
                .thenReturn(CreationResult.order(order));

        mockMvc.perform(post("/api/order")
                        .requestAttr("userId", 7L)
                        .header("Idempotency-Key", "request-12345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.resultType").value("ORDER"))
                .andExpect(jsonPath("$.data.id").value(20))
                .andExpect(jsonPath("$.data.productTitle").value("Snapshot"))
                .andExpect(jsonPath("$.data.userId").doesNotExist())
                .andExpect(jsonPath("$.data.upperRebate").doesNotExist())
                .andExpect(jsonPath("$.data.extraCommissionId").doesNotExist())
                .andExpect(jsonPath("$.data.linkId").doesNotExist())
                .andExpect(jsonPath("$.data.remarks").doesNotExist());
    }

    @Test
    void bonusUsesSuccessCodeAndSafeDto() throws Exception {
        OrderBonusTable bonus = new OrderBonusTable();
        bonus.setId(88L);
        bonus.setUserId(7L);
        bonus.setAmount(new BigDecimal("28.88"));
        bonus.setIsDistributed("1");
        bonus.setIsReceived("1");
        when(orderService.create(7L, "request-bonus-1234"))
                .thenReturn(CreationResult.bonus(bonus));

        mockMvc.perform(post("/api/order")
                        .requestAttr("userId", 7L)
                        .header("Idempotency-Key", "request-bonus-1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.resultType").value("BONUS"))
                .andExpect(jsonPath("$.data.id").value(88))
                .andExpect(jsonPath("$.data.userId").doesNotExist())
                .andExpect(jsonPath("$.data.isDistributed").doesNotExist())
                .andExpect(jsonPath("$.data.isReceived").doesNotExist());
    }

    @Test
    void newCreateRequiresAnIdempotencyKey() throws Exception {
        mockMvc.perform(post("/api/order").requestAttr("userId", 7L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(923))
                .andExpect(jsonPath("$.msg").value("Invalid request"));
    }

    @Test
    void submitUsesPost() throws Exception {
        when(orderService.submit(7L, 20L))
                .thenReturn(new SubmitResponse(20L, "0", false));

        mockMvc.perform(post("/api/order/20/submit").requestAttr("userId", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("0"));
        mockMvc.perform(get("/api/order/20/submit").requestAttr("userId", 7L))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(405))
                .andExpect(jsonPath("$.msg").value("Method not allowed"));
    }

    @Test
    void submitConflictUsesHttpConflict() throws Exception {
        when(orderService.submit(7L, 20L)).thenThrow(
                OrderApiException.conflict(ORDER_STATE_CONFLICT, "conflict"));

        mockMvc.perform(post("/api/order/20/submit").requestAttr("userId", 7L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ORDER_STATE_CONFLICT));
    }

    @Test
    void listDelegatesCurrentTokenUserAndReturnsPageContract() throws Exception {
        TableDataInfo page = new TableDataInfo();
        page.setCode(200);
        page.setMsg("success");
        page.setRows(List.of());
        page.setTotal(0);
        when(orderService.orders(7L, "0", 2, 10)).thenReturn(page);

        mockMvc.perform(get("/api/order")
                        .requestAttr("userId", 7L)
                        .param("status", "0")
                        .param("pageNum", "2")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.total").value(0));
    }

    private OrderInfo order() {
        OrderInfo order = new OrderInfo();
        order.setId(20L);
        order.setOrderNumber("O-20");
        order.setType("0");
        order.setOrderCount(2L);
        order.setAmount(new BigDecimal("30.00"));
        order.setRebatePercentage(new BigDecimal("1.00"));
        order.setRebate(new BigDecimal("0.30"));
        order.setStatus("1");
        order.setProductId(31L);
        order.setProductTitle("Snapshot");
        order.setProductImage("/snapshot.jpg");
        return order;
    }
}
