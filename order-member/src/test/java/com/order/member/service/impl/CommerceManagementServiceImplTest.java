package com.order.member.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.order.common.exception.ServiceException;
import com.order.member.mapper.CommerceManagementMapper;

@ExtendWith(MockitoExtension.class)
class CommerceManagementServiceImplTest
{
    @Mock
    private CommerceManagementMapper mapper;

    @InjectMocks
    private CommerceManagementServiceImpl service;

    @Test
    void adjustPointsUpdatesAccountAndCreatesImmutableFlow()
    {
        when(mapper.ensurePointsAccount(7L)).thenReturn(1);
        when(mapper.selectPointsAccountForUpdate(7L)).thenReturn(mapOf(
                "points", new BigDecimal("10"), "availablePoints", new BigDecimal("8")));
        when(mapper.selectUserSnapshot(7L)).thenReturn(mapOf("username", "alice", "phoneNumber", "13000000000"));
        when(mapper.updatePointsAccount(eq(7L), any(), any())).thenReturn(1);
        when(mapper.insertPointsFlow(any())).thenReturn(1);

        Map<String, Object> request = mapOf("amount", "2.5", "operationType", "add", "remark", "manual");
        assertEquals(1, service.adjustPoints(7L, request));

        verify(mapper).updatePointsAccount(7L, new BigDecimal("12.5"), new BigDecimal("10.5"));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(mapper).insertPointsFlow(captor.capture());
        assertEquals(new BigDecimal("8"), captor.getValue().get("beforePoints"));
        assertEquals(new BigDecimal("2.5"), captor.getValue().get("changePoints"));
        assertEquals(new BigDecimal("10.5"), captor.getValue().get("afterPoints"));
    }

    @Test
    void adjustPointsRejectsNegativeBalance()
    {
        when(mapper.selectUserSnapshot(7L)).thenReturn(mapOf("username", "alice"));
        when(mapper.ensurePointsAccount(7L)).thenReturn(0);
        when(mapper.selectPointsAccountForUpdate(7L)).thenReturn(mapOf(
                "points", BigDecimal.ONE, "availablePoints", BigDecimal.ONE));
        Map<String, Object> request = mapOf("amount", "2", "operationType", "subtract");
        assertThrows(ServiceException.class, () -> service.adjustPoints(7L, request));
    }

    @Test
    void activityTimesCannotBecomeNegative()
    {
        when(mapper.selectUserSnapshot(9L)).thenReturn(mapOf("username", "bob"));
        when(mapper.ensureActivityAccount(9L)).thenReturn(0);
        when(mapper.selectActivityAccountForUpdate(9L)).thenReturn(mapOf("availableTimes", 1L));
        Map<String, Object> request = mapOf("amount", 2, "operationType", "subtract");
        assertThrows(ServiceException.class, () -> service.adjustActivityTimes(9L, request));
    }

    @Test
    void enablingActivityRequiresOneHundredPercentProbability()
    {
        when(mapper.selectEnabledPrizeProbability(3L)).thenReturn(new BigDecimal("99.99"));
        Map<String, Object> request = validActivity();
        request.put("id", 3L);
        request.put("isEnabled", "1");
        assertThrows(ServiceException.class, () -> service.updateActivity(request));
    }

    @Test
    void orderStateTransitionRejectsInvalidSourceState()
    {
        when(mapper.shipGiftOrder(5L)).thenReturn(0);
        assertThrows(ServiceException.class, () -> service.shipGiftOrder(5L));
    }

    @Test
    void prizeProbabilityMustBeWithinRange()
    {
        when(mapper.selectActivityById(1L)).thenReturn(mapOf("id", 1L));
        Map<String, Object> request = validPrize();
        request.put("probability", "101");
        assertThrows(ServiceException.class, () -> service.insertActivityPrize(request));
    }

    @Test
    void pointsAdjustmentRejectsUnknownUserBeforeCreatingAccount()
    {
        when(mapper.selectUserSnapshot(404L)).thenReturn(null);

        Map<String, Object> request = mapOf("amount", 1, "operationType", "add");
        assertThrows(ServiceException.class, () -> service.adjustPoints(404L, request));
        verify(mapper, never()).ensurePointsAccount(404L);
    }

    @Test
    void activityTimesAdjustmentRejectsUnknownUserBeforeCreatingAccount()
    {
        when(mapper.selectUserSnapshot(404L)).thenReturn(null);

        Map<String, Object> request = mapOf("amount", 1, "operationType", "add");
        assertThrows(ServiceException.class, () -> service.adjustActivityTimes(404L, request));
        verify(mapper, never()).ensureActivityAccount(404L);
    }

    @Test
    void giftRequiresTitle()
    {
        Map<String, Object> request = validGift();
        request.put("title", " ");
        assertThrows(ServiceException.class, () -> service.insertGift(request));
    }

    @Test
    void giftRejectsUnknownKind()
    {
        Map<String, Object> request = validGift();
        request.put("kind", "9");
        assertThrows(ServiceException.class, () -> service.insertGift(request));
    }

    @Test
    void activityRejectsUnknownEnabledState()
    {
        Map<String, Object> request = validActivity();
        request.put("isEnabled", "yes");
        assertThrows(ServiceException.class, () -> service.insertActivity(request));
    }

    @Test
    void prizeRejectsUnknownKind()
    {
        when(mapper.selectActivityById(1L)).thenReturn(mapOf("id", 1L));
        Map<String, Object> request = validPrize();
        request.put("kind", "9");
        assertThrows(ServiceException.class, () -> service.insertActivityPrize(request));
    }

    @Test
    void prizeRequiresExistingActivity()
    {
        when(mapper.selectActivityById(1L)).thenReturn(null);
        assertThrows(ServiceException.class, () -> service.insertActivityPrize(validPrize()));
    }

    @Test
    void accountPrizeMustBelongToSelectedActivity()
    {
        when(mapper.selectUserSnapshot(7L)).thenReturn(mapOf("id", 7L));
        when(mapper.selectActivityById(1L)).thenReturn(mapOf("id", 1L));
        when(mapper.selectActivityPrizeById(8L)).thenReturn(mapOf("id", 8L, "activityId", 2L));

        Map<String, Object> request = mapOf("activityId", 1L, "prizeId", 8L,
                "sortOrder", 0, "isWinning", "1");
        assertThrows(ServiceException.class, () -> service.insertAccountPrize(7L, request));
        verify(mapper, never()).insertAccountPrize(any());
    }

    @Test
    void accountPrizeRejectsInvalidWinningFlag()
    {
        when(mapper.selectUserSnapshot(7L)).thenReturn(mapOf("id", 7L));
        when(mapper.selectActivityById(1L)).thenReturn(mapOf("id", 1L));
        when(mapper.selectActivityPrizeById(8L)).thenReturn(mapOf("id", 8L, "activityId", 1L));

        Map<String, Object> request = mapOf("activityId", 1L, "prizeId", 8L,
                "sortOrder", 0, "isWinning", "yes");
        assertThrows(ServiceException.class, () -> service.insertAccountPrize(7L, request));
    }

    @Test
    void accountPrizeUpdateRejectsMissingOrForeignRow()
    {
        when(mapper.selectUserSnapshot(7L)).thenReturn(mapOf("id", 7L));
        when(mapper.selectActivityById(1L)).thenReturn(mapOf("id", 1L));
        when(mapper.selectActivityPrizeById(8L)).thenReturn(mapOf("id", 8L, "activityId", 1L));
        when(mapper.updateAccountPrize(any())).thenReturn(0);

        Map<String, Object> request = mapOf("id", 10L, "activityId", 1L, "prizeId", 8L,
                "sortOrder", 0, "isWinning", "0");
        assertThrows(ServiceException.class, () -> service.updateAccountPrize(7L, request));
    }

    private Map<String, Object> validGift()
    {
        return mapOf("title", "Gift", "kind", "1", "isEnabled", "1", "points", 10,
                "price", 1, "image", "/gift.png");
    }

    private Map<String, Object> validActivity()
    {
        return mapOf("title", "Lucky", "isEnabled", "0", "sortOrder", 0,
                "image", "/activity.png", "noWinningTips", "Try again",
                "ruleContent", "Rules", "description", "Description");
    }

    private Map<String, Object> validPrize()
    {
        return mapOf("activityId", 1L, "title", "Prize", "probability", 10,
                "kind", "1", "isEnabled", "1", "price", 0, "sortOrder", 0,
                "image", "/prize.png");
    }

    private Map<String, Object> mapOf(Object... values)
    {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < values.length; i += 2)
        {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }
}
