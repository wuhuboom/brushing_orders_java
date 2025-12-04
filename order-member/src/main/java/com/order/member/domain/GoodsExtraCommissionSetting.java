package com.order.member.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.order.common.annotation.Excel;
import com.order.common.core.domain.BaseEntity;

/**
 * 额外佣金设置对象 goods_extra_commission_setting
 * 
 * @author order
 * @date 2025-11-08
 */
public class GoodsExtraCommissionSetting extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 用户id */
    @Excel(name = "用户id")
    private Long userId;

    /** 订单数 */
    @Excel(name = "订单数")
    private Integer orderCount;

    /** 商品价格 */
    @Excel(name = "商品价格")
    private BigDecimal productPrice;

    /** 金额 */
    @Excel(name = "金额")
    private BigDecimal amount;

    /** 是否锁定 */
    @Excel(name = "是否锁定")
    private String isLocked;

    /** 状态 */
    @Excel(name = "状态")
    private String status;

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

    public void setOrderCount(Integer orderCount) 
    {
        this.orderCount = orderCount;
    }

    public Integer getOrderCount() 
    {
        return orderCount;
    }

    public void setProductPrice(BigDecimal productPrice) 
    {
        this.productPrice = productPrice;
    }

    public BigDecimal getProductPrice() 
    {
        return productPrice;
    }

    public void setAmount(BigDecimal amount) 
    {
        this.amount = amount;
    }

    public BigDecimal getAmount() 
    {
        return amount;
    }

    public void setIsLocked(String isLocked) 
    {
        this.isLocked = isLocked;
    }

    public String getIsLocked() 
    {
        return isLocked;
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
            .append("userId", getUserId())
            .append("orderCount", getOrderCount())
            .append("productPrice", getProductPrice())
            .append("amount", getAmount())
            .append("isLocked", getIsLocked())
            .append("status", getStatus())
            .append("createTime", getCreateTime())
            .toString();
    }
}
