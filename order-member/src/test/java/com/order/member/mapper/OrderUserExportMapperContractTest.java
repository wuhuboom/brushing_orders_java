package com.order.member.mapper;

import com.order.member.domain.GoodsMemberLevel;
import com.order.member.domain.OrderUser;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderUserExportMapperContractTest {
    @Test
    void memberBatchUsesKeysetLimitAndOnlyAggregatesSelectedMembers() throws IOException {
        OrderUser query = batchQuery();
        MappedStatement statement = configuration().getMappedStatement(
                OrderUserMapper.class.getName() + ".selectOrderUserExportBatch");
        BoundSql bound = statement.getBoundSql(query);
        String sql = normalize(bound.getSql());

        assertTrue(sql.contains("ou.id > ? and ou.id <= ?"));
        assertTrue(sql.contains("order by ou.id asc limit ?"));
        assertFalse(sql.contains("offset"));
        assertFalse(sql.contains("last_logins as ("), "Unfiltered export must not rank the entire login table");
        assertTrue(sql.contains("from order_login_log oll join selected_users su on su.id = oll.user_id"));
        assertTrue(sql.contains("from order_withdrawal ow join selected_users su on su.id = ow.user_id"));
        assertTrue(sql.contains("from goods_recharge_record grr join selected_users su on su.id = grr.user_id"));
        assertTrue(sql.contains("from order_user child join selected_users su on su.id = child.parent_id"));
        for (String excluded : List.of("ou.password", "ou.trade_password", "ou.identity_number",
                "ou.identity_front_image", "ou.user_contract_content", "ou.formal_contract_content")) {
            assertFalse(sql.contains(excluded), "Large or sensitive payload must not enter a batch: " + excluded);
        }
        assertFalse(statement.isUseCache());
        assertTrue(statement.isFlushCacheRequired());
        assertEquals(500, statement.getFetchSize());
        assertEquals(List.of("params.exportAfterId", "params.exportMaxId", "params.exportBatchSize"),
                bound.getParameterMappings().stream().map(mapping -> mapping.getProperty()).toList());
    }

    @Test
    void listAndExportKeepTheSameFiltersIncludingGlobalExactKeywordPriority() throws IOException {
        Configuration configuration = configuration();
        OrderUser query = batchQuery();
        query.setKeyword("member-a");
        query.setParentUsername("parent");
        query.setLastLoginIp("127.0");
        query.setUsernameList("member-a,member-b");
        query.setBalanceMin(java.math.BigDecimal.ZERO);
        query.setAccountStatus("0");
        query.getParams().put("beginTime", "2026-11-01");
        query.getParams().put("endTime", "2026-11-06");
        String namespace = OrderUserMapper.class.getName();
        BoundSql listed = configuration.getMappedStatement(namespace + ".selectOrderUserList").getBoundSql(query);
        BoundSql exported = configuration.getMappedStatement(namespace + ".selectOrderUserExportBatch").getBoundSql(query);
        List<String> exportProperties = exported.getParameterMappings().stream()
                .map(mapping -> mapping.getProperty()).toList();
        assertEquals(listed.getParameterMappings().stream().map(mapping -> mapping.getProperty()).toList(),
                exportProperties.subList(2, exportProperties.size() - 1));
        String sql = normalize(exported.getSql());
        assertTrue(sql.contains("last_logins as ("));
        assertTrue(sql.contains("from order_user keyword_ou"));
        assertTrue(sql.contains("left join last_logins keyword_ll"));
        assertTrue(sql.contains("where latest.user_id = ou.id order by latest.create_time desc, latest.id desc limit 1"));
    }

    @Test
    void exportBoundaryIsASingleUncachedAggregate() throws IOException {
        MappedStatement statement = configuration().getMappedStatement(
                OrderUserMapper.class.getName() + ".selectOrderUserExportMaxId");
        assertEquals("select max(id) from order_user", normalize(statement.getBoundSql(null).getSql()));
        assertFalse(statement.isUseCache());
    }

    @Test
    void emptyExportScopeCannotFallThroughToAllMembers() throws IOException {
        BoundSql bound = statement(List.of());
        String sql = normalize(bound.getSql());

        assertTrue(sql.contains("select id, task_progress from order_user where 1 = 0"));
        assertFalse(sql.contains("in ()"));
        assertEquals(0, bound.getParameterMappings().size());
    }

    @Test
    void totalsUseOnlySelectedMembersAndPreserveTheExistingBonusEligibilityRules() throws IOException {
        BoundSql bound = statement(List.of(7L, 8L));
        String sql = normalize(bound.getSql());

        assertEquals(2, bound.getParameterMappings().size());
        assertTrue(sql.contains("select id, task_progress from order_user where id in ( ? , ? )"));
        assertTrue(sql.contains("gtf.transaction_type = 'fy'"));
        assertFalse(sql.contains("gtf.created_time"), "Cumulative commission must include all dates");
        assertTrue(sql.contains("obt.is_distributed = '0'"));
        assertTrue(sql.contains("obt.is_distributed = '1'"));
        assertTrue(sql.contains("obt.is_received = '0' or ( obt.is_received = '1'"));
        assertTrue(sql.contains("obt.order_num = coalesce(su.task_progress, 0) + 1"));
        assertTrue(sql.contains("obt.expiry_time is null or obt.expiry_time > now()"));
        assertTrue(sql.contains("oi.user_id = su.id and oi.status = '1'"));
        assertTrue(sql.contains("partition by obt.user_id"), "Pending bonus priority must apply per member");
        assertTrue(sql.contains("order by case when obt.is_received = '0' then 0 else 1 end,"));
        assertTrue(sql.contains("pb.user_id = su.id and pb.rn = 1"));
    }

    private BoundSql statement(List<Long> ids) throws IOException {
        return configuration().getMappedStatement(OrderUserMapper.class.getName() + ".selectOrderUserExportStatistics")
                .getBoundSql(Map.of("userIds", ids));
    }

    private static OrderUser batchQuery() {
        OrderUser query = new OrderUser();
        query.getParams().put("exportAfterId", 100L);
        query.getParams().put("exportMaxId", 1000L);
        query.getParams().put("exportBatchSize", 500);
        return query;
    }

    private Configuration configuration() throws IOException {
        Configuration configuration = new Configuration();
        configuration.getTypeAliasRegistry().registerAlias("OrderUser", OrderUser.class);
        configuration.getTypeAliasRegistry().registerAlias("GoodsMemberLevel", GoodsMemberLevel.class);
        String resource = "mapper/OrderUserMapper.xml";
        try (InputStream input = getClass().getResourceAsStream("/" + resource)) {
            assertNotNull(input, "Missing " + resource);
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        return configuration;
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").toLowerCase().trim();
    }
}
