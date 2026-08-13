package com.order.member.service.impl;

import java.math.BigDecimal;
import java.util.List;

import com.order.common.exception.ServiceException;
import com.order.member.domain.OrderInfo;
import com.order.member.domain.OrderUser;
import com.order.member.mapper.GoodsExtraCommissionSettingMapper;
import com.order.member.mapper.OrderInfoMapper;
import com.order.member.mapper.OrderUserMapper;
import com.order.member.service.IOrderInfoService;
import com.order.member.service.ITransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单Service业务层处理
 * 
 * @author order
 * @date 2025-11-10
 */
@Service
public class OrderInfoServiceImpl implements IOrderInfoService 
{
    private static final Logger log = LoggerFactory.getLogger(OrderInfoServiceImpl.class);

    private final OrderInfoMapper orderInfoMapper;
    private final OrderUserMapper orderUserMapper;
    private final GoodsExtraCommissionSettingMapper extraCommissionMapper;
    private final ITransactionService transactionService;

    public OrderInfoServiceImpl(
            OrderInfoMapper orderInfoMapper,
            OrderUserMapper orderUserMapper,
            GoodsExtraCommissionSettingMapper extraCommissionMapper,
            ITransactionService transactionService) {
        this.orderInfoMapper = orderInfoMapper;
        this.orderUserMapper = orderUserMapper;
        this.extraCommissionMapper = extraCommissionMapper;
        this.transactionService = transactionService;
    }

    /**
     * 查询订单
     * 
     * @param id 订单主键
     * @return 订单
     */
    @Override
    public OrderInfo selectOrderInfoById(Long id)
    {
        return orderInfoMapper.selectOrderInfoById(id);
    }

    /**
     * 查询订单列表
     * 
     * @param orderInfo 订单
     * @return 订单
     */
    @Override
    public List<OrderInfo> selectOrderInfoList(OrderInfo orderInfo)
    {
        return orderInfoMapper.selectOrderInfoList(orderInfo);
    }

    /**
     * 新增订单
     * 
     * @param orderInfo 订单
     * @return 结果
     */
    @Override
    public int insertOrderInfo(OrderInfo orderInfo)
    {
        throw new UnsupportedOperationException(
                "Financial orders must be created by OrderApplicationService");
    }

    /**
     * 修改订单
     * 
     * @param orderInfo 订单
     * @return 结果
     */
    @Override
    public int updateOrderInfo(OrderInfo orderInfo)
    {
        if (orderInfo == null || orderInfo.getId() == null) {
            throw new ServiceException("订单ID不能为空");
        }
        if (orderInfo.getExpiryTime() == null) {
            throw new ServiceException("过期时间不能为空");
        }
        return orderInfoMapper.updateOrderInfo(orderInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cancelPendingOrder(Long id)
    {
        if (id == null) {
            throw new ServiceException("订单ID不能为空");
        }

        OrderInfo snapshot = orderInfoMapper.selectOrderInfoById(id);
        if (snapshot == null || snapshot.getUserId() == null) {
            throw new ServiceException("订单不存在");
        }
        Long userId = snapshot.getUserId();
        if (orderUserMapper.lockUserById(userId) == null) {
            throw new ServiceException("订单用户不存在");
        }

        OrderInfo order = orderInfoMapper.selectOwnedOrderForUpdate(id, userId);
        if (order == null || !"1".equals(order.getStatus())) {
            throw new ServiceException("只有待提交订单可以取消");
        }
        if (order.getAmount() == null || order.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("订单金额不正确");
        }

        OrderUser user = orderUserMapper.selectOrderBalanceById(userId);
        if (user == null || user.getBalance() == null) {
            throw new ServiceException("订单用户余额不存在");
        }
        if (orderInfoMapper.transitionStatus(id, userId, "1", "3") != 1) {
            throw new ServiceException("订单状态已变更，请刷新后重试");
        }

        long progressDelta = "0".equals(order.getType()) ? 1L : 0L;
        if (orderUserMapper.releaseCancelledOrderFunds(
                userId, order.getAmount(), progressDelta) != 1) {
            throw new ServiceException("订单冻结金额不一致，取消失败");
        }
        releaseExtraCommission(userId, order);
        transactionService.recordFlowWithoutNotification(
                userId,
                "rw",
                order.getAmount(),
                user.getBalance(),
                "order-cancel:" + order.getOrderNumber());
        return 1;
    }

    private void releaseExtraCommission(Long userId, OrderInfo order) {
        Long settingId = order.getExtraCommissionId();
        if (settingId == null) {
            return;
        }
        if (extraCommissionMapper.releaseReserved(
                settingId,
                userId,
                order.getOrderCount(),
                order.getAmount()) == 1) {
            return;
        }
        if (order.getLinkId() != null && order.getLinkId().equals(settingId)) {
            log.warn(
                    "event=legacy_link_extra_commission_cancel_ignored userId={} orderId={} linkId={}",
                    userId, order.getId(), order.getLinkId());
            return;
        }
        throw new ServiceException("额外佣金配置状态已变更，取消失败");
    }

    /**
     * 批量删除订单
     * 
     * @param ids 需要删除的订单主键
     * @return 结果
     */
    @Override
    public int deleteOrderInfoByIds(Long[] ids)
    {
        throw new UnsupportedOperationException(
                "Financial order history cannot be deleted");
    }

    /**
     * 删除订单信息
     * 
     * @param id 订单主键
     * @return 结果
     */
    @Override
    public int deleteOrderInfoById(Long id)
    {
        throw new UnsupportedOperationException(
                "Financial order history cannot be deleted");
    }

    @Override
    public OrderInfo hasOpenOrders(Long userId) {
        return orderInfoMapper.hasOpenOrders(userId);
    }
}
