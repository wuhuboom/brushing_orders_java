package com.order.member.service;

import java.util.Map;

/**
 * Dashboard Service
 *
 * @author order
 * @date 2025-12-12
 */
public interface IDashboardService {

    /**
     * Get dashboard statistics
     *
     * @return
     */
    Map<String, Object> getStats();

    Map<String, Object> getHeaderStats();
}

