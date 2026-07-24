package com.order.member.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.common.utils.DateUtils;
import com.order.common.i18n.ITranslationsService;
import com.order.common.i18n.Translations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import com.order.member.mapper.OrderConfigMapper;
import com.order.member.domain.OrderConfig;
import com.order.member.service.IOrderConfigService;

/**
 * 网站设置Service业务层处理
 *
 * @author order
 * @date 2025-11-16
 */
@Service
@Transactional
public class OrderConfigServiceImpl implements IOrderConfigService
{
    private final OrderConfigMapper orderConfigMapper;
    private final ObjectMapper objectMapper;
    private final ITranslationsService translationsService;

    public OrderConfigServiceImpl(
            OrderConfigMapper orderConfigMapper,
            ObjectMapper objectMapper,
            ITranslationsService translationsService) {
        this.orderConfigMapper = orderConfigMapper;
        this.objectMapper = objectMapper;
        this.translationsService = translationsService;
    }

    /**
     * 查询网站设置
     *
     * @param id 网站设置主键
     * @return 网站设置
     */
    @Override
    @Transactional(readOnly = true)
    public OrderConfig selectOrderConfigById(Long id)
    {
        return attachTranslations(orderConfigMapper.selectOrderConfigById(id));
    }

    /**
     * 查询网站设置列表
     *
     * @param orderConfig 网站设置
     * @return 网站设置
     */
    @Override
    @Transactional(readOnly = true)
    public List<OrderConfig> selectOrderConfigList(OrderConfig orderConfig)
    {
        return orderConfigMapper.selectOrderConfigList(orderConfig);
    }

    /**
     * 新增网站设置
     *
     * @param orderConfig 网站设置
     * @return 结果
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "apiMessageCatalog", allEntries = true),
            @CacheEvict(value = "tradeConfigSnapshot", allEntries = true)
    })
    public int insertOrderConfig(OrderConfig orderConfig)
    {
        if (orderConfig.getCreateTime() == null) {
            orderConfig.setCreateTime(DateUtils.getNowDate());
        }
        if (orderConfig.getUpdateTime() == null) {
            orderConfig.setUpdateTime(DateUtils.getNowDate());
        }
        saveTranslations(orderConfig);
        return orderConfigMapper.insertOrderConfig(orderConfig);
    }

    /**
     * 修改网站设置
     *
     * @param orderConfig 网站设置
     * @return 结果
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "apiMessageCatalog", allEntries = true),
            @CacheEvict(value = "tradeConfigSnapshot", allEntries = true)
    })
    public int updateOrderConfig(OrderConfig orderConfig)
    {
        if (orderConfig.getId() == null) {
            orderConfig.setCreateTime(DateUtils.getNowDate());
        }
        orderConfig.setUpdateTime(DateUtils.getNowDate());

        // 处理 JSON content
        if (orderConfig.getContent() != null && !orderConfig.getContent().isEmpty()) {
            try {
                // 解析为 Map（前端传 JSON 字符串）
                Map<String, Object> contentMap = objectMapper.readValue(orderConfig.getContent(), Map.class);

                // 重新序列化为字符串存 DB
                orderConfig.setContent(objectMapper.writeValueAsString(contentMap));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("JSON 格式错误: " + e.getMessage());
            }
        }
        saveTranslations(orderConfig);
       return orderConfigMapper.updateOrderConfig(orderConfig);

    }

    private void saveTranslations(OrderConfig orderConfig) {
        Translations translations = orderConfig.getTranslations();
        if (translations == null || (translations.getId() == null && !translations.hasAnyValue())) {
            return;
        }
        if (translations.getId() != null) {
            translationsService.updateTranslations(translations);
            orderConfig.setTranslationsId(translations.getId());
        } else {
            translationsService.insertTranslations(translations);
            orderConfig.setTranslationsId(translations.getId());
        }
    }

    /**
     * 批量删除网站设置
     *
     * @param ids 需要删除的网站设置主键
     * @return 结果
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "apiMessageCatalog", allEntries = true),
            @CacheEvict(value = "tradeConfigSnapshot", allEntries = true)
    })
    public int deleteOrderConfigByIds(Long[] ids)
    {
        return orderConfigMapper.deleteOrderConfigByIds(ids);
    }

    /**
     * 删除网站设置信息
     *
     * @param id 网站设置主键
     * @return 结果
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "apiMessageCatalog", allEntries = true),
            @CacheEvict(value = "tradeConfigSnapshot", allEntries = true)
    })
    public int deleteOrderConfigById(Long id)
    {
        return orderConfigMapper.deleteOrderConfigById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderConfig selectOrderConfigByType(String type) {
        return attachTranslations(orderConfigMapper.selectOrderConfigByType(type));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderConfig> selectOrderConfigByTypes(List<String> types) {
        if (types == null || types.isEmpty()) {
            return List.of();
        }
        return orderConfigMapper.selectOrderConfigByTypes(types);
    }

    /**
     * 获取指定 type 下 content JSON 中的 key 值
     * 如果 type 不存在、content 为空或 key 不存在，返回 null
     *
     * @param type 配置类型
     * @param key  配置键
     * @return 值（Object 类型，根据 JSON 实际类型）
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Object> getConfigValue(String type, String key) {  // 返回 Optional<Object>
        OrderConfig config = selectOrderConfigByType(type);
        if (config == null || config.getContent() == null || config.getContent().isEmpty()) {
            // 可选：日志记录 "配置类型 {type} 不存在或 content 为空"
            return Optional.empty();  // 表示不存在
        }
        try {
            Map<String, Object> contentMap = objectMapper.readValue(config.getContent(), Map.class);
            Object value = contentMap.get(key);
            return Optional.ofNullable(value);
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    private OrderConfig attachTranslations(OrderConfig config) {
        if (config != null && config.getTranslationsId() != null) {
            config.setTranslations(translationsService.selectTranslationsById(config.getTranslationsId()));
        }
        return config;
    }


}
