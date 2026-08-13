package com.brushing.member.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.brushing.common.annotation.Excel;
import com.brushing.common.core.domain.BaseEntity;

/**
 * 充值地址对象 order_recharge_address
 *
 * @author brushing
 * @date 2026-01-08
 */
public class OrderRechargeAddress extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 名称 */
    @Excel(name = "名称")
    private String name;

    /** 二维码 */
    @Excel(name = "二维码")
    private String qrCode;

    /** 地址 */
    @Excel(name = "地址")
    private String url;

    /** 状态 */
    @Excel(name = "状态")
    private String status;

    /** 排序 */
    @Excel(name = "排序")
    private Long sort;

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

    public void setQrCode(String qrCode)
    {
        this.qrCode = qrCode;
    }

    public String getQrCode()
    {
        return qrCode;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getUrl()
    {
        return url;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getStatus()
    {
        return status;
    }

    public void setSort(Long sort)
    {
        this.sort = sort;
    }

    public Long getSort()
    {
        return sort;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("name", getName())
            .append("qrCode", getQrCode())
            .append("url", getUrl())
            .append("status", getStatus())
            .append("sort", getSort())
            .append("createTime", getCreateTime())
            .toString();
    }
}
