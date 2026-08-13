package com.brushing.member.domain.vo;

import java.math.BigDecimal;

/**
 * Statistics for a top-level (parent_id = 0) user and their downline within a time range.
 */
public class TopLevelUserStatVo {

    private Long userId;
    private String username;
    private Integer totalSubCount = 0; // all-level subordinates count
    private Integer directInviteCount = 0; // direct children count (may be range-limited)
    private BigDecimal totalBalance = BigDecimal.ZERO; // sum of balances (and frozen if desired)
    private BigDecimal rechargeAmount = BigDecimal.ZERO; // sum of topups in time range
    private BigDecimal totalCommission = BigDecimal.ZERO; // sum of commissions in subtree
    private BigDecimal withdrawAmount = BigDecimal.ZERO; // sum of withdrawals in time range
    private BigDecimal depositWithdrawDiff = BigDecimal.ZERO; // recharge - withdraw

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getTotalSubCount() {
        return totalSubCount;
    }

    public void setTotalSubCount(Integer totalSubCount) {
        this.totalSubCount = totalSubCount;
    }

    public Integer getDirectInviteCount() {
        return directInviteCount;
    }

    public void setDirectInviteCount(Integer directInviteCount) {
        this.directInviteCount = directInviteCount;
    }

    public BigDecimal getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(BigDecimal totalBalance) {
        this.totalBalance = totalBalance;
    }

    public BigDecimal getRechargeAmount() {
        return rechargeAmount;
    }

    public void setRechargeAmount(BigDecimal rechargeAmount) {
        this.rechargeAmount = rechargeAmount;
    }

    public BigDecimal getTotalCommission() {
        return totalCommission;
    }

    public void setTotalCommission(BigDecimal totalCommission) {
        this.totalCommission = totalCommission;
    }

    public BigDecimal getWithdrawAmount() {
        return withdrawAmount;
    }

    public void setWithdrawAmount(BigDecimal withdrawAmount) {
        this.withdrawAmount = withdrawAmount;
    }

    public BigDecimal getDepositWithdrawDiff() {
        return depositWithdrawDiff;
    }

    public void setDepositWithdrawDiff(BigDecimal depositWithdrawDiff) {
        this.depositWithdrawDiff = depositWithdrawDiff;
    }
}
