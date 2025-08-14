package com.brushing.member.domain;

import java.math.BigDecimal;

public class DashboardData {

    // 总用户数
    private Integer totalUsers;

    // 今日用户数
    private Integer todayUsers;

    // 总充值金额
    private BigDecimal totalRecharge;

    // 今日充值金额
    private BigDecimal todayRecharge;

    // 总提现金额
    private BigDecimal totalWithdrawal;

    // 今日提现金额
    private BigDecimal todayWithdrawal;

    // 今日订单数
    private Integer todayOrders;

    // Getters and Setters
    public Integer getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(Integer totalUsers) {
        this.totalUsers = totalUsers;
    }

    public Integer getTodayUsers() {
        return todayUsers;
    }

    public void setTodayUsers(Integer todayUsers) {
        this.todayUsers = todayUsers;
    }

    public BigDecimal getTotalRecharge() {
        return totalRecharge;
    }

    public void setTotalRecharge(BigDecimal totalRecharge) {
        this.totalRecharge = totalRecharge;
    }

    public BigDecimal getTodayRecharge() {
        return todayRecharge;
    }

    public void setTodayRecharge(BigDecimal todayRecharge) {
        this.todayRecharge = todayRecharge;
    }

    public BigDecimal getTotalWithdrawal() {
        return totalWithdrawal;
    }

    public void setTotalWithdrawal(BigDecimal totalWithdrawal) {
        this.totalWithdrawal = totalWithdrawal;
    }

    public BigDecimal getTodayWithdrawal() {
        return todayWithdrawal;
    }

    public void setTodayWithdrawal(BigDecimal todayWithdrawal) {
        this.todayWithdrawal = todayWithdrawal;
    }

    public Integer getTodayOrders() {
        return todayOrders;
    }

    public void setTodayOrders(Integer todayOrders) {
        this.todayOrders = todayOrders;
    }
}
