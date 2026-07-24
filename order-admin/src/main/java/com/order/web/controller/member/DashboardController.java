package com.order.web.controller.member;

import com.order.common.core.controller.BaseController;
import com.order.common.core.domain.AjaxResult;
import com.order.member.service.IDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Dashboard Controller
 *
 * @author order
 * @date 2025-12-12
 */
@RestController
@RequestMapping("/admin/dashboard")
public class DashboardController extends BaseController {

    @Autowired
    private IDashboardService dashboardService;

    /**
     * Get dashboard statistics
     */
    @GetMapping("/stats")
    public AjaxResult getStats() {
        Map<String, Object> stats = dashboardService.getStats();
        return AjaxResult.success(stats);
    }

    @GetMapping("/header-stats")
    public AjaxResult getHeaderStats() {
        return AjaxResult.success(dashboardService.getHeaderStats());
    }
}

