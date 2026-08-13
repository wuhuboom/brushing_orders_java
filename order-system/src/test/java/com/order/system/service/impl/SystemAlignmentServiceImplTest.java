package com.order.system.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.order.common.core.redis.RedisCache;
import com.order.common.exception.ServiceException;
import com.order.system.mapper.SystemAlignmentMapper;

class SystemAlignmentServiceImplTest
{
    @TempDir
    Path tempDir;

    private SystemAlignmentMapper mapper;
    private SystemAlignmentServiceImpl service;

    @BeforeEach
    void setUp() throws Exception
    {
        mapper = mock(SystemAlignmentMapper.class);
        service = new SystemAlignmentServiceImpl();
        setField("mapper", mapper);
        setField("redisCache", mock(RedisCache.class));
    }

    @Test
    void strategyCreationPersistsDirectResources()
    {
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("strategyCode", "customer-read");
        data.put("strategyName", "客户只读");
        data.put("menuIds", List.of(100L, 101L));
        when(mapper.insertStrategy(data)).thenAnswer(invocation -> { data.put("strategyId", 8L); return 1; });

        assertEquals(1, service.insertStrategy(data));
        verify(mapper).insertStrategyMenus(8L, List.of(100L, 101L));
    }

    @Test
    void duplicateResourceDataRulesAreRejected()
    {
        Map<String, Object> data = Map.of(
                "strategyIds", List.of(),
                "dataRules", List.of(
                        Map.of("menuId", 100L, "scopeType", "SELF"),
                        Map.of("menuId", 100L, "scopeType", "DEPT")));

        assertThrows(ServiceException.class, () -> service.replaceRoleAlignment(2L, data));
        verify(mapper, never()).insertRoleDataRules(anyLong(), any());
    }

    @Test
    void referencedFileCannotBeDeleted()
    {
        when(mapper.countFileReferences(any())).thenReturn(1);
        assertThrows(ServiceException.class, () -> service.deleteFiles(new Long[] { 5L }));
        verify(mapper, never()).deleteFiles(any());
    }

    @Test
    void staleDuplicateKeepsNewUploadAndRepairsExistingFileRecord() throws Exception
    {
        Path uploaded = Files.writeString(tempDir.resolve("new-upload.png"), "same-image");
        Map<String, Object> existing = new HashMap<>(Map.of(
                "fileId", 8L,
                "storagePath", tempDir.resolve("missing-old.png").toString(),
                "fileUrl", "https://old.example/profile/missing-old.png"));
        Map<String, Object> repaired = new HashMap<>(Map.of(
                "fileId", 8L,
                "storagePath", uploaded.toString(),
                "fileUrl", "https://new.example/profile/new-upload.png"));
        when(mapper.selectFileByHash(anyString(), anyString())).thenReturn(existing);
        when(mapper.updateFileStorageIfMatch(any())).thenReturn(1);
        when(mapper.selectFileById(8L)).thenReturn(repaired);

        Map<String, Object> result = service.registerFile(uploadData(uploaded,
                "https://new.example/profile/new-upload.png"));

        assertTrue(Files.isRegularFile(uploaded));
        assertEquals(8L, result.get("fileId"));
        assertEquals("https://new.example/profile/new-upload.png", result.get("fileUrl"));
        verify(mapper).updateFileStorageIfMatch(any());
        verify(mapper).insertFileReference(any());
    }

    @Test
    void healthyDuplicateDeletesRedundantNewUpload() throws Exception
    {
        Path indexed = Files.writeString(tempDir.resolve("indexed.png"), "same-image");
        Path uploaded = Files.writeString(tempDir.resolve("redundant.png"), "same-image");
        Map<String, Object> existing = new HashMap<>(Map.of(
                "fileId", 8L,
                "storagePath", indexed.toString(),
                "fileUrl", "https://example/profile/indexed.png"));
        when(mapper.selectFileByHash(anyString(), anyString())).thenReturn(existing);

        Map<String, Object> result = service.registerFile(uploadData(uploaded,
                "https://example/profile/redundant.png"));

        assertFalse(Files.exists(uploaded));
        assertTrue(Files.isRegularFile(indexed));
        assertEquals("https://example/profile/indexed.png", result.get("fileUrl"));
        verify(mapper, never()).updateFileStorageIfMatch(any());
    }

    @Test
    void concurrentStaleRepairUsesWinnerAndDeletesRedundantUpload() throws Exception
    {
        Path winner = Files.writeString(tempDir.resolve("winner.png"), "same-image");
        Path uploaded = Files.writeString(tempDir.resolve("loser.png"), "same-image");
        Map<String, Object> stale = new HashMap<>(Map.of(
                "fileId", 8L,
                "storagePath", tempDir.resolve("missing-old.png").toString(),
                "fileUrl", "https://old.example/profile/missing-old.png"));
        Map<String, Object> repairedByAnotherRequest = new HashMap<>(Map.of(
                "fileId", 8L,
                "storagePath", winner.toString(),
                "fileUrl", "https://example/profile/winner.png"));
        when(mapper.selectFileByHash(anyString(), anyString()))
                .thenReturn(stale, repairedByAnotherRequest);
        when(mapper.updateFileStorageIfMatch(any())).thenReturn(0);

        Map<String, Object> result = service.registerFile(uploadData(uploaded,
                "https://example/profile/loser.png"));

        assertFalse(Files.exists(uploaded));
        assertTrue(Files.isRegularFile(winner));
        assertEquals("https://example/profile/winner.png", result.get("fileUrl"));
    }

    @Test
    void phoneMaskingComesFromAnyEffectiveRole()
    {
        when(mapper.countPhoneMaskingRoles(9L)).thenReturn(1);
        assertTrue(service.shouldHidePhone(9L));
    }

    private Map<String, Object> uploadData(Path storagePath, String fileUrl)
    {
        Map<String, Object> data = new HashMap<>();
        data.put("bucket", "local");
        data.put("fileType", "png");
        data.put("contentType", "image/png");
        data.put("storagePath", storagePath.toString());
        data.put("fileUrl", fileUrl);
        data.put("referenceName", "test.png");
        data.put("referenceType", "ADMIN_UPLOAD");
        return data;
    }

    private void setField(String name, Object value) throws Exception
    {
        Field field = SystemAlignmentServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(service, value);
    }
}
