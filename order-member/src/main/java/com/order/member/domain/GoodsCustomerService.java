package com.order.member.domain;

import com.order.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.order.common.annotation.Excel;

/**
 * 客服对象 goods_customer_service
 * 
 * @author order
 * @date 2025-11-11
 */
public class GoodsCustomerService extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** ID */
    private String id;

    /** 名称 */
    @Excel(name = "名称")
    private String name;

    /** 序号 */
    @Excel(name = "序号")
    private Long sortOrder;

    /** 图片 */
    @Excel(name = "图片")
    private String image;

    /** 是否启用 */
    @Excel(name = "是否启用")
    private String isEnabled;

    /** 链接 */
    @Excel(name = "链接")
    private String link;

    /** 备注 */
    @Excel(name = "备注")
    private String remarks;

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

    public void setSortOrder(Long sortOrder) 
    {
        this.sortOrder = sortOrder;
    }

    public Long getSortOrder() 
    {
        return sortOrder;
    }

    public void setImage(String image) 
    {
        this.image = image;
    }

    public String getImage() 
    {
        return image;
    }

    public void setIsEnabled(String isEnabled) 
    {
        this.isEnabled = isEnabled;
    }

    public String getIsEnabled() 
    {
        return isEnabled;
    }

    public void setLink(String link) 
    {
        this.link = link;
    }

    public String getLink() 
    {
        return link;
    }

    public void setRemarks(String remarks) 
    {
        this.remarks = remarks;
    }

    public String getRemarks() 
    {
        return remarks;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("name", getName())
            .append("sortOrder", getSortOrder())
            .append("image", getImage())
            .append("isEnabled", getIsEnabled())
            .append("link", getLink())
            .append("remarks", getRemarks())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .toString();
    }
}
