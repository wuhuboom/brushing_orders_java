package com.order.member.domain.dto;

import java.math.BigDecimal;

/**
 * 交易结果（简要），用于返回生成的编号及最终余额
 */
public class TransactionResult {

    private String orderNumber;
    private String flowSerial;
    private String tradeCode;
    private BigDecimal finalBalance;

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

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

    public BigDecimal getFinalBalance() {
        return finalBalance;
    }

    public void setFinalBalance(BigDecimal finalBalance) {
        this.finalBalance = finalBalance;
    }
}

