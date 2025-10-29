package com.brushing.member.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.brushing.common.annotation.Excel;
import com.brushing.common.core.domain.BaseEntity;

/**
 * 银行钱包对象 order_bank_wallet
 * 
 * @author brushing
 * @date 2025-10-13
 */
public class OrderBankWallet extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 银行卡还是钱包 */
    @Excel(name = "银行卡还是钱包")
    private String type;

    /** 姓名 */
    @Excel(name = "姓名")
    private String name;

    /** 银行编码 */
    @Excel(name = "银行编码")
    private String bankCode;

    /** 银行卡号 */
    @Excel(name = "银行卡号")
    private String bankCard;

    private String bankType;

    /** 钱包类型 */
    @Excel(name = "钱包类型")
    private String walletType;

    /** 钱包地址 */
    @Excel(name = "钱包地址")
    private String walletAddress;

    public String getBankType() {
        return bankType;
    }

    public void setBankType(String bankType) {
        this.bankType = bankType;
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

    public void setName(String name) 
    {
        this.name = name;
    }

    public String getName() 
    {
        return name;
    }

    public void setBankCode(String bankCode) 
    {
        this.bankCode = bankCode;
    }

    public String getBankCode() 
    {
        return bankCode;
    }

    public void setBankCard(String bankCard) 
    {
        this.bankCard = bankCard;
    }

    public String getBankCard() 
    {
        return bankCard;
    }

    public void setWalletType(String walletType) 
    {
        this.walletType = walletType;
    }

    public String getWalletType() 
    {
        return walletType;
    }

    public void setWalletAddress(String walletAddress) 
    {
        this.walletAddress = walletAddress;
    }

    public String getWalletAddress() 
    {
        return walletAddress;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("userId", getUserId())
            .append("type", getType())
            .append("name", getName())
            .append("bankCode", getBankCode())
            .append("bankCard", getBankCard())
            .append("walletType", getWalletType())
            .append("walletAddress", getWalletAddress())
            .append("createTime", getCreateTime())
            .toString();
    }
}
