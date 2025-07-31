package com.brushing.set.service.impl;

import java.util.List;
import com.brushing.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.brushing.set.mapper.OrderTradeControlConfigMapper;
import com.brushing.set.domain.OrderTradeControlConfig;
import com.brushing.set.service.IOrderTradeControlConfigService;

/**
 * 交易控制配置Service业务层处理
 * 
 * @author brushing
 * @date 2025-07-31
 */
@Service
public class OrderTradeControlConfigServiceImpl implements IOrderTradeControlConfigService 
{
    @Autowired
    private OrderTradeControlConfigMapper orderTradeControlConfigMapper;

    /**
     * 查询交易控制配置
     * 
     * @param id 交易控制配置主键
     * @return 交易控制配置
     */
    @Override
    public OrderTradeControlConfig selectOrderTradeControlConfigById(Long id)
    {
        return orderTradeControlConfigMapper.selectOrderTradeControlConfigById(id);
    }

    /**
     * 查询交易控制配置列表
     * 
     * @param orderTradeControlConfig 交易控制配置
     * @return 交易控制配置
     */
    @Override
    public List<OrderTradeControlConfig> selectOrderTradeControlConfigList(OrderTradeControlConfig orderTradeControlConfig)
    {
        return orderTradeControlConfigMapper.selectOrderTradeControlConfigList(orderTradeControlConfig);
    }

    /**
     * 新增交易控制配置
     * 
     * @param orderTradeControlConfig 交易控制配置
     * @return 结果
     */
    @Override
    public int insertOrderTradeControlConfig(OrderTradeControlConfig orderTradeControlConfig)
    {
        orderTradeControlConfig.setCreateTime(DateUtils.getNowDate());
        return orderTradeControlConfigMapper.insertOrderTradeControlConfig(orderTradeControlConfig);
    }

    /**
     * 修改交易控制配置
     * 
     * @param orderTradeControlConfig 交易控制配置
     * @return 结果
     */
    @Override
    public int updateOrderTradeControlConfig(OrderTradeControlConfig orderTradeControlConfig)
    {
        orderTradeControlConfig.setUpdateTime(DateUtils.getNowDate());
        return orderTradeControlConfigMapper.updateOrderTradeControlConfig(orderTradeControlConfig);
    }

    /**
     * 批量删除交易控制配置
     * 
     * @param ids 需要删除的交易控制配置主键
     * @return 结果
     */
    @Override
    public int deleteOrderTradeControlConfigByIds(Long[] ids)
    {
        return orderTradeControlConfigMapper.deleteOrderTradeControlConfigByIds(ids);
    }

    /**
     * 删除交易控制配置信息
     * 
     * @param id 交易控制配置主键
     * @return 结果
     */
    @Override
    public int deleteOrderTradeControlConfigById(Long id)
    {
        return orderTradeControlConfigMapper.deleteOrderTradeControlConfigById(id);
    }
}
