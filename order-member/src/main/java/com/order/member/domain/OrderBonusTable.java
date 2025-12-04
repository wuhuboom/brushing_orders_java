package com.order.member.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.order.common.annotation.Excel;
import com.order.common.core.domain.BaseEntity;

/**
 * 彩金对象 order_bonus_table
 *
 * @author order
 * @date 2025-11-04
 */
public class OrderBonusTable extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 单数 */
    @Excel(name = "单数")
    private Long orderNum;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 金额 */
    @Excel(name = "金额")
    private BigDecimal amount;

    /** 动画时长 */
    @Excel(name = "动画时长")
    private Long animationDuration;

    /** 显示时长 */
    @Excel(name = "显示时长")
    private Long displayDuration;

    /** 发放类型 */
    @Excel(name = "发放类型")
    private String distributionType;

    /** 过期时间 */
    @JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.NUMBER)
    @Excel(name = "过期时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date expiryTime;

    /** 推送类型 */
    @Excel(name = "推送类型")
    private String pushType;

    /** 是否发放 */
    @Excel(name = "是否发放")
    private String isDistributed;

    /** 是否领取 */
    @Excel(name = "是否领取")
    private String isReceived;

    /** 领取时间 */
    @JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.NUMBER)
    @Excel(name = "领取时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date receivedTime;

    /** 发放时间 */
    @JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.NUMBER)
    @Excel(name = "发放时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date distributionTime;

    /** 推送用户 */
    @Excel(name = "推送用户")
    private String toUsers;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }

    public void setOrderNum(Long orderNum)
    {
        this.orderNum = orderNum;
    }

    public Long getOrderNum()
    {
        return orderNum;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setAmount(BigDecimal amount)
    {
        this.amount = amount;
    }

    public BigDecimal getAmount()
    {
        return amount;
    }

    public void setAnimationDuration(Long animationDuration)
    {
        this.animationDuration = animationDuration;
    }

    public Long getAnimationDuration()
    {
        return animationDuration;
    }

    public void setDisplayDuration(Long displayDuration)
    {
        this.displayDuration = displayDuration;
    }

    public Long getDisplayDuration()
    {
        return displayDuration;
    }

    public void setDistributionType(String distributionType)
    {
        this.distributionType = distributionType;
    }

    public String getDistributionType()
    {
        return distributionType;
    }

    public void setExpiryTime(Date expiryTime)
    {
        this.expiryTime = expiryTime;
    }

    public Date getExpiryTime()
    {
        return expiryTime;
    }

    public void setPushType(String pushType)
    {
        this.pushType = pushType;
    }

    public String getPushType()
    {
        return pushType;
    }

    public void setIsDistributed(String isDistributed)
    {
        this.isDistributed = isDistributed;
    }

    public String getIsDistributed()
    {
        return isDistributed;
    }

    public void setIsReceived(String isReceived)
    {
        this.isReceived = isReceived;
    }

    public String getIsReceived()
    {
        return isReceived;
    }

    public void setReceivedTime(Date receivedTime)
    {
        this.receivedTime = receivedTime;
    }

    public Date getReceivedTime()
    {
        return receivedTime;
    }

    public void setDistributionTime(Date distributionTime)
    {
        this.distributionTime = distributionTime;
    }

    public Date getDistributionTime()
    {
        return distributionTime;
    }

    public void setToUsers(String toUsers)
    {
        this.toUsers = toUsers;
    }

    public String getToUsers()
    {
        return toUsers;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("orderNum", getOrderNum())
                .append("userId", getUserId())
                .append("amount", getAmount())
                .append("animationDuration", getAnimationDuration())
                .append("displayDuration", getDisplayDuration())
                .append("distributionType", getDistributionType())
                .append("expiryTime", getExpiryTime())
                .append("pushType", getPushType())
                .append("createTime", getCreateTime())
                .append("isDistributed", getIsDistributed())
                .append("isReceived", getIsReceived())
                .append("receivedTime", getReceivedTime())
                .append("distributionTime", getDistributionTime())
                .append("toUsers", getToUsers())
                .toString();
    }
}
