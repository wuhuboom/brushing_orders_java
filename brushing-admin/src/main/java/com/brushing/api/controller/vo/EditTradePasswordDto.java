package com.brushing.api.controller.vo;

public class EditTradePasswordDto {

    private String oldTradePassword;

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
