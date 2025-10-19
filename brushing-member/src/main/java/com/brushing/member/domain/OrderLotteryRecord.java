package com.brushing.member.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.brushing.common.annotation.Excel;
import com.brushing.common.core.domain.BaseEntity;

/**
 * 抽奖记录对象 order_lottery_record
 * 
 * @author brushing
 * @date 2025-10-12
 */
public class OrderLotteryRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 活动配置ID */
    @Excel(name = "活动配置ID")
    private Long configId;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 中奖ID */
    @Excel(name = "中奖ID")
    private Long prizeId;

    /** 中奖金额 */
    @Excel(name = "中奖金额")
    private BigDecimal amount;

    /** 用户IP */
    @Excel(name = "用户IP")
    private String ipAddress;

    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

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

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setPrizeId(Long prizeId) 
    {
        this.prizeId = prizeId;
    }

    public Long getPrizeId() 
    {
        return prizeId;
    }

    public void setAmount(BigDecimal amount) 
    {
        this.amount = amount;
    }

    public BigDecimal getAmount() 
    {
        return amount;
    }

    public void setIpAddress(String ipAddress) 
    {
        this.ipAddress = ipAddress;
    }

    public String getIpAddress() 
    {
        return ipAddress;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("configId", getConfigId())
            .append("userId", getUserId())
            .append("prizeId", getPrizeId())
            .append("amount", getAmount())
            .append("createTime", getCreateTime())
            .append("ipAddress", getIpAddress())
            .toString();
    }
}
