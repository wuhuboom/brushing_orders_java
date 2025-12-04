package com.order.member.service;

import java.util.List;
import java.util.Optional;

import com.order.member.domain.OrderConfig;

/**
 * 网站设置Service接口
 * 
 * @author order
 * @date 2025-11-16
 */
public interface IOrderConfigService 
{
    /**
     * 查询网站设置
     * 
     * @param id 网站设置主键
     * @return 网站设置
     */
    public OrderConfig selectOrderConfigById(Long id);

    /**
     * 查询网站设置列表
     * 
     * @param orderConfig 网站设置
     * @return 网站设置集合
     */
    public List<OrderConfig> selectOrderConfigList(OrderConfig orderConfig);

    /**
     * 新增网站设置
     * 
     * @param orderConfig 网站设置
     * @return 结果
     */
    public int insertOrderConfig(OrderConfig orderConfig);

    /**
     * 修改网站设置
     * 
     * @param orderConfig 网站设置
     * @return 结果
     */
    public int updateOrderConfig(OrderConfig orderConfig);

    /**
     * 批量删除网站设置
     * 
     * @param ids 需要删除的网站设置主键集合
     * @return 结果
     */
    public int deleteOrderConfigByIds(Long[] ids);

    /**
     * 删除网站设置信息
     * 
     * @param id 网站设置主键
     * @return 结果
     */
    public int deleteOrderConfigById(Long id);

    public OrderConfig selectOrderConfigByType(String type);

    public Optional<Object> getConfigValue(String type, String key);
}
