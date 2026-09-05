package com.order.system.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.common.exception.ServiceException;
import com.order.system.domain.SysUserTableColumnConfig;
import com.order.system.domain.dto.TableColumnConfigDto;
import com.order.system.mapper.SysUserTableColumnConfigMapper;

class SysUserTableColumnConfigServiceImplTest
{
    private SysUserTableColumnConfigMapper mapper;
    private SysUserTableColumnConfigServiceImpl service;

    @BeforeEach
    void setUp()
    {
        mapper = org.mockito.Mockito.mock(SysUserTableColumnConfigMapper.class);
        service = new SysUserTableColumnConfigServiceImpl(mapper, new ObjectMapper());
    }

    @Test
    void missingConfigReturnsNullForCurrentUserAndTable()
    {
        when(mapper.selectByUserIdAndTableKey(7L, "member.orderuser.main")).thenReturn(null);

        assertNull(service.selectTableColumnConfig(7L, "member.orderuser.main"));
        verify(mapper).selectByUserIdAndTableKey(7L, "member.orderuser.main");
    }

    @Test
    void storedConfigIsDeserializedAsResponseObject()
    {
        SysUserTableColumnConfig stored = new SysUserTableColumnConfig();
        stored.setConfigContent("{\"version\":1,\"items\":[{\"id\":\"userName\",\"visible\":false,\"fixed\":\"left\"}]}");
        when(mapper.selectByUserIdAndTableKey(7L, "member.orderuser.main")).thenReturn(stored);

        TableColumnConfigDto result = service.selectTableColumnConfig(7L, "member.orderuser.main");

        assertEquals(1, result.getVersion());
        assertEquals(1, result.getItems().size());
        assertEquals("userName", result.getItems().get(0).getId());
        assertEquals(false, result.getItems().get(0).getVisible());
        assertEquals("left", result.getItems().get(0).getFixed());
    }

    @Test
    void saveUsesTrustedUserAndAtomicUpsertModel()
    {
        TableColumnConfigDto config = config(item("id", true, null), item("userName", false, "right"));

        service.saveTableColumnConfig(9L, "operator", "member.orderuser.main", config);

        ArgumentCaptor<SysUserTableColumnConfig> captor = ArgumentCaptor.forClass(SysUserTableColumnConfig.class);
        verify(mapper).upsert(captor.capture());
        SysUserTableColumnConfig saved = captor.getValue();
        assertEquals(9L, saved.getUserId());
        assertEquals("member.orderuser.main", saved.getTableKey());
        assertEquals("operator", saved.getCreateBy());
        assertEquals("operator", saved.getUpdateBy());
        assertEquals(
                "{\"version\":1,\"items\":[{\"id\":\"id\",\"visible\":true,\"fixed\":null},"
                        + "{\"id\":\"userName\",\"visible\":false,\"fixed\":\"right\"}]}",
                saved.getConfigContent());
    }

    @Test
    void invalidTableKeyIsRejectedBeforeDatabaseAccess()
    {
        assertThrows(ServiceException.class,
                () -> service.saveTableColumnConfig(9L, "operator", "member/orderuser", config()));
        assertThrows(ServiceException.class,
                () -> service.selectTableColumnConfig(9L, "a".repeat(129)));
        verify(mapper, never()).upsert(any());
        verify(mapper, never()).selectByUserIdAndTableKey(any(), any());
    }

    @Test
    void duplicateColumnIdsAreRejected()
    {
        TableColumnConfigDto config = config(item("id", true, null), item("id", false, null));

        assertThrows(ServiceException.class,
                () -> service.saveTableColumnConfig(9L, "operator", "member.orderuser.main", config));
        verify(mapper, never()).upsert(any());
    }

    @Test
    void unsupportedFixedPositionIsRejected()
    {
        TableColumnConfigDto config = config(item("id", true, "center"));

        assertThrows(ServiceException.class,
                () -> service.saveTableColumnConfig(9L, "operator", "member.orderuser.main", config));
        verify(mapper, never()).upsert(any());
    }

    @Test
    void unsupportedVersionAndMissingVisibleStateAreRejected()
    {
        TableColumnConfigDto unsupported = config(item("id", true, null));
        unsupported.setVersion(2);
        TableColumnConfigDto.Item missingVisibleItem = item("id", true, null);
        missingVisibleItem.setVisible(null);
        TableColumnConfigDto missingVisible = config(missingVisibleItem);

        assertThrows(ServiceException.class,
                () -> service.saveTableColumnConfig(9L, "operator", "member.orderuser.main", unsupported));
        assertThrows(ServiceException.class,
                () -> service.saveTableColumnConfig(9L, "operator", "member.orderuser.main", missingVisible));
        verify(mapper, never()).upsert(any());
    }

    @Test
    void columnCountAndColumnIdLengthAreBounded()
    {
        TableColumnConfigDto.Item[] tooManyItems = new TableColumnConfigDto.Item[201];
        for (int index = 0; index < tooManyItems.length; index++)
        {
            tooManyItems[index] = item("column-" + index, true, null);
        }
        TableColumnConfigDto tooMany = config(tooManyItems);
        TableColumnConfigDto overlongId = config(item("x".repeat(129), true, null));

        assertThrows(ServiceException.class,
                () -> service.saveTableColumnConfig(9L, "operator", "member.orderuser.main", tooMany));
        assertThrows(ServiceException.class,
                () -> service.saveTableColumnConfig(9L, "operator", "member.orderuser.main", overlongId));
        verify(mapper, never()).upsert(any());
    }

    @Test
    void serializedUtf8ContentCannotExceedTextCapacity()
    {
        List<TableColumnConfigDto.Item> items = new ArrayList<>();
        for (int index = 0; index < 200; index++)
        {
            items.add(item(String.format("%03d", index) + "列".repeat(125), true, null));
        }
        TableColumnConfigDto config = config(items.toArray(TableColumnConfigDto.Item[]::new));

        assertThrows(ServiceException.class,
                () -> service.saveTableColumnConfig(9L, "operator", "member.orderuser.main", config));
        verify(mapper, never()).upsert(any());
    }

    @Test
    void malformedStoredJsonIsRejected()
    {
        SysUserTableColumnConfig stored = new SysUserTableColumnConfig();
        stored.setConfigContent("not-json");
        when(mapper.selectByUserIdAndTableKey(7L, "member.orderuser.main")).thenReturn(stored);

        assertThrows(ServiceException.class,
                () -> service.selectTableColumnConfig(7L, "member.orderuser.main"));
    }

    @Test
    void deleteIsIdempotentAndScopedByUserAndTable()
    {
        when(mapper.deleteByUserIdAndTableKey(7L, "member.orderuser.main")).thenReturn(0);

        service.deleteTableColumnConfig(7L, "member.orderuser.main");

        verify(mapper).deleteByUserIdAndTableKey(7L, "member.orderuser.main");
    }

    private static TableColumnConfigDto config(TableColumnConfigDto.Item... items)
    {
        TableColumnConfigDto config = new TableColumnConfigDto();
        config.setVersion(1);
        config.setItems(List.of(items));
        return config;
    }

    private static TableColumnConfigDto.Item item(String id, boolean visible, String fixed)
    {
        TableColumnConfigDto.Item item = new TableColumnConfigDto.Item();
        item.setId(id);
        item.setVisible(visible);
        item.setFixed(fixed);
        return item;
    }
}
