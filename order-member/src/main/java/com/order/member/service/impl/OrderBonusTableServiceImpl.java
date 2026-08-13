package com.order.member.service.impl;

import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;
import com.order.common.utils.DateUtils;
import com.order.member.domain.GoodsMemberLevel;
import com.order.member.domain.OrderUser;
import com.order.member.mapper.OrderUserMapper;
import com.order.member.service.ITransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.order.member.mapper.OrderBonusTableMapper;
import com.order.member.domain.OrderBonusTable;
import com.order.member.service.IOrderBonusTableService;

/**
 * 彩金Service业务层处理
 * 
 * @author order
 * @date 2025-11-04
 */
@Service
public class OrderBonusTableServiceImpl implements IOrderBonusTableService 
{
    @Autowired
    private OrderBonusTableMapper orderBonusTableMapper;

    @Autowired
    private OrderUserMapper orderUserMapper;

    @Autowired
    private ITransactionService transactionService;

    /**
     * 查询彩金
     * 
     * @param id 彩金主键
     * @return 彩金
     */
    @Override
    public OrderBonusTable selectOrderBonusTableById(Long id)
    {
        return orderBonusTableMapper.selectOrderBonusTableById(id);
    }

    /**
     * 查询彩金列表
     * 
     * @param orderBonusTable 彩金
     * @return 彩金
     */
    @Override
    public List<OrderBonusTable> selectOrderBonusTableList(OrderBonusTable orderBonusTable)
    {
        return orderBonusTableMapper.selectOrderBonusTableList(orderBonusTable);
    }

    /**
     * 新增彩金
     * 
     * @param orderBonusTable 彩金
     * @return 结果
     */
    @Override
    public int insertOrderBonusTable(OrderBonusTable orderBonusTable)
    {
        if (orderBonusTable.getDisplayDuration() == null) {
            orderBonusTable.setDisplayDuration(0L);
        }
        if (orderBonusTable.getDistributionType() == null
                || orderBonusTable.getDistributionType().isBlank()) {
            orderBonusTable.setDistributionType("2");
        }
        if (orderBonusTable.getPushType() == null
                || orderBonusTable.getPushType().isBlank()) {
            orderBonusTable.setPushType("0");
        }
        validateEditableBonus(orderBonusTable);
        orderBonusTable.setAmount(
                orderBonusTable.getAmount().setScale(2, RoundingMode.HALF_UP));
        orderBonusTable.setIsReceived("1");
        orderBonusTable.setIsDistributed("1");
        orderBonusTable.setReceivedTime(null);
        orderBonusTable.setDistributionTime(null);
        orderBonusTable.setCreateTime(DateUtils.getNowDate());
        return orderBonusTableMapper.insertOrderBonusTable(orderBonusTable);
    }

    /**
     * 修改彩金
     * 
     * @param orderBonusTable 彩金
     * @return 结果
     */
    @Override
    public int updateOrderBonusTable(OrderBonusTable orderBonusTable)
    {
        if (orderBonusTable == null || orderBonusTable.getId() == null) {
            throw new IllegalArgumentException("Bonus id is required");
        }
        validateEditableBonus(orderBonusTable);
        orderBonusTable.setAmount(
                orderBonusTable.getAmount().setScale(2, RoundingMode.HALF_UP));
        orderBonusTable.setIsReceived(null);
        orderBonusTable.setIsDistributed(null);
        orderBonusTable.setReceivedTime(null);
        orderBonusTable.setDistributionTime(null);
        return orderBonusTableMapper.updateOrderBonusTable(orderBonusTable);
    }

    /**
     * 批量删除彩金
     * 
     * @param ids 需要删除的彩金主键
     * @return 结果
     */
    @Override
    public int deleteOrderBonusTableByIds(Long[] ids)
    {
        return orderBonusTableMapper.deleteOrderBonusTableByIds(ids);
    }

    /**
     * 删除彩金信息
     * 
     * @param id 彩金主键
     * @return 结果
     */
    @Override
    public int deleteOrderBonusTableById(Long id)
    {
        return orderBonusTableMapper.deleteOrderBonusTableById(id);
    }

    @Override
    public OrderBonusTable selectActiveDistributedReceivedByUserAndOrder(Long userId, Long orderNum) {
        return orderBonusTableMapper.selectActiveDistributedReceivedByUserAndOrder(userId,orderNum);
    }

    @Override
    public List<OrderBonusTable> selectBonusByType(Long userId) {
        return orderBonusTableMapper.selectBonusByType(userId);
    }

    @Override
    public OrderBonusTable userHaveBonus(Long userId, Integer orderNum) {
        return orderBonusTableMapper.userHaveBonus(userId,orderNum);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int receiveBonus(Long id) {
        OrderBonusTable bonus = orderBonusTableMapper.selectBonusForUpdate(id);
        if (bonus == null) {
            throw new IllegalArgumentException("Bonus does not exist");
        }
        if ("0".equals(bonus.getIsReceived())) {
            return 1;
        }
        int rows = orderBonusTableMapper.receiveBonus(id);
        if (rows != 1) {
            throw new IllegalStateException("Bonus is expired or unavailable");
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int distributeBonus(Long id) {
        OrderBonusTable snapshot = orderBonusTableMapper.selectOrderBonusTableById(id);
        if (snapshot == null) {
            throw new IllegalArgumentException("Bonus does not exist");
        }
        if ("0".equals(snapshot.getIsDistributed())) {
            return 1;
        }
        if (!"0".equals(snapshot.getIsReceived())) {
            throw new IllegalStateException("The bonus must be received before it can be distributed");
        }
        if (orderUserMapper.lockUserById(snapshot.getUserId()) == null) {
            throw new IllegalStateException("Bonus user does not exist");
        }
        OrderBonusTable bonus = orderBonusTableMapper.selectBonusForUpdate(id);
        OrderUser user = orderUserMapper.selectOrderBalanceById(snapshot.getUserId());
        if (bonus == null || user == null || user.getBalance() == null) {
            throw new IllegalStateException("Bonus is unavailable");
        }
        if ("0".equals(bonus.getIsDistributed())) {
            return 1;
        }
        if ("2".equals(bonus.getDistributionType())) {
            validateManualDistributionTiming(
                    bonus,
                    orderUserMapper.selectOrderTaskUserById(bonus.getUserId()));
        }
        BigDecimal amount = bonus.getAmount().setScale(2, RoundingMode.HALF_UP);
        if (orderBonusTableMapper.distributeBonus(id) != 1
                || orderUserMapper.creditBalance(bonus.getUserId(), amount) != 1) {
            throw new IllegalStateException("Unable to distribute bonus");
        }
        transactionService.recordFlow(
                bonus.getUserId(),
                "bonus",
                amount,
                user.getBalance(),
                "manual-bonus:" + id);
        return 1;
    }

    private void validateManualDistributionTiming(
            OrderBonusTable bonus, OrderUser taskUser) {
        if (!"2".equals(bonus.getDistributionType())) {
            return;
        }
        GoodsMemberLevel level = taskUser == null ? null : taskUser.getMemberLevel();
        if (level == null || level.getOrderCountPerDay() == null) {
            throw new IllegalStateException("Unable to verify the user's task progress");
        }
        long progress = taskUser.getTaskProgress() == null
                ? 0L
                : taskUser.getTaskProgress();
        if (progress < level.getOrderCountPerDay()) {
            throw new IllegalStateException(
                    "The current task group must be completed before the bonus can be distributed");
        }
    }

    private void validateEditableBonus(OrderBonusTable bonus) {
        if (bonus == null || bonus.getUserId() == null || bonus.getUserId() <= 0) {
            throw new IllegalArgumentException("Bonus user is required");
        }
        if (bonus.getAmount() == null
                || bonus.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Bonus amount must be positive");
        }
        if (bonus.getOrderNum() == null || bonus.getOrderNum() <= 0) {
            throw new IllegalArgumentException("Bonus order number must be positive");
        }
        if (bonus.getAnimationDuration() == null || bonus.getAnimationDuration() < 0) {
            throw new IllegalArgumentException("Animation duration is required");
        }
        if (bonus.getDisplayDuration() == null || bonus.getDisplayDuration() < 0) {
            throw new IllegalArgumentException("Display duration is required");
        }
        if (!"1".equals(bonus.getDistributionType())
                && !"2".equals(bonus.getDistributionType())) {
            throw new IllegalArgumentException("Invalid distribution type");
        }
        if (!"0".equals(bonus.getPushType())
                && !"1".equals(bonus.getPushType())
                && !"2".equals(bonus.getPushType())) {
            throw new IllegalArgumentException("Invalid push type");
        }
        if ("2".equals(bonus.getPushType())
                && (bonus.getToUsers() == null || bonus.getToUsers().isBlank())) {
            throw new IllegalArgumentException("Push users are required");
        }
        if (!"2".equals(bonus.getPushType())) {
            bonus.setToUsers(null);
        }
    }
}
