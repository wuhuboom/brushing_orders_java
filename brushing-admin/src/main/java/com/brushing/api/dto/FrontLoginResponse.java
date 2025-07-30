package com.brushing.api.dto;

import com.brushing.member.domain.OrderMemberUser;

public class FrontLoginResponse {
    private String token;
    private OrderMemberUser user;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public OrderMemberUser getUser() {
        return user;
    }

    public void setUser(OrderMemberUser user) {
        this.user = user;
    }
}
