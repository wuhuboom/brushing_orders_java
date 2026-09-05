package com.order.member.mapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderCompletionStatisticsMapperContractTest {
    private static final String COMPLETED_STATUSES =
            "status in ('0', 'completed', 'complete', 'success')";

    @Test
    void dashboardOrderAndTaskCountsOnlyIncludeCompletedOrders() throws IOException {
        String xml = mapper("DashboardMapper.xml");

        for (String statementId : new String[]{
                "getTodayTasksCompleted",
                "getTotalTasksCompleted",
                "getTodayBettingUsers",
                "getTotalOrders",
                "getTodayOrders",
                "getTotalBettingUsers"
        }) {
            String statement = statement(xml, "select", statementId);
            assertTrue(statement.contains(COMPLETED_STATUSES), statementId);
            assertFalse(statement.contains("status in ('2'"), statementId);
        }

        String weekly = statement(xml, "select", "getWeeklyTransactionCounts");
        assertTrue(occurrences(weekly, COMPLETED_STATUSES) >= 2);
        assertFalse(weekly.contains("status in ('2'"));
    }

    @Test
    void legacyMarketingOrderCountsOnlyIncludeCompletedOrders() throws IOException {
        String xml = mapper("LegacyMarketingMapper.xml");

        String daily = statement(xml, "sql", "dailyActivityUnion");
        assertTrue(daily.contains("oi.status in ('0', 'completed', 'complete', 'success')"));
        assertFalse(daily.contains("oi.status in ('2'"));

        String memberStatistics = statement(xml, "select", "selectMemberStatistics");
        assertTrue(memberStatistics.contains(COMPLETED_STATUSES));
        assertFalse(memberStatistics.contains("status in ('2'"));
    }

    @Test
    void progressMovesToTheCompletedOrderOnlyDuringSettlement() throws IOException {
        String xml = mapper("OrderUserMapper.xml");
        String reserve = statement(xml, "update", "reserveOrderFunds");
        String settle = statement(xml, "update", "settleOrderFunds");
        String cancel = statement(xml, "update", "releaseCancelledOrderFunds");
        String balanceProjection = statement(xml, "select", "selectOrderBalanceById");

        assertTrue(balanceProjection.contains("task_progress"));
        assertTrue(reserve.contains("task_progress = coalesce(task_progress, 0) + #{progressdelta}"));
        assertTrue(cancel.contains("coalesce(task_progress, 0) - #{progressdelta}"));
        assertTrue(settle.contains("task_progress = greatest("));
        assertTrue(settle.contains("#{completedordercount}"));
        assertFalse(settle.contains("task_progress = coalesce(task_progress, 0) + 1"));
    }

    @Test
    void taskProgressAdjustmentHasADedicatedWriteAndDetectsFrozenLinkedGroups()
            throws IOException {
        String userXml = mapper("OrderUserMapper.xml");
        String progressUpdate = statement(userXml, "update", "updateTaskProgress");
        assertTrue(progressUpdate.contains("task_progress = #{taskprogress}"));
        assertTrue(progressUpdate.contains("version = coalesce(version, 0) + 1"));

        String progressReset = statement(userXml, "update", "resetTaskProgress");
        assertTrue(progressReset.contains("task_progress = 0"));
        assertTrue(progressReset.contains("today_rest = coalesce(today_rest, 0) + 1"));
        assertTrue(progressReset.contains("total_rest = coalesce(total_rest, 0) + 1"));

        String orderXml = mapper("OrderInfoMapper.xml");
        String frozenLinked = statement(
                orderXml, "select", "hasFrozenLinkedOrders");
        assertTrue(frozenLinked.contains("type = '1'"));
        assertTrue(frozenLinked.contains("status = '2'"));
    }

    private String mapper(String name) throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/mapper/" + name)) {
            assertNotNull(input, "Missing mapper resource " + name);
            return normalize(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private String statement(String xml, String tag, String id) {
        String startTag = "<" + tag + " id=\"" + id.toLowerCase(Locale.ROOT) + "\"";
        int start = xml.indexOf(startTag);
        assertTrue(start >= 0, "Missing " + id);
        int end = xml.indexOf("</" + tag + ">", start);
        assertTrue(end > start, "Unclosed " + id);
        return xml.substring(start, end);
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private int occurrences(String value, String token) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(token, offset)) >= 0) {
            count++;
            offset += token.length();
        }
        return count;
    }
}
