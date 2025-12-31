package com.brushing.member.domain.vo;

import java.math.BigDecimal;

/**
 * Aggregated statistics for a single hierarchy level under a user.
 */
public class MemberHierarchyStatVo {

    private int level;
    private int totalMembers;
    private int newRegisterCount;
    private int orderUserCount;
    private BigDecimal orderAmount = BigDecimal.ZERO;
    private BigDecimal totalBalance = BigDecimal.ZERO;
    private int firstDepositCount;
    private BigDecimal rechargeAmount = BigDecimal.ZERO;
    private BigDecimal withdrawAmount = BigDecimal.ZERO;
    private int withdrawUserCount;
    private BigDecimal depositWithdrawDiff = BigDecimal.ZERO;

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getTotalMembers() {
        return totalMembers;
    }

    public void setTotalMembers(int totalMembers) {
        this.totalMembers = totalMembers;
    }

    public int getNewRegisterCount() {
        return newRegisterCount;
    }

    public void setNewRegisterCount(int newRegisterCount) {
        this.newRegisterCount = newRegisterCount;
    }

    public int getOrderUserCount() {
        return orderUserCount;
    }

    public void setOrderUserCount(int orderUserCount) {
        this.orderUserCount = orderUserCount;
    }

    public BigDecimal getOrderAmount() {
        return orderAmount;
    }

    public void setOrderAmount(BigDecimal orderAmount) {
        this.orderAmount = orderAmount;
    }

    public BigDecimal getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(BigDecimal totalBalance) {
        this.totalBalance = totalBalance;
    }

    public int getFirstDepositCount() {
        return firstDepositCount;
    }

    public void setFirstDepositCount(int firstDepositCount) {
        this.firstDepositCount = firstDepositCount;
    }

    public BigDecimal getRechargeAmount() {
        return rechargeAmount;
    }

    public void setRechargeAmount(BigDecimal rechargeAmount) {
        this.rechargeAmount = rechargeAmount;
    }

    public BigDecimal getWithdrawAmount() {
        return withdrawAmount;
    }

    public void setWithdrawAmount(BigDecimal withdrawAmount) {
        this.withdrawAmount = withdrawAmount;
    }

    public int getWithdrawUserCount() {
        return withdrawUserCount;
    }

    public void setWithdrawUserCount(int withdrawUserCount) {
        this.withdrawUserCount = withdrawUserCount;
    }

    public BigDecimal getDepositWithdrawDiff() {
        return depositWithdrawDiff;
    }

    public void setDepositWithdrawDiff(BigDecimal depositWithdrawDiff) {
        this.depositWithdrawDiff = depositWithdrawDiff;
    }
}

