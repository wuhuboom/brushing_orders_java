package com.order.member.mapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class DashboardPendingWithdrawalMapperContractTest
{
    @Test
    void headerCounterOnlyCountsPendingWithdrawals() throws IOException
    {
        String xml = mapperXml();
        String pending = statement(xml, "getPendingWithdrawals");
        String total = statement(xml, "getTotalWithdrawals");

        assertTrue(pending.contains("from order_withdrawal where status = '1'"));
        assertFalse(total.contains("where status"));
    }

    private String mapperXml() throws IOException
    {
        try (InputStream input = getClass().getResourceAsStream("/mapper/DashboardMapper.xml"))
        {
            assertNotNull(input, "Missing DashboardMapper.xml");
            return normalize(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private String statement(String xml, String id)
    {
        String startTag = "<select id=\"" + id.toLowerCase(Locale.ROOT) + "\"";
        int start = xml.indexOf(startTag);
        assertTrue(start >= 0, "Missing " + id);
        int end = xml.indexOf("</select>", start);
        assertTrue(end > start, "Unclosed " + id);
        return xml.substring(start, end);
    }

    private String normalize(String value)
    {
        return value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
