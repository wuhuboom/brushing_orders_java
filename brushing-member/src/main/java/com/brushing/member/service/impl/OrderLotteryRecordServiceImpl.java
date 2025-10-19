package com.brushing.member.service.impl;

import java.util.List;
import com.brushing.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.brushing.member.mapper.OrderLotteryRecordMapper;
import com.brushing.member.domain.OrderLotteryRecord;
import com.brushing.member.service.IOrderLotteryRecordService;

/**
 * 抽奖记录Service业务层处理
 * 
 * @author brushing
 * @date 2025-10-12
 */
@Service
public class OrderLotteryRecordServiceImpl implements IOrderLotteryRecordService 
{
    @Autowired
    private OrderLotteryRecordMapper orderLotteryRecordMapper;

    /**
     * 查询抽奖记录
     * 
     * @param id 抽奖记录主键
     * @return 抽奖记录
     */
    @Override
    public OrderLotteryRecord selectOrderLotteryRecordById(Long id)
    {
        return orderLotteryRecordMapper.selectOrderLotteryRecordById(id);
    }

    /**
     * 查询抽奖记录列表
     * 
     * @param orderLotteryRecord 抽奖记录
     * @return 抽奖记录
     */
    @Override
    public List<OrderLotteryRecord> selectOrderLotteryRecordList(OrderLotteryRecord orderLotteryRecord)
    {
        return orderLotteryRecordMapper.selectOrderLotteryRecordList(orderLotteryRecord);
    }

    /**
     * 新增抽奖记录
     * 
     * @param orderLotteryRecord 抽奖记录
     * @return 结果
     */
    @Override
    public int insertOrderLotteryRecord(OrderLotteryRecord orderLotteryRecord)
    {
        orderLotteryRecord.setCreateTime(DateUtils.getNowDate());
        return orderLotteryRecordMapper.insertOrderLotteryRecord(orderLotteryRecord);
    }

    /**
     * 修改抽奖记录
     * 
     * @param orderLotteryRecord 抽奖记录
     * @return 结果
     */
    @Override
    public int updateOrderLotteryRecord(OrderLotteryRecord orderLotteryRecord)
    {
        return orderLotteryRecordMapper.updateOrderLotteryRecord(orderLotteryRecord);
    }

    /**
     * 批量删除抽奖记录
     * 
     * @param ids 需要删除的抽奖记录主键
     * @return 结果
     */
    @Override
    public int deleteOrderLotteryRecordByIds(Long[] ids)
    {
        return orderLotteryRecordMapper.deleteOrderLotteryRecordByIds(ids);
    }

    /**
     * 删除抽奖记录信息
     * 
     * @param id 抽奖记录主键
     * @return 结果
     */
    @Override
    public int deleteOrderLotteryRecordById(Long id)
    {
        return orderLotteryRecordMapper.deleteOrderLotteryRecordById(id);
    }

    @Override
    public int countTodayDraws(Long userId) {
        return orderLotteryRecordMapper.countTodayDraws(userId);
    }
}
