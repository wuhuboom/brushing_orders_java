package com.order.system.domain;

import com.order.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 时区管理对象 sys_time_zone
 * 
 * @author betting
 * @date 2025-08-26
 */
public class SysTimeZone extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** $column.columnComment */
    private Long id;

    /** 显示名称 */
    private String name;

    /** IANA 时区名，如 Asia/Shanghai */
    private String tzName;

    /** 0=在用, 1=停用 */
    private Integer status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getName() 
    {
        return name;
    }

    public void setTzName(String tzName) 
    {
        this.tzName = tzName;
    }

    public String getTzName() 
    {
        return tzName;
    }

    public void setStatus(Integer status) 
    {
        this.status = status;
    }

    public Integer getStatus() 
    {
        return status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("name", getName())
            .append("tzName", getTzName())
            .append("status", getStatus())
            .append("createTime", getCreateTime())
            .toString();
    }
}
