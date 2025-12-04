package com.brushing.member.service.impl;

import java.util.List;
import com.brushing.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.brushing.member.mapper.OrderShopMapper;
import com.brushing.member.domain.OrderShop;
import com.brushing.member.service.IOrderShopService;

/**
 * 商品店铺Service业务层处理
 * 
 * @author brushing
 * @date 2025-11-17
 */
@Service
public class OrderShopServiceImpl implements IOrderShopService 
{
    @Autowired
    private OrderShopMapper orderShopMapper;

    /**
     * 查询商品店铺
     * 
     * @param id 商品店铺主键
     * @return 商品店铺
     */
    @Override
    public OrderShop selectOrderShopById(Long id)
    {
        return orderShopMapper.selectOrderShopById(id);
    }

    /**
     * 查询商品店铺列表
     * 
     * @param orderShop 商品店铺
     * @return 商品店铺
     */
    @Override
    public List<OrderShop> selectOrderShopList(OrderShop orderShop)
    {
        return orderShopMapper.selectOrderShopList(orderShop);
    }

    @Override
    public List<OrderShop> findByVipLevel(int vipLevel) {
        return orderShopMapper.findByVipLevel(vipLevel);
    }

    @Override
    public OrderShop selectOrderShopByVipLevel(Integer vipLevel) {
        return orderShopMapper.selectOrderShopByVipLevel(vipLevel);
    }

    /**
     * 新增商品店铺
     * 
     * @param orderShop 商品店铺
     * @return 结果
     */
    @Override
    public int insertOrderShop(OrderShop orderShop)
    {
        orderShop.setCreateTime(DateUtils.getNowDate());
        return orderShopMapper.insertOrderShop(orderShop);
    }

    /**
     * 修改商品店铺
     * 
     * @param orderShop 商品店铺
     * @return 结果
     */
    @Override
    public int updateOrderShop(OrderShop orderShop)
    {
        orderShop.setUpdateTime(DateUtils.getNowDate());
        return orderShopMapper.updateOrderShop(orderShop);
    }

    /**
     * 批量删除商品店铺
     * 
     * @param ids 需要删除的商品店铺主键
     * @return 结果
     */
    @Override
    public int deleteOrderShopByIds(Long[] ids)
    {
        return orderShopMapper.deleteOrderShopByIds(ids);
    }

    /**
     * 删除商品店铺信息
     * 
     * @param id 商品店铺主键
     * @return 结果
     */
    @Override
    public int deleteOrderShopById(Long id)
    {
        return orderShopMapper.deleteOrderShopById(id);
    }
}
