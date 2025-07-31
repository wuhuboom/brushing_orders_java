package com.brushing.set.service;

import java.util.List;
import com.brushing.set.domain.OrderEmailConfig;

/**
 * 系统邮箱配置Service接口
 * 
 * @author brushing
 * @date 2025-07-31
 */
public interface IOrderEmailConfigService 
{
    /**
     * 查询系统邮箱配置
     * 
     * @param id 系统邮箱配置主键
     * @return 系统邮箱配置
     */
    public OrderEmailConfig selectOrderEmailConfigById(Long id);

    /**
     * 查询系统邮箱配置列表
     * 
     * @param orderEmailConfig 系统邮箱配置
     * @return 系统邮箱配置集合
     */
    public List<OrderEmailConfig> selectOrderEmailConfigList(OrderEmailConfig orderEmailConfig);

    /**
     * 新增系统邮箱配置
     * 
     * @param orderEmailConfig 系统邮箱配置
     * @return 结果
     */
    public int insertOrderEmailConfig(OrderEmailConfig orderEmailConfig);

    /**
     * 修改系统邮箱配置
     * 
     * @param orderEmailConfig 系统邮箱配置
     * @return 结果
     */
    public int updateOrderEmailConfig(OrderEmailConfig orderEmailConfig);

    /**
     * 批量删除系统邮箱配置
     * 
     * @param ids 需要删除的系统邮箱配置主键集合
     * @return 结果
     */
    public int deleteOrderEmailConfigByIds(Long[] ids);

    /**
     * 删除系统邮箱配置信息
     * 
     * @param id 系统邮箱配置主键
     * @return 结果
     */
    public int deleteOrderEmailConfigById(Long id);
}
