package com.order.api.controller.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.member.domain.GoodsMemberLevel;
import com.order.member.domain.OrderUser;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserProfileResponseTest {

    @Test
    void publicProjectionDoesNotSerializeSecretsOrAdministrationFields() throws Exception {
        OrderUser user = new OrderUser();
        user.setId(10L);
        user.setUsername("member_1");
        user.setPassword("login-hash");
        user.setTradePassword("trade-hash");
        user.setAncestors("0,1");
        user.setRemarks("private remark");
        user.setWithdrawalPasswordFailCount(4);
        user.setBalance(new BigDecimal("12.50"));
        user.setTodayCommission(new BigDecimal("1.25"));

        GoodsMemberLevel level = new GoodsMemberLevel();
        level.setId(2L);
        level.setName("VIP2");
        user.setMemberLevel(level);

        String json = new ObjectMapper().writeValueAsString(UserProfileResponse.from(user));

        assertTrue(json.contains("\"username\":\"member_1\""));
        assertTrue(json.contains("\"balance\":12.50"));
        assertTrue(json.contains("\"memberLevel\""));
        assertFalse(json.contains("login-hash"));
        assertFalse(json.contains("trade-hash"));
        assertFalse(json.contains("ancestors"));
        assertFalse(json.contains("remarks"));
        assertFalse(json.contains("withdrawalPasswordFailCount"));
    }
}
