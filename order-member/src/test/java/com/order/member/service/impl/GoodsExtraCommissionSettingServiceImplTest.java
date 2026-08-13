package com.order.member.service.impl;

import com.order.common.exception.ServiceException;
import com.order.member.domain.GoodsExtraCommissionSetting;
import com.order.member.mapper.GoodsExtraCommissionSettingMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoodsExtraCommissionSettingServiceImplTest {

    private final GoodsExtraCommissionSettingMapper mapper =
            mock(GoodsExtraCommissionSettingMapper.class);
    private final GoodsExtraCommissionSettingServiceImpl service =
            new GoodsExtraCommissionSettingServiceImpl(mapper);

    @Test
    void newSettingAlwaysStartsIncompleteAndUnlocked() {
        GoodsExtraCommissionSetting setting = setting(77L, "0", "0");
        when(mapper.insertGoodsExtraCommissionSetting(setting)).thenReturn(1);

        assertEquals(1, service.insertGoodsExtraCommissionSetting(setting));

        assertEquals("1", setting.getStatus());
        assertEquals("1", setting.getIsLocked());
    }

    @Test
    void availableSettingCanBeEditedAfterBeingLockedForTheDecision() {
        GoodsExtraCommissionSetting current = setting(77L, "1", "1");
        GoodsExtraCommissionSetting update = setting(77L, "0", "0");
        when(mapper.selectByIdForUpdate(77L)).thenReturn(current);
        when(mapper.updateGoodsExtraCommissionSetting(update)).thenReturn(1);

        assertEquals(1, service.updateGoodsExtraCommissionSetting(update));

        assertNull(update.getStatus());
        assertNull(update.getIsLocked());
        verify(mapper).selectByIdForUpdate(77L);
        verify(mapper).updateGoodsExtraCommissionSetting(update);
    }

    @Test
    void reservedSettingCannotBeEdited() {
        GoodsExtraCommissionSetting reserved = setting(77L, "1", "0");
        when(mapper.selectByIdForUpdate(77L)).thenReturn(reserved);

        assertThrows(
                ServiceException.class,
                () -> service.updateGoodsExtraCommissionSetting(setting(77L, "1", "1")));

        verify(mapper, never()).updateGoodsExtraCommissionSetting(
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void completedSettingCannotBeDeleted() {
        GoodsExtraCommissionSetting completed = setting(77L, "0", "0");
        when(mapper.selectByIdForUpdate(77L)).thenReturn(completed);

        assertThrows(
                ServiceException.class,
                () -> service.deleteGoodsExtraCommissionSettingById(77L));

        verify(mapper, never()).deleteGoodsExtraCommissionSettingById(77L);
    }

    @Test
    void bulkDeleteLocksDistinctIdsInStableOrder() {
        when(mapper.selectByIdForUpdate(1L)).thenReturn(setting(1L, "1", "1"));
        when(mapper.selectByIdForUpdate(3L)).thenReturn(setting(3L, "1", "1"));
        when(mapper.deleteGoodsExtraCommissionSettingByIds(
                org.mockito.ArgumentMatchers.any())).thenReturn(2);

        assertEquals(
                2,
                service.deleteGoodsExtraCommissionSettingByIds(
                        new Long[]{3L, 1L, 3L}));

        ArgumentCaptor<Long[]> ids = ArgumentCaptor.forClass(Long[].class);
        verify(mapper).deleteGoodsExtraCommissionSettingByIds(ids.capture());
        org.junit.jupiter.api.Assertions.assertArrayEquals(
                new Long[]{1L, 3L}, ids.getValue());
    }

    private static GoodsExtraCommissionSetting setting(
            Long id, String status, String isLocked) {
        GoodsExtraCommissionSetting setting = new GoodsExtraCommissionSetting();
        setting.setId(id);
        setting.setStatus(status);
        setting.setIsLocked(isLocked);
        return setting;
    }
}
