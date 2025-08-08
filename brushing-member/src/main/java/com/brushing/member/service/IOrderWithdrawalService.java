package com.brushing.member.service;

import java.util.List;
import com.brushing.member.domain.OrderWithdrawal;

/**
 * 提现记录Service接口
 * 
 * @author brushing
 * @date 2025-08-07
 */
public interface IOrderWithdrawalService 
{
    /**
     * 查询提现记录
     * 
     * @param id 提现记录主键
     * @return 提现记录
     */
    public OrderWithdrawal selectOrderWithdrawalById(Long id);

    /**
     * 查询提现记录列表
     * 
     * @param orderWithdrawal 提现记录
     * @return 提现记录集合
     */
    public List<OrderWithdrawal> selectOrderWithdrawalList(OrderWithdrawal orderWithdrawal);

    /**
     * 新增提现记录
     * 
     * @param orderWithdrawal 提现记录
     * @return 结果
     */
    public int insertOrderWithdrawal(OrderWithdrawal orderWithdrawal);

    /**
     * 修改提现记录
     * 
     * @param orderWithdrawal 提现记录
     * @return 结果
     */
    public int updateOrderWithdrawal(OrderWithdrawal orderWithdrawal);

    /**
     * 批量删除提现记录
     * 
     * @param ids 需要删除的提现记录主键集合
     * @return 结果
     */
    public int deleteOrderWithdrawalByIds(Long[] ids);

    /**
     * 删除提现记录信息
     * 
     * @param id 提现记录主键
     * @return 结果
     */
    public int deleteOrderWithdrawalById(Long id);

    public List<OrderWithdrawal> selectOrderWithdrawalByUserId(Long userId);

    public OrderWithdrawal selectOrderWithdrawalByCode(String code);

}
