package com.brushing.web.controller.member.dto;

import com.brushing.common.core.domain.BaseEntity;

public class ScopeUser extends BaseEntity {

    private Long userId;

    private String scope;

    // 可选：按下级用户名模糊过滤
    private String subUsername;

    // 可选：按下级电话号码过滤（支持4位尾号匹配或完整号码）
    private String subPhone;

    public String getSubPhone() {
        return subPhone;
    }

    public void setSubPhone(String subPhone) {
        this.subPhone = subPhone;
    }

    public String getSubUsername() {
        return subUsername;
    }

    public void setSubUsername(String subUsername) {
        this.subUsername = subUsername;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
}
