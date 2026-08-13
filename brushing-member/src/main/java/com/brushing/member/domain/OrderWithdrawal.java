package com.brushing.member.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.brushing.common.annotation.Excel;
import com.brushing.common.core.domain.BaseEntity;

/**
 * 提现记录对象 order_withdrawal
 * 
 * @author brushing
 * @date 2025-08-07
 */
public class OrderWithdrawal extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;
    private Long userId;

    /** 提现编号 */
    @Excel(name = "提现编号")
    private String code;

    /** 提现金额 */
    @Excel(name = "提现金额")
    private BigDecimal amount;

    /** 到账金额 */
    @Excel(name = "到账金额")
    private BigDecimal creditedAmount;

    /** 手续费 */
    @Excel(name = "手续费")
    private BigDecimal fee;


    private BigDecimal withdrawFee;

    private Long agentUserId;


    private Long walletId;

    /** 申请时间 */
    @JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.NUMBER)
    private Date applicationTime;

    /** 审核时间 */
    @JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.NUMBER)
    private Date auditTime;

    /** 状态 */
    @Excel(name = "状态")
    private String status;

    private String withdrawName;

    /** 提现地址 */
    private String withdrawAddress;

    /** 提现类型 */
    private String withdrawType;


    // 当前用户字段
    private String username; // 当前用户名
    private String phone;    // 当前用户电话号码
    private String userRemark; // 当前用户备注
    private String isReal;   // 当前用户实名认证状态

    private String ip; // ip
    private String ipAddress; // 提现时的IP地址

    // 上级用户字段
    private String parentUsername; // 上级用户名
    private String parentPhone;   // 上级电话号码
    // 当日统计字段
    private Integer dailyOrderCount; // 当日订单数
    private Integer dailyWithdrawalCount; //

    private OrderBankWallet bankWallet;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
    }

    public OrderBankWallet getBankWallet() {
        return bankWallet;
    }

    public void setBankWallet(OrderBankWallet bankWallet) {
        this.bankWallet = bankWallet;
    }

    public Long getAgentUserId() {
        return agentUserId;
    }

    public void setAgentUserId(Long agentUserId) {
        this.agentUserId = agentUserId;
    }

    public BigDecimal getWithdrawFee() {
        return withdrawFee;
    }

    public void setWithdrawFee(BigDecimal withdrawFee) {
        this.withdrawFee = withdrawFee;
    }

    public String getUserRemark() {
        return userRemark;
    }

    public void setUserRemark(String userRemark) {
        this.userRemark = userRemark;
    }

    public String getIsReal() {
        return isReal;
    }

    public void setIsReal(String isReal) {
        this.isReal = isReal;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getParentUsername() {
        return parentUsername;
    }

    public void setParentUsername(String parentUsername) {
        this.parentUsername = parentUsername;
    }

    public String getParentPhone() {
        return parentPhone;
    }

    public void setParentPhone(String parentPhone) {
        this.parentPhone = parentPhone;
    }

    public Integer getDailyOrderCount() {
        return dailyOrderCount;
    }

    public void setDailyOrderCount(Integer dailyOrderCount) {
        this.dailyOrderCount = dailyOrderCount;
    }

    public Integer getDailyWithdrawalCount() {
        return dailyWithdrawalCount;
    }

    public void setDailyWithdrawalCount(Integer dailyWithdrawalCount) {
        this.dailyWithdrawalCount = dailyWithdrawalCount;
    }

    public String getWithdrawName() {
        return withdrawName;
    }

    public void setWithdrawName(String withdrawName) {
        this.withdrawName = withdrawName;
    }

    public String getWithdrawAddress() {
        return withdrawAddress;
    }

    public void setWithdrawAddress(String withdrawAddress) {
        this.withdrawAddress = withdrawAddress;
    }

    public String getWithdrawType() {
        return withdrawType;
    }

    public void setWithdrawType(String withdrawType) {
        this.withdrawType = withdrawType;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setCode(String code) 
    {
        this.code = code;
    }

    public String getCode() 
    {
        return code;
    }

    public void setAmount(BigDecimal amount) 
    {
        this.amount = amount;
    }

    public BigDecimal getAmount() 
    {
        return amount;
    }

    public void setCreditedAmount(BigDecimal creditedAmount) 
    {
        this.creditedAmount = creditedAmount;
    }

    public BigDecimal getCreditedAmount() 
    {
        return creditedAmount;
    }

    public void setFee(BigDecimal fee) 
    {
        this.fee = fee;
    }

    public BigDecimal getFee() 
    {
        return fee;
    }

    public void setApplicationTime(Date applicationTime) 
    {
        this.applicationTime = applicationTime;
    }

    public Date getApplicationTime() 
    {
        return applicationTime;
    }

    public void setAuditTime(Date auditTime) 
    {
        this.auditTime = auditTime;
    }

    public Date getAuditTime() 
    {
        return auditTime;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("code", getCode())
            .append("amount", getAmount())
            .append("creditedAmount", getCreditedAmount())
            .append("fee", getFee())
            .append("ip", getIp())
            .append("ipAddress", getIpAddress())
            .append("applicationTime", getApplicationTime())
            .append("auditTime", getAuditTime())
            .append("remark", getRemark())
            .append("status", getStatus())
            .toString();
    }
}
