package com.order.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.member.domain.OrderConfig;
import com.order.member.service.IOrderConfigService;
import com.order.system.domain.SysTimeZone;
import com.order.system.service.ISysTimeZoneService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static com.order.api.service.OrderErrorCodes.INVALID_CONFIG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderTradePolicyServiceTest {
    @Mock IOrderConfigService configService;
    @Mock ISysTimeZoneService timeZoneService;

    @Test
    void parsesNumericMatchPercentage() {
        arrange("""
                {
                  "tradeTimeRange":["00:00","00:00"],
                  "minTradeBalance":10,
                  "parentRebatePercentage":"5.5",
                  "matchRangePercentage":30
                }
                """);

        var policy = service().activePolicy();

        assertEquals(new BigDecimal("30"), policy.matchRange().minimum());
        assertEquals(new BigDecimal("30"), policy.matchRange().maximum());
        assertEquals(new BigDecimal("5.5"), policy.parentRebatePercentage());
    }

    @Test
    void parsesStringMatchRange() {
        arrange("""
                {
                  "tradeTimeRange":["00:00","00:00"],
                  "minTradeBalance":"20.00",
                  "parentRebatePercentage":5,
                  "matchRangePercentage":"30-60"
                }
                """);

        var policy = service().activePolicy();

        assertEquals(new BigDecimal("30"), policy.matchRange().minimum());
        assertEquals(new BigDecimal("60"), policy.matchRange().maximum());
        assertEquals(new BigDecimal("20.00"), policy.minimumBalance());
    }

    @Test
    void rejectsOutOfRangeTypedConfiguration() {
        arrange("""
                {
                  "tradeTimeRange":["00:00","00:00"],
                  "minTradeBalance":10,
                  "parentRebatePercentage":101,
                  "matchRangePercentage":"60-30"
                }
                """);

        OrderApiException error = assertThrows(
                OrderApiException.class, () -> service().activePolicy());

        assertEquals(INVALID_CONFIG, error.getBusinessCode());
    }

    private void arrange(String content) {
        OrderConfig config = new OrderConfig();
        config.setContent(content);
        SysTimeZone zone = new SysTimeZone();
        zone.setTzName("Asia/Shanghai");
        when(configService.selectOrderConfigByType("trade")).thenReturn(config);
        when(timeZoneService.getActive()).thenReturn(zone);
    }

    private OrderTradePolicyService service() {
        return new OrderTradePolicyService(new TradeConfigSnapshotService(
                configService, timeZoneService, new ObjectMapper()));
    }
}
