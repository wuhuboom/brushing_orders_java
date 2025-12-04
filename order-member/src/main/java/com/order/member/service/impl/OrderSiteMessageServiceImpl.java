package com.order.member.service.impl;

import java.util.List;
import com.order.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.order.member.mapper.OrderSiteMessageMapper;
import com.order.member.domain.OrderSiteMessage;
import com.order.member.service.IOrderSiteMessageService;

/**
 * 站内信Service业务层处理
 * 
 * @author order
 * @date 2025-11-10
 */
@Service
public class OrderSiteMessageServiceImpl implements IOrderSiteMessageService 
{
    @Autowired
    private OrderSiteMessageMapper orderSiteMessageMapper;

    /**
     * 查询站内信
     * 
     * @param id 站内信主键
     * @return 站内信
     */
    @Override
    public OrderSiteMessage selectOrderSiteMessageById(Long id)
    {
        return orderSiteMessageMapper.selectOrderSiteMessageById(id);
    }

    /**
     * 查询站内信列表
     * 
     * @param orderSiteMessage 站内信
     * @return 站内信
     */
    @Override
    public List<OrderSiteMessage> selectOrderSiteMessageList(OrderSiteMessage orderSiteMessage)
    {
        return orderSiteMessageMapper.selectOrderSiteMessageList(orderSiteMessage);
    }

    /**
     * 新增站内信
     * 
     * @param orderSiteMessage 站内信
     * @return 结果
     */
    @Override
    public int insertOrderSiteMessage(OrderSiteMessage orderSiteMessage)
    {
        orderSiteMessage.setCreateTime(DateUtils.getNowDate());
        return orderSiteMessageMapper.insertOrderSiteMessage(orderSiteMessage);
    }

    /**
     * 修改站内信
     * 
     * @param orderSiteMessage 站内信
     * @return 结果
     */
    @Override
    public int updateOrderSiteMessage(OrderSiteMessage orderSiteMessage)
    {
        return orderSiteMessageMapper.updateOrderSiteMessage(orderSiteMessage);
    }

    /**
     * 批量删除站内信
     * 
     * @param ids 需要删除的站内信主键
     * @return 结果
     */
    @Override
    public int deleteOrderSiteMessageByIds(Long[] ids)
    {
        return orderSiteMessageMapper.deleteOrderSiteMessageByIds(ids);
    }

    /**
     * 删除站内信信息
     * 
     * @param id 站内信主键
     * @return 结果
     */
    @Override
    public int deleteOrderSiteMessageById(Long id)
    {
        return orderSiteMessageMapper.deleteOrderSiteMessageById(id);
    }
}
