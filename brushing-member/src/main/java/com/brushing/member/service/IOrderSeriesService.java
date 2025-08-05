package com.brushing.member.service;

import java.util.List;
import com.brushing.member.domain.OrderSeries;
import org.apache.ibatis.annotations.Param;

/**
 * 连单Service接口
 * 
 * @author brushing
 * @date 2025-08-04
 */
public interface IOrderSeriesService 
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
     * 批量删除连单
     * 
     * @param ids 需要删除的连单主键集合
     * @return 结果
     */
    public int deleteOrderSeriesByIds(Long[] ids);

    /**
     * 删除连单信息
     * 
     * @param id 连单主键
     * @return 结果
     */
    public int deleteOrderSeriesById(Long id);

    public List<OrderSeries> selectSeriesListByUserId(Long userId);

    public List<OrderSeries> selectOrderSeriesByFrozen(Long userId);
}
