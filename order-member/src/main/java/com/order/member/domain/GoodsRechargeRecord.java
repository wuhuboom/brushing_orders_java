package com.order.member.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.order.common.annotation.Excel;
import com.order.common.core.domain.BaseEntity;

/**
 * 充值记录对象 goods_recharge_record
 * 
 * @author order
 * @date 2025-10-27
 */
public class GoodsRechargeRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 自增ID */
    private Long id;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 金额 */
    @Excel(name = "金额")
    private BigDecimal amount;

    /** 出金类型 */
    @Excel(name = "出金类型")
    private String withdrawalType;

    /** 赠送金额 */
    @Excel(name = "赠送金额")
    private BigDecimal giftAmount;

    /** 到账金额 */
    @Excel(name = "到账金额")
    private BigDecimal receivedAmount;

    /** 状态 */
    @Excel(name = "状态")
    private String status;


    /** 交易类型 */
    @Excel(name = "交易类型")
    private String transactionType;

    /** 订单号 */
    @Excel(name = "订单号")
    private String orderNumber;

    /** 是否隐藏，0否，1是 */
    @Excel(name = "是否隐藏，0否，1是")
    private String isHidden;

    /** 用户名（来自 order_user 联表） */
    private String username;

    /** 手机号（来自 order_user 联表） */
    private String phoneNumber;

    /** 上级用户名（来自 order_user 的 parent） */
    private String parentUsername;

    /** 钱包地址筛选条件（来自会员提现账户） */
    private String accountAddress;

    /** 是否假人（来自 order_user） */
    private String isFake;

    /** 充值账户展示值（脱敏） */
    private String rechargeAccount;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
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

    public void setWithdrawalType(String withdrawalType) 
    {
        this.withdrawalType = withdrawalType;
    }

    public String getWithdrawalType() 
    {
        return withdrawalType;
    }

    public void setGiftAmount(BigDecimal giftAmount) 
    {
        this.giftAmount = giftAmount;
    }

    public BigDecimal getGiftAmount() 
    {
        return giftAmount;
    }

    public void setReceivedAmount(BigDecimal receivedAmount) 
    {
        this.receivedAmount = receivedAmount;
    }

    public BigDecimal getReceivedAmount() 
    {
        return receivedAmount;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }



    public void setTransactionType(String transactionType) 
    {
        this.transactionType = transactionType;
    }

    public String getTransactionType() 
    {
        return transactionType;
    }

    public void setOrderNumber(String orderNumber) 
    {
        this.orderNumber = orderNumber;
    }

    public String getOrderNumber() 
    {
        return orderNumber;
    }

    public void setIsHidden(String isHidden) 
    {
        this.isHidden = isHidden;
    }

    public String getIsHidden() 
    {
        return isHidden;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getPhoneNumber()
    {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber)
    {
        this.phoneNumber = phoneNumber;
    }

    public String getParentUsername()
    {
        return parentUsername;
    }

    public void setParentUsername(String parentUsername)
    {
        this.parentUsername = parentUsername;
    }

    public String getAccountAddress()
    {
        return accountAddress;
    }

    public void setAccountAddress(String accountAddress)
    {
        this.accountAddress = accountAddress;
    }

    public String getIsFake()
    {
        return isFake;
    }

    public void setIsFake(String isFake)
    {
        this.isFake = isFake;
    }

    public String getRechargeAccount()
    {
        return rechargeAccount;
    }

    public void setRechargeAccount(String rechargeAccount)
    {
        this.rechargeAccount = rechargeAccount;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("userId", getUserId())
            .append("amount", getAmount())
            .append("withdrawalType", getWithdrawalType())
            .append("giftAmount", getGiftAmount())
            .append("receivedAmount", getReceivedAmount())
            .append("status", getStatus())
            .append("remark", getRemark())
            .append("transactionType", getTransactionType())
            .append("orderNumber", getOrderNumber())
            .append("isHidden", getIsHidden())
            .append("username", getUsername())
            .append("phoneNumber", getPhoneNumber())
            .append("parentUsername", getParentUsername())
            .append("accountAddress", getAccountAddress())
            .append("isFake", getIsFake())
            .append("rechargeAccount", getRechargeAccount())
            .toString();
    }
}
