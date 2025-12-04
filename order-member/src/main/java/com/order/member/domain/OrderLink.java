package com.order.member.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.order.member.domain.dto.GoodsDetails;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.order.common.annotation.Excel;
import com.order.common.core.domain.BaseEntity;

/**
 * 连单对象 order_link
 * 
 * @author order
 * @date 2025-10-29
 */
public class OrderLink extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 自增ID */
    private Long id;

    /** 连单ID */
    @Excel(name = "连单ID")
    private Long linkOrderId;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 单数 */
    @Excel(name = "单数")
    private Integer orderCount;

    /** 返佣倍数 */
    @Excel(name = "返佣倍数")
    private Integer commissionMultiple;

    /** 商品ID */
    @Excel(name = "商品ID")
    private Long productId;

    /** 商品标题（查询用，不入库） */
    private String productTitle;

    /** 商品图片（查询用，不入库） */
    private String productImage;

    /** 价格类型 */
    @Excel(name = "价格类型")
    private String priceType;

    /** 价格 */
    @Excel(name = "价格")
    private BigDecimal price;

    /** 状态 */
    @Excel(name = "状态")
    private String status;



    private List<GoodsDetails>  details;

    public List<GoodsDetails> getDetails() {
        return details;
    }
    public void setDetails(List<GoodsDetails> details) {
        this.details = details;
    }

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setLinkOrderId(Long linkOrderId) 
    {
        this.linkOrderId = linkOrderId;
    }

    public Long getLinkOrderId() 
    {
        return linkOrderId;
    }

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public Integer getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Integer orderCount) {
        this.orderCount = orderCount;
    }

    public Integer getCommissionMultiple() {
        return commissionMultiple;
    }

    public void setCommissionMultiple(Integer commissionMultiple) {
        this.commissionMultiple = commissionMultiple;
    }

    public void setProductId(Long productId)
    {
        this.productId = productId;
    }

    public Long getProductId() 
    {
        return productId;
    }

    public String getProductTitle() {
        return productTitle;
    }

    public void setProductTitle(String productTitle) {
        this.productTitle = productTitle;
    }

    public String getProductImage() {
        return productImage;
    }

    public void setProductImage(String productImage) {
        this.productImage = productImage;
    }

    public void setPriceType(String priceType)
    {
        this.priceType = priceType;
    }

    public String getPriceType() 
    {
        return priceType;
    }

    public void setPrice(BigDecimal price) 
    {
        this.price = price;
    }

    public BigDecimal getPrice() 
    {
        return price;
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
            .append("linkOrderId", getLinkOrderId())
            .append("userId", getUserId())
            .append("orderCount", getOrderCount())
            .append("commissionMultiple", getCommissionMultiple())
            .append("productId", getProductId())
            .append("productTitle", getProductTitle())
            .append("productImage", getProductImage())
            .append("priceType", getPriceType())
            .append("price", getPrice())
            .append("status", getStatus())
            .toString();
    }
}
