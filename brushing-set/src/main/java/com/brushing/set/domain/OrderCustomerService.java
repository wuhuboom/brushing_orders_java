package com.brushing.set.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.brushing.common.annotation.Excel;
import com.brushing.common.core.domain.BaseEntity;

/**
 * 客服管理对象 order_customer_service
 *
 * @author brushing
 * @date 2025-10-19
 */
public class OrderCustomerService extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 客服名称 */
    @Excel(name = "客服名称")
    private String name;

    /** 跳转地址（如客服聊天链接） */
    @Excel(name = "跳转地址", readConverterExp = "如=客服聊天链接")
    private String linkUrl;

    /** 图标URL */
    @Excel(name = "图标URL")
    private String iconUrl;

    /** 排序，数值越小越靠前 */
    @Excel(name = "排序，数值越小越靠前")
    private Long sort;

    /** 状态 */
    @Excel(name = "状态")
    private String status;

    /** 中文名称 */
    @Excel(name = "中文名称")
    private String nameZh;

    /** 日文名称 */
    @Excel(name = "日文名称")
    private String nameJp;

    /** 韩文名称 */
    @Excel(name = "韩文名称")
    private String nameKo;

    /** 泰文名称 */
    @Excel(name = "泰文名称")
    private String nameTh;

    @Excel(name = "葡萄牙")
    private String namePor;

    @Excel(name = "西班牙语名称")
    private String nameEs;

    /** 中文繁体名称 */
    @Excel(name = "中文繁体名称")
    private String nameZhTw;

    public String getNamePor() {
        return namePor;
    }

    public void setNamePor(String namePor) {
        this.namePor = namePor;
    }

    public String getNameEs() {
        return nameEs;
    }

    public void setNameEs(String nameEs) {
        this.nameEs = nameEs;
    }

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

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getStatus()
    {
        return status;
    }

    public void setNameZh(String nameZh)
    {
        this.nameZh = nameZh;
    }

    public String getNameZh()
    {
        return nameZh;
    }

    public void setNameJp(String nameJp)
    {
        this.nameJp = nameJp;
    }

    public String getNameJp()
    {
        return nameJp;
    }

    public void setNameKo(String nameKo)
    {
        this.nameKo = nameKo;
    }

    public String getNameKo()
    {
        return nameKo;
    }

    public void setNameTh(String nameTh)
    {
        this.nameTh = nameTh;
    }

    public String getNameTh()
    {
        return nameTh;
    }

    public void setNameZhTw(String nameZhTw)
    {
        this.nameZhTw = nameZhTw;
    }

    public String getNameZhTw()
    {
        return nameZhTw;
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
                .append("nameZh", getNameZh())
                .append("nameJp", getNameJp())
                .append("nameKo", getNameKo())
                .append("nameTh", getNameTh())
                .append("nameZhTw", getNameZhTw())
                .toString();
    }
}
