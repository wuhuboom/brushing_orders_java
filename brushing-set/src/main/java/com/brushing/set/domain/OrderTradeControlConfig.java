package com.brushing.set.domain;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.brushing.common.annotation.Excel;
import com.brushing.common.core.domain.BaseEntity;

/**
 * 交易控制配置对象 order_trade_control_config
 * 
 * @author brushing
 * @date 2025-07-31
 */
public class OrderTradeControlConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 是否开启提现 */
    @Excel(name = "是否开启提现")
    private String withdrawEnabled;

    /** 交易最低用户余额 */
    @Excel(name = "交易最低用户余额")
    private BigDecimal minUserBalance;

    /** 单笔提现最低信誉分 */
    @Excel(name = "单笔提现最低信誉分")
    private BigDecimal minWithdrawCreditScore;

    /** 单笔提现最低金额 */
    @Excel(name = "单笔提现最低金额")
    private BigDecimal minWithdrawAmount;

    /** 单笔提现最高金额 */
    @Excel(name = "单笔提现最高金额")
    private BigDecimal maxWithdrawAmount;

    /** 单日总提现最大金额 */
    @Excel(name = "单日总提现最大金额")
    private BigDecimal dailyWithdrawLimit;

    /** 提现手续费（%） */
    @Excel(name = "提现手续费", readConverterExp = "%=")
    private BigDecimal withdrawFeePercent;

    /** 上一级会员交易佣金（%） */
    @Excel(name = "上一级会员交易佣金", readConverterExp = "%=")
    private BigDecimal level1CommissionPercent;

    /** 上二级会员交易佣金（%） */
    @Excel(name = "上二级会员交易佣金", readConverterExp = "%=")
    private BigDecimal level2CommissionPercent;

    /** 上三级会员交易佣金（%） */
    @Excel(name = "上三级会员交易佣金", readConverterExp = "%=")
    private BigDecimal level3CommissionPercent;

    /** 上四级会员交易佣金（%） */
    @Excel(name = "上四级会员交易佣金", readConverterExp = "%=")
    private BigDecimal level4CommissionPercent;

    /** 上五级会员交易佣金（%） */
    @Excel(name = "上五级会员交易佣金", readConverterExp = "%=")
    private BigDecimal level5CommissionPercent;

    /** 交易范围区间最小百分比 */
    @Excel(name = "交易范围区间最小百分比")
    private BigDecimal tradeRangePercentMin;

    /** 交易范围区间最大百分比 */
    @Excel(name = "交易范围区间最大百分比")
    private BigDecimal tradeRangePercentMax;

    /** 充值时间开始 */
    @JsonFormat(pattern = "HH:mm")
    private LocalTime rechargeTimeStart;

    /** 充值时间结束 */
    @JsonFormat(pattern = "HH:mm")
    private LocalTime rechargeTimeEnd;

    /** 提现时间开始 */
    @JsonFormat(pattern = "HH:mm")
    private LocalTime withdrawTimeStart;

    /** 提现时间结束 */
    @JsonFormat(pattern = "HH:mm")
    private LocalTime withdrawTimeEnd;

    /** 抢单时间开始 */
    @JsonFormat(pattern = "HH:mm")
    private LocalTime orderTimeStart;

    /** 抢单时间结束 */
    @JsonFormat(pattern = "HH:mm")
    private LocalTime orderTimeEnd;

    /** 工作时间开始 */
    @JsonFormat(pattern = "HH:mm")
    private LocalTime workTimeStart;

    /** 工作时间结束 */
    @JsonFormat(pattern = "HH:mm")
    private LocalTime workTimeEnd;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public String getWithdrawEnabled() {
        return withdrawEnabled;
    }

    public void setWithdrawEnabled(String withdrawEnabled) {
        this.withdrawEnabled = withdrawEnabled;
    }

    public void setMinUserBalance(BigDecimal minUserBalance)
    {
        this.minUserBalance = minUserBalance;
    }

    public BigDecimal getMinUserBalance() 
    {
        return minUserBalance;
    }

    public void setMinWithdrawCreditScore(BigDecimal minWithdrawCreditScore) 
    {
        this.minWithdrawCreditScore = minWithdrawCreditScore;
    }

    public BigDecimal getMinWithdrawCreditScore() 
    {
        return minWithdrawCreditScore;
    }

    public void setMinWithdrawAmount(BigDecimal minWithdrawAmount) 
    {
        this.minWithdrawAmount = minWithdrawAmount;
    }

    public BigDecimal getMinWithdrawAmount() 
    {
        return minWithdrawAmount;
    }

    public void setMaxWithdrawAmount(BigDecimal maxWithdrawAmount) 
    {
        this.maxWithdrawAmount = maxWithdrawAmount;
    }

    public BigDecimal getMaxWithdrawAmount() 
    {
        return maxWithdrawAmount;
    }

    public void setDailyWithdrawLimit(BigDecimal dailyWithdrawLimit) 
    {
        this.dailyWithdrawLimit = dailyWithdrawLimit;
    }

    public BigDecimal getDailyWithdrawLimit() 
    {
        return dailyWithdrawLimit;
    }

    public void setWithdrawFeePercent(BigDecimal withdrawFeePercent) 
    {
        this.withdrawFeePercent = withdrawFeePercent;
    }

    public BigDecimal getWithdrawFeePercent() 
    {
        return withdrawFeePercent;
    }

    public void setLevel1CommissionPercent(BigDecimal level1CommissionPercent) 
    {
        this.level1CommissionPercent = level1CommissionPercent;
    }

    public BigDecimal getLevel1CommissionPercent() 
    {
        return level1CommissionPercent;
    }

    public void setLevel2CommissionPercent(BigDecimal level2CommissionPercent) 
    {
        this.level2CommissionPercent = level2CommissionPercent;
    }

    public BigDecimal getLevel2CommissionPercent() 
    {
        return level2CommissionPercent;
    }

    public void setLevel3CommissionPercent(BigDecimal level3CommissionPercent) 
    {
        this.level3CommissionPercent = level3CommissionPercent;
    }

    public BigDecimal getLevel3CommissionPercent() 
    {
        return level3CommissionPercent;
    }

    public void setLevel4CommissionPercent(BigDecimal level4CommissionPercent) 
    {
        this.level4CommissionPercent = level4CommissionPercent;
    }

    public BigDecimal getLevel4CommissionPercent() 
    {
        return level4CommissionPercent;
    }

    public void setLevel5CommissionPercent(BigDecimal level5CommissionPercent) 
    {
        this.level5CommissionPercent = level5CommissionPercent;
    }

    public BigDecimal getLevel5CommissionPercent() 
    {
        return level5CommissionPercent;
    }

    public void setTradeRangePercentMin(BigDecimal tradeRangePercentMin) 
    {
        this.tradeRangePercentMin = tradeRangePercentMin;
    }

    public BigDecimal getTradeRangePercentMin() 
    {
        return tradeRangePercentMin;
    }

    public void setTradeRangePercentMax(BigDecimal tradeRangePercentMax) 
    {
        this.tradeRangePercentMax = tradeRangePercentMax;
    }

    public BigDecimal getTradeRangePercentMax() 
    {
        return tradeRangePercentMax;
    }

    public LocalTime getRechargeTimeStart() {
        return rechargeTimeStart;
    }

    public void setRechargeTimeStart(LocalTime rechargeTimeStart) {
        this.rechargeTimeStart = rechargeTimeStart;
    }

    public LocalTime getRechargeTimeEnd() {
        return rechargeTimeEnd;
    }

    public void setRechargeTimeEnd(LocalTime rechargeTimeEnd) {
        this.rechargeTimeEnd = rechargeTimeEnd;
    }

    public LocalTime getWithdrawTimeStart() {
        return withdrawTimeStart;
    }

    public void setWithdrawTimeStart(LocalTime withdrawTimeStart) {
        this.withdrawTimeStart = withdrawTimeStart;
    }

    public LocalTime getWithdrawTimeEnd() {
        return withdrawTimeEnd;
    }

    public void setWithdrawTimeEnd(LocalTime withdrawTimeEnd) {
        this.withdrawTimeEnd = withdrawTimeEnd;
    }

    public LocalTime getOrderTimeStart() {
        return orderTimeStart;
    }

    public void setOrderTimeStart(LocalTime orderTimeStart) {
        this.orderTimeStart = orderTimeStart;
    }

    public LocalTime getOrderTimeEnd() {
        return orderTimeEnd;
    }

    public void setOrderTimeEnd(LocalTime orderTimeEnd) {
        this.orderTimeEnd = orderTimeEnd;
    }

    public LocalTime getWorkTimeStart() {
        return workTimeStart;
    }

    public void setWorkTimeStart(LocalTime workTimeStart) {
        this.workTimeStart = workTimeStart;
    }

    public LocalTime getWorkTimeEnd() {
        return workTimeEnd;
    }

    public void setWorkTimeEnd(LocalTime workTimeEnd) {
        this.workTimeEnd = workTimeEnd;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("withdrawEnabled", getWithdrawEnabled())
            .append("minUserBalance", getMinUserBalance())
            .append("minWithdrawCreditScore", getMinWithdrawCreditScore())
            .append("minWithdrawAmount", getMinWithdrawAmount())
            .append("maxWithdrawAmount", getMaxWithdrawAmount())
            .append("dailyWithdrawLimit", getDailyWithdrawLimit())
            .append("withdrawFeePercent", getWithdrawFeePercent())
            .append("level1CommissionPercent", getLevel1CommissionPercent())
            .append("level2CommissionPercent", getLevel2CommissionPercent())
            .append("level3CommissionPercent", getLevel3CommissionPercent())
            .append("level4CommissionPercent", getLevel4CommissionPercent())
            .append("level5CommissionPercent", getLevel5CommissionPercent())
            .append("tradeRangePercentMin", getTradeRangePercentMin())
            .append("tradeRangePercentMax", getTradeRangePercentMax())
            .append("rechargeTimeStart", getRechargeTimeStart())
            .append("rechargeTimeEnd", getRechargeTimeEnd())
            .append("withdrawTimeStart", getWithdrawTimeStart())
            .append("withdrawTimeEnd", getWithdrawTimeEnd())
            .append("orderTimeStart", getOrderTimeStart())
            .append("orderTimeEnd", getOrderTimeEnd())
            .append("workTimeStart", getWorkTimeStart())
            .append("workTimeEnd", getWorkTimeEnd())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
