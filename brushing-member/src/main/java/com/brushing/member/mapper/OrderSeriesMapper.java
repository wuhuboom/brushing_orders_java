package com.brushing.member.mapper;

import java.util.List;
import com.brushing.member.domain.OrderSeries;
import org.apache.ibatis.annotations.Param;

/**
 * 连单Mapper接口
 * 
 * @author brushing
 * @date 2025-08-04
 */
public interface OrderSeriesMapper 
{
    /**
     * 查询连单
     * 
     * @param id 连单主键
     * @return 连单
     */
    public OrderSeries selectOrderSeriesById(Long id);

    /**
     * 查询连单列表
     * 
     * @param orderSeries 连单
     * @return 连单集合
     */
    public List<OrderSeries> selectOrderSeriesList(OrderSeries orderSeries);

    /**
     * 新增连单
     * 
     * @param orderSeries 连单
     * @return 结果
     */
    public int insertOrderSeries(OrderSeries orderSeries);

    /**
     * 修改连单
     * 
     * @param orderSeries 连单
     * @return 结果
     */
    public int updateOrderSeries(OrderSeries orderSeries);

    /**
     * 删除连单
     * 
     * @param id 连单主键
     * @return 结果
     */
    public int deleteOrderSeriesById(Long id);

    /**
     * 批量删除连单
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteOrderSeriesByIds(Long[] ids);

    public List<OrderSeries> selectByUserIdAndOrderIndexes(@Param("userId") Long userId,
                                                           @Param("orderIndexList") List<Integer> orderIndexList);

    /**
     * 通过用户id 查询是否有需要连单的订单
     * @param userId
     * @return
     */
    public List<OrderSeries> selectSeriesListByUserId(@Param("userId") Long userId);

    /**
     * 查询冻结的订单
     * @param userId 用户id
     * @return
     */
    public List<OrderSeries> selectOrderSeriesByFrozen(@Param("userId") Long userId);




}
