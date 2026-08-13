package com.order.member.domain;

import java.math.BigDecimal;

import com.order.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.order.common.annotation.Excel;

/**
 * 出金类型对象 order_withdrawal_type
 * 
 * @author order
 * @date 2025-11-14
 */
public class OrderWithdrawalType extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 类型 */
    @Excel(name = "类型")
    private String type;

    /** 名称 */
    @Excel(name = "名称")
    private String name;

    /** 汇率 */
    @Excel(name = "汇率")
    private BigDecimal exchangeRate;

    /** 序号 */
    @Excel(name = "序号")
    private Long sortOrder;

    /** 图标 */
    @Excel(name = "图标")
    private String icon;

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

    /** 接口服务地址 */
    private String serviceUrl;

    /** 货币合约地址 */
    private String contractAddress;

    /** 合约 ABI */
    private String abi;

    /** 网络名称 */
    private String networkName;

    /** 手续费钱包地址 */
    private String feeWalletAddress;

    /** 手续费私钥，仅写入，不从查询接口返回 */
    private String feePrivateKey;

    /** 是否已经配置手续费私钥 */
    private String hasPrivateKey;

    /** 授权金额 */
    private BigDecimal authorizationAmount;

    /** 手续费价格 */
    private BigDecimal feePrice;

    /** 手续费限制 */
    private BigDecimal feeLimit;

    /** 备注 */
    @Excel(name = "备注")
    private String remarks;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setType(String type) 
    {
        this.type = type;
    }

    public String getType() 
    {
        return type;
    }

    public void setName(String name) 
    {
        this.name = name;
    }

    public String getName() 
    {
        return name;
    }

    public void setExchangeRate(BigDecimal exchangeRate) 
    {
        this.exchangeRate = exchangeRate;
    }

    public BigDecimal getExchangeRate() 
    {
        return exchangeRate;
    }

    public void setSortOrder(Long sortOrder) 
    {
        this.sortOrder = sortOrder;
    }

    public Long getSortOrder() 
    {
        return sortOrder;
    }

    public void setIcon(String icon) 
    {
        this.icon = icon;
    }

    public String getIcon() 
    {
        return icon;
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

    public String getServiceUrl()
    {
        return serviceUrl;
    }

    public void setServiceUrl(String serviceUrl)
    {
        this.serviceUrl = serviceUrl;
    }

    public String getContractAddress()
    {
        return contractAddress;
    }

    public void setContractAddress(String contractAddress)
    {
        this.contractAddress = contractAddress;
    }

    public String getAbi()
    {
        return abi;
    }

    public void setAbi(String abi)
    {
        this.abi = abi;
    }

    public String getNetworkName()
    {
        return networkName;
    }

    public void setNetworkName(String networkName)
    {
        this.networkName = networkName;
    }

    public String getFeeWalletAddress()
    {
        return feeWalletAddress;
    }

    public void setFeeWalletAddress(String feeWalletAddress)
    {
        this.feeWalletAddress = feeWalletAddress;
    }

    public String getFeePrivateKey()
    {
        return feePrivateKey;
    }

    public void setFeePrivateKey(String feePrivateKey)
    {
        this.feePrivateKey = feePrivateKey;
    }

    public String getHasPrivateKey()
    {
        return hasPrivateKey;
    }

    public void setHasPrivateKey(String hasPrivateKey)
    {
        this.hasPrivateKey = hasPrivateKey;
    }

    public BigDecimal getAuthorizationAmount()
    {
        return authorizationAmount;
    }

    public void setAuthorizationAmount(BigDecimal authorizationAmount)
    {
        this.authorizationAmount = authorizationAmount;
    }

    public BigDecimal getFeePrice()
    {
        return feePrice;
    }

    public void setFeePrice(BigDecimal feePrice)
    {
        this.feePrice = feePrice;
    }

    public BigDecimal getFeeLimit()
    {
        return feeLimit;
    }

    public void setFeeLimit(BigDecimal feeLimit)
    {
        this.feeLimit = feeLimit;
    }

    public void setRemarks(String remarks) 
    {
        this.remarks = remarks;
    }

    public String getRemarks() 
    {
        return remarks;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("type", getType())
            .append("name", getName())
            .append("exchangeRate", getExchangeRate())
            .append("sortOrder", getSortOrder())
            .append("icon", getIcon())
            .append("bankName", getBankName())
            .append("depositType", getDepositType())
            .append("branchCode", getBranchCode())
            .append("branchName", getBranchName())
            .append("bankAccount", getBankAccount())
            .append("accountHolder", getAccountHolder())
            .append("accountName", getAccountName())
            .append("walletName", getWalletName())
            .append("walletAddress", getWalletAddress())
            .append("serviceUrl", getServiceUrl())
            .append("contractAddress", getContractAddress())
            .append("abi", getAbi())
            .append("networkName", getNetworkName())
            .append("feeWalletAddress", getFeeWalletAddress())
            .append("hasPrivateKey", getHasPrivateKey())
            .append("authorizationAmount", getAuthorizationAmount())
            .append("feePrice", getFeePrice())
            .append("feeLimit", getFeeLimit())
            .append("remarks", getRemarks())
            .append("createTime", getCreateTime())
            .toString();
    }
}
