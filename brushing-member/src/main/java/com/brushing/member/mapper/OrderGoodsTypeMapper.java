package com.brushing.member.mapper;

import java.util.List;
import com.brushing.member.domain.OrderGoodsType;

/**
 * 商品类别Mapper接口
 * 
 * @author brushing
 * @date 2025-08-01
 */
public interface OrderGoodsTypeMapper 
{
    /**
     * 查询商品类别
     * 
     * @param id 商品类别主键
     * @return 商品类别
     */
    public OrderGoodsType selectOrderGoodsTypeById(Long id);

    /**
     * 查询商品类别列表
     * 
     * @param orderGoodsType 商品类别
     * @return 商品类别集合
     */
    public List<OrderGoodsType> selectOrderGoodsTypeList(OrderGoodsType orderGoodsType);

    /**
     * 新增商品类别
     * 
     * @param orderGoodsType 商品类别
     * @return 结果
     */
    public int insertOrderGoodsType(OrderGoodsType orderGoodsType);

    /**
     * 修改商品类别
     * 
     * @param orderGoodsType 商品类别
     * @return 结果
     */
    public int updateOrderGoodsType(OrderGoodsType orderGoodsType);

    /**
     * 删除商品类别
     * 
     * @param id 商品类别主键
     * @return 结果
     */
    public int deleteOrderGoodsTypeById(Long id);

    /**
     * 批量删除商品类别
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteOrderGoodsTypeByIds(Long[] ids);
}
