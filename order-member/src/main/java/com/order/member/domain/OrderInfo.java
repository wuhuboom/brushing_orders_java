package com.order.member.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.order.common.annotation.Excel;
import com.order.common.core.domain.BaseEntity;

/**
 * 订单对象 order_info
 * 
 * @author order
 * @date 2025-11-10
 */
public class OrderInfo extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 订单标号 */
    @Excel(name = "订单标号")
    private String orderNumber;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 类型 */
    @Excel(name = "类型")
    private String type;

    /** 单数 */
    @Excel(name = "单数")
    private Long orderCount;

    /** 金额 */
    @Excel(name = "金额")
    private BigDecimal amount;

    /** 返佣百分比 */
    @Excel(name = "返佣百分比")
    private BigDecimal rebatePercentage;

    /** 返佣 */
    @Excel(name = "返佣")
    private BigDecimal rebate;

    /** 上级返佣百分比 */
    @Excel(name = "上级返佣百分比")
    private BigDecimal upperRebatePercentage;

    /** 上级返佣 */
    @Excel(name = "上级返佣")
    private BigDecimal upperRebate;

    /** 状态 */
    @Excel(name = "状态")
    private String status;

    /** 过期时间 */
    @JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.NUMBER)
    @Excel(name = "过期时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date expiryTime;

    /** 商品ID */
    @Excel(name = "商品ID")
    private Long productId;

    /** 额外佣金ID */
    @Excel(name = "额外佣金ID")
    private Long extraCommissionId;

    /** 备注 */
    @Excel(name = "备注")
    private String remarks;

    /** 评论ID */
    @Excel(name = "评论ID")
    private Long commentId;

    /** 用户名（来自 order_user.username） */
    private String username;

    /** 商品图片（来自 goods.image） */
    private String productImage;

    /** 商品标题（来自 goods.title） */
    private String productTitle;

    private Long linkId;

    public Long getLinkId() {
        return linkId;
    }

    public void setLinkId(Long linkId) {
        this.linkId = linkId;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setOrderNumber(String orderNumber) 
    {
        this.orderNumber = orderNumber;
    }

    public String getOrderNumber() 
    {
        return orderNumber;
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

    public void setOrderCount(Long orderCount) 
    {
        this.orderCount = orderCount;
    }

    public Long getOrderCount() 
    {
        return orderCount;
    }

    public void setAmount(BigDecimal amount) 
    {
        this.amount = amount;
    }

    public BigDecimal getAmount() 
    {
        return amount;
    }

    public BigDecimal getRebatePercentage() {
        return rebatePercentage;
    }

    public void setRebatePercentage(BigDecimal rebatePercentage) {
        this.rebatePercentage = rebatePercentage;
    }

    public BigDecimal getUpperRebatePercentage() {
        return upperRebatePercentage;
    }

    public void setUpperRebatePercentage(BigDecimal upperRebatePercentage) {
        this.upperRebatePercentage = upperRebatePercentage;
    }

    public void setRebate(BigDecimal rebate)
    {
        this.rebate = rebate;
    }

    public BigDecimal getRebate() 
    {
        return rebate;
    }



    public void setUpperRebate(BigDecimal upperRebate) 
    {
        this.upperRebate = upperRebate;
    }

    public BigDecimal getUpperRebate() 
    {
        return upperRebate;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public void setExpiryTime(Date expiryTime) 
    {
        this.expiryTime = expiryTime;
    }

    public Date getExpiryTime() 
    {
        return expiryTime;
    }

    public void setProductId(Long productId) 
    {
        this.productId = productId;
    }

    public Long getProductId() 
    {
        return productId;
    }

    public void setExtraCommissionId(Long extraCommissionId) 
    {
        this.extraCommissionId = extraCommissionId;
    }

    public Long getExtraCommissionId() 
    {
        return extraCommissionId;
    }

    public void setRemarks(String remarks) 
    {
        this.remarks = remarks;
    }

    public String getRemarks() 
    {
        return remarks;
    }

    public void setCommentId(Long commentId) 
    {
        this.commentId = commentId;
    }

    public Long getCommentId() 
    {
        return commentId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getProductImage() {
        return productImage;
    }

    public void setProductImage(String productImage) {
        this.productImage = productImage;
    }

    public String getProductTitle() {
        return productTitle;
    }

    public void setProductTitle(String productTitle) {
        this.productTitle = productTitle;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("orderNumber", getOrderNumber())
            .append("userId", getUserId())
            .append("type", getType())
            .append("orderCount", getOrderCount())
            .append("amount", getAmount())
            .append("rebatePercentage", getRebatePercentage())
            .append("rebate", getRebate())
            .append("upperRebatePercentage", getUpperRebatePercentage())
            .append("upperRebate", getUpperRebate())
            .append("status", getStatus())
            .append("expiryTime", getExpiryTime())
            .append("productId", getProductId())
            .append("extraCommissionId", getExtraCommissionId())
            .append("createTime", getCreateTime())
            .append("remarks", getRemarks())
            .append("commentId", getCommentId())
            .append("username", getUsername())
            .append("productImage", getProductImage())
            .append("productTitle", getProductTitle())
            .toString();
    }
}
