package com.brushing.member.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.brushing.common.annotation.Excel;
import com.brushing.common.core.domain.BaseEntity;

/**
 * 商品店铺对象 order_shop
 * 
 * @author brushing
 * @date 2025-11-17
 */
public class OrderShop extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 商店名称 */
    @Excel(name = "商店名称")
    private String name;

    /** 商店图标 */
    @Excel(name = "商店图标")
    private String icon;

    /** VIP等级 */
    @Excel(name = "VIP等级")
    private Integer vipLevel;
    private Integer autoVip;
    private Integer maxAutoVip;

    /** 最小交易金额 */
    @Excel(name = "最小交易金额")
    private BigDecimal minMoney;

    /** 最大交易金额 */
    @Excel(name = "最大交易金额")
    private BigDecimal maxMoney;

    /** 佣金百分比 */
    @Excel(name = "佣金百分比")
    private BigDecimal commissionPercentage;

    public Integer getMaxAutoVip() {
        return maxAutoVip;
    }

    public void setMaxAutoVip(Integer maxAutoVip) {
        this.maxAutoVip = maxAutoVip;
    }

    public Integer getAutoVip() {
        return autoVip;
    }

    public void setAutoVip(Integer autoVip) {
        this.autoVip = autoVip;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setName(String name) 
    {
        this.name = name;
    }

    public String getName() 
    {
        return name;
    }

    public void setIcon(String icon) 
    {
        this.icon = icon;
    }

    public String getIcon() 
    {
        return icon;
    }

    public void setVipLevel(Integer vipLevel) 
    {
        this.vipLevel = vipLevel;
    }

    public Integer getVipLevel() 
    {
        return vipLevel;
    }

    public void setMinMoney(BigDecimal minMoney) 
    {
        this.minMoney = minMoney;
    }

    public BigDecimal getMinMoney() 
    {
        return minMoney;
    }

    public void setMaxMoney(BigDecimal maxMoney) 
    {
        this.maxMoney = maxMoney;
    }

    public BigDecimal getMaxMoney() 
    {
        return maxMoney;
    }

    public void setCommissionPercentage(BigDecimal commissionPercentage) 
    {
        this.commissionPercentage = commissionPercentage;
    }

    public BigDecimal getCommissionPercentage() 
    {
        return commissionPercentage;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("name", getName())
            .append("icon", getIcon())
            .append("vipLevel", getVipLevel())
            .append("minMoney", getMinMoney())
            .append("maxMoney", getMaxMoney())
            .append("commissionPercentage", getCommissionPercentage())
            .append("remark", getRemark())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
