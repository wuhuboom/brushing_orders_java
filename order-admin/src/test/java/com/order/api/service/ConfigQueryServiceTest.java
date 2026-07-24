package com.order.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.api.controller.dto.ConfigApiDtos;
import com.order.common.i18n.ITranslationsService;
import com.order.common.i18n.SupportedLocale;
import com.order.common.i18n.Translations;
import com.order.member.domain.GoodsMemberLevel;
import com.order.member.domain.GoodsCustomerService;
import com.order.member.domain.OrderConfig;
import com.order.member.service.IGoodsCustomerServiceService;
import com.order.member.service.IGoodsMemberLevelService;
import com.order.member.service.IOrderConfigService;
import com.order.system.service.ISysNoticeService;
import com.order.system.service.ISysTimeZoneService;
import com.order.system.domain.SysTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ConfigQueryServiceTest {
    @Mock
    private IOrderConfigService orderConfigService;
    @Mock
    private IGoodsCustomerServiceService customerServiceService;
    @Mock
    private IGoodsMemberLevelService levelService;
    @Mock
    private ISysNoticeService noticeService;
    @Mock
    private ISysTimeZoneService timeZoneService;
    @Mock
    private ITranslationsService translationsService;

    private ConfigQueryService service;

    @BeforeEach
    void setUp() {
        service = new ConfigQueryService(
                orderConfigService,
                customerServiceService,
                levelService,
                noticeService,
                timeZoneService,
                translationsService,
                new ObjectMapper());
    }

    @Test
    void contentUsesPerFieldFallbackWithOneBusinessAndOneTranslationQuery() {
        OrderConfig register = config(
                1L,
                11L,
                "register",
                "{\"protocolContent\":\"Base protocol\"}");
        OrderConfig about = config(
                2L,
                12L,
                "about",
                "{\"aboutContent\":\"Base about\"}");

        Translations registerTranslations = translations(
                11L,
                "{\"protocolContent\":\"English protocol\"}",
                "{\"unrelated\":\"Sin valor\"}");
        Translations aboutTranslations = translations(
                12L,
                null,
                "{\"aboutContent\":\"Acerca de\"}");

        when(orderConfigService.selectOrderConfigByTypes(anyList())).thenReturn(List.of(register, about));
        when(translationsService.selectTranslationsByIds(anyCollection()))
                .thenReturn(Map.of(11L, registerTranslations, 12L, aboutTranslations));

        ConfigApiDtos.GlobalContentResponse result = service.content(SupportedLocale.ES_ES);

        assertEquals("English protocol", result.protocolContent());
        assertEquals("Acerca de", result.aboutContent());
        assertNull(result.helpContent());
        verify(orderConfigService).selectOrderConfigByTypes(anyList());
        verify(translationsService).selectTranslationsByIds(anyCollection());
    }

    @Test
    void memberLevelSupportsLegacyRawDescriptionAndJsonNameFallback() {
        GoodsMemberLevel level = new GoodsMemberLevel();
        level.setId(8L);
        level.setTranslationsId(80L);
        level.setName("Base name");
        level.setDescription("Base description");

        Translations translations = translations(
                80L,
                "{\"name\":\"English VIP\",\"description\":\"English description\"}",
                "Descripción heredada");

        when(levelService.selectGoodsMemberLevelList(null)).thenReturn(List.of(level));
        when(translationsService.selectTranslationsByIds(anyCollection())).thenReturn(Map.of(80L, translations));

        ConfigApiDtos.MemberLevelResponse result =
                service.memberLevels(SupportedLocale.ES_ES).get(0);

        assertEquals("English VIP", result.name());
        assertEquals("Descripción heredada", result.description());
    }

    @Test
    void damagedStrictJsonFallsBackInsteadOfLeakingBrokenTranslation() {
        GoodsMemberLevel level = new GoodsMemberLevel();
        level.setId(9L);
        level.setTranslationsId(90L);
        level.setName("Base name");
        level.setDescription("Base description");

        Translations translations = translations(
                90L,
                "{\"name\":\"English name\",\"description\":\"English description\"}",
                "{\"name\":");

        when(levelService.selectGoodsMemberLevelList(null)).thenReturn(List.of(level));
        when(translationsService.selectTranslationsByIds(anyCollection())).thenReturn(Map.of(90L, translations));

        ConfigApiDtos.MemberLevelResponse result =
                service.memberLevels(SupportedLocale.ES_ES).get(0);

        assertEquals("English name", result.name());
        assertEquals("English description", result.description());
    }

    @Test
    void tradeReturnsOnlyThePublicAllowlist() {
        OrderConfig trade = config(
                3L,
                null,
                "trade",
                """
                {
                  "registerBonusAmount": 12,
                  "minTradeBalance": 100,
                  "matchDelaySeconds": 999,
                  "passwordFailureThreshold": 3
                }
                """);
        when(orderConfigService.selectOrderConfigByType("trade")).thenReturn(trade);

        ConfigApiDtos.TradeConfigResponse result = service.trade();

        assertEquals(12, result.registerBonusAmount());
        assertEquals(100, result.minTradeBalance());
    }

    @Test
    void customerServicesFilterEnabledRowsAndUseStrictJsonFallback() {
        SysTimeZone timeZone = new SysTimeZone();
        timeZone.setTzName("UTC");
        when(timeZoneService.getActive()).thenReturn(timeZone);
        when(orderConfigService.getConfigValue("trade", "serviceTimeRange"))
                .thenReturn(Optional.of(List.of("00:00", "00:00")));

        GoodsCustomerService customer = new GoodsCustomerService();
        customer.setId("customer-1");
        customer.setName("Base support");
        customer.setSortOrder(10L);
        customer.setTranslationsId(101L);
        when(customerServiceService.selectGoodsCustomerServiceList(any()))
                .thenReturn(List.of(customer));
        Translations translations = translations(
                101L,
                "{\"name\":\"English support\"}",
                "{\"name\":");
        when(translationsService.selectTranslationsByIds(anyCollection()))
                .thenReturn(Map.of(101L, translations));

        ConfigApiDtos.CustomerServiceResponse result =
                service.customerServices(SupportedLocale.ES_ES).get(0);

        assertEquals("English support", result.name());
        ArgumentCaptor<GoodsCustomerService> criteria =
                ArgumentCaptor.forClass(GoodsCustomerService.class);
        verify(customerServiceService).selectGoodsCustomerServiceList(criteria.capture());
        assertEquals("1", criteria.getValue().getIsEnabled());
    }

    private OrderConfig config(Long id, Long translationsId, String type, String content) {
        OrderConfig config = new OrderConfig();
        config.setId(id);
        config.setTranslationsId(translationsId);
        config.setType(type);
        config.setContent(content);
        return config;
    }

    private Translations translations(Long id, String english, String spanish) {
        Translations translations = new Translations();
        translations.setId(id);
        translations.setEnUs(english);
        translations.setEsEs(spanish);
        return translations;
    }
}
