package com.order.member.mapper;

import java.util.List;
import com.order.member.domain.OrderInfo;

/**
 * 订单Mapper接口
 * 
 * @author order
 * @date 2025-11-10
 */
public interface OrderInfoMapper 
{
    /**
     * 查询订单
     * 
     * @param id 订单主键
     * @return 订单
     */
    public OrderInfo selectOrderInfoById(Long id);

    /**
     * 查询订单列表
     * 
     * @param orderInfo 订单
     * @return 订单集合
     */
    public List<OrderInfo> selectOrderInfoList(OrderInfo orderInfo);

    /**
     * 新增订单
     * 
     * @param orderInfo 订单
     * @return 结果
     */
    public int insertOrderInfo(OrderInfo orderInfo);

    /**
     * 修改订单
     * 
     * @param orderInfo 订单
     * @return 结果
     */
    public int updateOrderInfo(OrderInfo orderInfo);

    /**
     * 删除订单
     * 
     * @param id 订单主键
     * @return 结果
     */
    public int deleteOrderInfoById(Long id);

    /**
     * 批量删除订单
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteOrderInfoByIds(Long[] ids);

    public int hasOpenOrders(Long userId);
}
