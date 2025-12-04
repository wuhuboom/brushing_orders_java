package com.order.member.service.impl;

import java.util.List;
import com.order.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.order.member.mapper.OrderWithdrawalTypeMapper;
import com.order.member.domain.OrderWithdrawalType;
import com.order.member.service.IOrderWithdrawalTypeService;

/**
 * 出金类型Service业务层处理
 * 
 * @author order
 * @date 2025-11-14
 */
@Service
public class OrderWithdrawalTypeServiceImpl implements IOrderWithdrawalTypeService 
{
    @Autowired
    private OrderWithdrawalTypeMapper orderWithdrawalTypeMapper;

    /**
     * 查询出金类型
     * 
     * @param id 出金类型主键
     * @return 出金类型
     */
    @Override
    public OrderWithdrawalType selectOrderWithdrawalTypeById(Long id)
    {
        return orderWithdrawalTypeMapper.selectOrderWithdrawalTypeById(id);
    }

    /**
     * 查询出金类型列表
     * 
     * @param orderWithdrawalType 出金类型
     * @return 出金类型
     */
    @Override
    public List<OrderWithdrawalType> selectOrderWithdrawalTypeList(OrderWithdrawalType orderWithdrawalType)
    {
        return orderWithdrawalTypeMapper.selectOrderWithdrawalTypeList(orderWithdrawalType);
    }

    /**
     * 新增出金类型
     * 
     * @param orderWithdrawalType 出金类型
     * @return 结果
     */
    @Override
    public int insertOrderWithdrawalType(OrderWithdrawalType orderWithdrawalType)
    {
        orderWithdrawalType.setCreateTime(DateUtils.getNowDate());
        return orderWithdrawalTypeMapper.insertOrderWithdrawalType(orderWithdrawalType);
    }

    /**
     * 修改出金类型
     * 
     * @param orderWithdrawalType 出金类型
     * @return 结果
     */
    @Override
    public int updateOrderWithdrawalType(OrderWithdrawalType orderWithdrawalType)
    {
        return orderWithdrawalTypeMapper.updateOrderWithdrawalType(orderWithdrawalType);
    }

    /**
     * 批量删除出金类型
     * 
     * @param ids 需要删除的出金类型主键
     * @return 结果
     */
    @Override
    public int deleteOrderWithdrawalTypeByIds(Long[] ids)
    {
        return orderWithdrawalTypeMapper.deleteOrderWithdrawalTypeByIds(ids);
    }

    /**
     * 删除出金类型信息
     * 
     * @param id 出金类型主键
     * @return 结果
     */
    @Override
    public int deleteOrderWithdrawalTypeById(Long id)
    {
        return orderWithdrawalTypeMapper.deleteOrderWithdrawalTypeById(id);
    }
}
