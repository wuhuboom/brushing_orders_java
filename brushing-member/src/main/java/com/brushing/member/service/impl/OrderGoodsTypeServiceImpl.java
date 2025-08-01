package com.brushing.member.service.impl;

import java.util.List;
import com.brushing.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.brushing.member.mapper.OrderGoodsTypeMapper;
import com.brushing.member.domain.OrderGoodsType;
import com.brushing.member.service.IOrderGoodsTypeService;

/**
 * 商品类别Service业务层处理
 * 
 * @author brushing
 * @date 2025-08-01
 */
@Service
public class OrderGoodsTypeServiceImpl implements IOrderGoodsTypeService 
{
    @Autowired
    private OrderGoodsTypeMapper orderGoodsTypeMapper;

    /**
     * 查询商品类别
     * 
     * @param id 商品类别主键
     * @return 商品类别
     */
    @Override
    public OrderGoodsType selectOrderGoodsTypeById(Long id)
    {
        return orderGoodsTypeMapper.selectOrderGoodsTypeById(id);
    }

    /**
     * 查询商品类别列表
     * 
     * @param orderGoodsType 商品类别
     * @return 商品类别
     */
    @Override
    public List<OrderGoodsType> selectOrderGoodsTypeList(OrderGoodsType orderGoodsType)
    {
        return orderGoodsTypeMapper.selectOrderGoodsTypeList(orderGoodsType);
    }

    /**
     * 新增商品类别
     * 
     * @param orderGoodsType 商品类别
     * @return 结果
     */
    @Override
    public int insertOrderGoodsType(OrderGoodsType orderGoodsType)
    {
        orderGoodsType.setCreateTime(DateUtils.getNowDate());
        return orderGoodsTypeMapper.insertOrderGoodsType(orderGoodsType);
    }

    /**
     * 修改商品类别
     * 
     * @param orderGoodsType 商品类别
     * @return 结果
     */
    @Override
    public int updateOrderGoodsType(OrderGoodsType orderGoodsType)
    {
        orderGoodsType.setUpdateTime(DateUtils.getNowDate());
        return orderGoodsTypeMapper.updateOrderGoodsType(orderGoodsType);
    }

    /**
     * 批量删除商品类别
     * 
     * @param ids 需要删除的商品类别主键
     * @return 结果
     */
    @Override
    public int deleteOrderGoodsTypeByIds(Long[] ids)
    {
        return orderGoodsTypeMapper.deleteOrderGoodsTypeByIds(ids);
    }

    /**
     * 删除商品类别信息
     * 
     * @param id 商品类别主键
     * @return 结果
     */
    @Override
    public int deleteOrderGoodsTypeById(Long id)
    {
        return orderGoodsTypeMapper.deleteOrderGoodsTypeById(id);
    }
}
