package com.brushing.member.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.brushing.common.annotation.Excel;
import com.brushing.common.core.domain.BaseEntity;

/**
 * 订单列表对象 order_info
 * 
 * @author brushing
 * @date 2025-08-02
 */
public class OrderInfo extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private String id;

    /** 订单号 */
    @Excel(name = "订单号")
    private String orderNo;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 商品ID */
    @Excel(name = "商品ID")
    private Long productId;

    /** 商品数量 */
    @Excel(name = "商品数量")
    private Long quantity;

    /** 佣金金额 */
    @Excel(name = "佣金金额")
    private BigDecimal commission;

    /** 佣金比例（%） */
    @Excel(name = "佣金比例", readConverterExp = "%=")
    private BigDecimal commissionRate;

    /** 佣金发放状态 */
    @Excel(name = "佣金发放状态")
    private String commissionStatus;

    /** 下单时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "下单时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date orderTime;

    /** 提交时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "提交时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date submitTime;

    /** 订单类型 */
    @Excel(name = "订单类型")
    private String orderType;

    /** 订单状态 */
    @Excel(name = "订单状态")
    private String status;

    public void setId(String id) 
    {
        this.id = id;
    }

    public String getId() 
    {
        return id;
    }

    public void setOrderNo(String orderNo) 
    {
        this.orderNo = orderNo;
    }

    public String getOrderNo() 
    {
        return orderNo;
    }

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setProductId(Long productId) 
    {
        this.productId = productId;
    }

    public Long getProductId() 
    {
        return productId;
    }

    public void setQuantity(Long quantity) 
    {
        this.quantity = quantity;
    }

    public Long getQuantity() 
    {
        return quantity;
    }

    public void setCommission(BigDecimal commission) 
    {
        this.commission = commission;
    }

    public BigDecimal getCommission() 
    {
        return commission;
    }

    public void setCommissionRate(BigDecimal commissionRate) 
    {
        this.commissionRate = commissionRate;
    }

    public BigDecimal getCommissionRate() 
    {
        return commissionRate;
    }

    public void setCommissionStatus(String commissionStatus) 
    {
        this.commissionStatus = commissionStatus;
    }

    public String getCommissionStatus() 
    {
        return commissionStatus;
    }

    public void setOrderTime(Date orderTime) 
    {
        this.orderTime = orderTime;
    }

    public Date getOrderTime() 
    {
        return orderTime;
    }

    public void setSubmitTime(Date submitTime) 
    {
        this.submitTime = submitTime;
    }

    public Date getSubmitTime() 
    {
        return submitTime;
    }

    public void setOrderType(String orderType) 
    {
        this.orderType = orderType;
    }

    public String getOrderType() 
    {
        return orderType;
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
            .append("orderNo", getOrderNo())
            .append("userId", getUserId())
            .append("productId", getProductId())
            .append("quantity", getQuantity())
            .append("commission", getCommission())
            .append("commissionRate", getCommissionRate())
            .append("commissionStatus", getCommissionStatus())
            .append("orderTime", getOrderTime())
            .append("submitTime", getSubmitTime())
            .append("orderType", getOrderType())
            .append("status", getStatus())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
