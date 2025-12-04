package com.order.member.service;

import java.util.List;
import com.order.member.domain.OrderWithdrawal;

/**
 * 提现Service接口
 * 
 * @author order
 * @date 2025-11-11
 */
public interface IOrderWithdrawalService 
{
    /**
     * 查询提现
     * 
     * @param id 提现主键
     * @return 提现
     */
    public OrderWithdrawal selectOrderWithdrawalById(Long id);

    /**
     * 查询提现列表
     * 
     * @param orderWithdrawal 提现
     * @return 提现集合
     */
    public List<OrderWithdrawal> selectOrderWithdrawalList(OrderWithdrawal orderWithdrawal);

    public List<OrderWithdrawal> selectOrderWithdrawalByUserId(Long userId);
    /**
     * 新增提现
     * 
     * @param orderWithdrawal 提现
     * @return 结果
     */
    public int insertOrderWithdrawal(OrderWithdrawal orderWithdrawal);

    /**
     * 修改提现
     * 
     * @param orderWithdrawal 提现
     * @return 结果
     */
    public int updateOrderWithdrawal(OrderWithdrawal orderWithdrawal);

    /**
     * 批量删除提现
     * 
     * @param ids 需要删除的提现主键集合
     * @return 结果
     */
    public int deleteOrderWithdrawalByIds(Long[] ids);

    /**
     * 删除提现信息
     * 
     * @param id 提现主键
     * @return 结果
     */
    public int deleteOrderWithdrawalById(Long id);
}
