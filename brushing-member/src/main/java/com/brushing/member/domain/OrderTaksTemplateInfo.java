package com.brushing.member.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.brushing.common.annotation.Excel;
import com.brushing.common.core.domain.BaseEntity;

/**
 * 连单模板任务
对象 order_taks_template_info
 *
 * @author brushing
 * @date 2026-01-10
 */
public class OrderTaksTemplateInfo extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 模板id */
    @Excel(name = "模板id")
    private Long templateId;

    /** 商品ID */
    @Excel(name = "商品ID")
    private Long productId;

    /** 价格 */
    @Excel(name = "价格")
    private BigDecimal price;

    /** 佣金倍数 */
    @Excel(name = "佣金倍数")
    private Integer commissionRatio;

    /** 第几单 */
    @Excel(name = "第几单")
    private Integer orderIndex;

    /** 状态 */
    @Excel(name = "状态")
    private String status;

    /** 价格类型 */
    @Excel(name = "价格类型")
    private String type;

    /** 模板名称（关联 order_task_template.name） */
    private String templateName;

    /** 商品名称（关联 order_goods.name） */
    private String productName;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }

    public void setTemplateId(Long templateId)
    {
        this.templateId = templateId;
    }

    public Long getTemplateId()
    {
        return templateId;
    }

    public void setProductId(Long productId)
    {
        this.productId = productId;
    }

    public Long getProductId()
    {
        return productId;
    }

    public void setPrice(BigDecimal price)
    {
        this.price = price;
    }

    public BigDecimal getPrice()
    {
        return price;
    }

    public Integer getCommissionRatio() {
        return commissionRatio;
    }

    public void setCommissionRatio(Integer commissionRatio) {
        this.commissionRatio = commissionRatio;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getStatus()
    {
        return status;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getType()
    {
        return type;
    }

    // new getters/setters for joined names
    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("templateId", getTemplateId())
            .append("templateName", getTemplateName())
            .append("productId", getProductId())
            .append("productName", getProductName())
            .append("price", getPrice())
            .append("commissionRatio", getCommissionRatio())
            .append("orderIndex", getOrderIndex())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("type", getType())
            .toString();
    }
}
