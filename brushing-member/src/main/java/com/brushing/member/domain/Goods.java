package com.brushing.member.domain;

import com.brushing.common.annotation.Excel;
import com.brushing.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.math.BigDecimal;

/**
 * 商品对象 goods
 * 
 * @author order
 * @date 2025-10-11
 */
public class Goods extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 标题 */
    @Excel(name = "标题")
    private String title;

    /** 类目 */
    @Excel(name = "类目")
    private Long typeId;

    /** 是否启用 */
    @Excel(name = "是否启用")
    private String isEnabled;


    private String typeTitle;

    /** 价格 */
    @Excel(name = "价格")
    private BigDecimal price;

    /** 序号 */
    @Excel(name = "序号")
    private Long serialNumber;

    /** 图片 */
    @Excel(name = "图片")
    private String image;

    /** 二级标题 */
    @Excel(name = "二级标题")
    private String subTitle;

    /** 单价 */
    @Excel(name = "单价")
    private BigDecimal unitPrice;

    /** 数量 */
    @Excel(name = "数量")
    private Integer quantity;

    /** 星级 */
    @Excel(name = "星级")
    private Integer starRating;

    /** 评分 */
    @Excel(name = "评分")
    private BigDecimal rating;

    /** 说明 */
    @Excel(name = "说明")
    private String description;

    public String getTypeTitle() {
        return typeTitle;
    }

    public void setTypeTitle(String typeTitle) {
        this.typeTitle = typeTitle;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setTitle(String title) 
    {
        this.title = title;
    }

    public String getTitle() 
    {
        return title;
    }

    public void setTypeId(Long typeId) 
    {
        this.typeId = typeId;
    }

    public Long getTypeId() 
    {
        return typeId;
    }

    public void setIsEnabled(String isEnabled) 
    {
        this.isEnabled = isEnabled;
    }

    public String getIsEnabled() 
    {
        return isEnabled;
    }

    public void setPrice(BigDecimal price) 
    {
        this.price = price;
    }

    public BigDecimal getPrice() 
    {
        return price;
    }

    public void setSerialNumber(Long serialNumber) 
    {
        this.serialNumber = serialNumber;
    }

    public Long getSerialNumber() 
    {
        return serialNumber;
    }

    public void setImage(String image) 
    {
        this.image = image;
    }

    public String getImage() 
    {
        return image;
    }

    public void setSubTitle(String subTitle) 
    {
        this.subTitle = subTitle;
    }

    public String getSubTitle() 
    {
        return subTitle;
    }

    public void setUnitPrice(BigDecimal unitPrice) 
    {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getUnitPrice() 
    {
        return unitPrice;
    }

    public void setQuantity(Integer quantity) 
    {
        this.quantity = quantity;
    }

    public Integer getQuantity() 
    {
        return quantity;
    }

    public void setStarRating(Integer starRating) 
    {
        this.starRating = starRating;
    }

    public Integer getStarRating() 
    {
        return starRating;
    }

    public void setRating(BigDecimal rating) 
    {
        this.rating = rating;
    }

    public BigDecimal getRating() 
    {
        return rating;
    }

    public void setDescription(String description) 
    {
        this.description = description;
    }

    public String getDescription() 
    {
        return description;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("title", getTitle())
            .append("typeId", getTypeId())
            .append("isEnabled", getIsEnabled())
            .append("price", getPrice())
            .append("serialNumber", getSerialNumber())
            .append("image", getImage())
            .append("subTitle", getSubTitle())
            .append("unitPrice", getUnitPrice())
            .append("quantity", getQuantity())
            .append("starRating", getStarRating())
            .append("rating", getRating())
            .append("description", getDescription())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .toString();
    }
}
