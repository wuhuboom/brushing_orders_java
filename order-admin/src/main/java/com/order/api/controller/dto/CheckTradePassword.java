package com.order.api.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CheckTradePassword {

    @NotBlank(message = "Trade password must not be blank")
    @Size(min = 6, max = 18, message = "Trade password length must be between 6 and 18 characters")
    private String tradePassword;

    public String getTradePassword() {
        return tradePassword;
    }

    public void setTradePassword(String tradePassword) {
        this.tradePassword = tradePassword;
    }
}
