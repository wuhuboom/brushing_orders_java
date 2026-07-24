package com.order.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.common.i18n.ITranslationsService;
import com.order.member.domain.OrderConfig;
import com.order.member.mapper.OrderConfigMapper;
import com.order.member.service.IOrderConfigService;
import com.order.member.service.impl.OrderConfigServiceImpl;
import com.order.system.domain.SysTimeZone;
import com.order.system.mapper.SysTimeZoneMapper;
import com.order.system.service.ISysTimeZoneService;
import com.order.system.service.impl.SysTimeZoneServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TradeConfigSnapshotCacheTest {
    private AnnotationConfigApplicationContext context;
    private OrderConfigMapper configMapper;
    private SysTimeZoneMapper timeZoneMapper;
    private TradeConfigSnapshotService snapshotService;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(TestConfig.class);
        configMapper = context.getBean(OrderConfigMapper.class);
        timeZoneMapper = context.getBean(SysTimeZoneMapper.class);
        snapshotService = context.getBean(TradeConfigSnapshotService.class);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void updatingTradeConfigurationEvictsSnapshotImmediately() {
        OrderConfig initial = config("{\"minTradeBalance\":10}");
        OrderConfig updated = config("{\"minTradeBalance\":20}");
        when(configMapper.selectOrderConfigByType("trade")).thenReturn(initial, updated);
        when(configMapper.updateOrderConfig(any())).thenReturn(1);
        when(timeZoneMapper.getActive()).thenReturn(zone("UTC"));

        assertEquals("10", String.valueOf(
                snapshotService.snapshot().values().get("minTradeBalance")));
        IOrderConfigService configService = context.getBean(IOrderConfigService.class);
        configService.updateOrderConfig(updated);
        assertEquals("20", String.valueOf(
                snapshotService.snapshot().values().get("minTradeBalance")));

        verify(configMapper, org.mockito.Mockito.times(2))
                .selectOrderConfigByType("trade");
    }

    @Test
    void switchingActiveTimezoneEvictsSnapshotImmediately() {
        when(configMapper.selectOrderConfigByType("trade"))
                .thenReturn(config("{\"tradeTimeRange\":[\"00:00\",\"00:00\"]}"));
        when(timeZoneMapper.getActive()).thenReturn(zone("UTC"), zone("Asia/Shanghai"));
        when(timeZoneMapper.setActiveById(2L)).thenReturn(1);

        assertEquals("UTC", snapshotService.snapshot().timeZone());
        ISysTimeZoneService timeZoneService = context.getBean(ISysTimeZoneService.class);
        timeZoneService.setActiveById(2L);
        assertEquals("Asia/Shanghai", snapshotService.snapshot().timeZone());

        verify(timeZoneMapper, org.mockito.Mockito.times(2)).getActive();
    }

    private OrderConfig config(String content) {
        OrderConfig config = new OrderConfig();
        config.setId(1L);
        config.setType("trade");
        config.setContent(content);
        return config;
    }

    private SysTimeZone zone(String name) {
        SysTimeZone zone = new SysTimeZone();
        zone.setTzName(name);
        return zone;
    }

    @Configuration
    @EnableCaching(proxyTargetClass = true)
    static class TestConfig {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                    TradeConfigSnapshotService.CACHE_NAME, "apiMessageCatalog");
        }

        @Bean
        OrderConfigMapper orderConfigMapper() {
            return mock(OrderConfigMapper.class);
        }

        @Bean
        SysTimeZoneMapper sysTimeZoneMapper() {
            return mock(SysTimeZoneMapper.class);
        }

        @Bean
        ITranslationsService translationsService() {
            return mock(ITranslationsService.class);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        IOrderConfigService orderConfigService(
                OrderConfigMapper mapper,
                ObjectMapper objectMapper,
                ITranslationsService translationsService) {
            return new OrderConfigServiceImpl(mapper, objectMapper, translationsService);
        }

        @Bean
        ISysTimeZoneService timeZoneService(SysTimeZoneMapper mapper) {
            return new SysTimeZoneServiceImpl(mapper);
        }

        @Bean
        TradeConfigSnapshotService snapshotService(
                IOrderConfigService configService,
                ISysTimeZoneService timeZoneService,
                ObjectMapper objectMapper) {
            return new TradeConfigSnapshotService(
                    configService, timeZoneService, objectMapper);
        }
    }
}
