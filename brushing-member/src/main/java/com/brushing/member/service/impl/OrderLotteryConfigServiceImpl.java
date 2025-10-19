package com.brushing.member.service.impl;

import java.util.List;
import com.brushing.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.brushing.member.mapper.OrderLotteryConfigMapper;
import com.brushing.member.domain.OrderLotteryConfig;
import com.brushing.member.service.IOrderLotteryConfigService;

/**
 * 抽奖配置Service业务层处理
 * 
 * @author brushing
 * @date 2025-10-12
 */
@Service
public class OrderLotteryConfigServiceImpl implements IOrderLotteryConfigService 
{
    @Autowired
    private OrderLotteryConfigMapper orderLotteryConfigMapper;

    /**
     * 查询抽奖配置
     * 
     * @param id 抽奖配置主键
     * @return 抽奖配置
     */
    @Override
    public OrderLotteryConfig selectOrderLotteryConfigById(Long id)
    {
        return orderLotteryConfigMapper.selectOrderLotteryConfigById(id);
    }

    /**
     * 查询抽奖配置列表
     * 
     * @param orderLotteryConfig 抽奖配置
     * @return 抽奖配置
     */
    @Override
    public List<OrderLotteryConfig> selectOrderLotteryConfigList(OrderLotteryConfig orderLotteryConfig)
    {
        return orderLotteryConfigMapper.selectOrderLotteryConfigList(orderLotteryConfig);
    }

    /**
     * 新增抽奖配置
     * 
     * @param orderLotteryConfig 抽奖配置
     * @return 结果
     */
    @Override
    public int insertOrderLotteryConfig(OrderLotteryConfig orderLotteryConfig)
    {
        orderLotteryConfig.setCreateTime(DateUtils.getNowDate());
        return orderLotteryConfigMapper.insertOrderLotteryConfig(orderLotteryConfig);
    }

    /**
     * 修改抽奖配置
     * 
     * @param orderLotteryConfig 抽奖配置
     * @return 结果
     */
    @Override
    public int updateOrderLotteryConfig(OrderLotteryConfig orderLotteryConfig)
    {
        orderLotteryConfig.setUpdateTime(DateUtils.getNowDate());
        return orderLotteryConfigMapper.updateOrderLotteryConfig(orderLotteryConfig);
    }

    /**
     * 批量删除抽奖配置
     * 
     * @param ids 需要删除的抽奖配置主键
     * @return 结果
     */
    @Override
    public int deleteOrderLotteryConfigByIds(Long[] ids)
    {
        return orderLotteryConfigMapper.deleteOrderLotteryConfigByIds(ids);
    }

    /**
     * 删除抽奖配置信息
     * 
     * @param id 抽奖配置主键
     * @return 结果
     */
    @Override
    public int deleteOrderLotteryConfigById(Long id)
    {
        return orderLotteryConfigMapper.deleteOrderLotteryConfigById(id);
    }
}
