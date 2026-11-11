package com.order.member.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.common.i18n.ITranslationsService;
import com.order.common.i18n.Translations;
import com.order.member.domain.Goods;
import com.order.member.domain.GoodsMemberLevel;
import com.order.member.domain.OrderConfig;
import com.order.member.domain.OrderSiteMessage;
import com.order.member.mapper.GoodsMapper;
import com.order.member.mapper.GoodsMemberLevelMapper;
import com.order.member.mapper.OrderConfigMapper;
import com.order.member.mapper.OrderSiteMessageMapper;
import com.order.member.mapper.TranslationsMapper;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RichTextRelativePersistenceTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String ABSOLUTE_IMAGE = "https://old.example.com/profile/upload/picture.jpg";
    private static final String RELATIVE_IMAGE = "/profile/upload/picture.jpg";
    private static final String HTML = "<p>Saved description</p><img src=\"" + ABSOLUTE_IMAGE + "\">";
    private static final String FORGED = "<p>Unrelated client-supplied content</p>";

    @Test
    void goodsInsertPersistsDerivedCopyAndPreservesOriginalHtml() {
        GoodsMapper mapper = mock(GoodsMapper.class);
        GoodsServiceImpl service = new GoodsServiceImpl(mapper);
        Goods goods = new Goods();
        goods.setDescription(HTML);
        goods.setDescriptionRelative(FORGED);

        service.insertGoods(goods);

        verify(mapper).insertGoods(goods);
        assertEquals(HTML, goods.getDescription());
        assertRelativeImage(goods.getDescriptionRelative());
    }

    @Test
    void relativeGoodsInputProducesAbsoluteLegacyCopyUsingConfiguredResourceBase() {
        String previous = com.order.common.config.OrderConfig.getResourceBaseUrl();
        com.order.common.config.OrderConfig configuration = new com.order.common.config.OrderConfig();
        configuration.setResourceBaseUrl("https://assets.example.com");
        try {
            GoodsMapper mapper = mock(GoodsMapper.class);
            GoodsServiceImpl service = new GoodsServiceImpl(mapper);
            Goods goods = new Goods();
            goods.setDescription(HTML.replace(ABSOLUTE_IMAGE, RELATIVE_IMAGE));

            service.insertGoods(goods);

            verify(mapper).insertGoods(goods);
            assertTrue(goods.getDescription().contains("https://assets.example.com" + RELATIVE_IMAGE));
            assertRelativeImage(goods.getDescriptionRelative());
        } finally {
            configuration.setResourceBaseUrl(previous);
        }
    }

    @Test
    void goodsPartialUpdateCannotReplaceDerivedContentWithoutOriginal() {
        GoodsMapper mapper = mock(GoodsMapper.class);
        GoodsServiceImpl service = new GoodsServiceImpl(mapper);
        Goods update = new Goods();
        update.setId(12L);
        update.setTitle("New title only");
        update.setDescriptionRelative(FORGED);

        service.updateGoods(update);

        verify(mapper).updateGoods(update);
        assertNull(update.getDescription());
        assertNull(update.getDescriptionRelative());
    }

    @Test
    void explicitlyClearingGoodsDescriptionAlsoClearsRelativeCopy() {
        GoodsMapper mapper = mock(GoodsMapper.class);
        GoodsServiceImpl service = new GoodsServiceImpl(mapper);
        Goods update = new Goods();
        update.setId(12L);
        update.setDescription("");
        update.setDescriptionRelative(FORGED);

        service.updateGoods(update);

        verify(mapper).updateGoods(update);
        assertEquals("", update.getDescription());
        assertEquals("", update.getDescriptionRelative());
    }

    @Test
    void configurationCopiesHtmlInsideJsonOnInsertAndSubsequentEdit() throws Exception {
        OrderConfigMapper mapper = mock(OrderConfigMapper.class);
        OrderConfigServiceImpl service = new OrderConfigServiceImpl(
                mapper, JSON, mock(ITranslationsService.class));
        OrderConfig config = new OrderConfig();
        config.setType("certificate");
        config.setContent(JSON.writeValueAsString(java.util.Map.of("certificateContent", HTML)));
        config.setContentRelative(FORGED);
        String original = config.getContent();

        service.insertOrderConfig(config);

        verify(mapper).insertOrderConfig(config);
        assertEquals(JSON.readTree(original), JSON.readTree(config.getContent()));
        assertRelativeImage(JSON.readTree(config.getContentRelative()).path("certificateContent").asText());

        config.setId(13L);
        config.setContent(JSON.writeValueAsString(java.util.Map.of(
                "certificateContent", HTML.replace("picture.jpg", "replacement.jpg"))));
        config.setContentRelative(FORGED);
        String edited = config.getContent();

        service.updateOrderConfig(config);

        verify(mapper).updateOrderConfig(config);
        assertEquals(JSON.readTree(edited), JSON.readTree(config.getContent()));
        String relative = JSON.readTree(config.getContentRelative()).path("certificateContent").asText();
        assertTrue(relative.contains("/profile/upload/replacement.jpg"));
        assertFalse(relative.contains("old.example.com"));
        assertFalse(relative.contains("picture.jpg"));
    }

    @Test
    void configurationPartialUpdateAndExplicitEmptyHaveDistinctSemantics() {
        OrderConfigMapper mapper = mock(OrderConfigMapper.class);
        OrderConfigServiceImpl service = new OrderConfigServiceImpl(
                mapper, JSON, mock(ITranslationsService.class));
        OrderConfig partial = new OrderConfig();
        partial.setId(13L);
        partial.setSort(2L);
        partial.setContentRelative(FORGED);

        service.updateOrderConfig(partial);

        assertNull(partial.getContent());
        assertNull(partial.getContentRelative());

        OrderConfig empty = new OrderConfig();
        empty.setId(13L);
        empty.setContent("");
        empty.setContentRelative(FORGED);

        service.updateOrderConfig(empty);

        verify(mapper).updateOrderConfig(empty);
        assertEquals("", empty.getContent());
        assertEquals("", empty.getContentRelative());
    }

    @Test
    void memberLevelInsertAndUpdateRegenerateDescriptionCopy() throws Exception {
        GoodsMemberLevelMapper mapper = mock(GoodsMemberLevelMapper.class);
        GoodsMemberLevelServiceImpl service = new GoodsMemberLevelServiceImpl();
        inject(service, "goodsMemberLevelMapper", mapper);
        inject(service, "translationsService", mock(ITranslationsService.class));
        GoodsMemberLevel level = new GoodsMemberLevel();
        level.setDescription(HTML);
        level.setDescriptionRelative(FORGED);

        service.insertGoodsMemberLevel(level);

        verify(mapper).insertGoodsMemberLevel(level);
        assertEquals(HTML, level.getDescription());
        assertRelativeImage(level.getDescriptionRelative());

        level.setId(3L);
        level.setDescription("");
        level.setDescriptionRelative(FORGED);

        service.updateGoodsMemberLevel(level);

        verify(mapper).updateGoodsMemberLevel(level);
        assertEquals("", level.getDescriptionRelative());

        level.setDescription(null);
        level.setDescriptionRelative(FORGED);
        service.updateGoodsMemberLevel(level);
        assertNull(level.getDescriptionRelative());
    }

    @Test
    void siteMessageInsertAndUpdateKeepOriginalAndDerivedContentTogether() {
        OrderSiteMessageMapper mapper = mock(OrderSiteMessageMapper.class);
        OrderSiteMessageServiceImpl service = new OrderSiteMessageServiceImpl(
                mapper, mock(ITranslationsService.class));
        OrderSiteMessage message = new OrderSiteMessage();
        message.setContent(HTML);
        message.setContentRelative(FORGED);

        service.insertOrderSiteMessage(message);

        verify(mapper).insertOrderSiteMessage(message);
        assertEquals(HTML, message.getContent());
        assertRelativeImage(message.getContentRelative());

        message.setId(15L);
        message.setContent("");
        message.setContentRelative(FORGED);
        service.updateOrderSiteMessage(message);

        verify(mapper).updateOrderSiteMessage(message);
        assertEquals("", message.getContent());
        assertEquals("", message.getContentRelative());
    }

    @Test
    void siteMessagePartialUpdateIgnoresForgedDerivedContent() {
        OrderSiteMessageMapper mapper = mock(OrderSiteMessageMapper.class);
        OrderSiteMessageServiceImpl service = new OrderSiteMessageServiceImpl(
                mapper, mock(ITranslationsService.class));
        OrderSiteMessage message = new OrderSiteMessage();
        message.setId(15L);
        message.setTitle("New title only");
        message.setContentRelative(FORGED);

        service.updateOrderSiteMessage(message);

        verify(mapper).updateOrderSiteMessage(message);
        assertNull(message.getContent());
        assertNull(message.getContentRelative());
    }

    @Test
    void translationInsertDerivesAllProvidedLanguagesAndRejectsForgedCopy() throws Exception {
        TranslationsMapper mapper = mock(TranslationsMapper.class);
        TranslationsServiceImpl service = translationService(mapper);
        Translations translations = new Translations();
        translations.setEnUs(JSON.writeValueAsString(java.util.Map.of("description", HTML)));
        translations.setZhCn(HTML);
        translations.setRelativeContent("{\"fr_FR\":\"forged\"}");

        service.insertTranslations(translations);

        verify(mapper).insertTranslations(translations);
        assertEquals(HTML, translations.getZhCn());
        assertEquals(HTML, JSON.readTree(translations.getEnUs()).path("description").asText());
        JsonNode copies = JSON.readTree(translations.getRelativeContent());
        assertRelativeImage(copies.path("zh_CN").asText());
        assertRelativeImage(JSON.readTree(copies.path("en_US").asText()).path("description").asText());
        assertFalse(copies.has("fr_FR"));
    }

    @Test
    void translationPartialUpdatePreservesOtherLanguagesAndReplacesEditedCopy() throws Exception {
        TranslationsMapper mapper = mock(TranslationsMapper.class);
        TranslationsServiceImpl service = translationService(mapper);
        Translations stored = new Translations();
        stored.setId(41L);
        stored.setEnUs(HTML);
        stored.setJaJp("<p>Japanese content</p>");
        stored.setRelativeContent("invalid stored derived JSON");
        when(mapper.selectTranslationsByIdForUpdate(41L)).thenReturn(stored);
        Translations update = new Translations();
        update.setId(41L);
        update.setEnUs(HTML.replace("picture.jpg", "edited.jpg"));
        update.setRelativeContent("{\"ja_JP\":\"forged\"}");

        service.updateTranslations(update);

        verify(mapper).updateTranslations(update);
        JsonNode copies = JSON.readTree(update.getRelativeContent());
        assertEquals("<p>Japanese content</p>", copies.path("ja_JP").asText());
        assertTrue(copies.path("en_US").asText().contains("/profile/upload/edited.jpg"));
        assertFalse(copies.path("en_US").asText().contains("old.example.com"));
        assertNull(update.getJaJp());
        assertEquals(HTML.replace("picture.jpg", "edited.jpg"), update.getEnUs());
    }

    @Test
    void translationExplicitEmptyClearsOnlyThatLanguage() throws Exception {
        TranslationsMapper mapper = mock(TranslationsMapper.class);
        TranslationsServiceImpl service = translationService(mapper);
        Translations stored = new Translations();
        stored.setId(41L);
        stored.setEnUs(HTML);
        stored.setJaJp("Unchanged Japanese content");
        stored.setRelativeContent(JSON.writeValueAsString(java.util.Map.of(
                "en_US", HTML.replace(ABSOLUTE_IMAGE, RELATIVE_IMAGE),
                "ja_JP", "Unchanged Japanese content")));
        when(mapper.selectTranslationsByIdForUpdate(41L)).thenReturn(stored);
        Translations update = new Translations();
        update.setId(41L);
        update.setEnUs("");
        update.setRelativeContent("forged invalid JSON");

        service.updateTranslations(update);

        verify(mapper).updateTranslations(update);
        JsonNode copies = JSON.readTree(update.getRelativeContent());
        assertEquals("", copies.path("en_US").asText());
        assertEquals("Unchanged Japanese content", copies.path("ja_JP").asText());
        assertEquals("", update.getEnUs());
        assertNull(update.getJaJp());
    }

    @Test
    void translatedRelativeHtmlInsideJsonGetsBothPersistedAddressForms() throws Exception {
        String previous = com.order.common.config.OrderConfig.getResourceBaseUrl();
        com.order.common.config.OrderConfig configuration = new com.order.common.config.OrderConfig();
        configuration.setResourceBaseUrl("https://assets.example.com");
        try {
            TranslationsMapper mapper = mock(TranslationsMapper.class);
            TranslationsServiceImpl service = translationService(mapper);
            Translations translations = new Translations();
            translations.setEnUs(JSON.writeValueAsString(java.util.Map.of(
                    "description", HTML.replace(ABSOLUTE_IMAGE, RELATIVE_IMAGE))));

            service.insertTranslations(translations);

            verify(mapper).insertTranslations(translations);
            String legacy = JSON.readTree(translations.getEnUs()).path("description").asText();
            assertTrue(legacy.contains("https://assets.example.com" + RELATIVE_IMAGE));
            JsonNode copies = JSON.readTree(translations.getRelativeContent());
            assertRelativeImage(JSON.readTree(copies.path("en_US").asText()).path("description").asText());
        } finally {
            configuration.setResourceBaseUrl(previous);
        }
    }

    private static TranslationsServiceImpl translationService(TranslationsMapper mapper) throws Exception {
        TranslationsServiceImpl service = new TranslationsServiceImpl();
        inject(service, "translationsMapper", mapper);
        return service;
    }

    private static void inject(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void assertRelativeImage(String html) {
        assertTrue(html.contains(RELATIVE_IMAGE), html);
        assertFalse(html.contains(ABSOLUTE_IMAGE), html);
        assertFalse(html.contains("Unrelated client-supplied"), html);
        assertTrue(html.contains("Saved description"), html);
    }
}
