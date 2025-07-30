package com.brushing.member.service;

import java.util.List;
import com.brushing.member.domain.OrderMemberLevel;

/**
 * 会员等级Service接口
 * 
 * @author brushing
 * @date 2025-07-30
 */
public interface IOrderMemberLevelService 
{
    /**
     * 查询会员等级
     * 
     * @param id 会员等级主键
     * @return 会员等级
     */
    public OrderMemberLevel selectOrderMemberLevelById(Long id);

    /**
     * 查询会员等级列表
     * 
     * @param orderMemberLevel 会员等级
     * @return 会员等级集合
     */
    public List<OrderMemberLevel> selectOrderMemberLevelList(OrderMemberLevel orderMemberLevel);

    /**
     * 新增会员等级
     * 
     * @param orderMemberLevel 会员等级
     * @return 结果
     */
    public int insertOrderMemberLevel(OrderMemberLevel orderMemberLevel);

    /**
     * 修改会员等级
     * 
     * @param orderMemberLevel 会员等级
     * @return 结果
     */
    public int updateOrderMemberLevel(OrderMemberLevel orderMemberLevel);

    /**
     * 批量删除会员等级
     * 
     * @param ids 需要删除的会员等级主键集合
     * @return 结果
     */
    public int deleteOrderMemberLevelByIds(Long[] ids);

    /**
     * 删除会员等级信息
     * 
     * @param id 会员等级主键
     * @return 结果
     */
    public int deleteOrderMemberLevelById(Long id);
}
