package com.order.member.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.order.member.domain.OrderUser;
import com.order.member.mapper.OrderInfoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberTaskProgressServiceTest {
    private static final String CONFIG_KEY = "includePendingLinkedOrderInTaskProgress";

    @Mock
    private IOrderConfigService configService;

    @Mock
    private OrderInfoMapper orderInfoMapper;

    private MemberTaskProgressService service;

    @BeforeEach
    void setUp() {
        service = new MemberTaskProgressService(configService, orderInfoMapper);
    }

    @Test
    void absentSettingKeepsCompletedProgressAndClearsPreviouslyPopulatedDisplay() {
        when(configService.getConfigValue("trade", CONFIG_KEY)).thenReturn(Optional.empty());
        OrderUser user = user(7L, 1L);
        user.setDisplayTaskProgress(2L);

        service.populate(user);

        assertProgress(user, 1L, 1L);
        // The display must fall back to the raw value, rather than retain a copy.
        user.setTaskProgress(3L);
        assertProgress(user, 3L, 3L);
        verifyNoInteractions(orderInfoMapper);
    }

    @Test
    void disabledAndInvalidValuesNeverActivateTheSetting() {
        for (Object disabled : List.of("1", "", "true", "false", "invalid", " 0", "0 ", 1, true, false)) {
            clearInvocations(orderInfoMapper);
            when(configService.getConfigValue("trade", CONFIG_KEY)).thenReturn(Optional.of(disabled));
            OrderUser user = user(7L, 1L);
            user.setDisplayTaskProgress(2L);

            service.populate(user);

            assertProgress(user, 1L, 1L);
            verifyNoInteractions(orderInfoMapper);
        }
    }

    @Test
    void numericZeroIsAcceptedForExistingJsonConfigurationCompatibility() {
        when(configService.getConfigValue("trade", CONFIG_KEY)).thenReturn(Optional.of(0));
        when(orderInfoMapper.selectActiveLinkedTaskProgress(any()))
                .thenReturn(List.of(user(7L, 2L)));
        OrderUser user = user(7L, 1L);

        service.populate(user);

        assertProgress(user, 1L, 2L);
    }

    @Test
    void theFirstPendingDetailAtOrderTwoDisplaysTwoWithoutAdvancingCompletedProgress() {
        enable();
        when(orderInfoMapper.selectActiveLinkedTaskProgress(any()))
                .thenReturn(List.of(user(7L, 2L)));
        OrderUser user = user(7L, 1L);

        service.populate(user);

        assertProgress(user, 1L, 2L);
        verify(orderInfoMapper).selectActiveLinkedTaskProgress(Set.of(7L));
    }

    @Test
    void batchesKeepMembersIsolatedPreservePageMetadataAndQueryDistinctIdsOnce() {
        enable();
        when(orderInfoMapper.selectActiveLinkedTaskProgress(any()))
                .thenReturn(List.of(user(7L, 2L), user(8L, 4L)));
        OrderUser first = user(7L, 1L);
        OrderUser duplicate = user(7L, 1L);
        OrderUser second = user(8L, 3L);
        OrderUser noActivity = user(9L, 5L);
        OrderUser noId = user(null, 6L);
        Page<OrderUser> page = new Page<>(2, 20);
        page.setTotal(95L);
        page.addAll(Arrays.asList(first, duplicate, second, noActivity, noId, null));

        service.populate(page);

        verify(configService).getConfigValue("trade", CONFIG_KEY);
        verify(orderInfoMapper).selectActiveLinkedTaskProgress(Set.of(7L, 8L, 9L));
        assertSame(first, page.get(0));
        assertEquals(2, page.getPageNum());
        assertEquals(20, page.getPageSize());
        assertEquals(95L, page.getTotal());
        assertProgress(first, 1L, 2L);
        assertProgress(duplicate, 1L, 2L);
        assertProgress(second, 3L, 4L);
        assertProgress(noActivity, 5L, 5L);
        assertProgress(noId, 6L, 6L);
    }

    @Test
    void activeOrdersNeverReduceAnAlreadyHigherCompletedCount() {
        enable();
        when(orderInfoMapper.selectActiveLinkedTaskProgress(any()))
                .thenReturn(List.of(user(7L, 2L)));
        OrderUser user = user(7L, 5L);

        service.populate(user);

        assertProgress(user, 5L, 5L);
    }

    @Test
    void nullCompletedProgressCanDisplayTheFirstActiveLinkedOrder() {
        enable();
        when(orderInfoMapper.selectActiveLinkedTaskProgress(any()))
                .thenReturn(List.of(user(7L, 1L)));
        OrderUser user = user(7L, null);

        service.populate(user);

        assertProgress(user, null, 1L);
    }

    @Test
    void pendingFrozenAndLaterDetailsKeepTheSameCountAndCompletionDoesNotAddAgain() {
        enable();
        when(orderInfoMapper.selectActiveLinkedTaskProgress(any()))
                .thenReturn(List.of(user(7L, 2L)), List.of(user(7L, 2L)),
                        List.of(user(7L, 2L)), List.of());
        OrderUser user = user(7L, 1L);

        service.populate(user); // First detail pending.
        assertProgress(user, 1L, 2L);
        service.populate(user); // First detail frozen, before the next grab.
        assertProgress(user, 1L, 2L);
        service.populate(user); // A later detail of the same group is pending.
        assertProgress(user, 1L, 2L);
        user.setTaskProgress(2L); // The normal settlement advances completed progress once.
        service.populate(user);
        assertProgress(user, 2L, 2L);

        verify(orderInfoMapper, times(4)).selectActiveLinkedTaskProgress(Set.of(7L));
    }

    @Test
    void cancellingTheOnlyPendingDetailRemovesItsDisplayButFrozenSiblingsKeepTheGroupVisible() {
        enable();
        when(orderInfoMapper.selectActiveLinkedTaskProgress(any()))
                .thenReturn(List.of(user(7L, 2L)), List.of(),
                        List.of(user(7L, 2L)), List.of(user(7L, 2L)));
        OrderUser user = user(7L, 1L);

        service.populate(user);
        assertProgress(user, 1L, 2L);
        service.populate(user); // The first pending detail was cancelled.
        assertProgress(user, 1L, 1L);
        service.populate(user); // The group is grabbed again.
        assertProgress(user, 1L, 2L);
        service.populate(user); // A later pending detail is cancelled; a sibling stays frozen.
        assertProgress(user, 1L, 2L);
    }

    @Test
    void changingTheSettingDuringTheGroupOnlyChangesTheDisplay() {
        when(configService.getConfigValue("trade", CONFIG_KEY))
                .thenReturn(Optional.of("0"), Optional.of("1"), Optional.of("0"));
        when(orderInfoMapper.selectActiveLinkedTaskProgress(any()))
                .thenReturn(List.of(user(7L, 2L)));
        OrderUser user = user(7L, 1L);

        service.populate(user);
        assertProgress(user, 1L, 2L);
        service.populate(user);
        assertProgress(user, 1L, 1L);
        service.populate(user);
        assertProgress(user, 1L, 2L);

        verify(orderInfoMapper, times(2)).selectActiveLinkedTaskProgress(Set.of(7L));
    }

    @Test
    void nullAndEmptyCollectionsDoNotReadConfigurationOrQueryOrders() {
        service.populate((OrderUser) null);
        service.populate((Collection<OrderUser>) null);
        service.populate(List.of());
        service.populate(Arrays.asList(null, null));

        verifyNoInteractions(configService, orderInfoMapper);
    }

    @Test
    void displayProgressIsAReadOnlyJsonProjectionAndFallsBackToCompletedProgress() throws Exception {
        ObjectMapper json = new ObjectMapper();
        OrderUser user = user(7L, 1L);
        assertProgress(user, 1L, 1L);
        user.setDisplayTaskProgress(2L);

        var serialized = json.valueToTree(user);
        assertEquals(1L, serialized.get("taskProgress").longValue());
        assertEquals(2L, serialized.get("displayTaskProgress").longValue());

        OrderUser posted = json.readValue(
                "{\"id\":7,\"taskProgress\":1,\"displayTaskProgress\":999}", OrderUser.class);
        assertProgress(posted, 1L, 1L);
        posted.setTaskProgress(null);
        assertNull(posted.getDisplayTaskProgress());
    }

    private void enable() {
        when(configService.getConfigValue("trade", CONFIG_KEY)).thenReturn(Optional.of("0"));
    }

    private static OrderUser user(Long id, Long completed) {
        OrderUser user = new OrderUser();
        user.setId(id);
        user.setTaskProgress(completed);
        return user;
    }

    private static void assertProgress(OrderUser user, Long completed, Long displayed) {
        assertEquals(completed, user.getTaskProgress(), "completed progress must remain unchanged");
        assertEquals(displayed, user.getDisplayTaskProgress(), "displayed task progress");
    }
}
