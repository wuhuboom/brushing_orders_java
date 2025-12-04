package com.order.member.service;

import java.util.List;
import com.order.member.domain.OrderWithdrawalType;

/**
 * 出金类型Service接口
 * 
 * @author order
 * @date 2025-11-14
 */
public interface IOrderWithdrawalTypeService 
{
    /**
     * 查询出金类型
     * 
     * @param id 出金类型主键
     * @return 出金类型
     */
    public OrderWithdrawalType selectOrderWithdrawalTypeById(Long id);

    /**
     * 查询出金类型列表
     * 
     * @param orderWithdrawalType 出金类型
     * @return 出金类型集合
     */
    public List<OrderWithdrawalType> selectOrderWithdrawalTypeList(OrderWithdrawalType orderWithdrawalType);

    /**
     * 新增出金类型
     * 
     * @param orderWithdrawalType 出金类型
     * @return 结果
     */
    public int insertOrderWithdrawalType(OrderWithdrawalType orderWithdrawalType);

    /**
     * 修改出金类型
     * 
     * @param orderWithdrawalType 出金类型
     * @return 结果
     */
    public int updateOrderWithdrawalType(OrderWithdrawalType orderWithdrawalType);

    /**
     * 批量删除出金类型
     * 
     * @param ids 需要删除的出金类型主键集合
     * @return 结果
     */
    public int deleteOrderWithdrawalTypeByIds(Long[] ids);

    /**
     * 删除出金类型信息
     * 
     * @param id 出金类型主键
     * @return 结果
     */
    public int deleteOrderWithdrawalTypeById(Long id);
}
