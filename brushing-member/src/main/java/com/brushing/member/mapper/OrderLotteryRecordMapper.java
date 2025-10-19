package com.brushing.member.mapper;

import java.util.List;
import com.brushing.member.domain.OrderLotteryRecord;
import org.apache.ibatis.annotations.Select;

/**
 * 抽奖记录Mapper接口
 * 
 * @author brushing
 * @date 2025-10-12
 */
public interface OrderLotteryRecordMapper 
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
     * 删除抽奖记录
     * 
     * @param id 抽奖记录主键
     * @return 结果
     */
    public int deleteOrderLotteryRecordById(Long id);

    /**
     * 批量删除抽奖记录
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteOrderLotteryRecordByIds(Long[] ids);

    @Select("SELECT COUNT(1) FROM order_lottery_record WHERE user_id = #{userId} AND DATE(create_time) = CURDATE()")
    int countTodayDraws(Long userId);
}
