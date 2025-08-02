package com.brushing.member.service.impl;

import java.util.List;
import com.brushing.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.brushing.member.mapper.OrderAccountChangeMapper;
import com.brushing.member.domain.OrderAccountChange;
import com.brushing.member.service.IOrderAccountChangeService;

/**
 * 账户变动Service业务层处理
 * 
 * @author brushing
 * @date 2025-08-02
 */
@Service
public class OrderAccountChangeServiceImpl implements IOrderAccountChangeService 
{
    @Autowired
    private OrderAccountChangeMapper orderAccountChangeMapper;

    /**
     * 查询账户变动
     * 
     * @param id 账户变动主键
     * @return 账户变动
     */
    @Override
    public OrderAccountChange selectOrderAccountChangeById(String id)
    {
        return orderAccountChangeMapper.selectOrderAccountChangeById(id);
    }

    /**
     * 查询账户变动列表
     * 
     * @param orderAccountChange 账户变动
     * @return 账户变动
     */
    @Override
    public List<OrderAccountChange> selectOrderAccountChangeList(OrderAccountChange orderAccountChange)
    {
        return orderAccountChangeMapper.selectOrderAccountChangeList(orderAccountChange);
    }

    /**
     * 新增账户变动
     * 
     * @param orderAccountChange 账户变动
     * @return 结果
     */
    @Override
    public int insertOrderAccountChange(OrderAccountChange orderAccountChange)
    {
        orderAccountChange.setCreateTime(DateUtils.getNowDate());
        return orderAccountChangeMapper.insertOrderAccountChange(orderAccountChange);
    }

    /**
     * 修改账户变动
     * 
     * @param orderAccountChange 账户变动
     * @return 结果
     */
    @Override
    public int updateOrderAccountChange(OrderAccountChange orderAccountChange)
    {
        return orderAccountChangeMapper.updateOrderAccountChange(orderAccountChange);
    }

    /**
     * 批量删除账户变动
     * 
     * @param ids 需要删除的账户变动主键
     * @return 结果
     */
    @Override
    public int deleteOrderAccountChangeByIds(String[] ids)
    {
        return orderAccountChangeMapper.deleteOrderAccountChangeByIds(ids);
    }

    /**
     * 删除账户变动信息
     * 
     * @param id 账户变动主键
     * @return 结果
     */
    @Override
    public int deleteOrderAccountChangeById(String id)
    {
        return orderAccountChangeMapper.deleteOrderAccountChangeById(id);
    }
}
