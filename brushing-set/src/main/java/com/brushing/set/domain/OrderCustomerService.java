package com.brushing.set.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.brushing.common.annotation.Excel;
import com.brushing.common.core.domain.BaseEntity;

/**
 * 客服管理对象 order_customer_service
 * 
 * @author brushing
 * @date 2025-07-31
 */
public class OrderCustomerService extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 客服名称 */
    @Excel(name = "客服名称")
    private String name;

    /** 跳转地址 */
    @Excel(name = "跳转地址")
    private String linkUrl;

    /** 图标URL */
    @Excel(name = "图标URL")
    private String iconUrl;

    /** 排序 */
    @Excel(name = "排序")
    private Long sort;

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

    public void setName(String name) 
    {
        this.name = name;
    }

    public String getName() 
    {
        return name;
    }

    public void setLinkUrl(String linkUrl) 
    {
        this.linkUrl = linkUrl;
    }

    public String getLinkUrl() 
    {
        return linkUrl;
    }

    public void setIconUrl(String iconUrl) 
    {
        this.iconUrl = iconUrl;
    }

    public String getIconUrl() 
    {
        return iconUrl;
    }

    public void setSort(Long sort) 
    {
        this.sort = sort;
    }

    public Long getSort() 
    {
        return sort;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("name", getName())
            .append("linkUrl", getLinkUrl())
            .append("iconUrl", getIconUrl())
            .append("sort", getSort())
            .append("status", getStatus())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
