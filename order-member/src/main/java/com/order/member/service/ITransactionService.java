package com.order.member.service;

import com.order.member.domain.dto.TransactionDto;
import com.order.member.domain.dto.TransactionResult;
import com.order.member.domain.dto.TransactionFlowResult;

/**
 * 交易处理相关服务
 */
public interface ITransactionService {
    /**
     * 处理一次交易（充值/扣款/赠送）
     * @param dto 请求 DTO
     * @return true 成功，false 失败（异常会抛出）
     */
    public boolean processTransaction(TransactionDto dto);

    /**
     * 通用的：记录充值记录并写交易流水，同时更新用户余额。
     * 其他模块/方法可直接调用此方法减少重复代码。
     * @param userId 用户ID
     * @param amount 充值金额
     * @param giftAmount 赠送金额（可为 0）
     * @param transactionType 交易类型字符串
     * @param remark 备注
     * @return TransactionResult 包含生成的订单号、流水号、最终余额等
     */
    TransactionResult recordRecharge(Long userId, java.math.BigDecimal amount, java.math.BigDecimal giftAmount, String transactionType, String remark);

    /**
     * 通用的：记录一条交易流水（不负责更新用户余额），
     * amount 为正表示加，负表示减；balanceBefore 必须由调用方传入。
     * @return TransactionFlowResult 包含生成的流水编号、交易编号与计算后的余额
     */
    TransactionFlowResult recordFlow(Long userId, String transactionType, java.math.BigDecimal amount, java.math.BigDecimal balanceBefore, String remark);
}
