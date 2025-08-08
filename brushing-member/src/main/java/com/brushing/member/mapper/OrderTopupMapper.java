package com.brushing.member.mapper;

import java.util.List;
import com.brushing.member.domain.OrderTopup;

/**
 * 充值记录Mapper接口
 * 
 * @author brushing
 * @date 2025-08-04
 */
public interface OrderTopupMapper 
{
    /**
     * 查询充值记录
     * 
     * @param id 充值记录主键
     * @return 充值记录
     */
    public OrderTopup selectOrderTopupById(Long id);

    public OrderTopup selectOrderTopupByCode(String code);

    /**
     * 查询充值记录列表
     * 
     * @param orderTopup 充值记录
     * @return 充值记录集合
     */
    public List<OrderTopup> selectOrderTopupList(OrderTopup orderTopup);


    public List<OrderTopup> selectOrderTopupByUserId(Long userId);

    /**
     * 新增充值记录
     * 
     * @param orderTopup 充值记录
     * @return 结果
     */
    public int insertOrderTopup(OrderTopup orderTopup);

    /**
     * 修改充值记录
     * 
     * @param orderTopup 充值记录
     * @return 结果
     */
    public int updateOrderTopup(OrderTopup orderTopup);

    /**
     * 删除充值记录
     * 
     * @param id 充值记录主键
     * @return 结果
     */
    public int deleteOrderTopupById(Long id);

    /**
     * 批量删除充值记录
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteOrderTopupByIds(Long[] ids);
}
