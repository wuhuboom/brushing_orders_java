package com.order.member.domain;

import com.order.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.order.common.annotation.Excel;

/**
 * 横幅：用于存储横幅广告相关信息对象 goods_banner
 * 
 * @author order
 * @date 2025-11-11
 */
public class GoodsBanner extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 名称 */
    @Excel(name = "名称")
    private String name;

    /** 类型 */
    @Excel(name = "类型")
    private String type;

    /** 是否启用 */
    @Excel(name = "是否启用")
    private String isEnabled;

    /** 序号 */
    @Excel(name = "序号")
    private Long sortOrder;

    /** 链接 */
    @Excel(name = "链接")
    private String link;

    /** 媒体 */
    @Excel(name = "媒体")
    private String media;

    /** 备注 */
    @Excel(name = "备注")
    private String remarks;

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

    public void setType(String type) 
    {
        this.type = type;
    }

    public String getType() 
    {
        return type;
    }

    public void setIsEnabled(String isEnabled) 
    {
        this.isEnabled = isEnabled;
    }

    public String getIsEnabled() 
    {
        return isEnabled;
    }

    public void setSortOrder(Long sortOrder) 
    {
        this.sortOrder = sortOrder;
    }

    public Long getSortOrder() 
    {
        return sortOrder;
    }

    public void setLink(String link) 
    {
        this.link = link;
    }

    public String getLink() 
    {
        return link;
    }

    public void setMedia(String media) 
    {
        this.media = media;
    }

    public String getMedia() 
    {
        return media;
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
            .append("type", getType())
            .append("isEnabled", getIsEnabled())
            .append("sortOrder", getSortOrder())
            .append("link", getLink())
            .append("media", getMedia())
            .append("remarks", getRemarks())
            .toString();
    }
}
