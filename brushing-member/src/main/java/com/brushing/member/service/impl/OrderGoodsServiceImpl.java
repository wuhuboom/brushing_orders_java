package com.brushing.member.service.impl;

import java.math.BigDecimal;
import java.util.List;
import com.brushing.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.brushing.member.mapper.OrderGoodsMapper;
import com.brushing.member.domain.OrderGoods;
import com.brushing.member.service.IOrderGoodsService;

/**
 * 商品列表Service业务层处理
 * 
 * @author brushing
 * @date 2025-08-01
 */
@Service
public class OrderGoodsServiceImpl implements IOrderGoodsService 
{
    @Autowired
    private OrderGoodsMapper orderGoodsMapper;

    /**
     * 查询商品列表
     * 
     * @param id 商品列表主键
     * @return 商品列表
     */
    @Override
    public OrderGoods selectOrderGoodsById(Long id)
    {
        return orderGoodsMapper.selectOrderGoodsById(id);
    }

    /**
     * 查询商品列表列表
     * 
     * @param orderGoods 商品列表
     * @return 商品列表
     */
    @Override
    public List<OrderGoods> selectOrderGoodsList(OrderGoods orderGoods)
    {
        return orderGoodsMapper.selectOrderGoodsList(orderGoods);
    }

    @Override
    public List<OrderGoods> selectRandomOrderGoods() {
        return orderGoodsMapper.selectRandomOrderGoods();
    }

    /**
     * 新增商品列表
     * 
     * @param orderGoods 商品列表
     * @return 结果
     */
    @Override
    public int insertOrderGoods(OrderGoods orderGoods)
    {
        orderGoods.setCreateTime(DateUtils.getNowDate());
        return orderGoodsMapper.insertOrderGoods(orderGoods);
    }

    /**
     * 修改商品列表
     * 
     * @param orderGoods 商品列表
     * @return 结果
     */
    @Override
    public int updateOrderGoods(OrderGoods orderGoods)
    {
        orderGoods.setUpdateTime(DateUtils.getNowDate());
        return orderGoodsMapper.updateOrderGoods(orderGoods);
    }

    /**
     * 批量删除商品列表
     * 
     * @param ids 需要删除的商品列表主键
     * @return 结果
     */
    @Override
    public int deleteOrderGoodsByIds(String[] ids)
    {
        return orderGoodsMapper.deleteOrderGoodsByIds(ids);
    }

    /**
     * 删除商品列表信息
     * 
     * @param id 商品列表主键
     * @return 结果
     */
    @Override
    public int deleteOrderGoodsById(String id)
    {
        return orderGoodsMapper.deleteOrderGoodsById(id);
    }

    @Override
    public OrderGoods selectNearestPriceGoods(BigDecimal price) {
        return orderGoodsMapper.selectNearestPriceGoods(price);
    }
}
