package com.brushing.set.mapper;

import java.util.List;
import com.brushing.set.domain.OrderTradeControlConfig;

/**
 * 交易控制配置Mapper接口
 * 
 * @author brushing
 * @date 2025-07-31
 */
public interface OrderTradeControlConfigMapper 
{
    /**
     * 查询交易控制配置
     * 
     * @param id 交易控制配置主键
     * @return 交易控制配置
     */
    public OrderTradeControlConfig selectOrderTradeControlConfigById(Long id);

    /**
     * 查询交易控制配置列表
     * 
     * @param orderTradeControlConfig 交易控制配置
     * @return 交易控制配置集合
     */
    public List<OrderTradeControlConfig> selectOrderTradeControlConfigList(OrderTradeControlConfig orderTradeControlConfig);

    /**
     * 新增交易控制配置
     * 
     * @param orderTradeControlConfig 交易控制配置
     * @return 结果
     */
    public int insertOrderTradeControlConfig(OrderTradeControlConfig orderTradeControlConfig);

    /**
     * 修改交易控制配置
     * 
     * @param orderTradeControlConfig 交易控制配置
     * @return 结果
     */
    public int updateOrderTradeControlConfig(OrderTradeControlConfig orderTradeControlConfig);

    /**
     * 删除交易控制配置
     * 
     * @param id 交易控制配置主键
     * @return 结果
     */
    public int deleteOrderTradeControlConfigById(Long id);

    /**
     * 批量删除交易控制配置
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteOrderTradeControlConfigByIds(Long[] ids);
}
