package com.order.member.service;

import java.util.List;
import com.order.member.domain.OrderSequenceManager;

/**
 * 序列管理Service接口
 * 
 * @author order
 * @date 2025-10-25
 */
public interface IOrderSequenceManagerService 
{
    /**
     * 查询序列管理
     * 
     * @param id 序列管理主键
     * @return 序列管理
     */
    public OrderSequenceManager selectOrderSequenceManagerById(Long id);

    /**
     * 查询序列管理列表
     * 
     * @param orderSequenceManager 序列管理
     * @return 序列管理集合
     */
    public List<OrderSequenceManager> selectOrderSequenceManagerList(OrderSequenceManager orderSequenceManager);

    /**
     * 新增序列管理
     * 
     * @param orderSequenceManager 序列管理
     * @return 结果
     */
    public int insertOrderSequenceManager(OrderSequenceManager orderSequenceManager);

    /**
     * 修改序列管理
     * 
     * @param orderSequenceManager 序列管理
     * @return 结果
     */
    public int updateOrderSequenceManager(OrderSequenceManager orderSequenceManager);

    /**
     * 批量删除序列管理
     * 
     * @param ids 需要删除的序列管理主键集合
     * @return 结果
     */
    public int deleteOrderSequenceManagerByIds(Long[] ids);

    /**
     * 删除序列管理信息
     * 
     * @param id 序列管理主键
     * @return 结果
     */
    public int deleteOrderSequenceManagerById(Long id);

    public String generateCode(String seqType);

    public Long getAndIncrementWithLock(String seqType, String dateStr, String timeStr);

}
