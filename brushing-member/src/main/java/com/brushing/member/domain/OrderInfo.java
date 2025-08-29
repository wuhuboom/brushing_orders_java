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
    private Long id;

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
    private Integer quantity;

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
    @JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.NUMBER)
    private Date orderTime;

    /** 提交时间 */
    @JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.NUMBER)
    private Date submitTime;

    /** 订单类型 */
    @Excel(name = "订单类型")
    private String orderType;

    /** 订单状态 */
    @Excel(name = "订单状态")
    private String status;

    /**
     * 商品价格
     */
    private BigDecimal price;

    private String username;
    private String phone;
    private Integer dealCount;

    /**
     * 连单id
     */
    private Long seriesId;

    public Long getSeriesId() {
        return seriesId;
    }

    public void setSeriesId(Long seriesId) {
        this.seriesId = seriesId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
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

    public Integer getDealCount() {
        return dealCount;
    }

    public void setDealCount(Integer dealCount) {
        this.dealCount = dealCount;
    }

    private OrderGoods product;

    public OrderGoods getProduct() {
        return product;
    }

    public void setProduct(OrderGoods product) {
        this.product = product;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
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
