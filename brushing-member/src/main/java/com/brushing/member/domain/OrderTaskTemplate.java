package com.brushing.member.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.brushing.common.annotation.Excel;
import com.brushing.common.core.domain.BaseEntity;

/**
 * 连单模板对象 order_task_template
 *
 * @author brushing
 * @date 2026-01-10
 */
public class OrderTaskTemplate extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 名称 */
    @Excel(name = "名称")
    private String name;

    /** 描述 */
    @Excel(name = "描述")
    private String description;

    /** 任务数量 */
    @Excel(name = "任务数量")
    private Integer taskCount;

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

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getDescription()
    {
        return description;
    }

    public void setTaskCount(Integer taskCount)
    {
        this.taskCount = taskCount;
    }

    public Integer getTaskCount()
    {
        return taskCount;
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
            .append("description", getDescription())
            .append("taskCount", getTaskCount())
            .append("status", getStatus())
            .append("sort", getSort())
            .append("createTime", getCreateTime())
            .append("createBy", getCreateBy())
            .toString();
    }
}
