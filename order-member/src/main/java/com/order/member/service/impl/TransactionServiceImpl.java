package com.order.member.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

import com.order.member.mapper.OrderUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.order.common.utils.DateUtils;
import com.order.member.domain.GoodsRechargeRecord;
import com.order.member.domain.GoodsTransactionFlow;
import com.order.member.domain.OrderUser;
import com.order.member.domain.dto.TransactionDto;
import com.order.member.domain.dto.TransactionFlowResult;
import com.order.member.domain.dto.TransactionResult;
import com.order.member.service.IGoodsRechargeRecordService;
import com.order.member.service.IGoodsTransactionFlowService;
import com.order.member.service.IOrderSequenceManagerService;
import com.order.member.service.IOrderUserService;
import com.order.member.service.ITransactionService;

/**
 * 交易处理实现
 */
@Service
public class TransactionServiceImpl implements ITransactionService {

    @Autowired
    private OrderUserMapper orderUserMapper;

    @Autowired
    private IGoodsRechargeRecordService goodsRechargeRecordService;

    @Autowired
    private IGoodsTransactionFlowService goodsTransactionFlowService;

    @Autowired
    private IOrderSequenceManagerService orderSequenceManagerService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean processTransaction(TransactionDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("dto 不能为空");
        }
        if (dto.getUserId() == null) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        if (dto.getAmount() == null) {
            throw new IllegalArgumentException("amount 不能为空");
        }

        BigDecimal amount = money(dto.getAmount());
        BigDecimal giftAmount = dto.getGiftAmount() == null
                ? BigDecimal.ZERO.setScale(2)
                : money(dto.getGiftAmount());
        if (amount.compareTo(BigDecimal.ZERO) <= 0
                || giftAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("amount must be positive");
        }

        if (orderUserMapper.lockUserById(dto.getUserId()) == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        OrderUser user = orderUserMapper.selectOrderUserById(dto.getUserId());
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        // 操作类型：0 = 加，1 = 减
        Integer opType = dto.getOperationType();
        if (opType == null) {
            throw new IllegalArgumentException("operationType 不能为空");
        }

        Date now = DateUtils.getNowDate();

        // 获取当前余额
        BigDecimal beforeBalance = money(
                user.getBalance() == null ? BigDecimal.ZERO : user.getBalance());
        BigDecimal afterBalance = beforeBalance;

        if (opType == 0) { // 加：充值
            // 使用通用方法记录充值并写流水、更新余额
            TransactionResult result = recordRecharge(
                    dto.getUserId(),
                    amount,
                    giftAmount,
                    dto.getTransactionType(),
                    dto.getRemark());
            return result != null;
        } else if (opType == 1) { // 减：扣款
             if (beforeBalance.compareTo(amount) < 0) {
                 throw new IllegalArgumentException("余额不足");
             }
             // 使用通用的写流水方法（传入负数表示扣款）
             TransactionFlowResult flowRes = recordFlow(
                     user.getId(),
                     dto.getTransactionType(),
                     amount.negate(),
                     beforeBalance,
                     dto.getRemark());
             afterBalance = flowRes.getBalanceAfter();

             // 更新用户余额
            if (orderUserMapper.debitBalance(user.getId(), amount) != 1) {
                throw new IllegalStateException("Unable to update user balance");
            }
             return true;
         } else {
             throw new IllegalArgumentException("未知的 operationType: " + opType);
         }
     }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TransactionResult recordRecharge(Long userId, BigDecimal amount, BigDecimal giftAmount, String transactionType, String remark) {
        if (userId == null) throw new IllegalArgumentException("userId 不能为空");
        if (amount == null) throw new IllegalArgumentException("amount 不能为空");

        amount = money(amount);
        giftAmount = giftAmount == null
                ? BigDecimal.ZERO.setScale(2)
                : money(giftAmount);
        if (amount.compareTo(BigDecimal.ZERO) <= 0
                || giftAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("amount must be positive");
        }

        if (orderUserMapper.lockUserById(userId) == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        OrderUser user = orderUserMapper.selectOrderUserById(userId);
        if (user == null) throw new IllegalArgumentException("用户不存在");

        Date now = DateUtils.getNowDate();
        BigDecimal beforeBalance = money(
                user.getBalance() == null ? BigDecimal.ZERO : user.getBalance());
        BigDecimal afterBalance = beforeBalance.add(amount);
        afterBalance = afterBalance.add(giftAmount);

        // 1) 插入充值记录
        GoodsRechargeRecord recharge = new GoodsRechargeRecord();
        recharge.setUserId(user.getId());
        recharge.setAmount(amount);
        recharge.setGiftAmount(giftAmount);
        recharge.setReceivedAmount(amount.add(giftAmount));
        recharge.setStatus("0");
        recharge.setCreateTime(now);
        recharge.setTransactionType(transactionType);
        String orderNo = orderSequenceManagerService.generateCode("TRADE_NO");
        recharge.setOrderNumber(orderNo);
        if (goodsRechargeRecordService.insertGoodsRechargeRecord(recharge) != 1) {
            throw new IllegalStateException("Unable to persist recharge record");
        }

        // 2) 插入交易流水：记录充值金额（使用通用方法）
        TransactionFlowResult flowRes = recordFlow(user.getId(), transactionType, amount, beforeBalance, remark);
        String flowSerial1 = flowRes.getFlowSerial();
        String tradeCode1 = flowRes.getTradeCode();
        BigDecimal midBalance = flowRes.getBalanceAfter();

        // 3) 如果有赠送金额 >0，则再保存一条交易流水
        String flowSerialGift = null;
        String tradeCodeGift = null;
        if (giftAmount.compareTo(BigDecimal.ZERO) > 0) {
            TransactionFlowResult giftRes = recordFlow(user.getId(), "zs", giftAmount, midBalance, remark);
            flowSerialGift = giftRes.getFlowSerial();
            tradeCodeGift = giftRes.getTradeCode();
            // midBalance 更新为包含赠送后的余额
            BigDecimal finalBalance = giftRes.getBalanceAfter();
            midBalance = finalBalance;
        }

        // 4) 更新用户余额
        if (orderUserMapper.creditBalance(user.getId(), amount.add(giftAmount)) != 1) {
            throw new IllegalStateException("Unable to update user balance");
        }

        // 5) 返回结果
        TransactionResult result = new TransactionResult();
        result.setOrderNumber(orderNo);
        result.setFlowSerial(flowSerial1);
        result.setTradeCode(tradeCode1);
        result.setFinalBalance(afterBalance);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TransactionFlowResult recordFlow(Long userId, String transactionType, BigDecimal amount, BigDecimal balanceBefore, String remark) {
        if (userId == null) throw new IllegalArgumentException("userId 不能为空");
        if (balanceBefore == null) balanceBefore = BigDecimal.ZERO;
        if (amount == null) throw new IllegalArgumentException("amount 不能为空");

        amount = money(amount);
        balanceBefore = money(balanceBefore);

        GoodsTransactionFlow flow = new GoodsTransactionFlow();
        flow.setUserId(userId);
        flow.setTransactionType(transactionType);
        flow.setBalanceBefore(balanceBefore);
        // 存储交易金额为绝对值，方向由 balanceAfter = balanceBefore + amount 决定
        flow.setTransactionAmount(amount);
        BigDecimal balanceAfter = balanceBefore.add(amount);
        flow.setBalanceAfter(balanceAfter);
        flow.setCreatedTime(DateUtils.getNowDate());
        String flowSerial = orderSequenceManagerService.generateCode("TRADE_NO");
        String tradeCode = orderSequenceManagerService.generateCode("TRADE_NO");
        flow.setSerialCode(flowSerial);
        flow.setTransactionCode(tradeCode);
        flow.setRemark(remark);
        if (goodsTransactionFlowService.insertGoodsTransactionFlow(flow) != 1) {
            throw new IllegalStateException("Unable to persist transaction flow");
        }

        TransactionFlowResult res = new TransactionFlowResult();
        res.setFlowSerial(flowSerial);
        res.setTradeCode(tradeCode);
        res.setBalanceAfter(balanceAfter);
        return res;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
