package com.order.member.service.impl;

import java.util.List;
import com.order.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.order.member.mapper.OrderBonusTableMapper;
import com.order.member.domain.OrderBonusTable;
import com.order.member.service.IOrderBonusTableService;

/**
 * 彩金Service业务层处理
 * 
 * @author order
 * @date 2025-11-04
 */
@Service
public class OrderBonusTableServiceImpl implements IOrderBonusTableService 
{
    @Autowired
    private OrderBonusTableMapper orderBonusTableMapper;

    /**
     * 查询彩金
     * 
     * @param id 彩金主键
     * @return 彩金
     */
    @Override
    public OrderBonusTable selectOrderBonusTableById(Long id)
    {
        return orderBonusTableMapper.selectOrderBonusTableById(id);
    }

    /**
     * 查询彩金列表
     * 
     * @param orderBonusTable 彩金
     * @return 彩金
     */
    @Override
    public List<OrderBonusTable> selectOrderBonusTableList(OrderBonusTable orderBonusTable)
    {
        return orderBonusTableMapper.selectOrderBonusTableList(orderBonusTable);
    }

    /**
     * 新增彩金
     * 
     * @param orderBonusTable 彩金
     * @return 结果
     */
    @Override
    public int insertOrderBonusTable(OrderBonusTable orderBonusTable)
    {
        orderBonusTable.setCreateTime(DateUtils.getNowDate());
        return orderBonusTableMapper.insertOrderBonusTable(orderBonusTable);
    }

    /**
     * 修改彩金
     * 
     * @param orderBonusTable 彩金
     * @return 结果
     */
    @Override
    public int updateOrderBonusTable(OrderBonusTable orderBonusTable)
    {
        return orderBonusTableMapper.updateOrderBonusTable(orderBonusTable);
    }

    /**
     * 批量删除彩金
     * 
     * @param ids 需要删除的彩金主键
     * @return 结果
     */
    @Override
    public int deleteOrderBonusTableByIds(Long[] ids)
    {
        return orderBonusTableMapper.deleteOrderBonusTableByIds(ids);
    }

    /**
     * 删除彩金信息
     * 
     * @param id 彩金主键
     * @return 结果
     */
    @Override
    public int deleteOrderBonusTableById(Long id)
    {
        return orderBonusTableMapper.deleteOrderBonusTableById(id);
    }
}
