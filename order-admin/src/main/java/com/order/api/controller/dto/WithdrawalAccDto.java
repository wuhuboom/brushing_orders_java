package com.order.api.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class WithdrawalAccDto {

    /** ID */
    private Long id;

    /** 用户id */
    private Long userId;

    /** 类型 */
    private String type;

    /** 出金类型 */
    @NotBlank(message = "Withdrawal type is required")
    @Size(max = 20, message = "Withdrawal type is too long")
    private String withdrawalTypeId;

    /** 是否默认 */
    private String isDefault;

    /** 银行名称 */
    @Size(max = 100)
    private String bankName;

    /** 存款种类 */
    @Size(max = 50)
    private String depositType;

    /** 支行代码 */
    @Size(max = 50)
    private String branchCode;

    /** 支行名称 */
    @Size(max = 100)
    private String branchName;

    /** 银行账号 */
    @Size(max = 100)
    private String bankAccount;

    /** 账户持有人 */
    @Size(max = 100)
    private String accountHolder;

    /** 账户名称 */
    @Size(max = 100)
    private String accountName;

    /** 钱包名称 */
    @Size(max = 100)
    private String walletName;

    /** 钱包地址 */
    @Size(max = 255)
    private String walletAddress;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getWithdrawalTypeId() {
        return withdrawalTypeId;
    }

    public void setWithdrawalTypeId(String withdrawalTypeId) {
        this.withdrawalTypeId = withdrawalTypeId;
    }

    public String getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(String isDefault) {
        this.isDefault = isDefault;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getDepositType() {
        return depositType;
    }

    public void setDepositType(String depositType) {
        this.depositType = depositType;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(String bankAccount) {
        this.bankAccount = bankAccount;
    }

    public String getAccountHolder() {
        return accountHolder;
    }

    public void setAccountHolder(String accountHolder) {
        this.accountHolder = accountHolder;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getWalletName() {
        return walletName;
    }

    public void setWalletName(String walletName) {
        this.walletName = walletName;
    }

    public String getWalletAddress() {
        return walletAddress;
    }

    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }
}
