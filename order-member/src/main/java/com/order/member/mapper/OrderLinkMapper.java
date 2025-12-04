package com.order.member.mapper;

import java.util.List;
import com.order.member.domain.OrderLink;
import org.apache.ibatis.annotations.Param;

/**
 * 连单Mapper接口
 * 
 * @author order
 * @date 2025-10-29
 */
public interface OrderLinkMapper 
{
    /**
     * 查询连单
     * 
     * @param id 连单主键
     * @return 连单
     */
    public OrderLink selectOrderLinkById(Long id);

    /**
     * 查询连单列表
     * 
     * @param orderLink 连单
     * @return 连单集合
     */
    public List<OrderLink> selectOrderLinkList(OrderLink orderLink);

    /**
     * 新增连单
     * 
     * @param orderLink 连单
     * @return 结果
     */
    public int insertOrderLink(OrderLink orderLink);

    /**
     * 修改连单
     * 
     * @param orderLink 连单
     * @return 结果
     */
    public int updateOrderLink(OrderLink orderLink);

    /**
     * 删除连单
     * 
     * @param id 连单主键
     * @return 结果
     */
    public int deleteOrderLinkById(Long id);

    /**
     * 批量删除连单
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteOrderLinkByIds(Long[] ids);

    /**
     * 获取当前表中最大的 link_order_id，用于生成批次级 linkOrderId
     *
     * @return 最大的 link_order_id
     */
    public Long selectMaxLinkOrderId();

    /**
     * 根据用户的 id 和订单数查询连单数据
     *
     * @param id 用户 id
     * @param orderCount 订单数量
     * @return List<OrderLink> 订单链接结果列表
     */
    public List<OrderLink> selectOrderLinkByUserId(@Param("userId") Long userId, @Param("orderCount") Long orderCount);
}
