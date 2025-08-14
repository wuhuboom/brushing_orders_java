package com.brushing.web.controller.member.dto;

import com.brushing.common.core.domain.BaseEntity;

public class ScopeUser extends BaseEntity {

    private Long userId;

    private String scope;

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
