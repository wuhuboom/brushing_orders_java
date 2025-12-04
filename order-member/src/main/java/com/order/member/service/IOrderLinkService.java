package com.order.member.service;

import java.util.List;
import com.order.member.domain.OrderLink;
import org.apache.ibatis.annotations.Param;

/**
 * 连单Service接口
 * 
 * @author order
 * @date 2025-10-29
 */
public interface IOrderLinkService 
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
     * 批量删除连单
     * 
     * @param ids 需要删除的连单主键集合
     * @return 结果
     */
    public int deleteOrderLinkByIds(Long[] ids);

    /**
     * 删除连单信息
     * 
     * @param id 连单主键
     * @return 结果
     */
    public int deleteOrderLinkById(Long id);

    public List<OrderLink> selectOrderLinkByUserId(Long id,Long orderCount);
}
