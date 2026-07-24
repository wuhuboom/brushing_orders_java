package com.order.member.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.order.common.annotation.Excel;
import com.order.common.core.domain.BaseEntity;

/**
 * 交易流水对象 goods_transaction_flow
 * 
 * @author order
 * @date 2025-10-27
 */
public class GoodsTransactionFlow extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 流水编号 */
    @Excel(name = "流水编号")
    private String serialCode;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 交易类型 */
    @Excel(name = "交易类型")
    private String transactionType;

    /** 交易前余额 */
    @Excel(name = "交易前余额")
    private BigDecimal balanceBefore;

    /** 交易金额 */
    @Excel(name = "交易金额")
    private BigDecimal transactionAmount;

    /** 交易后余额 */
    @Excel(name = "交易后余额")
    private BigDecimal balanceAfter;

    /** 是否隐藏 */
    @Excel(name = "是否隐藏")
    private String isHidden;

    /** 交易编号 */
    @Excel(name = "交易编号")
    private String transactionCode;

    /** 创建时间 */
    @JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.NUMBER)
    private Date createdTime;

    /** 用户名（来自 order_user 联表） */
    private String username;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setSerialCode(String serialCode) 
    {
        this.serialCode = serialCode;
    }

    public String getSerialCode() 
    {
        return serialCode;
    }

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setTransactionType(String transactionType) 
    {
        this.transactionType = transactionType;
    }

    public String getTransactionType() 
    {
        return transactionType;
    }

    public void setBalanceBefore(BigDecimal balanceBefore) 
    {
        this.balanceBefore = balanceBefore;
    }

    public BigDecimal getBalanceBefore() 
    {
        return balanceBefore;
    }

    public void setTransactionAmount(BigDecimal transactionAmount) 
    {
        this.transactionAmount = transactionAmount;
    }

    public BigDecimal getTransactionAmount() 
    {
        return transactionAmount;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) 
    {
        this.balanceAfter = balanceAfter;
    }

    public BigDecimal getBalanceAfter() 
    {
        return balanceAfter;
    }

    public void setIsHidden(String isHidden) 
    {
        this.isHidden = isHidden;
    }

    public String getIsHidden() 
    {
        return isHidden;
    }

    public void setTransactionCode(String transactionCode) 
    {
        this.transactionCode = transactionCode;
    }

    public String getTransactionCode() 
    {
        return transactionCode;
    }

    public void setCreatedTime(Date createdTime) 
    {
        this.createdTime = createdTime;
    }

    public Date getCreatedTime() 
    {
        return createdTime;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("serialCode", getSerialCode())
            .append("userId", getUserId())
            .append("transactionType", getTransactionType())
            .append("balanceBefore", getBalanceBefore())
            .append("transactionAmount", getTransactionAmount())
            .append("balanceAfter", getBalanceAfter())
            .append("isHidden", getIsHidden())
            .append("transactionCode", getTransactionCode())
            .append("createdTime", getCreatedTime())
            .append("remark", getRemark())
            .append("username", getUsername())
            .toString();
    }
}
