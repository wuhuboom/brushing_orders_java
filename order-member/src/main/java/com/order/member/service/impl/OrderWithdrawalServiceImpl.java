package com.order.member.service.impl;

import java.util.List;
import com.order.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.order.member.mapper.OrderWithdrawalMapper;
import com.order.member.domain.OrderWithdrawal;
import com.order.member.service.IOrderWithdrawalService;

/**
 * 提现Service业务层处理
 * 
 * @author order
 * @date 2025-11-11
 */
@Service
public class OrderWithdrawalServiceImpl implements IOrderWithdrawalService 
{
    @Autowired
    private OrderWithdrawalMapper orderWithdrawalMapper;

    /**
     * 查询提现
     * 
     * @param id 提现主键
     * @return 提现
     */
    @Override
    public OrderWithdrawal selectOrderWithdrawalById(Long id)
    {
        return orderWithdrawalMapper.selectOrderWithdrawalById(id);
    }

    /**
     * 查询提现列表
     * 
     * @param orderWithdrawal 提现
     * @return 提现
     */
    @Override
    public List<OrderWithdrawal> selectOrderWithdrawalList(OrderWithdrawal orderWithdrawal)
    {
        return orderWithdrawalMapper.selectOrderWithdrawalList(orderWithdrawal);
    }

    @Override
    public List<OrderWithdrawal> selectOrderWithdrawalByUserId(Long userId) {
        return orderWithdrawalMapper.selectOrderWithdrawalByUserId(userId);
    }

    /**
     * 新增提现
     * 
     * @param orderWithdrawal 提现
     * @return 结果
     */
    @Override
    public int insertOrderWithdrawal(OrderWithdrawal orderWithdrawal)
    {
        orderWithdrawal.setCreateTime(DateUtils.getNowDate());
        return orderWithdrawalMapper.insertOrderWithdrawal(orderWithdrawal);
    }

    /**
     * 修改提现
     * 
     * @param orderWithdrawal 提现
     * @return 结果
     */
    @Override
    public int updateOrderWithdrawal(OrderWithdrawal orderWithdrawal)
    {
        return orderWithdrawalMapper.updateOrderWithdrawal(orderWithdrawal);
    }

    /**
     * 批量删除提现
     * 
     * @param ids 需要删除的提现主键
     * @return 结果
     */
    @Override
    public int deleteOrderWithdrawalByIds(Long[] ids)
    {
        return orderWithdrawalMapper.deleteOrderWithdrawalByIds(ids);
    }

    /**
     * 删除提现信息
     * 
     * @param id 提现主键
     * @return 结果
     */
    @Override
    public int deleteOrderWithdrawalById(Long id)
    {
        return orderWithdrawalMapper.deleteOrderWithdrawalById(id);
    }
}
