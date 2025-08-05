package com.brushing.member.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.brushing.common.utils.DateUtils;
import com.brushing.common.utils.StringUtils;
import com.brushing.member.domain.OrderGoods;
import com.brushing.member.mapper.OrderGoodsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.brushing.member.mapper.OrderSeriesMapper;
import com.brushing.member.domain.OrderSeries;
import com.brushing.member.service.IOrderSeriesService;

/**
 * 连单Service业务层处理
 * 
 * @author brushing
 * @date 2025-08-04
 */
@Service
public class OrderSeriesServiceImpl implements IOrderSeriesService 
{
    @Autowired
    private OrderSeriesMapper orderSeriesMapper;

    @Autowired
    private OrderGoodsMapper orderGoodsMapper;

    /**
     * 查询连单
     * 
     * @param id 连单主键
     * @return 连单
     */
    @Override
    public OrderSeries selectOrderSeriesById(Long id)
    {
        return orderSeriesMapper.selectOrderSeriesById(id);
    }

    /**
     * 查询连单列表
     * 
     * @param orderSeries 连单
     * @return 连单
     */
    @Override
    public List<OrderSeries> selectOrderSeriesList(OrderSeries orderSeries)
    {
        return orderSeriesMapper.selectOrderSeriesList(orderSeries);
    }

    /**
     * 新增连单
     * 
     * @param orderSeries 连单
     * @return 结果
     */
    @Override
    public int insertOrderSeries(OrderSeries orderSeries)
    {
        //查询
        Integer orderIndex = orderSeries.getOrderIndex();  // 起始值
        Long[] goodsIds = orderSeries.getGoodsIds();       // 商品ID数组

        List<Integer> orderIndexes = new ArrayList<>();
        for (int i = 0; i < goodsIds.length; i++) {
            orderIndexes.add(orderIndex + i);
        }
        List<OrderSeries> orderSeries1 = orderSeriesMapper.selectByUserIdAndOrderIndexes(orderSeries.getUserId(), orderIndexes);
        if (orderSeries1.size()>0){
            return 5;
        }
        //创建连单
        for (int i = 0; i < goodsIds.length; i++) {
            OrderGoods orderGoods = orderGoodsMapper.selectOrderGoodsById(goodsIds[i]);
            if (StringUtils.isNull(orderGoods)){
                return 6;
            }
            OrderSeries series= new OrderSeries();
            series.setOrderIndex(orderIndex+i);
            series.setCommissionRatio(orderSeries.getCommissionRatio());
            series.setProductId(goodsIds[i]);
            series.setUserId(orderSeries.getUserId());
            series.setPrice(orderGoods.getPrice());
            series.setCreateTime(DateUtils.getNowDate());
            series.setCreateBy(orderSeries.getCreateBy());
            orderSeriesMapper.insertOrderSeries(series);
        }
        return 1;
    }

    /**
     * 修改连单
     * 
     * @param orderSeries 连单
     * @return 结果
     */
    @Override
    public int updateOrderSeries(OrderSeries orderSeries)
    {
        return orderSeriesMapper.updateOrderSeries(orderSeries);
    }

    /**
     * 批量删除连单
     * 
     * @param ids 需要删除的连单主键
     * @return 结果
     */
    @Override
    public int deleteOrderSeriesByIds(Long[] ids)
    {
        return orderSeriesMapper.deleteOrderSeriesByIds(ids);
    }

    /**
     * 删除连单信息
     * 
     * @param id 连单主键
     * @return 结果
     */
    @Override
    public int deleteOrderSeriesById(Long id)
    {
        return orderSeriesMapper.deleteOrderSeriesById(id);
    }

    @Override
    public List<OrderSeries> selectSeriesListByUserId(Long userId) {
        return orderSeriesMapper.selectSeriesListByUserId(userId);
    }

    @Override
    public List<OrderSeries> selectOrderSeriesByFrozen(Long userId) {
        return orderSeriesMapper.selectOrderSeriesByFrozen(userId);
    }
}
