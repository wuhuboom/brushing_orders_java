package com.order.member.mapper;

import com.order.member.domain.GoodsMemberLevel;
import com.order.member.domain.OrderUser;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderUserProductMatchingMapperTest {
    private static final String NAMESPACE = OrderUserMapper.class.getName();

    @Test
    void memberAndLevelRatiosUseDistinctResultColumns() throws IOException {
        Configuration configuration = configuration();
        ResultMap member = configuration.getResultMap(NAMESPACE + ".OrderUserResult");
        ResultMap level = configuration.getResultMap(NAMESPACE + ".GoodsMemberLevelResult");
        assertEquals("member_product_match_min", column(member, "productMatchMin"));
        assertEquals("member_product_match_max", column(member, "productMatchMax"));
        assertEquals("product_match_min", column(level, "productMatchMin"));
        assertEquals("product_match_max", column(level, "productMatchMax"));
    }

    @Test
    void adminDetailsListsProfilesAndTaskReadsProjectPersonalRatios() throws IOException {
        Configuration configuration = configuration();
        OrderUser query = new OrderUser();
        query.getParams().put("exportAfterId", 0L);
        query.getParams().put("exportMaxId", 10L);
        query.getParams().put("exportBatchSize", 2);
        for (String statement : List.of("selectOrderUserList", "selectOrderUserById",
                "selectUserProfileById", "selectOrderTaskUserById", "selectOrderUserExportBatch")) {
            String sql = sql(configuration, statement, query);
            assertTrue(sql.contains("ou.product_match_min AS member_product_match_min"), statement);
            assertTrue(sql.contains("ou.product_match_max AS member_product_match_max"), statement);
            assertTrue(sql.contains("ou.product_matching"), statement);
        }
        String adminSql = sql(configuration, "selectOrderUserById", 7L);
        assertTrue(adminSql.contains("gml.product_match_min"));
        assertTrue(adminSql.contains("gml.product_match_max"));
    }

    @Test
    void insertAndUpdateBindPersonalRatiosAndOmitThemForUnrelatedPatches() throws IOException {
        Configuration configuration = configuration();
        OrderUser incoming = new OrderUser();
        incoming.setId(7L);
        incoming.setVersion(4L);
        incoming.setProductMatching("0");
        incoming.setProductMatchMin(new BigDecimal("20"));
        incoming.setProductMatchMax(new BigDecimal("50"));
        for (String statement : List.of("insertOrderUser", "updateOrderUser")) {
            BoundSql bound = configuration.getMappedStatement(NAMESPACE + "." + statement).getBoundSql(incoming);
            String sql = bound.getSql().replaceAll("\\s+", " ");
            assertTrue(sql.contains("product_match_min"), statement);
            assertTrue(sql.contains("product_match_max"), statement);
            List<String> properties = bound.getParameterMappings().stream().map(mapping -> mapping.getProperty()).toList();
            assertTrue(properties.contains("productMatchMin"), statement);
            assertTrue(properties.contains("productMatchMax"), statement);
        }
        incoming.setProductMatching(null);
        incoming.setProductMatchMin(null);
        incoming.setProductMatchMax(null);
        incoming.setRemarks("unrelated update");
        String sql = sql(configuration, "updateOrderUser", incoming);
        assertFalse(sql.contains("product_match_min"));
        assertFalse(sql.contains("product_match_max"));
        assertTrue(sql.contains("version = ?"));
    }

    private static String column(ResultMap resultMap, String property) {
        return resultMap.getResultMappings().stream()
                .filter(mapping -> property.equals(mapping.getProperty()))
                .findFirst().orElseThrow().getColumn();
    }

    private static String sql(Configuration configuration, String statement, Object parameter) {
        return configuration.getMappedStatement(NAMESPACE + "." + statement)
                .getBoundSql(parameter).getSql().replaceAll("\\s+", " ");
    }

    private static Configuration configuration() throws IOException {
        Configuration configuration = new Configuration();
        configuration.getTypeAliasRegistry().registerAlias("OrderUser", OrderUser.class);
        configuration.getTypeAliasRegistry().registerAlias("GoodsMemberLevel", GoodsMemberLevel.class);
        try (InputStream input = OrderUserProductMatchingMapperTest.class
                .getResourceAsStream("/mapper/OrderUserMapper.xml")) {
            assertNotNull(input);
            new XMLMapperBuilder(input, configuration, "mapper/OrderUserMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
        return configuration;
    }
}
