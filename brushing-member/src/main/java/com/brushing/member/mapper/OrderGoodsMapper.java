package com.brushing.member.mapper;

import java.math.BigDecimal;
import java.util.List;
import com.brushing.member.domain.OrderGoods;
import org.apache.ibatis.annotations.Param;

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
    public OrderGoods selectOrderGoodsById(Long id);

    /**
     * 查询用户余额 最接近的一条商品数据
     * @param price 用户余额
     * @return
     */
    public OrderGoods selectNearestPriceGoods(@Param("price") BigDecimal price);


    /**
     * 查询商品列表列表
     * 
     * @param orderGoods 商品列表
     * @return 商品列表集合
     */
    public List<OrderGoods> selectOrderGoodsList(OrderGoods orderGoods);

    /**
     * 获取随机商品
     * @return
     */
    public List<OrderGoods> selectRandomOrderGoods();

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
