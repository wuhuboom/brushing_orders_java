package com.order.web.controller.member;

import com.order.member.domain.dto.TransactionDto;
import com.order.member.service.AdminTransactionRequestService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderUserGiftIdempotencyTest {
    @Test
    void giftUsesDurableRequestIdAndIgnoresClientMoneyOperationType() {
        OrderUserController controller = new OrderUserController() {
            @Override public Long getUserId() { return 42L; }
        };
        AdminTransactionRequestService requests = mock(AdminTransactionRequestService.class);
        ReflectionTestUtils.setField(controller, "adminTransactionRequests", requests);
        when(requests.submit(eq(42L), any())).thenReturn(
                new AdminTransactionRequestService.Result("gift-request-00001", "SUCCEEDED", "OK"));
        TransactionDto input = new TransactionDto();
        input.setRequestId("gift-request-00001");
        input.setUserId(5L);
        input.setAmount(new BigDecimal("10.00"));
        input.setOperationType(1);
        input.setTransactionType("kk");
        input.setGiftAmount(new BigDecimal("999.00"));
        assertEquals(200, controller.giftAmount(input).get("code"));
        ArgumentCaptor<TransactionDto> saved = ArgumentCaptor.forClass(TransactionDto.class);
        verify(requests).submit(eq(42L), saved.capture());
        assertEquals("gift-request-00001", saved.getValue().getRequestId());
        assertEquals(0, saved.getValue().getOperationType());
        assertEquals("zs", saved.getValue().getTransactionType());
        assertEquals(BigDecimal.ZERO, saved.getValue().getGiftAmount());
        assertEquals(input.getAmount(), saved.getValue().getAmount());
    }
}
