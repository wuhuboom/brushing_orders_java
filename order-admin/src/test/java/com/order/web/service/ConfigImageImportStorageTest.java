package com.order.web.service;

import com.order.common.config.OrderConfig;
import com.order.framework.config.ServerConfig;
import com.order.system.service.ISystemAlignmentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

class ConfigImageImportStorageTest {
    @TempDir
    Path directory;
    private String previousProfile;
    private ISystemAlignmentService alignment;
    private ConfigImageImportStorage storage;

    @BeforeEach
    void setUp() {
        previousProfile = OrderConfig.getProfile();
        new OrderConfig().setProfile(directory.toString());
        ServerConfig server = mock(ServerConfig.class);
        when(server.getUrl()).thenReturn("https://example.test");
        alignment = mock(ISystemAlignmentService.class);
        storage = new ConfigImageImportStorage(server, alignment);
    }

    @AfterEach
    void restoreProfile() {
        new OrderConfig().setProfile(previousProfile);
    }

    @Test
    void storesOnlyBasenameAsReferenceAndKeepsFullPathOutOfDatabaseName() throws Exception {
        when(alignment.registerFile(anyMap())).thenReturn(Map.of("fileUrl", "https://cdn.test/existing.png"));
        var saved = storage.store("long-folder/".repeat(30) + "v01.png", "png", new byte[]{1, 2, 3}, "vip", 42L);
        ArgumentCaptor<Map<String, Object>> file = ArgumentCaptor.forClass(Map.class);
        verify(alignment).registerFile(file.capture());
        assertEquals("v01.png", file.getValue().get("referenceName"));
        assertEquals("MEMBER_LEVEL", file.getValue().get("referenceType"));
        assertEquals("42", file.getValue().get("referenceTargetId"));
        assertEquals("https://cdn.test/existing.png", saved.url());
        assertTrue(saved.writtenPath().startsWith(directory));
        assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(saved.writtenPath()));
    }

    @Test
    void removesNewPhysicalFileWhenFileIndexRegistrationFails() throws Exception {
        when(alignment.registerFile(anyMap())).thenThrow(new IllegalStateException("Index failure"));
        assertThrows(IllegalStateException.class,
                () -> storage.store("s01.png", "png", new byte[]{1, 2, 3}, "event", 20L));
        try (var paths = Files.walk(directory)) {
            assertFalse(paths.anyMatch(Files::isRegularFile));
        }
    }

    @Test
    void cleanupRemovesOnlyTheGeneratedFileAndLeavesExistingFiles() throws Exception {
        Path existing = Files.write(directory.resolve("existing.png"), new byte[]{7});
        Path generated = Files.write(directory.resolve("new.png"), new byte[]{8});
        storage.delete(generated);
        storage.delete(generated);
        assertFalse(Files.exists(generated));
        assertArrayEquals(new byte[]{7}, Files.readAllBytes(existing));
    }
}
