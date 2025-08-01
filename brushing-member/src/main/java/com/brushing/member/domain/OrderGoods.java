package com.brushing.member.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.brushing.common.annotation.Excel;
import com.brushing.common.core.domain.BaseEntity;

/**
 * 商品列表对象 order_goods
 * 
 * @author brushing
 * @date 2025-08-01
 */
public class OrderGoods extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private String id;

    /** 商品名称 */
    @Excel(name = "商品名称")
    private String name;

    /** 商品封面图URL */
    @Excel(name = "商品封面图URL")
    private String coverUrl;

    /** 商品价格 */
    @Excel(name = "商品价格")
    private BigDecimal price;

    /** 商品描述 */
    @Excel(name = "商品描述")
    private String description;

    /** 状态 */
    @Excel(name = "状态")
    private String status;

    private String typeName;

    private Long typeId;

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public Long getTypeId() {
        return typeId;
    }

    public void setTypeId(Long typeId) {
        this.typeId = typeId;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getId() 
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

    public void setCoverUrl(String coverUrl) 
    {
        this.coverUrl = coverUrl;
    }

    public String getCoverUrl() 
    {
        return coverUrl;
    }

    public void setPrice(BigDecimal price) 
    {
        this.price = price;
    }

    public BigDecimal getPrice() 
    {
        return price;
    }

    public void setDescription(String description) 
    {
        this.description = description;
    }

    public String getDescription() 
    {
        return description;
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
            .append("name", getName())
            .append("coverUrl", getCoverUrl())
            .append("price", getPrice())
            .append("description", getDescription())
            .append("status", getStatus())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
