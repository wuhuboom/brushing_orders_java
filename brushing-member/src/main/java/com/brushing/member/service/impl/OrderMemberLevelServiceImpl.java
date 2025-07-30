package com.brushing.member.service.impl;

import java.util.List;
import com.brushing.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.brushing.member.mapper.OrderMemberLevelMapper;
import com.brushing.member.domain.OrderMemberLevel;
import com.brushing.member.service.IOrderMemberLevelService;

/**
 * 会员等级Service业务层处理
 * 
 * @author brushing
 * @date 2025-07-30
 */
@Service
public class OrderMemberLevelServiceImpl implements IOrderMemberLevelService 
{
    @Autowired
    private OrderMemberLevelMapper orderMemberLevelMapper;

    /**
     * 查询会员等级
     * 
     * @param id 会员等级主键
     * @return 会员等级
     */
    @Override
    public OrderMemberLevel selectOrderMemberLevelById(Long id)
    {
        return orderMemberLevelMapper.selectOrderMemberLevelById(id);
    }

    /**
     * 查询会员等级列表
     * 
     * @param orderMemberLevel 会员等级
     * @return 会员等级
     */
    @Override
    public List<OrderMemberLevel> selectOrderMemberLevelList(OrderMemberLevel orderMemberLevel)
    {
        return orderMemberLevelMapper.selectOrderMemberLevelList(orderMemberLevel);
    }

    /**
     * 新增会员等级
     * 
     * @param orderMemberLevel 会员等级
     * @return 结果
     */
    @Override
    public int insertOrderMemberLevel(OrderMemberLevel orderMemberLevel)
    {
        orderMemberLevel.setCreateTime(DateUtils.getNowDate());
        return orderMemberLevelMapper.insertOrderMemberLevel(orderMemberLevel);
    }

    /**
     * 修改会员等级
     * 
     * @param orderMemberLevel 会员等级
     * @return 结果
     */
    @Override
    public int updateOrderMemberLevel(OrderMemberLevel orderMemberLevel)
    {
        return orderMemberLevelMapper.updateOrderMemberLevel(orderMemberLevel);
    }

    /**
     * 批量删除会员等级
     * 
     * @param ids 需要删除的会员等级主键
     * @return 结果
     */
    @Override
    public int deleteOrderMemberLevelByIds(Long[] ids)
    {
        return orderMemberLevelMapper.deleteOrderMemberLevelByIds(ids);
    }

    /**
     * 删除会员等级信息
     * 
     * @param id 会员等级主键
     * @return 结果
     */
    @Override
    public int deleteOrderMemberLevelById(Long id)
    {
        return orderMemberLevelMapper.deleteOrderMemberLevelById(id);
    }
}
