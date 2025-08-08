package com.brushing.api.controller.enumvo;

public  enum CommissionStatus {
    PENDING("1", "待发放"),
    ISSUED("0", "已发放");

    private final String code;
    private final String description;

    CommissionStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }
}
