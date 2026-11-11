package com.order.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.framework.web.service.PermissionService;
import com.order.member.domain.GoodsMemberLevel;
import com.order.member.domain.OrderConfig;
import com.order.member.mapper.ConfigImageImportMapper;
import com.order.member.service.IGoodsMemberLevelService;
import com.order.member.service.IOrderConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Base64;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ConfigImageImportServiceTest {
    private IGoodsMemberLevelService levels;
    private IOrderConfigService configs;
    private PermissionService permissions;
    private ConfigImageImportStorage storage;
    private ConfigImageImportMapper importMapper;
    private ConfigImageImportService service;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        levels = mock(IGoodsMemberLevelService.class);
        configs = mock(IOrderConfigService.class);
        permissions = mock(PermissionService.class);
        storage = mock(ConfigImageImportStorage.class);
        importMapper = mock(ConfigImageImportMapper.class);
        service = new ConfigImageImportService(levels, configs, permissions, mapper, storage, importMapper);
        when(importMapper.selectByTypeForUpdate(anyString()))
                .thenAnswer(call -> configs.selectOrderConfigByType(call.getArgument(0)));
        when(permissions.hasPermi(anyString())).thenReturn(true);
        GoodsMemberLevel level1 = level(901L, 1L);
        GoodsMemberLevel level2 = level(101L, 2L);
        when(levels.selectGoodsMemberLevelList(any())).thenReturn(List.of(level2, level1));
        OrderConfig event = config(20L, "event", "{\"eventContent\":\"<p>Existing event</p>\",\"other\":7}");
        OrderConfig certificate = config(30L, "certificate", "{\"certificateContent\":\"<p>Existing certificate</p>\",\"certificateSwitch\":\"1\",\"certificateTip\":\"notice\"}");
        when(configs.selectOrderConfigByTypes(anyList())).thenReturn(List.of(event, certificate));
        when(configs.selectOrderConfigByType("event")).thenReturn(event);
        when(configs.selectOrderConfigByType("certificate")).thenReturn(certificate);
        when(levels.updateGoodsMemberLevel(any())).thenReturn(1);
        when(configs.updateOrderConfig(any())).thenReturn(1);
        when(storage.store(anyString(), anyString(), any(), anyString(), anyLong())).thenAnswer(call -> {
            String name = call.getArgument(0);
            return new ConfigImageImportStorage.StoredImage("https://example.test/" + name, Path.of("/tmp/import-" + name));
        });
    }

    @Test
    void previewMatchesVipByActualLevelAndSortsImagesNumericallyWithoutWriting() throws Exception {
        Map<String, byte[]> files = new LinkedHashMap<>();
        files.put("folder/V02.PNG", image("png"));
        files.put("S10.gif", image("gif"));
        files.put("s02.bmp", image("bmp"));
        files.put("v01.JPEG", image("jpeg"));
        files.put("z01.jpg", image("jpeg"));
        ConfigImageImportService.ImportResult result = service.preview(zip(files), "append");
        assertTrue(result.valid(), result.errors().toString());
        assertEquals(Map.of("vip", 2, "event", 2, "certificate", 1), result.summary());
        assertEquals(101L, result.entries().stream().filter(e -> e.fileName().contains("V02")).findFirst().orElseThrow().targetId());
        assertEquals(List.of(2L, 10L), result.entries().stream().filter(e -> e.group().equals("event")).map(ConfigImageImportService.ImportEntry::sequence).toList());
        assertTrue(result.entries().stream().filter(e -> e.group().equals("event")).allMatch(e -> e.targetId().equals(20L)));
        verifyNoInteractions(storage);
        verify(levels, never()).updateGoodsMemberLevel(any());
        verify(configs, never()).updateOrderConfig(any());
    }

    @Test
    void importsAllGroupsAndOnlyPatchesVipIconAndConfigContent() throws Exception {
        var result = service.importImages(zip(Map.of("v01.png", image("png"), "s02.png", image("png"),
                "s01.png", image("png"), "z01.gif", image("gif"))), "append");
        assertTrue(result.valid(), result.errors().toString());
        ArgumentCaptor<GoodsMemberLevel> vipPatch = ArgumentCaptor.forClass(GoodsMemberLevel.class);
        verify(levels).updateGoodsMemberLevel(vipPatch.capture());
        assertEquals(901L, vipPatch.getValue().getId());
        assertEquals("https://example.test/v01.png", vipPatch.getValue().getIcon());
        assertNull(vipPatch.getValue().getName());
        assertNull(vipPatch.getValue().getLevel());
        assertNull(vipPatch.getValue().getTranslations());
        ArgumentCaptor<OrderConfig> patches = ArgumentCaptor.forClass(OrderConfig.class);
        verify(configs, times(2)).updateOrderConfig(patches.capture());
        for (OrderConfig patch : patches.getAllValues()) {
            JsonNode content = mapper.readTree(patch.getContent());
            assertNull(patch.getTranslations());
            assertNull(patch.getTranslationsId());
            assertNull(patch.getType());
            if (patch.getId().equals(20L)) {
                String html = content.path("eventContent").asText();
                assertTrue(html.startsWith("<p>Existing event</p>"));
                assertTrue(html.indexOf("s01.png") < html.indexOf("s02.png"));
                assertEquals(7, content.path("other").asInt());
            } else {
                assertTrue(content.path("certificateContent").asText().startsWith("<p>Existing certificate</p>"));
                assertEquals("1", content.path("certificateSwitch").asText());
                assertEquals("notice", content.path("certificateTip").asText());
            }
        }
    }

    @Test
    void replacePreservesCertificateAliasesSwitchNoticeAndNestedUnknownFields() throws Exception {
        OrderConfig certificate = config(30L, "certificate", "{\"certificateSwitch\":1,\"certificateTip\":\"keep\",\"certificate\":\"old\",\"content\":\"old\",\"custom\":true,\"value\":{\"certificate\":\"nested\",\"content\":\"nested\",\"certificateTip\":\"nested notice\",\"other\":8}}");
        when(configs.selectOrderConfigByTypes(anyList())).thenReturn(List.of(certificate));
        when(configs.selectOrderConfigByType("certificate")).thenReturn(certificate);
        assertTrue(service.importImages(zip(Map.of("z01.png", image("png"))), "replace").valid());
        ArgumentCaptor<OrderConfig> patch = ArgumentCaptor.forClass(OrderConfig.class);
        verify(configs).updateOrderConfig(patch.capture());
        JsonNode content = mapper.readTree(patch.getValue().getContent());
        String html = content.path("certificateContent").asText();
        assertTrue(html.startsWith("<p><img"));
        assertEquals(html, content.path("certificate").asText());
        assertEquals(html, content.path("content").asText());
        assertEquals(html, content.path("value").path("certificateContent").asText());
        assertEquals(html, content.path("value").path("certificate").asText());
        assertEquals(html, content.path("value").path("content").asText());
        assertEquals(1, content.path("certificateSwitch").asInt());
        assertEquals("keep", content.path("certificateTip").asText());
        assertEquals("nested notice", content.path("value").path("certificateTip").asText());
        assertEquals(8, content.path("value").path("other").asInt());
        assertTrue(content.path("custom").asBoolean());
        verifyNoInteractions(levels);
    }

    @Test
    void appendSupportsLegacyRawHtmlAndEventAliases() throws Exception {
        OrderConfig event = config(20L, "event", "{\"event\":\"<p>Legacy event</p>\",\"value\":{\"content\":\"old\",\"other\":5}}");
        when(configs.selectOrderConfigByTypes(anyList())).thenReturn(List.of(event));
        when(configs.selectOrderConfigByType("event")).thenReturn(event);
        assertTrue(service.importImages(zip(Map.of("s01.png", image("png"))), "append").valid());
        ArgumentCaptor<OrderConfig> patch = ArgumentCaptor.forClass(OrderConfig.class);
        verify(configs).updateOrderConfig(patch.capture());
        JsonNode content = mapper.readTree(patch.getValue().getContent());
        String html = content.path("eventContent").asText();
        assertTrue(html.startsWith("<p>Legacy event</p>"));
        assertEquals(html, content.path("event").asText());
        assertEquals(html, content.path("value").path("content").asText());
        assertEquals(5, content.path("value").path("other").asInt());

        reset(configs);
        OrderConfig certificate = config(30L, "certificate", "<p>Raw certificate</p>");
        when(configs.selectOrderConfigByTypes(anyList())).thenReturn(List.of(certificate));
        when(configs.selectOrderConfigByType("certificate")).thenReturn(certificate);
        when(configs.updateOrderConfig(any())).thenReturn(1);
        assertTrue(service.importImages(zip(Map.of("z01.png", image("png"))), "append").valid());
        verify(configs).updateOrderConfig(patch.capture());
        assertTrue(mapper.readTree(patch.getValue().getContent()).path("certificateContent").asText().startsWith("<p>Raw certificate</p>"));
    }

    @Test
    void rejectsDuplicatePositionsRegardlessOfCaseExtensionOrZeroPadding() throws Exception {
        var result = service.importImages(zip(Map.of("v01.png", image("png"), "V001.jpeg", image("jpeg"))), "append");
        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("重复")));
        verifyNoInteractions(storage);
        verify(levels, never()).updateGoodsMemberLevel(any());
    }

    @Test
    void rejectsMissingAndAmbiguousVipLevels() throws Exception {
        when(levels.selectGoodsMemberLevelList(any())).thenReturn(List.of(level(1L, 1L), level(2L, 1L)));
        var result = service.preview(zip(Map.of("v01.png", image("png"), "v02.png", image("png"))), "append");
        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("多条")));
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("不存在")));
    }

    @Test
    void eachGroupRequiresItsOwnPermissionAndAnInvalidMixedPackageWritesNothing() throws Exception {
        when(permissions.hasPermi("member:level:edit")).thenReturn(false);
        var result = service.importImages(zip(Map.of("v01.png", image("png"), "s01.png", image("png"))), "append");
        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("VIP") && e.contains("权限")));
        verifyNoInteractions(storage);
        verify(configs, never()).updateOrderConfig(any());
        verify(levels, never()).selectGoodsMemberLevelList(any());
        assertTrue(service.preview(zip(Map.of("s01.png", image("png"))), "append").valid());
    }

    @Test
    void vipOnlyImportDoesNotRequireOrderConfigPermission() throws Exception {
        when(permissions.hasPermi("member:orderconfig:edit")).thenReturn(false);
        assertTrue(service.importImages(zip(Map.of("v01.png", image("png"))), "append").valid());
        verifyNoInteractions(configs);
    }

    @Test
    void rejectsTraversalUnknownNamesZeroSequenceAndDisguisedImages() throws Exception {
        assertInvalid("../v01.png", image("png"), "路径");
        assertInvalid("C:\\v01.png", image("png"), "路径");
        assertInvalid("s1.png", image("png"), "规则");
        assertInvalid("v00.png", image("png"), "正整数");
        assertInvalid("s01.jpg", image("png"), "图片内容无效");
        assertInvalid("s01.png", "not an image".getBytes(), "图片内容无效");
        assertInvalid("s01.webp", "RIFF garbage WEBP".getBytes(), "图片内容无效");
        assertInvalid("instructions.html", "<script>alert(1)</script>".getBytes(), "规则");
        assertInvalid("s" + "0".repeat(300) + "1.png", image("png"), "255");
        verifyNoInteractions(storage);
    }

    @Test
    void decodesRealWebpAndRejectsHeaderOnlyWebp() throws Exception {
        byte[] real = Base64.getDecoder().decode("UklGRjwAAABXRUJQVlA4IDAAAADQAQCdASoCAAIAAgA0JaACdLoB+AADsAD+8MQL/yC5YXXI1/8gP+QH/ID/+PIAAAA=");
        assertTrue(service.preview(zip(Map.of("s01.webp", real)), "append").valid());
        byte[] fake = new byte[26];
        System.arraycopy("RIFF".getBytes(), 0, fake, 0, 4);
        fake[4] = 18;
        System.arraycopy("WEBPVP8L".getBytes(), 0, fake, 8, 8);
        fake[16] = 5;
        fake[20] = 0x2f;
        assertInvalid("s01.webp", fake, "图片内容无效");
    }

    @Test
    void rejectsTruncatedZipMissingCentralDirectoryAndChecksumCorruption() throws Exception {
        byte[] complete = zip(Map.of("s01.png", image("png"))).getBytes();
        int central = -1;
        for (int i = 0; i < complete.length - 4; i++) {
            if (complete[i] == 'P' && complete[i + 1] == 'K' && complete[i + 2] == 1 && complete[i + 3] == 2) {
                central = i;
                break;
            }
        }
        assertTrue(central > 0);
        MockMultipartFile truncated = new MockMultipartFile("file", "images.zip", "application/zip", Arrays.copyOf(complete, central));
        assertFalse(service.importImages(truncated, "append").valid());
        complete[central + 16] ^= 1;
        MockMultipartFile corrupted = new MockMultipartFile("file", "images.zip", "application/zip", complete);
        var result = service.importImages(corrupted, "append");
        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("损坏")));
        verifyNoInteractions(storage);
    }

    @Test
    void rejectsGifWithHugeLogicalCanvasEvenWhenItsFirstFrameIsTiny() throws Exception {
        byte[] gif = Base64.getDecoder().decode("R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7");
        assertTrue(service.preview(zip(Map.of("s01.gif", gif)), "append").valid());
        Arrays.fill(gif, 6, 10, (byte) 0xff);
        assertInvalid("s01.gif", gif, "图片内容无效");
    }

    @Test
    void ignoresMacAndWindowsMetadataButAcceptsSafeSubfolders() throws Exception {
        var result = service.preview(zip(Map.of("images/v01.png", image("png"), "__MACOSX/._v01.png", new byte[0],
                ".DS_Store", new byte[0], "Thumbs.db", new byte[0])), "append");
        assertTrue(result.valid(), result.errors().toString());
        assertEquals(1, result.entries().size());
    }

    @Test
    void enforcesPerImageAndEntryLimitsAndRejectsInvalidMode() throws Exception {
        assertInvalid("s01.png", new byte[(int) ConfigImageImportService.MAX_IMAGE_BYTES + 1], "10MB");
        Map<String, byte[]> tooManyEntries = new LinkedHashMap<>();
        for (int i = 0; i <= ConfigImageImportService.MAX_ENTRIES; i++) tooManyEntries.put(".metadata/" + i, new byte[0]);
        assertTrue(service.preview(zip(tooManyEntries), "append").errors().stream().anyMatch(e -> e.contains("300")));
        assertFalse(service.preview(zip(Map.of("s01.png", image("png"))), "unknown").valid());
        MockMultipartFile empty = new MockMultipartFile("file", "empty.zip", "application/zip", new byte[0]);
        assertFalse(service.preview(empty, "append").valid());
    }

    @Test
    void cleansNewFilesIfDatabaseUpdateFails() throws Exception {
        when(configs.updateOrderConfig(any())).thenThrow(new IllegalStateException("DB failed"));
        assertThrows(IllegalStateException.class,
                () -> service.importImages(zip(Map.of("s01.png", image("png"))), "append"));
        verify(storage).delete(Path.of("/tmp/import-s01.png"));
    }

    @Test
    void appendsToCurrentLockedContentAndLocksGroupsInFixedOrderBeforeSavingImages() throws Exception {
        when(importMapper.selectByTypeForUpdate("event"))
                .thenReturn(config(20L, "event", "{\"eventContent\":\"concurrently appended event\"}"));
        when(importMapper.selectByTypeForUpdate("certificate"))
                .thenReturn(config(30L, "certificate", "{\"certificateContent\":\"concurrently appended certificate\"}"));
        assertTrue(service.importImages(zip(Map.of("s01.png", image("png"), "z01.png", image("png"))), "append").valid());
        var order = inOrder(importMapper, storage);
        order.verify(importMapper).selectByTypeForUpdate("event");
        order.verify(importMapper).selectByTypeForUpdate("certificate");
        order.verify(storage, times(2)).store(anyString(), anyString(), any(), anyString(), anyLong());
        ArgumentCaptor<OrderConfig> patches = ArgumentCaptor.forClass(OrderConfig.class);
        verify(configs, times(2)).updateOrderConfig(patches.capture());
        assertTrue(mapper.readTree(patches.getAllValues().get(0).getContent()).path("eventContent").asText()
                .startsWith("concurrently appended event"));
        assertTrue(mapper.readTree(patches.getAllValues().get(1).getContent()).path("certificateContent").asText()
                .startsWith("concurrently appended certificate"));
    }

    @Test
    void cleansNewFilesWhenTheOuterTransactionRollsBackAfterMethodReturns() throws Exception {
        TransactionSynchronizationManager.initSynchronization();
        try {
            assertTrue(service.importImages(zip(Map.of("s01.png", image("png"))), "append").valid());
            verify(storage, never()).delete(any());
            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            }
            verify(storage).delete(Path.of("/tmp/import-s01.png"));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void escapesImageUrlBeforeGeneratingHtml() throws Exception {
        when(storage.store(anyString(), anyString(), any(), anyString(), anyLong()))
                .thenReturn(new ConfigImageImportStorage.StoredImage("https://example.test/a?x=1&y=\"test\"", Path.of("/tmp/safe")));
        assertTrue(service.importImages(zip(Map.of("s01.png", image("png"))), "append").valid());
        ArgumentCaptor<OrderConfig> patch = ArgumentCaptor.forClass(OrderConfig.class);
        verify(configs).updateOrderConfig(patch.capture());
        String html = mapper.readTree(patch.getValue().getContent()).path("eventContent").asText();
        assertTrue(html.contains("x=1&amp;y=&quot;test&quot;"));
    }

    private void assertInvalid(String name, byte[] data, String message) throws Exception {
        var result = service.preview(zip(Map.of(name, data)), "append");
        assertFalse(result.valid(), name);
        assertTrue(result.errors().stream().anyMatch(error -> error.contains(message)), result.errors().toString());
    }

    private GoodsMemberLevel level(Long id, Long number) {
        GoodsMemberLevel level = new GoodsMemberLevel();
        level.setId(id);
        level.setLevel(number);
        level.setName("VIP" + number);
        return level;
    }

    private OrderConfig config(Long id, String type, String content) {
        OrderConfig config = new OrderConfig();
        config.setId(id);
        config.setType(type);
        config.setContent(content);
        config.setTranslationsId(77L);
        return config;
    }

    private byte[] image(String format) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), format, output));
        return output.toByteArray();
    }

    private MockMultipartFile zip(Map<String, byte[]> files) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            for (Map.Entry<String, byte[]> file : files.entrySet()) {
                zip.putNextEntry(new ZipEntry(file.getKey()));
                zip.write(file.getValue());
                zip.closeEntry();
            }
        }
        return new MockMultipartFile("file", "images.zip", "application/zip", output.toByteArray());
    }
}
