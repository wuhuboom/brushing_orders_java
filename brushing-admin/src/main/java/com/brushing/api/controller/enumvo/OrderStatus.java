package com.brushing.api.controller.enumvo;

public enum OrderStatus {
    COMPLETED("0", "已完成"),
    PENDING_SUBMIT("2", "待提交"),
    FROZEN("1", "冻结");

    private final String code;
    private final String description;

    OrderStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }
}
