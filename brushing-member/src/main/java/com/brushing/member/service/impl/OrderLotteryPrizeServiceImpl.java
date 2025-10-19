package com.brushing.member.service.impl;

import java.util.List;
import com.brushing.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.brushing.member.mapper.OrderLotteryPrizeMapper;
import com.brushing.member.domain.OrderLotteryPrize;
import com.brushing.member.service.IOrderLotteryPrizeService;
import org.springframework.transaction.annotation.Transactional;

/**
 * 奖品配置Service业务层处理
 * 
 * @author brushing
 * @date 2025-10-12
 */
@Service
public class OrderLotteryPrizeServiceImpl implements IOrderLotteryPrizeService 
{
    @Autowired
    private OrderLotteryPrizeMapper orderLotteryPrizeMapper;

    /**
     * 查询奖品配置
     * 
     * @param id 奖品配置主键
     * @return 奖品配置
     */
    @Override
    public OrderLotteryPrize selectOrderLotteryPrizeById(Long id)
    {
        return orderLotteryPrizeMapper.selectOrderLotteryPrizeById(id);
    }

    /**
     * 查询奖品配置列表
     * 
     * @param orderLotteryPrize 奖品配置
     * @return 奖品配置
     */
    @Override
    public List<OrderLotteryPrize> selectOrderLotteryPrizeList()
    {
        OrderLotteryPrize orderLotteryPrize=new OrderLotteryPrize();
        orderLotteryPrize.setConfigId(1L);
        return orderLotteryPrizeMapper.selectOrderLotteryPrizeList(orderLotteryPrize);
    }

    /**
     * 新增奖品配置
     * 
     * @param orderLotteryPrize 奖品配置
     * @return 结果
     */
    @Override
    @Transactional
    public int insertOrderLotteryPrize(List<OrderLotteryPrize> orderLotteryPrizes)
    {
        Long configId = orderLotteryPrizes.get(0).getConfigId();
        orderLotteryPrizeMapper.deleteOrderLotteryPrizeByConfigId(configId);
        for (OrderLotteryPrize prizes:orderLotteryPrizes ){
            prizes.setCreateTime(DateUtils.getNowDate());
            orderLotteryPrizeMapper.insertOrderLotteryPrize(prizes);
        }
        return 1;
    }

    /**
     * 修改奖品配置
     * 
     * @param orderLotteryPrize 奖品配置
     * @return 结果
     */
    @Override
    public int updateOrderLotteryPrize(OrderLotteryPrize orderLotteryPrize)
    {
        return orderLotteryPrizeMapper.updateOrderLotteryPrize(orderLotteryPrize);
    }

    /**
     * 批量删除奖品配置
     * 
     * @param ids 需要删除的奖品配置主键
     * @return 结果
     */
    @Override
    public int deleteOrderLotteryPrizeByIds(Long[] ids)
    {
        return orderLotteryPrizeMapper.deleteOrderLotteryPrizeByIds(ids);
    }

    /**
     * 删除奖品配置信息
     * 
     * @param id 奖品配置主键
     * @return 结果
     */
    @Override
    public int deleteOrderLotteryPrizeById(Long id)
    {
        return orderLotteryPrizeMapper.deleteOrderLotteryPrizeById(id);
    }

    @Override
    public Integer getTotalRemainCount(Long configId) {
        return orderLotteryPrizeMapper.getTotalRemainCount(configId);
    }
}
