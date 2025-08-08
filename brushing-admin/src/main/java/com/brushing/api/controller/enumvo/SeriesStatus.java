package com.brushing.api.controller.enumvo;

public enum SeriesStatus {
    COMPLETED("0", "已完成"),
    PENDING_SUBMIT("2", "待提交"),
    FROZEN("3", "冻结"),
    INCOMPLETE("1", "未完成");


    private final String code;
    private final String description;

    SeriesStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }
}
