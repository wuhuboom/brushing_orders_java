package com.brushing.member.service.impl;

import java.util.List;
import com.brushing.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.brushing.member.mapper.OrderInfoMapper;
import com.brushing.member.domain.OrderInfo;
import com.brushing.member.service.IOrderInfoService;

/**
 * 订单列表Service业务层处理
 * 
 * @author brushing
 * @date 2025-08-02
 */
@Service
public class OrderInfoServiceImpl implements IOrderInfoService 
{
    @Autowired
    private OrderInfoMapper orderInfoMapper;

    /**
     * 查询订单列表
     * 
     * @param id 订单列表主键
     * @return 订单列表
     */
    @Override
    public OrderInfo selectOrderInfoById(Long id)
    {
        return orderInfoMapper.selectOrderInfoById(id);
    }

    @Override
    public OrderInfo selectOrderInfoByCode(String code) {
        return orderInfoMapper.selectOrderInfoByCode(code);
    }

    /**
     * 查询订单列表列表
     * 
     * @param orderInfo 订单列表
     * @return 订单列表
     */
    @Override
    public List<OrderInfo> selectOrderInfoList(OrderInfo orderInfo)
    {
        return orderInfoMapper.selectOrderInfoList(orderInfo);
    }

    @Override
    public List<OrderInfo> selectOrderInfosByUser(OrderInfo orderInfo) {
        return orderInfoMapper.selectOrderInfosByUser(orderInfo);
    }

    /**
     * 新增订单列表
     * 
     * @param orderInfo 订单列表
     * @return 结果
     */
    @Override
    public int insertOrderInfo(OrderInfo orderInfo)
    {
        return orderInfoMapper.insertOrderInfo(orderInfo);
    }

    /**
     * 修改订单列表
     * 
     * @param orderInfo 订单列表
     * @return 结果
     */
    @Override
    public int updateOrderInfo(OrderInfo orderInfo)
    {
        orderInfo.setUpdateTime(DateUtils.getNowDate());
        return orderInfoMapper.updateOrderInfo(orderInfo);
    }

    /**
     * 批量删除订单列表
     * 
     * @param ids 需要删除的订单列表主键
     * @return 结果
     */
    @Override
    public int deleteOrderInfoByIds(String[] ids)
    {
        return orderInfoMapper.deleteOrderInfoByIds(ids);
    }

    /**
     * 删除订单列表信息
     * 
     * @param id 订单列表主键
     * @return 结果
     */
    @Override
    public int deleteOrderInfoById(String id)
    {
        return orderInfoMapper.deleteOrderInfoById(id);
    }

    @Override
    public int countUnfinishedOrders(Long userId) {
        return orderInfoMapper.countUnfinishedOrders(userId);
    }

    @Override
    public List<OrderInfo> selectOrderInfoBySeries(Long userId) {
        return orderInfoMapper.selectOrderInfoBySeries(userId);
    }

    @Override
    public int countStatusOneInOrderInfo() {
        return orderInfoMapper.countStatusOneInOrderInfo();
    }
}
