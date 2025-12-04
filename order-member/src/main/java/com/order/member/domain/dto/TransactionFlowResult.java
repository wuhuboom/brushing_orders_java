package com.order.member.domain.dto;

import java.math.BigDecimal;

/**
 * 单条交易流水结果
 */
public class TransactionFlowResult {
    private String flowSerial;
    private String tradeCode;
    private BigDecimal balanceAfter;

    public String getFlowSerial() {
        return flowSerial;
    }

    public void setFlowSerial(String flowSerial) {
        this.flowSerial = flowSerial;
    }

    public String getTradeCode() {
        return tradeCode;
    }

    public void setTradeCode(String tradeCode) {
        this.tradeCode = tradeCode;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }
}

