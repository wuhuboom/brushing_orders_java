package com.brushing.member.mapper;

import java.util.List;
import com.brushing.member.domain.OrderGoods;

/**
 * 商品列表Mapper接口
 * 
 * @author brushing
 * @date 2025-08-01
 */
public interface OrderGoodsMapper 
{
    /**
     * 查询商品列表
     * 
     * @param id 商品列表主键
     * @return 商品列表
     */
    public OrderGoods selectOrderGoodsById(String id);

    /**
     * 查询商品列表列表
     * 
     * @param orderGoods 商品列表
     * @return 商品列表集合
     */
    public List<OrderGoods> selectOrderGoodsList(OrderGoods orderGoods);

    /**
     * 新增商品列表
     * 
     * @param orderGoods 商品列表
     * @return 结果
     */
    public int insertOrderGoods(OrderGoods orderGoods);

    /**
     * 修改商品列表
     * 
     * @param orderGoods 商品列表
     * @return 结果
     */
    public int updateOrderGoods(OrderGoods orderGoods);

    /**
     * 删除商品列表
     * 
     * @param id 商品列表主键
     * @return 结果
     */
    public int deleteOrderGoodsById(String id);

    /**
     * 批量删除商品列表
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteOrderGoodsByIds(String[] ids);
}
