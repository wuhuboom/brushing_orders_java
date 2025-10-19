package com.brushing.member.service.impl;

import java.math.BigDecimal;
import java.util.List;

import com.brushing.common.exception.ServiceException;
import com.brushing.common.utils.DateUtils;
import com.brushing.common.utils.OrderNoGenerator;
import com.brushing.member.service.IOrderMemberUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.brushing.member.mapper.OrderAccountChangeMapper;
import com.brushing.member.domain.OrderAccountChange;
import com.brushing.member.service.IOrderAccountChangeService;
import org.springframework.transaction.annotation.Transactional;

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
    public OrderAccountChange selectOrderAccountChangeById(Long id)
    {
        return orderAccountChangeMapper.selectOrderAccountChangeById(id);
    }

    @Override
    public OrderAccountChange selectOrderAccountChangeByCode(String changeNo) {
        return orderAccountChangeMapper.selectOrderAccountChangeByCode(changeNo);
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
        int i = orderAccountChangeMapper.insertOrderAccountChange(orderAccountChange);
        return i;
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

    @Override
    public void recordAccountChange(Long userId, String username, String changeType, BigDecimal beforeAmount,
                                    BigDecimal changeAmount, BigDecimal afterAmount, String action) {
        String changeNo = generateUniqueChangeNo();
        if (changeNo == null) {
            throw new ServiceException("Please try again later");
        }

        OrderAccountChange change = new OrderAccountChange();
        change.setChangeNo(changeNo);
        change.setType(changeType);
        change.setUserId(userId);
        change.setBeforeAmount(beforeAmount);
        change.setChangeAmount(changeAmount);
        change.setAfterAmount(afterAmount);
        change.setDescription(action);
        change.setCreateTime(DateUtils.getNowDate());
        orderAccountChangeMapper.insertOrderAccountChange(change);
    }

    private String generateUniqueChangeNo() {
        int maxAttempts = 5;
        for (int i = 0; i < maxAttempts; i++) {
            String changeNo = OrderNoGenerator.generateOrderId();
            if (orderAccountChangeMapper.selectOrderAccountChangeByCode(changeNo) == null) {
                return changeNo;
            }
        }
        return null;
    }
}
