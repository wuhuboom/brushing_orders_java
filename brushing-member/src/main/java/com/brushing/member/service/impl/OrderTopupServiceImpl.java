package com.brushing.member.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.brushing.common.core.domain.AjaxResult;
import com.brushing.common.exception.ServiceException;
import com.brushing.common.utils.DateUtils;
import com.brushing.common.utils.OrderNoGenerator;
import com.brushing.common.utils.SnowflakeIdGenerator;
import com.brushing.member.domain.OrderAccountChange;
import com.brushing.member.domain.OrderMemberUser;
import com.brushing.member.mapper.OrderAccountChangeMapper;
import com.brushing.member.mapper.OrderMemberLevelMapper;
import com.brushing.member.mapper.OrderMemberUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.brushing.member.mapper.OrderTopupMapper;
import com.brushing.member.domain.OrderTopup;
import com.brushing.member.service.IOrderTopupService;
import org.springframework.transaction.annotation.Transactional;

/**
 * 充值记录Service业务层处理
 * 
 * @author brushing
 * @date 2025-08-04
 */
@Service
@Transactional
public class OrderTopupServiceImpl implements IOrderTopupService 
{
    @Autowired
    private OrderTopupMapper orderTopupMapper;

    @Autowired
    private OrderAccountChangeMapper orderAccountChangeMapper;

    @Autowired
    private OrderMemberUserMapper orderMemberUserMapper;

    /**
     * 查询充值记录
     * 
     * @param id 充值记录主键
     * @return 充值记录
     */
    @Override
    public OrderTopup selectOrderTopupById(Long id)
    {
        return orderTopupMapper.selectOrderTopupById(id);
    }

    /**
     * 查询充值记录列表
     * 
     * @param orderTopup 充值记录
     * @return 充值记录
     */
    @Override
    public List<OrderTopup> selectOrderTopupList(OrderTopup orderTopup)
    {
        return orderTopupMapper.selectOrderTopupList(orderTopup);
    }

    /**
     * 新增充值记录
     * 
     * @param orderTopup 充值记录
     * @return 结果
     */
    @Override
    public int insertOrderTopup(OrderTopup orderTopup)
    {
        orderTopup.setCreateTime(DateUtils.getNowDate());
        return orderTopupMapper.insertOrderTopup(orderTopup);
    }

    /**
     * 修改充值记录
     * 
     * @param orderTopup 充值记录
     * @return 结果
     */
    @Override
    public int updateOrderTopup(OrderTopup orderTopup)
    {
        return orderTopupMapper.updateOrderTopup(orderTopup);
    }

    /**
     * 批量删除充值记录
     * 
     * @param ids 需要删除的充值记录主键
     * @return 结果
     */
    @Override
    public int deleteOrderTopupByIds(Long[] ids)
    {
        return orderTopupMapper.deleteOrderTopupByIds(ids);
    }

    /**
     * 删除充值记录信息
     * 
     * @param id 充值记录主键
     * @return 结果
     */
    @Override
    public int deleteOrderTopupById(Long id)
    {
        return orderTopupMapper.deleteOrderTopupById(id);
    }

    @Override
    public int upOrDown(Long userId, BigDecimal amount, String type) {

        OrderMemberUser orderMemberUser = orderMemberUserMapper.selectOrderMemberUserById(userId);
        //上
        if (type.equals("0")){
            BigDecimal balance = orderMemberUser.getBalance();
            BigDecimal add = balance.add(amount);
            //记录账变信息
            recordAccountChange(userId,orderMemberUser.getUsername(),"2",balance,amount,add,"增加用户余额, 操作");
            //保存用户信息
            orderMemberUser.setBalance(add);
            orderMemberUserMapper.updateOrderMemberUser(orderMemberUser);
            //充值记录
           return setLog(userId,amount);
        }else{
            //当前余额
            BigDecimal balance = orderMemberUser.getBalance();
            //变动后的余额
            BigDecimal subtract = balance.subtract(amount);
            //变动金额
            BigDecimal subtract1 = new BigDecimal("0").subtract(amount);
            //记录账变信息
            recordAccountChange(userId,orderMemberUser.getUsername(),"2",balance,subtract1,subtract,"减少用户余额, 操作");
            //保存用户信息
            orderMemberUser.setBalance(subtract);
            orderMemberUserMapper.updateOrderMemberUser(orderMemberUser);
            //充值记录
            return setLog(userId,subtract1);
        }
    }

    private void recordAccountChange(Long userId, String username, String changeType,
                                           BigDecimal beforeAmount, BigDecimal changeAmount,
                                           BigDecimal afterAmount, String action) {
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
        change.setDescription(buildChangeDescription(userId, username, action, changeAmount));
        change.setCreateTime(new Date());
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
    private String buildChangeDescription(Long userId, String username, String action, BigDecimal amount) {
        return String.format("用户ID: %d, 用户名: %s, %s, 金额: %s",
                userId, username, action, amount.toPlainString());
    }

    public int setLog(Long userId,BigDecimal changeAmount){
        String code = generateUniqueTopupNo();
        if (code == null) {
            throw new ServiceException("Please try again later");
        }
        OrderTopup orderTopup =new OrderTopup();
        orderTopup.setAmout(changeAmount);
        orderTopup.setUserId(userId);
        orderTopup.setCode(code);
        orderTopup.setCreateTime(new Date());
      return  orderTopupMapper.insertOrderTopup(orderTopup);
    }

    private String generateUniqueTopupNo() {
        int maxAttempts = 5;
        for (int i = 0; i < maxAttempts; i++) {
            SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
            long l = generator.nextId();
            String code = String.valueOf(l);
            if (orderAccountChangeMapper.selectOrderAccountChangeByCode(code) == null) {
                return code;
            }
        }
        return null;
    }
}
