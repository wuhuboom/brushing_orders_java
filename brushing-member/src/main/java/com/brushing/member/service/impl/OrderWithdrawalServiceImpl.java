package com.brushing.member.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.brushing.member.mapper.OrderWithdrawalMapper;
import com.brushing.member.domain.OrderWithdrawal;
import com.brushing.member.service.IOrderWithdrawalService;

/**
 * 提现记录Service业务层处理
 * 
 * @author brushing
 * @date 2025-08-07
 */
@Service
public class OrderWithdrawalServiceImpl implements IOrderWithdrawalService 
{
    @Autowired
    private OrderWithdrawalMapper orderWithdrawalMapper;

    /**
     * 查询提现记录
     * 
     * @param id 提现记录主键
     * @return 提现记录
     */
    @Override
    public OrderWithdrawal selectOrderWithdrawalById(Long id)
    {
        return orderWithdrawalMapper.selectOrderWithdrawalById(id);
    }

    /**
     * 查询提现记录列表
     * 
     * @param orderWithdrawal 提现记录
     * @return 提现记录
     */
    @Override
    public List<OrderWithdrawal> selectOrderWithdrawalList(OrderWithdrawal orderWithdrawal)
    {
        return orderWithdrawalMapper.selectOrderWithdrawalList(orderWithdrawal);
    }

    /**
     * 新增提现记录
     * 
     * @param orderWithdrawal 提现记录
     * @return 结果
     */
    @Override
    public int insertOrderWithdrawal(OrderWithdrawal orderWithdrawal)
    {
        return orderWithdrawalMapper.insertOrderWithdrawal(orderWithdrawal);
    }

    /**
     * 修改提现记录
     * 
     * @param orderWithdrawal 提现记录
     * @return 结果
     */
    @Override
    public int updateOrderWithdrawal(OrderWithdrawal orderWithdrawal)
    {
        return orderWithdrawalMapper.updateOrderWithdrawal(orderWithdrawal);
    }

    /**
     * 批量删除提现记录
     * 
     * @param ids 需要删除的提现记录主键
     * @return 结果
     */
    @Override
    public int deleteOrderWithdrawalByIds(Long[] ids)
    {
        return orderWithdrawalMapper.deleteOrderWithdrawalByIds(ids);
    }

    /**
     * 删除提现记录信息
     * 
     * @param id 提现记录主键
     * @return 结果
     */
    @Override
    public int deleteOrderWithdrawalById(Long id)
    {
        return orderWithdrawalMapper.deleteOrderWithdrawalById(id);
    }

    @Override
    public List<OrderWithdrawal> selectOrderWithdrawalByUserId(Long userId) {
        return orderWithdrawalMapper.selectOrderWithdrawalByUserId(userId);
    }

    @Override
    public OrderWithdrawal selectOrderWithdrawalByCode(String code) {
        return orderWithdrawalMapper.selectOrderWithdrawalByCode(code);
    }

    @Override
    public int countStatusOneInOrderWithdrawal() {
        return orderWithdrawalMapper.countStatusOneInOrderWithdrawal();
    }
}
