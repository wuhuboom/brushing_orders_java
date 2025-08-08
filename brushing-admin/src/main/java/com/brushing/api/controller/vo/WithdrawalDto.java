package com.brushing.api.controller.vo;

import java.math.BigDecimal;

public class WithdrawalDto {

    //提现金额
    private BigDecimal amount;

    //交易密码
    private String tradePassword;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getTradePassword() {
        return tradePassword;
    }

    public void setTradePassword(String tradePassword) {
        this.tradePassword = tradePassword;
    }
}
