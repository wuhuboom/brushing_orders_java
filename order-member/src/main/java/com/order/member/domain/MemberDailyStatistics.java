package com.order.member.domain;

import java.math.BigDecimal;

/** Daily member totals calculated in the active business timezone. */
public class MemberDailyStatistics {
    private Long userId;
    private BigDecimal todayCommission;
    private BigDecimal todayParentCommission;
    private Integer todayWithdrawalCount;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getTodayCommission() {
        return todayCommission;
    }

    public void setTodayCommission(BigDecimal todayCommission) {
        this.todayCommission = todayCommission;
    }

    public BigDecimal getTodayParentCommission() {
        return todayParentCommission;
    }

    public void setTodayParentCommission(BigDecimal todayParentCommission) {
        this.todayParentCommission = todayParentCommission;
    }

    public Integer getTodayWithdrawalCount() {
        return todayWithdrawalCount;
    }

    public void setTodayWithdrawalCount(Integer todayWithdrawalCount) {
        this.todayWithdrawalCount = todayWithdrawalCount;
    }
}
