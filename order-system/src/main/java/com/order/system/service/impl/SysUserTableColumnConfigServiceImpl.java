package com.order.system.service.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.common.exception.ServiceException;
import com.order.common.utils.StringUtils;
import com.order.system.domain.SysUserTableColumnConfig;
import com.order.system.domain.dto.TableColumnConfigDto;
import com.order.system.mapper.SysUserTableColumnConfigMapper;
import com.order.system.service.ISysUserTableColumnConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 后台用户表格列配置服务实现。
 */
@Service
public class SysUserTableColumnConfigServiceImpl implements ISysUserTableColumnConfigService
{
    static final int MAX_TABLE_KEY_LENGTH = 128;
    static final int MAX_ITEM_COUNT = 200;
    static final int MAX_COLUMN_ID_LENGTH = 128;
    static final int MAX_CONFIG_CONTENT_BYTES = 65_535;

    private static final int SUPPORTED_VERSION = 1;
    private static final Pattern TABLE_KEY_PATTERN = Pattern.compile("[A-Za-z0-9._:-]+");

    private final SysUserTableColumnConfigMapper configMapper;
    private final ObjectMapper objectMapper;

    public SysUserTableColumnConfigServiceImpl(
            SysUserTableColumnConfigMapper configMapper,
            ObjectMapper objectMapper)
    {
        this.configMapper = configMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public TableColumnConfigDto selectTableColumnConfig(Long userId, String tableKey)
    {
        validateIdentityAndTableKey(userId, tableKey);
        SysUserTableColumnConfig stored = configMapper.selectByUserIdAndTableKey(userId, tableKey);
        if (stored == null)
        {
            return null;
        }
        if (StringUtils.isEmpty(stored.getConfigContent()))
        {
            throw new ServiceException("表格列配置内容不是有效JSON");
        }
        try
        {
            byte[] content = stored.getConfigContent().getBytes(StandardCharsets.UTF_8);
            if (content.length > MAX_CONFIG_CONTENT_BYTES)
            {
                throw new ServiceException("表格列配置内容超过64KiB");
            }
            TableColumnConfigDto config = objectMapper.readValue(content, TableColumnConfigDto.class);
            validateConfig(config);
            return config;
        }
        catch (IOException e)
        {
            throw new ServiceException("表格列配置内容不是有效JSON");
        }
    }

    @Override
    @Transactional
    public void saveTableColumnConfig(
            Long userId,
            String username,
            String tableKey,
            TableColumnConfigDto config)
    {
        validateIdentityAndTableKey(userId, tableKey);
        validateConfig(config);

        byte[] content;
        try
        {
            content = objectMapper.writeValueAsBytes(config);
        }
        catch (JsonProcessingException e)
        {
            throw new ServiceException("表格列配置序列化失败");
        }
        if (content.length > MAX_CONFIG_CONTENT_BYTES)
        {
            throw new ServiceException("表格列配置内容超过64KiB");
        }

        SysUserTableColumnConfig stored = new SysUserTableColumnConfig();
        stored.setUserId(userId);
        stored.setTableKey(tableKey);
        stored.setConfigContent(new String(content, StandardCharsets.UTF_8));
        String operator = StringUtils.defaultIfEmpty(username, "");
        stored.setCreateBy(operator);
        stored.setUpdateBy(operator);
        configMapper.upsert(stored);
    }

    @Override
    @Transactional
    public void deleteTableColumnConfig(Long userId, String tableKey)
    {
        validateIdentityAndTableKey(userId, tableKey);
        configMapper.deleteByUserIdAndTableKey(userId, tableKey);
    }

    private void validateIdentityAndTableKey(Long userId, String tableKey)
    {
        if (userId == null)
        {
            throw new ServiceException("获取用户ID异常");
        }
        if (StringUtils.isEmpty(tableKey)
                || tableKey.length() > MAX_TABLE_KEY_LENGTH
                || !TABLE_KEY_PATTERN.matcher(tableKey).matches())
        {
            throw new ServiceException("表格标识只能包含字母、数字、点、下划线、横线或冒号，且不能超过128个字符");
        }
    }

    private void validateConfig(TableColumnConfigDto config)
    {
        if (config == null || config.getVersion() == null || config.getVersion() != SUPPORTED_VERSION)
        {
            throw new ServiceException("仅支持版本1的表格列配置");
        }
        List<TableColumnConfigDto.Item> items = config.getItems();
        if (items == null)
        {
            throw new ServiceException("表格列配置项不能为空");
        }
        if (items.size() > MAX_ITEM_COUNT)
        {
            throw new ServiceException("表格列配置项不能超过200个");
        }

        Set<String> columnIds = new HashSet<>();
        for (TableColumnConfigDto.Item item : items)
        {
            if (item == null)
            {
                throw new ServiceException("表格列配置项不能为空");
            }
            String columnId = item.getId();
            if (StringUtils.isBlank(columnId) || columnId.length() > MAX_COLUMN_ID_LENGTH)
            {
                throw new ServiceException("列标识不能为空且不能超过128个字符");
            }
            if (!columnIds.add(columnId))
            {
                throw new ServiceException("表格列配置中存在重复的列标识");
            }
            if (item.getVisible() == null)
            {
                throw new ServiceException("列显示状态不能为空");
            }
            String fixed = item.getFixed();
            if (fixed != null && !"left".equals(fixed) && !"right".equals(fixed))
            {
                throw new ServiceException("列固定位置只能是left、right或null");
            }
        }
    }
}
