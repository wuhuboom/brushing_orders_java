package com.brushing.member.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.brushing.common.annotation.Excel;
import com.brushing.common.core.domain.BaseEntity;

/**
 * 会员等级对象 order_member_level
 * 
 * @author brushing
 * @date 2025-07-30
 */
public class OrderMemberLevel extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 会员图标 */
    @Excel(name = "会员图标")
    private String icon;

    /** 中文名称 */
    @Excel(name = "中文名称")
    private String nameZh;

    /** 英文名称 */
    @Excel(name = "英文名称")
    private String nameEn;

    /** 价格 */
    @Excel(name = "价格")
    private BigDecimal price;

    /** 自动升级需邀请人数 */
    @Excel(name = "自动升级需邀请人数")
    private Integer autoUpgradeInviteCount;

    /** 佣金比例（%） */
    @Excel(name = "佣金比例", readConverterExp = "%=")
    private BigDecimal commissionRatio;

    /** 连单佣金比例（%） */
    @Excel(name = "连单佣金比例", readConverterExp = "%=")
    private BigDecimal streakCommissionRatio;

    /** 最低余额 */
    @Excel(name = "最低余额")
    private BigDecimal minBalance;

    /** 接单次数 */
    @Excel(name = "接单次数")
    private Integer orderCount;

    /** 提现次数 */
    @Excel(name = "提现次数")
    private Integer withdrawCount;

    /** 提现限额 */
    @Excel(name = "提现限额")
    private BigDecimal withdrawLimit;

    /** 最低提现金额 */
    @Excel(name = "最低提现金额")
    private BigDecimal minWithdrawAmount;

    /** 最高提现金额 */
    @Excel(name = "最高提现金额")
    private BigDecimal maxWithdrawAmount;

    /** 提现手续费（%） */
    @Excel(name = "提现手续费", readConverterExp = "%=")
    private BigDecimal withdrawFee;

    /** 每天多少单可以提现 */
    @Excel(name = "每天多少单可以提现")
    private Double withdrawOrderPerDay;

    /** 中文描述 */
    @Excel(name = "中文描述")
    private String descriptionZh;

    /** 英文描述 */
    @Excel(name = "英文描述")
    private String descriptionEn;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setIcon(String icon) 
    {
        this.icon = icon;
    }

    public String getIcon() 
    {
        return icon;
    }

    public void setNameZh(String nameZh) 
    {
        this.nameZh = nameZh;
    }

    public String getNameZh() 
    {
        return nameZh;
    }

    public void setNameEn(String nameEn) 
    {
        this.nameEn = nameEn;
    }

    public String getNameEn() 
    {
        return nameEn;
    }

    public void setPrice(BigDecimal price) 
    {
        this.price = price;
    }

    public BigDecimal getPrice() 
    {
        return price;
    }

    public void setAutoUpgradeInviteCount(Integer autoUpgradeInviteCount) 
    {
        this.autoUpgradeInviteCount = autoUpgradeInviteCount;
    }

    public Integer getAutoUpgradeInviteCount() 
    {
        return autoUpgradeInviteCount;
    }

    public void setCommissionRatio(BigDecimal commissionRatio) 
    {
        this.commissionRatio = commissionRatio;
    }

    public BigDecimal getCommissionRatio() 
    {
        return commissionRatio;
    }

    public void setStreakCommissionRatio(BigDecimal streakCommissionRatio) 
    {
        this.streakCommissionRatio = streakCommissionRatio;
    }

    public BigDecimal getStreakCommissionRatio() 
    {
        return streakCommissionRatio;
    }

    public void setMinBalance(BigDecimal minBalance) 
    {
        this.minBalance = minBalance;
    }

    public BigDecimal getMinBalance() 
    {
        return minBalance;
    }

    public void setOrderCount(Integer orderCount) 
    {
        this.orderCount = orderCount;
    }

    public Integer getOrderCount() 
    {
        return orderCount;
    }

    public void setWithdrawCount(Integer withdrawCount) 
    {
        this.withdrawCount = withdrawCount;
    }

    public Integer getWithdrawCount() 
    {
        return withdrawCount;
    }

    public void setWithdrawLimit(BigDecimal withdrawLimit) 
    {
        this.withdrawLimit = withdrawLimit;
    }

    public BigDecimal getWithdrawLimit() 
    {
        return withdrawLimit;
    }

    public void setMinWithdrawAmount(BigDecimal minWithdrawAmount) 
    {
        this.minWithdrawAmount = minWithdrawAmount;
    }

    public BigDecimal getMinWithdrawAmount() 
    {
        return minWithdrawAmount;
    }

    public void setMaxWithdrawAmount(BigDecimal maxWithdrawAmount) 
    {
        this.maxWithdrawAmount = maxWithdrawAmount;
    }

    public BigDecimal getMaxWithdrawAmount() 
    {
        return maxWithdrawAmount;
    }

    public void setWithdrawFee(BigDecimal withdrawFee) 
    {
        this.withdrawFee = withdrawFee;
    }

    public BigDecimal getWithdrawFee() 
    {
        return withdrawFee;
    }

    public void setWithdrawOrderPerDay(Double withdrawOrderPerDay) 
    {
        this.withdrawOrderPerDay = withdrawOrderPerDay;
    }

    public Double getWithdrawOrderPerDay() 
    {
        return withdrawOrderPerDay;
    }

    public void setDescriptionZh(String descriptionZh) 
    {
        this.descriptionZh = descriptionZh;
    }

    public String getDescriptionZh() 
    {
        return descriptionZh;
    }

    public void setDescriptionEn(String descriptionEn) 
    {
        this.descriptionEn = descriptionEn;
    }

    public String getDescriptionEn() 
    {
        return descriptionEn;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("icon", getIcon())
            .append("nameZh", getNameZh())
            .append("nameEn", getNameEn())
            .append("price", getPrice())
            .append("autoUpgradeInviteCount", getAutoUpgradeInviteCount())
            .append("commissionRatio", getCommissionRatio())
            .append("streakCommissionRatio", getStreakCommissionRatio())
            .append("minBalance", getMinBalance())
            .append("orderCount", getOrderCount())
            .append("withdrawCount", getWithdrawCount())
            .append("withdrawLimit", getWithdrawLimit())
            .append("minWithdrawAmount", getMinWithdrawAmount())
            .append("maxWithdrawAmount", getMaxWithdrawAmount())
            .append("withdrawFee", getWithdrawFee())
            .append("withdrawOrderPerDay", getWithdrawOrderPerDay())
            .append("descriptionZh", getDescriptionZh())
            .append("descriptionEn", getDescriptionEn())
            .append("createTime", getCreateTime())
            .toString();
    }
}
