package com.order.member.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.order.common.annotation.Excel;
import com.order.common.core.domain.BaseEntity;

/**
 * 等级对象 goods_member_level
 *
 * @author order
 * @date 2025-10-12
 */
public class GoodsMemberLevel extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 名称 */
    @Excel(name = "名称")
    private String name;

    /** 级别 */
    @Excel(name = "级别")
    private Long level;

    /** 图标 */
    @Excel(name = "图标")
    private String icon;

    /** 会员等级价格 */
    @Excel(name = "会员等级价格")
    private BigDecimal price;

    /** 最低余额 */
    @Excel(name = "最低余额")
    private BigDecimal minBalance;

    /** 自动升级所需邀请人数 */
    @Excel(name = "自动升级所需邀请人数")
    private Long inviteCount;

    /** 接单次数/天 */
    @Excel(name = "接单次数/天")
    private Long orderCountPerDay;

    /** 最低返佣百分比 */
    @Excel(name = "最低返佣百分比")
    private BigDecimal minCommissionRate;

    /** 最高返佣百分比 */
    @Excel(name = "最高返佣百分比")
    private BigDecimal maxCommissionRate;

    /** 最低连单返佣百分比 */
    @Excel(name = "最低连单返佣百分比")
    private BigDecimal minContinuousCommissionRate;

    /** 最高连单返佣百分比 */
    @Excel(name = "最高连单返佣百分比")
    private BigDecimal maxContinuousCommissionRate;

    /** 任务完成组数/天 */
    @Excel(name = "任务完成组数/天")
    private Long taskCountPerDay;

    /** 提现次数/天 */
    @Excel(name = "提现次数/天")
    private Long withdrawCountPerDay;

    /** 提现手续费率 */
    @Excel(name = "提现手续费率")
    private BigDecimal withdrawFeeRate;

    /** 提现最低单数 */
    @Excel(name = "提现最低单数")
    private BigDecimal minWithdrawAmount;

    /** 提现限额/天 */
    @Excel(name = "提现限额/天")
    private BigDecimal withdrawLimitPerDay;

    /** 最低提现金额 */
    @Excel(name = "最低提现金额")
    private BigDecimal minWithdraw;

    /** 最高提现金额 */
    @Excel(name = "最高提现金额")
    private BigDecimal maxWithdraw;

    /** 描述 */
    @Excel(name = "描述")
    private String description;

    /** 产品匹配状态 */
    @Excel(name = "产品匹配状态")
    private String productMatchEnabled;

    /** 匹配最小比列 */
    @Excel(name = "匹配最小比列")
    private BigDecimal productMatchMin;

    /** 匹配最大比列 */
    @Excel(name = "匹配最大比列")
    private BigDecimal productMatchMax;

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

    public void setLevel(Long level)
    {
        this.level = level;
    }

    public Long getLevel()
    {
        return level;
    }

    public void setIcon(String icon)
    {
        this.icon = icon;
    }

    public String getIcon()
    {
        return icon;
    }

    public void setPrice(BigDecimal price)
    {
        this.price = price;
    }

    public BigDecimal getPrice()
    {
        return price;
    }

    public void setMinBalance(BigDecimal minBalance)
    {
        this.minBalance = minBalance;
    }

    public BigDecimal getMinBalance()
    {
        return minBalance;
    }

    public void setInviteCount(Long inviteCount)
    {
        this.inviteCount = inviteCount;
    }

    public Long getInviteCount()
    {
        return inviteCount;
    }

    public void setOrderCountPerDay(Long orderCountPerDay)
    {
        this.orderCountPerDay = orderCountPerDay;
    }

    public Long getOrderCountPerDay()
    {
        return orderCountPerDay;
    }

    public void setMinCommissionRate(BigDecimal minCommissionRate)
    {
        this.minCommissionRate = minCommissionRate;
    }

    public BigDecimal getMinCommissionRate()
    {
        return minCommissionRate;
    }

    public void setMaxCommissionRate(BigDecimal maxCommissionRate)
    {
        this.maxCommissionRate = maxCommissionRate;
    }

    public BigDecimal getMaxCommissionRate()
    {
        return maxCommissionRate;
    }

    public void setMinContinuousCommissionRate(BigDecimal minContinuousCommissionRate)
    {
        this.minContinuousCommissionRate = minContinuousCommissionRate;
    }

    public BigDecimal getMinContinuousCommissionRate()
    {
        return minContinuousCommissionRate;
    }

    public void setMaxContinuousCommissionRate(BigDecimal maxContinuousCommissionRate)
    {
        this.maxContinuousCommissionRate = maxContinuousCommissionRate;
    }

    public BigDecimal getMaxContinuousCommissionRate()
    {
        return maxContinuousCommissionRate;
    }

    public void setTaskCountPerDay(Long taskCountPerDay)
    {
        this.taskCountPerDay = taskCountPerDay;
    }

    public Long getTaskCountPerDay()
    {
        return taskCountPerDay;
    }

    public void setWithdrawCountPerDay(Long withdrawCountPerDay)
    {
        this.withdrawCountPerDay = withdrawCountPerDay;
    }

    public Long getWithdrawCountPerDay()
    {
        return withdrawCountPerDay;
    }

    public void setWithdrawFeeRate(BigDecimal withdrawFeeRate)
    {
        this.withdrawFeeRate = withdrawFeeRate;
    }

    public BigDecimal getWithdrawFeeRate()
    {
        return withdrawFeeRate;
    }

    public void setMinWithdrawAmount(BigDecimal minWithdrawAmount)
    {
        this.minWithdrawAmount = minWithdrawAmount;
    }

    public BigDecimal getMinWithdrawAmount()
    {
        return minWithdrawAmount;
    }

    public void setWithdrawLimitPerDay(BigDecimal withdrawLimitPerDay)
    {
        this.withdrawLimitPerDay = withdrawLimitPerDay;
    }

    public BigDecimal getWithdrawLimitPerDay()
    {
        return withdrawLimitPerDay;
    }

    public void setMinWithdraw(BigDecimal minWithdraw)
    {
        this.minWithdraw = minWithdraw;
    }

    public BigDecimal getMinWithdraw()
    {
        return minWithdraw;
    }

    public void setMaxWithdraw(BigDecimal maxWithdraw)
    {
        this.maxWithdraw = maxWithdraw;
    }

    public BigDecimal getMaxWithdraw()
    {
        return maxWithdraw;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getDescription()
    {
        return description;
    }

    public void setProductMatchEnabled(String productMatchEnabled)
    {
        this.productMatchEnabled = productMatchEnabled;
    }

    public String getProductMatchEnabled()
    {
        return productMatchEnabled;
    }

    public void setProductMatchMin(BigDecimal productMatchMin)
    {
        this.productMatchMin = productMatchMin;
    }

    public BigDecimal getProductMatchMin()
    {
        return productMatchMin;
    }

    public void setProductMatchMax(BigDecimal productMatchMax)
    {
        this.productMatchMax = productMatchMax;
    }

    public BigDecimal getProductMatchMax()
    {
        return productMatchMax;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("name", getName())
                .append("level", getLevel())
                .append("icon", getIcon())
                .append("price", getPrice())
                .append("minBalance", getMinBalance())
                .append("inviteCount", getInviteCount())
                .append("orderCountPerDay", getOrderCountPerDay())
                .append("minCommissionRate", getMinCommissionRate())
                .append("maxCommissionRate", getMaxCommissionRate())
                .append("minContinuousCommissionRate", getMinContinuousCommissionRate())
                .append("maxContinuousCommissionRate", getMaxContinuousCommissionRate())
                .append("taskCountPerDay", getTaskCountPerDay())
                .append("withdrawCountPerDay", getWithdrawCountPerDay())
                .append("withdrawFeeRate", getWithdrawFeeRate())
                .append("minWithdrawAmount", getMinWithdrawAmount())
                .append("withdrawLimitPerDay", getWithdrawLimitPerDay())
                .append("minWithdraw", getMinWithdraw())
                .append("maxWithdraw", getMaxWithdraw())
                .append("description", getDescription())
                .append("createTime", getCreateTime())
                .append("productMatchEnabled", getProductMatchEnabled())
                .append("productMatchMin", getProductMatchMin())
                .append("productMatchMax", getProductMatchMax())
                .toString();
    }
}
