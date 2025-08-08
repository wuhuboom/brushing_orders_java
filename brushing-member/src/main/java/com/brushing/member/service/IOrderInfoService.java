package com.brushing.member.service;

import java.util.List;
import com.brushing.member.domain.OrderInfo;
import org.apache.ibatis.annotations.Param;

/**
 * 订单列表Service接口
 * 
 * @author brushing
 * @date 2025-08-02
 */
public interface IOrderInfoService 
{
    /**
     * 查询订单列表
     * 
     * @param id 订单列表主键
     * @return 订单列表
     */
    public OrderInfo selectOrderInfoById(Long id);

    public OrderInfo selectOrderInfoByCode(String code);

    /**
     * 查询订单列表列表
     * 
     * @param orderInfo 订单列表
     * @return 订单列表集合
     */
    public List<OrderInfo> selectOrderInfoList(OrderInfo orderInfo);

    /**
     * 查询用户订单列表
     * @param
     * @return
     */
    public List<OrderInfo> selectOrderInfosByUser(OrderInfo orderInfo);
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
     * 批量删除订单列表
     * 
     * @param ids 需要删除的订单列表主键集合
     * @return 结果
     */
    public int deleteOrderInfoByIds(String[] ids);

    /**
     * 删除订单列表信息
     * 
     * @param id 订单列表主键
     * @return 结果
     */
    public int deleteOrderInfoById(String id);

    public int countUnfinishedOrders(Long userId);

    public List<OrderInfo> selectOrderInfoBySeries(Long userId);
}
