package com.order.member.mapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class OrderWithdrawalAdminProjectionMapperContractTest
{
    @Test
    void withdrawalAdminProjectionKeepsFullLegacyAccountValues() throws IOException
    {
        String xml = mapper("OrderWithdrawalMapper.xml");

        assertTrue(xml.contains("gwa.bank_account as gwa_bank_account"));
        assertTrue(xml.contains("gwa.account_holder as gwa_account_holder"));
        assertTrue(xml.contains("gwa.account_name as gwa_account_name"));
        assertTrue(xml.contains("gwa.wallet_address as gwa_wallet_address"));
        assertFalse(xml.contains("CONCAT('****'"));
        assertFalse(xml.contains("CONCAT(LEFT(gwa."));
    }

    private String mapper(String name) throws IOException
    {
        try (InputStream input = getClass().getResourceAsStream("/mapper/" + name))
        {
            assertNotNull(input, "Missing mapper resource " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
