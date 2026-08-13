package com.brushing.member.domain.vo;

import java.math.BigDecimal;

public class RebateStatVo {
    private Long userId;
    private String username;
    private String inviteCode;
    private BigDecimal rebateToday;
    private BigDecimal rebateYesterday;
    private BigDecimal rebateTotal;
    private Integer subordinatesCount;
    private Integer totalTradingUsers;
    private Integer todayTradingUsers;
    private BigDecimal totalSubordinateBalance;

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

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public BigDecimal getRebateToday() {
        return rebateToday;
    }

    public void setRebateToday(BigDecimal rebateToday) {
        this.rebateToday = rebateToday;
    }

    public BigDecimal getRebateYesterday() {
        return rebateYesterday;
    }

    public void setRebateYesterday(BigDecimal rebateYesterday) {
        this.rebateYesterday = rebateYesterday;
    }

    public BigDecimal getRebateTotal() {
        return rebateTotal;
    }

    public void setRebateTotal(BigDecimal rebateTotal) {
        this.rebateTotal = rebateTotal;
    }

    public Integer getSubordinatesCount() {
        return subordinatesCount;
    }

    public void setSubordinatesCount(Integer subordinatesCount) {
        this.subordinatesCount = subordinatesCount;
    }

    public Integer getTotalTradingUsers() {
        return totalTradingUsers;
    }

    public void setTotalTradingUsers(Integer totalTradingUsers) {
        this.totalTradingUsers = totalTradingUsers;
    }

    public Integer getTodayTradingUsers() {
        return todayTradingUsers;
    }

    public void setTodayTradingUsers(Integer todayTradingUsers) {
        this.todayTradingUsers = todayTradingUsers;
    }

    public BigDecimal getTotalSubordinateBalance() {
        return totalSubordinateBalance;
    }

    public void setTotalSubordinateBalance(BigDecimal totalSubordinateBalance) {
        this.totalSubordinateBalance = totalSubordinateBalance;
    }
}
