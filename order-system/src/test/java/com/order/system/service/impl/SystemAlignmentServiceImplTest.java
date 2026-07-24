package com.order.system.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.order.common.core.redis.RedisCache;
import com.order.common.exception.ServiceException;
import com.order.system.mapper.SystemAlignmentMapper;

class SystemAlignmentServiceImplTest
{
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
    void phoneMaskingComesFromAnyEffectiveRole()
    {
        when(mapper.countPhoneMaskingRoles(9L)).thenReturn(1);
        assertTrue(service.shouldHidePhone(9L));
    }

    private void setField(String name, Object value) throws Exception
    {
        Field field = SystemAlignmentServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(service, value);
    }
}
