package com.order.member.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Dashboard Mapper
 *
 * @author order
 * @date 2025-12-12
 */
public interface DashboardMapper {

    // 1 --今日充值金额 ，今日赠送金额 ，总赠送金额 ，总充值金额
    BigDecimal getTodayRechargeAmount();
    BigDecimal getTodayGiftAmount();
    BigDecimal getTotalGiftAmount();
    BigDecimal getTotalRechargeAmount();

    // 2--今日提现金额，今日返佣金额，总返佣金额，总提现金额
    BigDecimal getTodayWithdrawalAmount();
    BigDecimal getTodayCommissionAmount();
    BigDecimal getTotalCommissionAmount();
    BigDecimal getTotalWithdrawalAmount();
    Long getTotalWithdrawals();

    // 3--今日会员注册数量，今日任务完成次数，总任务完成次数，总会员注册数量
    Long getTodayRegisteredUsers();
    Long getTodayTasksCompleted();
    Long getTotalTasksCompleted();
    Long getTotalRegisteredUsers();

    // 4--今日投注人数，总订单数量，今日订单数量，总投注人数
    Long getTodayBettingUsers();
    Long getTotalOrders();
    Long getTodayOrders();
    Long getTotalBettingUsers();

    // 5-- 会员注册数量（需要最近一周的数据 ，每天有多少人注册）
    List<Map<String, Object>> getWeeklyRegisteredUsers();

    // 6--交易金额 （ 最近一周的数据 ）需要统计类型有 充值（cz）,提现（tx）,返佣（fy），下级返佣（xjfy），赠送（zs）,签到 （qd）
    List<Map<String, Object>> getWeeklyTransactionAmounts();

    // 7--交易数量 (最近一周的数据) 需要统计类型有 ，充值次数 ，体现次数，返佣次数，上级返佣次数，赠送次数，订单数量 ，投注人数 ，任务完成量
    List<Map<String, Object>> getWeeklyTransactionCounts();
}

