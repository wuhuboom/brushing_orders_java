package com.order.member.service;

import java.util.List;
import com.order.member.domain.OrderSiteMessage;

/**
 * 站内信Service接口
 * 
 * @author order
 * @date 2025-11-10
 */
public interface IOrderSiteMessageService 
{
    /**
     * 查询站内信
     * 
     * @param id 站内信主键
     * @return 站内信
     */
    public OrderSiteMessage selectOrderSiteMessageById(Long id);

    /**
     * 查询站内信列表
     * 
     * @param orderSiteMessage 站内信
     * @return 站内信集合
     */
    public List<OrderSiteMessage> selectOrderSiteMessageList(OrderSiteMessage orderSiteMessage);

    /**
     * 新增站内信
     * 
     * @param orderSiteMessage 站内信
     * @return 结果
     */
    public int insertOrderSiteMessage(OrderSiteMessage orderSiteMessage);

    /**
     * 修改站内信
     * 
     * @param orderSiteMessage 站内信
     * @return 结果
     */
    public int updateOrderSiteMessage(OrderSiteMessage orderSiteMessage);

    /**
     * 批量删除站内信
     * 
     * @param ids 需要删除的站内信主键集合
     * @return 结果
     */
    public int deleteOrderSiteMessageByIds(Long[] ids);

    /**
     * 删除站内信信息
     * 
     * @param id 站内信主键
     * @return 结果
     */
    public int deleteOrderSiteMessageById(Long id);
}
