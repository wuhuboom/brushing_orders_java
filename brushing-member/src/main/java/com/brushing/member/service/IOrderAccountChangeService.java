package com.brushing.member.service;

import java.math.BigDecimal;
import java.util.List;
import com.brushing.member.domain.OrderAccountChange;

/**
 * 账户变动Service接口
 * 
 * @author brushing
 * @date 2025-08-02
 */
public interface IOrderAccountChangeService 
{
    /**
     * 查询账户变动
     * 
     * @param id 账户变动主键
     * @return 账户变动
     */
    public OrderAccountChange selectOrderAccountChangeById(Long id);

    public OrderAccountChange selectOrderAccountChangeByCode(String changeNo);

    /**
     * 查询账户变动列表
     * 
     * @param orderAccountChange 账户变动
     * @return 账户变动集合
     */
    public List<OrderAccountChange> selectOrderAccountChangeList(OrderAccountChange orderAccountChange);

    /**
     * 新增账户变动
     * 
     * @param orderAccountChange 账户变动
     * @return 结果
     */
    public int insertOrderAccountChange(OrderAccountChange orderAccountChange);

    /**
     * 修改账户变动
     * 
     * @param orderAccountChange 账户变动
     * @return 结果
     */
    public int updateOrderAccountChange(OrderAccountChange orderAccountChange);

    /**
     * 批量删除账户变动
     * 
     * @param ids 需要删除的账户变动主键集合
     * @return 结果
     */
    public int deleteOrderAccountChangeByIds(String[] ids);

    /**
     * 删除账户变动信息
     * 
     * @param id 账户变动主键
     * @return 结果
     */
    public int deleteOrderAccountChangeById(String id);

    public void recordAccountChange(Long userId, String username, String changeType,
                                     BigDecimal beforeAmount, BigDecimal changeAmount,
                                     BigDecimal afterAmount, String action);
}
