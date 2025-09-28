package com.brushing.framework.web.domain;

public class MfaVerifyReq {
    private Long userId;     // 用户ID，用于识别当前请求的用户
    private int code;        // Google 验证器（TOTP）生成的验证码

    // 构造器
    public MfaVerifyReq() {}

    public MfaVerifyReq(Long userId, int code) {
        this.userId = userId;
        this.code = code;
    }

    // Getter 和 Setter
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return "MfaVerifyReq{" +
                "userId=" + userId +
                ", code=" + code +
                '}';
    }
}

