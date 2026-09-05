package com.order.member.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.member.domain.OrderConfig;
import com.order.member.domain.OrderSiteMessage;
import com.order.member.domain.OrderUser;
import com.order.member.mapper.OrderUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SiteMessageNotificationServiceTest {
    @Mock
    private IOrderConfigService configService;
    @Mock
    private IOrderSiteMessageService messageService;
    @Mock
    private OrderUserMapper userMapper;

    private SiteMessageNotificationService service;

    @BeforeEach
    void setUp() {
        service = new SiteMessageNotificationService(
                configService, messageService, userMapper, new ObjectMapper());
    }

    @Test
    void createsRenderedRechargeMessageForOnlyTheAffectedMember() {
        OrderConfig notification = new OrderConfig();
        notification.setContent("""
                {
                  "recharge": {
                    "enabled": 1,
                    "formatAmount": 1,
                    "title": "Balance changed for {username}",
                    "content": "<p>${amount} {currencyUnit}: {beforeBalance} -> {afterBalance}</p>"
                  }
                }
                """);
        OrderConfig website = new OrderConfig();
        website.setContent("{\"currencyUnit\":\"USD\"}");
        when(configService.selectOrderConfigByType("notification")).thenReturn(notification);
        when(configService.selectOrderConfigByType("website")).thenReturn(website);

        OrderUser user = new OrderUser();
        user.setId(7L);
        user.setUsername("member01");
        user.setPhoneNumber("13800000000");
        when(userMapper.selectOrderUserById(7L)).thenReturn(user);

        service.createForTransaction(
                7L,
                "cz",
                new BigDecimal("1234.5"),
                new BigDecimal("10"),
                new BigDecimal("1244.5"));

        ArgumentCaptor<OrderSiteMessage> captor =
                ArgumentCaptor.forClass(OrderSiteMessage.class);
        verify(messageService).insertOrderSiteMessage(captor.capture());
        OrderSiteMessage message = captor.getValue();
        assertEquals("7", message.getMemberList());
        assertEquals(1, message.getIsEnabled());
        assertEquals("Balance changed for member01", message.getTitle());
        assertEquals("<p>1,234.50 USD: 10.00 -> 1,244.50</p>", message.getContent());
    }

    @Test
    void disabledTemplateDoesNotCreateMessage() {
        OrderConfig notification = new OrderConfig();
        notification.setContent("""
                {
                  "task": {
                    "enabled": 0,
                    "formatAmount": 0,
                    "title": "Task",
                    "content": "Task created"
                  }
                }
                """);
        when(configService.selectOrderConfigByType("notification")).thenReturn(notification);

        service.createForTransaction(
                7L,
                "rw",
                BigDecimal.TEN.negate(),
                new BigDecimal("100"),
                new BigDecimal("90"));

        verify(userMapper, never()).selectOrderUserById(7L);
        verify(messageService, never()).insertOrderSiteMessage(
                org.mockito.ArgumentMatchers.any(OrderSiteMessage.class));
    }

    @Test
    void unformattedAmountsKeepTheirDatabaseScale() {
        OrderConfig notification = new OrderConfig();
        notification.setContent("""
                {
                  "deposit": {
                    "enabled": 1,
                    "formatAmount": 0,
                    "title": "Deposit",
                    "content": "Amount: ${amount}"
                  }
                }
                """);
        when(configService.selectOrderConfigByType("notification")).thenReturn(notification);
        when(configService.selectOrderConfigByType("website")).thenReturn(null);

        OrderUser user = new OrderUser();
        user.setId(7L);
        when(userMapper.selectOrderUserById(7L)).thenReturn(user);

        service.createForTransaction(
                7L,
                "ck",
                new BigDecimal("20.00"),
                new BigDecimal("100.00"),
                new BigDecimal("120.00"));

        ArgumentCaptor<OrderSiteMessage> captor =
                ArgumentCaptor.forClass(OrderSiteMessage.class);
        verify(messageService).insertOrderSiteMessage(captor.capture());
        assertEquals("Amount: 20.00", captor.getValue().getContent());
    }

    @Test
    void actualTransactionDictionaryCodesResolveToTheirNotificationTemplates() {
        assertEquals("bonus", service.resolveTemplateKey("jj", BigDecimal.TEN));
        assertEquals("bonus", service.resolveTemplateKey("bonus", BigDecimal.TEN));
        assertEquals("deduction", service.resolveTemplateKey("kk", BigDecimal.TEN.negate()));
        assertEquals("withdrawing", service.resolveTemplateKey("txz", BigDecimal.TEN));
        assertEquals("withdrawalUnfreeze", service.resolveTemplateKey("txjd", BigDecimal.TEN));
        assertEquals("withdrawal", service.resolveTemplateKey("tx", BigDecimal.TEN));
    }
}
