package com.order.web.controller.member;

import com.order.api.service.WithdrawalApplicationService;
import com.order.member.service.IOrderWithdrawalService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderWithdrawalNotificationTest {
    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean(name = "ss") Gate gate() { return new Gate(); }
        @Bean IOrderWithdrawalService queryService() { return mock(IOrderWithdrawalService.class); }
        @Bean WithdrawalApplicationService applicationService() { return mock(WithdrawalApplicationService.class); }
        @Bean OrderWithdrawalController controller(IOrderWithdrawalService query, WithdrawalApplicationService application) {
            return new OrderWithdrawalController(query, application);
        }
    }

    public static class Gate {
        boolean allowed;
        public boolean hasPermi(String permission) {
            return allowed && "member:withdrawal:list".equals(permission);
        }
    }

    @Test
    void permissionIsEnforcedAndResponseContainsOnlyIdsWithNoCache() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("admin", "unused", List.of()));
            var controller = context.getBean(OrderWithdrawalController.class);
            var service = context.getBean(IOrderWithdrawalService.class);
            var application = context.getBean(WithdrawalApplicationService.class);
            assertThrows(AccessDeniedException.class, controller::notifications);
            verifyNoInteractions(service, application);
            context.getBean(Gate.class).allowed = true;
            when(service.selectPendingNotificationIds()).thenReturn(List.of("12", "9007199254740993"));
            var response = controller.notifications();
            assertEquals(200, response.getStatusCode().value());
            assertEquals("no-store", response.getHeaders().getCacheControl());
            assertEquals(List.of("12", "9007199254740993"), response.getBody().get("data"));
            verify(service).selectPendingNotificationIds();
            verifyNoInteractions(application);
        } finally { SecurityContextHolder.clearContext(); }
    }
}
