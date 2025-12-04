package com.order.member.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.order.common.annotation.Excel;
import com.order.common.core.domain.BaseEntity;

/**
 * 类目管理对象 goods_type
 * 
 * @author order
 * @date 2025-10-11
 */
public class GoodsType extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 标题 */
    @Excel(name = "标题")
    private String title;

    /** 是否启用 */
    @Excel(name = "是否启用")
    private String isEnabled;

    /** 序号 */
    @Excel(name = "序号")
    private String serialNumber;

    /** 图片 */
    @Excel(name = "图片")
    private String image;

    /** 二级标题 */
    @Excel(name = "二级标题")
    private String subTitle;

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

    public void setTitle(String title) 
    {
        this.title = title;
    }

    public String getTitle() 
    {
        return title;
    }

    public void setIsEnabled(String isEnabled) 
    {
        this.isEnabled = isEnabled;
    }

    public String getIsEnabled() 
    {
        return isEnabled;
    }

    public void setSerialNumber(String serialNumber) 
    {
        this.serialNumber = serialNumber;
    }

    public String getSerialNumber() 
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
            .append("title", getTitle())
            .append("isEnabled", getIsEnabled())
            .append("serialNumber", getSerialNumber())
            .append("image", getImage())
            .append("subTitle", getSubTitle())
            .append("remarks", getRemarks())
            .toString();
    }
}
