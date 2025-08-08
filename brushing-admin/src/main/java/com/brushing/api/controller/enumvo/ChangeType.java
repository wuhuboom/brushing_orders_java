package com.brushing.api.controller.enumvo;

public enum ChangeType {
    ORDER("1", "订单下单"),
    COMMISSION("5", "佣金返还"),
    REBATE("6", "下级返佣"),
    REFUND("7", "本金返还");

    private final String code;
    private final String description;

    ChangeType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }
}
