package com.brushing.api.controller.vo;

import java.math.BigDecimal;

public class WithdrawalDto {

    //提现金额
    private BigDecimal amount;

    //交易密码
    private String tradePassword;


    private Long walletId;

    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
    }

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
