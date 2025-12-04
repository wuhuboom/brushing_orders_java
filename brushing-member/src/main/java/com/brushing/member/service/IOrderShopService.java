package com.brushing.member.service;

import java.util.List;
import com.brushing.member.domain.OrderShop;
import org.apache.ibatis.annotations.Param;

/**
 * 商品店铺Service接口
 * 
 * @author brushing
 * @date 2025-11-17
 */
public interface IOrderShopService 
{
    /**
     * 查询商品店铺
     * 
     * @param id 商品店铺主键
     * @return 商品店铺
     */
    public OrderShop selectOrderShopById(Long id);

    /**
     * 查询商品店铺列表
     * 
     * @param orderShop 商品店铺
     * @return 商品店铺集合
     */
    public List<OrderShop> selectOrderShopList(OrderShop orderShop);

    public List<OrderShop> findByVipLevel(int vipLevel);

    public OrderShop selectOrderShopByVipLevel(Integer vipLevel);

    /**
     * 新增商品店铺
     * 
     * @param orderShop 商品店铺
     * @return 结果
     */
    public int insertOrderShop(OrderShop orderShop);

    /**
     * 修改商品店铺
     * 
     * @param orderShop 商品店铺
     * @return 结果
     */
    public int updateOrderShop(OrderShop orderShop);

    /**
     * 批量删除商品店铺
     * 
     * @param ids 需要删除的商品店铺主键集合
     * @return 结果
     */
    public int deleteOrderShopByIds(Long[] ids);

    /**
     * 删除商品店铺信息
     * 
     * @param id 商品店铺主键
     * @return 结果
     */
    public int deleteOrderShopById(Long id);
}
