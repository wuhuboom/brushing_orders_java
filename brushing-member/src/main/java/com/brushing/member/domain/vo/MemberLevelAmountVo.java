package com.brushing.member.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.util.Date;

/**
 * VO for returning user-level sums for first 3 levels under a given username
 */
public class MemberLevelAmountVo {
    private Long userId;
    private String username;
    private Integer level; // 1,2,3
    @JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.NUMBER)
    private Date createTime;
    private BigDecimal rechargeAmount;
    private BigDecimal withdrawAmount;

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

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
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


}
