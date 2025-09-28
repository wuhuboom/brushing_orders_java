package com.brushing.member.mapper;

import java.util.List;
import com.brushing.member.domain.OrderInfo;
import org.apache.ibatis.annotations.Param;

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
    public OrderInfo selectOrderInfoById(Long id);

    public OrderInfo selectOrderInfoByCode(String orderNo);

    /**
     * 查询订单列表列表
     * 
     * @param orderInfo 订单列表
     * @return 订单列表集合
     */
    public List<OrderInfo> selectOrderInfoList(OrderInfo orderInfo);

    public List<OrderInfo> selectOrderInfoBySeries(@Param("userId") Long userId);

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

    /**
     * 查询当前用户是否存在未完成的订单
     * @return
     */
    public int countUnfinishedOrders(@Param("userId") Long userId);


    public int  countStatusOneInOrderInfo();

}
