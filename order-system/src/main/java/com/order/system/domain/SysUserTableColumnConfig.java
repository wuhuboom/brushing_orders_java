package com.order.system.domain;

import com.order.common.core.domain.BaseEntity;

/**
 * 后台用户表格列配置对象 sys_user_table_column_config。
 */
public class SysUserTableColumnConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 配置ID */
    private Long configId;

    /** 后台用户ID */
    private Long userId;

    /** 前端表格稳定标识 */
    private String tableKey;

    /** 列显隐、顺序及固定状态JSON */
    private String configContent;

    public Long getConfigId()
    {
        return configId;
    }

    public void setConfigId(Long configId)
    {
        this.configId = configId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getTableKey()
    {
        return tableKey;
    }

    public void setTableKey(String tableKey)
    {
        this.tableKey = tableKey;
    }

    public String getConfigContent()
    {
        return configContent;
    }

    public void setConfigContent(String configContent)
    {
        this.configContent = configContent;
    }
}
