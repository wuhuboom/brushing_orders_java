package com.brushing.member.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.brushing.common.annotation.Excel;
import com.brushing.common.core.domain.BaseEntity;

/**
 * 奖品配置对象 order_lottery_prize
 * 
 * @author brushing
 * @date 2025-10-12
 */
public class OrderLotteryPrize extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 配置id */
    @Excel(name = "配置id")
    private Long configId;

    /** 奖品金额 */
    @Excel(name = "奖品金额")
    private BigDecimal amount;

    /** 奖品个数 */
    @Excel(name = "奖品个数")
    private Integer totalCount;

    /** 剩余中奖次数 */
    @Excel(name = "剩余中奖次数")
    private Integer remainCount;

    /** 概率 */
    @Excel(name = "概率")
    private BigDecimal probability;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setConfigId(Long configId) 
    {
        this.configId = configId;
    }

    public Long getConfigId() 
    {
        return configId;
    }

    public void setAmount(BigDecimal amount) 
    {
        this.amount = amount;
    }

    public BigDecimal getAmount() 
    {
        return amount;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getRemainCount() {
        return remainCount;
    }

    public void setRemainCount(Integer remainCount) {
        this.remainCount = remainCount;
    }

    public void setProbability(BigDecimal probability)
    {
        this.probability = probability;
    }

    public BigDecimal getProbability() 
    {
        return probability;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("configId", getConfigId())
            .append("amount", getAmount())
            .append("totalCount", getTotalCount())
            .append("remainCount", getRemainCount())
            .append("probability", getProbability())
            .append("createTime", getCreateTime())
            .toString();
    }
}
