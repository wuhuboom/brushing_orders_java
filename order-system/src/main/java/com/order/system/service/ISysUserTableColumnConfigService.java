package com.order.system.service;

import com.order.system.domain.dto.TableColumnConfigDto;

/**
 * 后台用户表格列配置服务。
 */
public interface ISysUserTableColumnConfigService
{
    TableColumnConfigDto selectTableColumnConfig(Long userId, String tableKey);

    void saveTableColumnConfig(
            Long userId,
            String username,
            String tableKey,
            TableColumnConfigDto config);

    void deleteTableColumnConfig(Long userId, String tableKey);
}
