package com.order.system.mapper;

import org.apache.ibatis.annotations.Param;
import com.order.system.domain.SysUserTableColumnConfig;

/**
 * 后台用户表格列配置Mapper。
 */
public interface SysUserTableColumnConfigMapper
{
    SysUserTableColumnConfig selectByUserIdAndTableKey(
            @Param("userId") Long userId,
            @Param("tableKey") String tableKey);

    int upsert(SysUserTableColumnConfig config);

    int deleteByUserIdAndTableKey(
            @Param("userId") Long userId,
            @Param("tableKey") String tableKey);
}
