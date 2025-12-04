package com.order.member.mapper;

import java.util.List;
import com.order.member.domain.OrderBonusTable;

/**
 * 彩金Mapper接口
 * 
 * @author order
 * @date 2025-11-04
 */
public interface OrderBonusTableMapper 
{
    /**
     * 查询彩金
     * 
     * @param id 彩金主键
     * @return 彩金
     */
    public OrderBonusTable selectOrderBonusTableById(Long id);

    /**
     * 查询彩金列表
     * 
     * @param orderBonusTable 彩金
     * @return 彩金集合
     */
    public List<OrderBonusTable> selectOrderBonusTableList(OrderBonusTable orderBonusTable);

    /**
     * 新增彩金
     * 
     * @param orderBonusTable 彩金
     * @return 结果
     */
    public int insertOrderBonusTable(OrderBonusTable orderBonusTable);

    /**
     * 修改彩金
     * 
     * @param orderBonusTable 彩金
     * @return 结果
     */
    public int updateOrderBonusTable(OrderBonusTable orderBonusTable);

    /**
     * 删除彩金
     * 
     * @param id 彩金主键
     * @return 结果
     */
    public int deleteOrderBonusTableById(Long id);

    /**
     * 批量删除彩金
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteOrderBonusTableByIds(Long[] ids);
}
