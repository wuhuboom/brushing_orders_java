package com.brushing.member.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.brushing.common.annotation.Excel;
import com.brushing.common.core.domain.BaseEntity;

/**
 * 字段设置对象 order_field_setting
 *
 * @author brushing
 * @date 2026-01-26
 */
public class OrderFieldSetting extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 表名 */
    @Excel(name = "表名")
    private String tableName;

    /** 字段名 */
    @Excel(name = "字段名")
    private String fieldName;

    /** 类型（唯一） */
    @Excel(name = "类型")
    private String type;

    /** 状态（char） */
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

    public void setTableName(String tableName)
    {
        this.tableName = tableName;
    }

    public String getTableName()
    {
        return tableName;
    }

    public void setFieldName(String fieldName)
    {
        this.fieldName = fieldName;
    }

    public String getFieldName()
    {
        return fieldName;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getType()
    {
        return type;
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
            .append("tableName", getTableName())
            .append("fieldName", getFieldName())
            .append("type", getType())
            .append("status", getStatus())
            .append("createTime", getCreateTime())
            .toString();
    }
}
