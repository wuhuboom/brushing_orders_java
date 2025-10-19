package com.brushing.member.mapper;

import java.util.List;
import com.brushing.member.domain.OrderLotteryPrize;
import org.apache.ibatis.annotations.Select;

/**
 * 奖品配置Mapper接口
 * 
 * @author brushing
 * @date 2025-10-12
 */
public interface OrderLotteryPrizeMapper 
{
    /**
     * 查询奖品配置
     * 
     * @param id 奖品配置主键
     * @return 奖品配置
     */
    public OrderLotteryPrize selectOrderLotteryPrizeById(Long id);

    /**
     * 查询奖品配置列表
     * 
     * @param orderLotteryPrize 奖品配置
     * @return 奖品配置集合
     */
    public List<OrderLotteryPrize> selectOrderLotteryPrizeList(OrderLotteryPrize orderLotteryPrize);

    /**
     * 新增奖品配置
     * 
     * @param orderLotteryPrize 奖品配置
     * @return 结果
     */
    public int insertOrderLotteryPrize(OrderLotteryPrize orderLotteryPrize);

    /**
     * 修改奖品配置
     * 
     * @param orderLotteryPrize 奖品配置
     * @return 结果
     */
    public int updateOrderLotteryPrize(OrderLotteryPrize orderLotteryPrize);

    /**
     * 删除奖品配置
     * 
     * @param id 奖品配置主键
     * @return 结果
     */
    public int deleteOrderLotteryPrizeById(Long id);

    public int deleteOrderLotteryPrizeByConfigId(Long configId);

    /**
     * 批量删除奖品配置
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteOrderLotteryPrizeByIds(Long[] ids);

    @Select("SELECT IFNULL(SUM(remain_count), 0) FROM order_lottery_prize WHERE config_id = #{configId}")
    public Integer getTotalRemainCount(Long configId);
}
