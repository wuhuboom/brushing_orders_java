package com.brushing.member.service;

import java.util.List;
import com.brushing.member.domain.OrderLotteryPrize;

/**
 * 奖品配置Service接口
 * 
 * @author brushing
 * @date 2025-10-12
 */
public interface IOrderLotteryPrizeService 
{
    /**
     * 查询奖品配置
     * 
     * @param id 奖品配置主键
     * @return 奖品配置
     */
    public OrderLotteryPrize selectOrderLotteryPrizeById(Long id);

    /**
     * 查询奖品配置列表
     * 
     * @param orderLotteryPrize 奖品配置
     * @return 奖品配置集合
     */
    public List<OrderLotteryPrize> selectOrderLotteryPrizeList();

    /**
     * 新增奖品配置
     * 
     * @param orderLotteryPrize 奖品配置
     * @return 结果
     */
    public int insertOrderLotteryPrize(List<OrderLotteryPrize> orderLotteryPrize);

    /**
     * 修改奖品配置
     * 
     * @param orderLotteryPrize 奖品配置
     * @return 结果
     */
    public int updateOrderLotteryPrize(OrderLotteryPrize orderLotteryPrize);

    /**
     * 批量删除奖品配置
     * 
     * @param ids 需要删除的奖品配置主键集合
     * @return 结果
     */
    public int deleteOrderLotteryPrizeByIds(Long[] ids);

    /**
     * 删除奖品配置信息
     * 
     * @param id 奖品配置主键
     * @return 结果
     */
    public int deleteOrderLotteryPrizeById(Long id);

    public Integer getTotalRemainCount(Long configId);
}
