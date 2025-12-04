package com.order.member.service.impl;

import java.util.List;
import com.order.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.order.member.mapper.OrderInfoMapper;
import com.order.member.domain.OrderInfo;
import com.order.member.service.IOrderInfoService;

/**
 * 订单Service业务层处理
 * 
 * @author order
 * @date 2025-11-10
 */
@Service
public class OrderInfoServiceImpl implements IOrderInfoService 
{
    @Autowired
    private OrderInfoMapper orderInfoMapper;

    /**
     * 查询订单
     * 
     * @param id 订单主键
     * @return 订单
     */
    @Override
    public OrderInfo selectOrderInfoById(Long id)
    {
        return orderInfoMapper.selectOrderInfoById(id);
    }

    /**
     * 查询订单列表
     * 
     * @param orderInfo 订单
     * @return 订单
     */
    @Override
    public List<OrderInfo> selectOrderInfoList(OrderInfo orderInfo)
    {
        return orderInfoMapper.selectOrderInfoList(orderInfo);
    }

    /**
     * 新增订单
     * 
     * @param orderInfo 订单
     * @return 结果
     */
    @Override
    public int insertOrderInfo(OrderInfo orderInfo)
    {
        orderInfo.setCreateTime(DateUtils.getNowDate());
        return orderInfoMapper.insertOrderInfo(orderInfo);
    }

    /**
     * 修改订单
     * 
     * @param orderInfo 订单
     * @return 结果
     */
    @Override
    public int updateOrderInfo(OrderInfo orderInfo)
    {
        return orderInfoMapper.updateOrderInfo(orderInfo);
    }

    /**
     * 批量删除订单
     * 
     * @param ids 需要删除的订单主键
     * @return 结果
     */
    @Override
    public int deleteOrderInfoByIds(Long[] ids)
    {
        return orderInfoMapper.deleteOrderInfoByIds(ids);
    }

    /**
     * 删除订单信息
     * 
     * @param id 订单主键
     * @return 结果
     */
    @Override
    public int deleteOrderInfoById(Long id)
    {
        return orderInfoMapper.deleteOrderInfoById(id);
    }

    @Override
    public int hasOpenOrders(Long userId) {
        return orderInfoMapper.hasOpenOrders(userId);
    }
}
