package com.brushing.member.service;

import java.util.List;
import com.brushing.member.domain.OrderLotteryConfig;

/**
 * 抽奖配置Service接口
 * 
 * @author brushing
 * @date 2025-10-12
 */
public interface IOrderLotteryConfigService 
{
    /**
     * 查询抽奖配置
     * 
     * @param id 抽奖配置主键
     * @return 抽奖配置
     */
    public OrderLotteryConfig selectOrderLotteryConfigById(Long id);

    /**
     * 查询抽奖配置列表
     * 
     * @param orderLotteryConfig 抽奖配置
     * @return 抽奖配置集合
     */
    public List<OrderLotteryConfig> selectOrderLotteryConfigList(OrderLotteryConfig orderLotteryConfig);

    /**
     * 新增抽奖配置
     * 
     * @param orderLotteryConfig 抽奖配置
     * @return 结果
     */
    public int insertOrderLotteryConfig(OrderLotteryConfig orderLotteryConfig);

    /**
     * 修改抽奖配置
     * 
     * @param orderLotteryConfig 抽奖配置
     * @return 结果
     */
    public int updateOrderLotteryConfig(OrderLotteryConfig orderLotteryConfig);

    /**
     * 批量删除抽奖配置
     * 
     * @param ids 需要删除的抽奖配置主键集合
     * @return 结果
     */
    public int deleteOrderLotteryConfigByIds(Long[] ids);

    /**
     * 删除抽奖配置信息
     * 
     * @param id 抽奖配置主键
     * @return 结果
     */
    public int deleteOrderLotteryConfigById(Long id);
}
