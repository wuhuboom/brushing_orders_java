package com.order.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.common.i18n.SupportedLocale;
import com.order.common.i18n.Translations;
import com.order.member.domain.OrderConfig;
import com.order.member.service.IOrderConfigService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocalizedMessageCatalogServiceTest {

    @Test
    void messagesFallBackFromRequestedToEnglishToBase() {
        IOrderConfigService configService = mock(IOrderConfigService.class);
        OrderConfig config = new OrderConfig();
        config.setId(7L);
        config.setContent("""
                {"codes":[
                  {"code":"701","message":"Base no data"},
                  {"code":"703","message":"Base upload"},
                  {"code":"920","message":"Base unavailable"}
                ]}
                """);
        Translations translations = new Translations();
        translations.setEnUs("""
                {"codes":[
                  {"code":"701","message":"English no data"},
                  {"code":"703","message":"English upload"}
                ]}
                """);
        translations.setEsEs("""
                {"codes":[
                  {"code":"701","message":"Sin datos"}
                ]}
                """);
        config.setTranslations(translations);
        when(configService.selectOrderConfigByType("error")).thenReturn(config);

        LocalizedMessageCatalogService service =
                new LocalizedMessageCatalogService(configService, new ObjectMapper());
        Map<String, String> catalog = service.catalog(SupportedLocale.ES_ES);

        assertEquals("Sin datos", catalog.get("701"));
        assertEquals("English upload", catalog.get("703"));
        assertEquals("Base unavailable", catalog.get("920"));
    }

    @Test
    void damagedRequestedCatalogStillReturnsEnglishAndBaseMessages() {
        IOrderConfigService configService = mock(IOrderConfigService.class);
        OrderConfig config = new OrderConfig();
        config.setId(8L);
        config.setContent("{\"codes\":[{\"code\":\"920\",\"message\":\"Base\"}]}");
        Translations translations = new Translations();
        translations.setEnUs("{\"codes\":[{\"code\":\"701\",\"message\":\"English\"}]}");
        translations.setFrFr("{\"codes\":");
        config.setTranslations(translations);
        when(configService.selectOrderConfigByType("error")).thenReturn(config);

        LocalizedMessageCatalogService service =
                new LocalizedMessageCatalogService(configService, new ObjectMapper());
        Map<String, String> catalog = service.catalog(SupportedLocale.FR_FR);

        assertEquals("English", catalog.get("701"));
        assertEquals("Base", catalog.get("920"));
    }
}
