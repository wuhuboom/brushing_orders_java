package com.order.member.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.order.common.annotation.Excel;
import com.order.common.core.domain.BaseEntity;

/**
 * 提现账户对象 goods_withdrawal_account
 * 
 * @author order
 * @date 2025-11-05
 */
public class GoodsWithdrawalAccount extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 用户id */
    @Excel(name = "用户id")
    private Long userId;

    /** 类型 */
    @Excel(name = "类型")
    private String type;

    /** 出金类型 */
    @Excel(name = "出金类型")
    private String withdrawalTypeId;

    /** 是否默认 */
    @Excel(name = "是否默认")
    private String isDefault;

    /** 银行名称 */
    @Excel(name = "银行名称")
    private String bankName;

    /** 存款种类 */
    @Excel(name = "存款种类")
    private String depositType;

    /** 支行代码 */
    @Excel(name = "支行代码")
    private String branchCode;

    /** 支行名称 */
    @Excel(name = "支行名称")
    private String branchName;

    /** 银行账号 */
    @Excel(name = "银行账号")
    private String bankAccount;

    /** 账户持有人 */
    @Excel(name = "账户持有人")
    private String accountHolder;

    /** 账户名称 */
    @Excel(name = "账户名称")
    private String accountName;

    /** 钱包名称 */
    @Excel(name = "钱包名称")
    private String walletName;

    /** 钱包地址 */
    @Excel(name = "钱包地址")
    private String walletAddress;

    private String withdrawalType;

    /** 软删除：0=有效，1=删除 */
    private String deleted;
    private Date deletedTime;

    /** 第一阶段迁移使用的密文影子列 */
    @JsonIgnore
    private String bankAccountEncrypted;
    @JsonIgnore
    private String accountHolderEncrypted;
    @JsonIgnore
    private String accountNameEncrypted;
    @JsonIgnore
    private String walletAddressEncrypted;

    /** 无需解密即可安全展示的掩码 */
    private String bankAccountMask;
    private String accountHolderMask;
    private String accountNameMask;
    private String walletAddressMask;

    public String getWithdrawalType() {
        return withdrawalType;
    }

    public void setWithdrawalType(String withdrawalType) {
        this.withdrawalType = withdrawalType;
    }

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

    public void setType(String type) 
    {
        this.type = type;
    }

    public String getType() 
    {
        return type;
    }

    public String getWithdrawalTypeId() {
        return withdrawalTypeId;
    }

    public void setWithdrawalTypeId(String withdrawalTypeId) {
        this.withdrawalTypeId = withdrawalTypeId;
    }

    public void setIsDefault(String isDefault)
    {
        this.isDefault = isDefault;
    }

    public String getIsDefault() 
    {
        return isDefault;
    }

    public void setBankName(String bankName) 
    {
        this.bankName = bankName;
    }

    public String getBankName() 
    {
        return bankName;
    }

    public void setDepositType(String depositType) 
    {
        this.depositType = depositType;
    }

    public String getDepositType() 
    {
        return depositType;
    }

    public void setBranchCode(String branchCode) 
    {
        this.branchCode = branchCode;
    }

    public String getBranchCode() 
    {
        return branchCode;
    }

    public void setBranchName(String branchName) 
    {
        this.branchName = branchName;
    }

    public String getBranchName() 
    {
        return branchName;
    }

    public void setBankAccount(String bankAccount) 
    {
        this.bankAccount = bankAccount;
    }

    public String getBankAccount() 
    {
        return bankAccount;
    }

    public void setAccountHolder(String accountHolder) 
    {
        this.accountHolder = accountHolder;
    }

    public String getAccountHolder() 
    {
        return accountHolder;
    }

    public void setAccountName(String accountName) 
    {
        this.accountName = accountName;
    }

    public String getAccountName() 
    {
        return accountName;
    }

    public void setWalletName(String walletName) 
    {
        this.walletName = walletName;
    }

    public String getWalletName() 
    {
        return walletName;
    }

    public void setWalletAddress(String walletAddress) 
    {
        this.walletAddress = walletAddress;
    }

    public String getWalletAddress() 
    {
        return walletAddress;
    }

    public String getDeleted() {
        return deleted;
    }

    public void setDeleted(String deleted) {
        this.deleted = deleted;
    }

    public Date getDeletedTime() {
        return deletedTime;
    }

    public void setDeletedTime(Date deletedTime) {
        this.deletedTime = deletedTime;
    }

    public String getBankAccountEncrypted() {
        return bankAccountEncrypted;
    }

    public void setBankAccountEncrypted(String bankAccountEncrypted) {
        this.bankAccountEncrypted = bankAccountEncrypted;
    }

    public String getAccountHolderEncrypted() {
        return accountHolderEncrypted;
    }

    public void setAccountHolderEncrypted(String accountHolderEncrypted) {
        this.accountHolderEncrypted = accountHolderEncrypted;
    }

    public String getAccountNameEncrypted() {
        return accountNameEncrypted;
    }

    public void setAccountNameEncrypted(String accountNameEncrypted) {
        this.accountNameEncrypted = accountNameEncrypted;
    }

    public String getWalletAddressEncrypted() {
        return walletAddressEncrypted;
    }

    public void setWalletAddressEncrypted(String walletAddressEncrypted) {
        this.walletAddressEncrypted = walletAddressEncrypted;
    }

    public String getBankAccountMask() {
        return bankAccountMask;
    }

    public void setBankAccountMask(String bankAccountMask) {
        this.bankAccountMask = bankAccountMask;
    }

    public String getAccountHolderMask() {
        return accountHolderMask;
    }

    public void setAccountHolderMask(String accountHolderMask) {
        this.accountHolderMask = accountHolderMask;
    }

    public String getAccountNameMask() {
        return accountNameMask;
    }

    public void setAccountNameMask(String accountNameMask) {
        this.accountNameMask = accountNameMask;
    }

    public String getWalletAddressMask() {
        return walletAddressMask;
    }

    public void setWalletAddressMask(String walletAddressMask) {
        this.walletAddressMask = walletAddressMask;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("userId", getUserId())
            .append("type", getType())
            .append("isDefault", getIsDefault())
            .append("bankName", getBankName())
            .append("depositType", getDepositType())
            .append("branchCode", getBranchCode())
            .append("branchName", getBranchName())
            .append("bankAccount", maskForLog(getBankAccount()))
            .append("accountHolder", maskForLog(getAccountHolder()))
            .append("accountName", maskForLog(getAccountName()))
            .append("walletName", getWalletName())
            .append("walletAddress", maskForLog(getWalletAddress()))
            .append("createTime", getCreateTime())
            .toString();
    }

    private String maskForLog(String value) {
        return value == null || value.isEmpty() ? value : "***";
    }
}
