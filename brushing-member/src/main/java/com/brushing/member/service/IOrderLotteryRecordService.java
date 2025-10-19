package com.brushing.member.service;

import java.util.List;
import com.brushing.member.domain.OrderLotteryRecord;

/**
 * 抽奖记录Service接口
 * 
 * @author brushing
 * @date 2025-10-12
 */
public interface IOrderLotteryRecordService 
{
    /**
     * 查询抽奖记录
     * 
     * @param id 抽奖记录主键
     * @return 抽奖记录
     */
    public OrderLotteryRecord selectOrderLotteryRecordById(Long id);

    /**
     * 查询抽奖记录列表
     * 
     * @param orderLotteryRecord 抽奖记录
     * @return 抽奖记录集合
     */
    public List<OrderLotteryRecord> selectOrderLotteryRecordList(OrderLotteryRecord orderLotteryRecord);

    /**
     * 新增抽奖记录
     * 
     * @param orderLotteryRecord 抽奖记录
     * @return 结果
     */
    public int insertOrderLotteryRecord(OrderLotteryRecord orderLotteryRecord);

    /**
     * 修改抽奖记录
     * 
     * @param orderLotteryRecord 抽奖记录
     * @return 结果
     */
    public int updateOrderLotteryRecord(OrderLotteryRecord orderLotteryRecord);

    /**
     * 批量删除抽奖记录
     * 
     * @param ids 需要删除的抽奖记录主键集合
     * @return 结果
     */
    public int deleteOrderLotteryRecordByIds(Long[] ids);

    /**
     * 删除抽奖记录信息
     * 
     * @param id 抽奖记录主键
     * @return 结果
     */
    public int deleteOrderLotteryRecordById(Long id);

    public int countTodayDraws(Long userId);
}
