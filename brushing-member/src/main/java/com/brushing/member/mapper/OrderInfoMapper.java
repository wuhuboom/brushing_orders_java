package com.brushing.member.mapper;

import java.util.List;
import com.brushing.member.domain.OrderInfo;

/**
 * 订单列表Mapper接口
 * 
 * @author brushing
 * @date 2025-08-02
 */
public interface OrderInfoMapper 
{
    /**
     * 查询订单列表
     * 
     * @param id 订单列表主键
     * @return 订单列表
     */
    public OrderInfo selectOrderInfoById(String id);

    /**
     * 查询订单列表列表
     * 
     * @param orderInfo 订单列表
     * @return 订单列表集合
     */
    public List<OrderInfo> selectOrderInfoList(OrderInfo orderInfo);

    /**
     * 新增订单列表
     * 
     * @param orderInfo 订单列表
     * @return 结果
     */
    public int insertOrderInfo(OrderInfo orderInfo);

    /**
     * 修改订单列表
     * 
     * @param orderInfo 订单列表
     * @return 结果
     */
    public int updateOrderInfo(OrderInfo orderInfo);

    /**
     * 删除订单列表
     * 
     * @param id 订单列表主键
     * @return 结果
     */
    public int deleteOrderInfoById(String id);

    /**
     * 批量删除订单列表
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteOrderInfoByIds(String[] ids);
}
