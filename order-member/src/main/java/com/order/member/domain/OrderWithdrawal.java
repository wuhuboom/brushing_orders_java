package com.order.member.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.order.common.annotation.Excel;
import com.order.common.core.domain.BaseEntity;

/**
 * 提现对象 order_withdrawal
 * 
 * @author order
 * @date 2025-11-11
 */
public class OrderWithdrawal extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** ID */
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

    /** 状态 */
    @Excel(name = "状态")
    private String status;

    /** 备注 */
    @Excel(name = "备注")
    private String remarks;

    /** 交易类型 */
    @Excel(name = "交易类型")
    private String transactionType;

    /** 订单号 */
    @Excel(name = "订单号")
    private String orderNumber;

    /** 是否隐藏 */
    @Excel(name = "是否隐藏")
    private String isHidden;

    /** 手续费 */
    @Excel(name = "手续费")
    private BigDecimal fee;

    /** 提现账号ID */
    @Excel(name = "提现账号ID")
    private Long withdrawalAccountId;

    /** 用户名（来自 order_user） */
    private String username;

    /** 手机号（来自 order_user） */
    private String phoneNumber;

    /** 上级用户名（来自 order_user 的 parent） */
    private String parentUsername;

    /** 钱包地址筛选条件（来自提现账户） */
    private String accountAddress;

    /** 是否假人（来自 order_user） */
    private String isFake;

    /** 当前余额信息（来自 order_user，用于后台审核视图） */
    private BigDecimal userBalance;
    private BigDecimal userFrozenBalance;

    /** 提现账户详情（来自 goods_withdrawal_account） */
    private GoodsWithdrawalAccount withdrawalAccountInfo;

    /** 客户端幂等号、净额和提交时的账户快照 */
    private String requestId;
    private BigDecimal netAmount;
    @JsonIgnore
    private String accountSnapshotEncrypted;
    @JsonIgnore
    private String accountMask;
    @JsonIgnore
    @Excel(name = "提现账户")
    private String adminAccountDisplay;
    private LocalDate businessDate;

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

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public void setRemarks(String remarks) 
    {
        this.remarks = remarks;
    }

    public String getRemarks() 
    {
        return remarks;
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

    public void setFee(BigDecimal fee) 
    {
        this.fee = fee;
    }

    public BigDecimal getFee() 
    {
        return fee;
    }

    public void setWithdrawalAccountId(Long withdrawalAccountId) 
    {
        this.withdrawalAccountId = withdrawalAccountId;
    }

    public Long getWithdrawalAccountId() 
    {
        return withdrawalAccountId;
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

    public GoodsWithdrawalAccount getWithdrawalAccountInfo()
    {
        return withdrawalAccountInfo;
    }

    public void setWithdrawalAccountInfo(GoodsWithdrawalAccount withdrawalAccountInfo)
    {
        this.withdrawalAccountInfo = withdrawalAccountInfo;
    }

    public BigDecimal getUserBalance() {
        return userBalance;
    }

    public void setUserBalance(BigDecimal userBalance) {
        this.userBalance = userBalance;
    }

    public BigDecimal getUserFrozenBalance() {
        return userFrozenBalance;
    }

    public void setUserFrozenBalance(BigDecimal userFrozenBalance) {
        this.userFrozenBalance = userFrozenBalance;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    public String getAccountSnapshotEncrypted() {
        return accountSnapshotEncrypted;
    }

    public void setAccountSnapshotEncrypted(String accountSnapshotEncrypted) {
        this.accountSnapshotEncrypted = accountSnapshotEncrypted;
    }

    public String getAccountMask() {
        return accountMask;
    }

    public void setAccountMask(String accountMask) {
        this.accountMask = accountMask;
    }

    public String getAdminAccountDisplay() {
        return adminAccountDisplay;
    }

    public void setAdminAccountDisplay(String adminAccountDisplay) {
        this.adminAccountDisplay = adminAccountDisplay;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public void setBusinessDate(LocalDate businessDate) {
        this.businessDate = businessDate;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("userId", getUserId())
            .append("amount", getAmount())
            .append("withdrawalType", getWithdrawalType())
            .append("status", getStatus())
            .append("createTime", getCreateTime())
            .append("remarks", getRemarks())
            .append("transactionType", getTransactionType())
            .append("orderNumber", getOrderNumber())
            .append("isHidden", getIsHidden())
            .append("fee", getFee())
            .append("withdrawalAccountId", getWithdrawalAccountId())
            .append("requestId", getRequestId())
            .append("netAmount", getNetAmount())
            .append("accountMask", getAccountMask())
            .append("username", getUsername())
            .append("phoneNumber", getPhoneNumber())
            .append("parentUsername", getParentUsername())
            .append("accountAddress", getAccountAddress())
            .append("isFake", getIsFake())
            .append("userBalance", getUserBalance())
            .append("userFrozenBalance", getUserFrozenBalance())
            .append("withdrawalAccountInfo", getWithdrawalAccountInfo())
            .toString();
    }
}
