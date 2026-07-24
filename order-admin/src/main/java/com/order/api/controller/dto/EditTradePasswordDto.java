package com.order.api.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EditTradePasswordDto {

    @NotBlank(message = "Old trade password must not be blank")
    @Size(max = 18, message = "Old trade password is too long")
    private String oldTradePassword;

    @NotBlank(message = "New trade password must not be blank")
    @Size(min = 6, max = 18, message = "Trade password length must be between 6 and 18 characters")
    private String newTradePassword;

    public String getOldTradePassword() {
        return oldTradePassword;
    }

    public void setOldTradePassword(String oldTradePassword) {
        this.oldTradePassword = oldTradePassword;
    }

    public String getNewTradePassword() {
        return newTradePassword;
    }

    public void setNewTradePassword(String newTradePassword) {
        this.newTradePassword = newTradePassword;
    }
}
