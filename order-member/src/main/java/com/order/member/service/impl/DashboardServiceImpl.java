package com.order.member.service.impl;

import com.order.member.mapper.DashboardMapper;
import com.order.member.service.IDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dashboard Service Impl
 *
 * @author order
 * @date 2025-12-12
 */
@Service
public class DashboardServiceImpl implements IDashboardService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    @Autowired
    private DashboardMapper dashboardMapper;

    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        // 1 --今日充值金额 ，今日赠送金额 ，总赠送金额 ，总充值金额
        stats.put("todayRechargeAmount", amount(dashboardMapper.getTodayRechargeAmount()));
        stats.put("todayGiftAmount", amount(dashboardMapper.getTodayGiftAmount()));
        stats.put("totalGiftAmount", amount(dashboardMapper.getTotalGiftAmount()));
        stats.put("totalRechargeAmount", amount(dashboardMapper.getTotalRechargeAmount()));

        // 2--今日提现金额，今日返佣���额，总返佣金额，总提现金额
        stats.put("todayWithdrawalAmount", amount(dashboardMapper.getTodayWithdrawalAmount()));
        stats.put("todayCommissionAmount", amount(dashboardMapper.getTodayCommissionAmount()));
        stats.put("totalCommissionAmount", amount(dashboardMapper.getTotalCommissionAmount()));
        stats.put("totalWithdrawalAmount", amount(dashboardMapper.getTotalWithdrawalAmount()));
        stats.put("totalWithdrawals", count(dashboardMapper.getTotalWithdrawals()));

        // 3--今日会员注册数量，今日任务完成次数，总任务完成次数，总会员注册数量
        stats.put("todayRegisteredUsers", count(dashboardMapper.getTodayRegisteredUsers()));
        stats.put("todayTasksCompleted", count(dashboardMapper.getTodayTasksCompleted()));
        stats.put("totalTasksCompleted", count(dashboardMapper.getTotalTasksCompleted()));
        stats.put("totalRegisteredUsers", count(dashboardMapper.getTotalRegisteredUsers()));

        // 4--今日投注人数，总订单数量，今日订单数量，总投注人数
        stats.put("todayBettingUsers", count(dashboardMapper.getTodayBettingUsers()));
        stats.put("totalOrders", count(dashboardMapper.getTotalOrders()));
        stats.put("todayOrders", count(dashboardMapper.getTodayOrders()));
        stats.put("totalBettingUsers", count(dashboardMapper.getTotalBettingUsers()));

        // 5-- 会员注���数量（需要最近一周的数据 ，每天有多少人注册）
        stats.put("weeklyRegisteredUsers", normalizeWeekly(
                dashboardMapper.getWeeklyRegisteredUsers(),
                Collections.singletonMap("count", 0L)));

        // 6--交易金额 （ 最近一周的数据 ）需要统计类型有 充值（cz）,提现（tx）,返佣（fy），下级返佣（xjfy），赠送（zs）,签到 （qd）
        Map<String, Object> amountDefaults = new LinkedHashMap<>();
        amountDefaults.put("recharge_amount", BigDecimal.ZERO);
        amountDefaults.put("withdrawal_amount", BigDecimal.ZERO);
        amountDefaults.put("commission_amount", BigDecimal.ZERO);
        amountDefaults.put("sub_commission_amount", BigDecimal.ZERO);
        amountDefaults.put("gift_amount", BigDecimal.ZERO);
        amountDefaults.put("signin_amount", BigDecimal.ZERO);
        stats.put("weeklyTransactionAmounts", normalizeWeekly(
                dashboardMapper.getWeeklyTransactionAmounts(), amountDefaults));

        // 7--交易数量 (最近一周的数据) 需要统计类型有 ，充值次数 ，体现次数，返佣次数，上级返佣次数，赠送次数，订单数量 ，投注人数 ，任务完成量
        Map<String, Object> countDefaults = new LinkedHashMap<>();
        countDefaults.put("recharge_count", 0L);
        countDefaults.put("withdrawal_count", 0L);
        countDefaults.put("commission_count", 0L);
        countDefaults.put("sub_commission_count", 0L);
        countDefaults.put("gift_count", 0L);
        countDefaults.put("signin_count", 0L);
        countDefaults.put("order_count", 0L);
        countDefaults.put("betting_user_count", 0L);
        countDefaults.put("task_completion_count", 0L);
        countDefaults.put("reset_count", 0L);
        stats.put("weeklyTransactionCounts", normalizeWeekly(
                dashboardMapper.getWeeklyTransactionCounts(), countDefaults));

        return stats;
    }

    @Override
    public Map<String, Object> getHeaderStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", count(dashboardMapper.getTotalOrders()));
        stats.put("totalWithdrawals", count(dashboardMapper.getTotalWithdrawals()));
        return stats;
    }

    private BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Long count(Long value) {
        return value == null ? 0L : value;
    }

    private List<Map<String, Object>> normalizeWeekly(List<Map<String, Object>> rows,
                                                       Map<String, Object> defaults) {
        Map<String, Map<String, Object>> rowsByDate = new HashMap<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                if (row == null || row.get("date") == null) {
                    continue;
                }
                rowsByDate.put(String.valueOf(row.get("date")), row);
            }
        }

        List<Map<String, Object>> result = new java.util.ArrayList<>(7);
        LocalDate start = LocalDate.now().minusDays(6);
        for (int index = 0; index < 7; index++) {
            String date = start.plusDays(index).format(DATE_FORMATTER);
            Map<String, Object> normalized = new LinkedHashMap<>();
            normalized.put("date", date);
            normalized.putAll(defaults);
            Map<String, Object> source = rowsByDate.get(date);
            if (source != null) {
                for (String key : defaults.keySet()) {
                    if (source.get(key) != null) {
                        normalized.put(key, source.get(key));
                    }
                }
            }
            result.add(normalized);
        }
        return result;
    }
}

