package com.order.member.service;

import com.order.member.domain.OrderUser;
import com.order.member.mapper.OrderInfoMapper;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Projects active linked groups for display without advancing completed task progress. */
@Service
public class MemberTaskProgressService {
    public static final String INCLUDE_PENDING_LINKED_ORDER = "includePendingLinkedOrderInTaskProgress";

    private final IOrderConfigService configService;
    private final OrderInfoMapper orderInfoMapper;

    public MemberTaskProgressService(IOrderConfigService configService, OrderInfoMapper orderInfoMapper) {
        this.configService = configService;
        this.orderInfoMapper = orderInfoMapper;
    }

    public void populate(OrderUser user) {
        if (user != null) {
            populate(List.of(user));
        }
    }

    public void populate(Collection<OrderUser> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        Set<Long> userIds = new LinkedHashSet<>();
        for (OrderUser user : users) {
            if (user != null) {
                user.setDisplayTaskProgress(null);
                if (user.getId() != null) {
                    userIds.add(user.getId());
                }
            }
        }
        if (userIds.isEmpty() || !configService.getConfigValue("trade", INCLUDE_PENDING_LINKED_ORDER)
                .map(value -> "0".equals(String.valueOf(value)))
                .orElse(false)) {
            return;
        }

        Map<Long, Long> activeProgress = new HashMap<>();
        for (OrderUser row : orderInfoMapper.selectActiveLinkedTaskProgress(userIds)) {
            if (row != null && row.getId() != null && row.getTaskProgress() != null) {
                activeProgress.merge(row.getId(), row.getTaskProgress(), Math::max);
            }
        }
        for (OrderUser user : users) {
            if (user == null) {
                continue;
            }
            Long active = activeProgress.get(user.getId());
            if (active != null) {
                user.setDisplayTaskProgress(Math.max(
                        user.getTaskProgress() == null ? 0L : user.getTaskProgress(), active));
            }
        }
    }
}
