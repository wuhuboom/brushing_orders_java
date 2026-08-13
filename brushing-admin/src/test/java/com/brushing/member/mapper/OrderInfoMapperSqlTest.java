package com.brushing.member.mapper;

import com.brushing.member.domain.OrderGoods;
import com.brushing.member.domain.OrderInfo;
import com.brushing.member.domain.vo.OrderInfoVo;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderInfoMapperSqlTest {

    private static Configuration configuration;

    @BeforeAll
    static void loadMapper() throws IOException {
        configuration = new Configuration();
        configuration.getTypeAliasRegistry().registerAlias("OrderInfo", OrderInfo.class);
        configuration.getTypeAliasRegistry().registerAlias("OrderGoods", OrderGoods.class);
        configuration.getTypeAliasRegistry().registerAlias("OrderInfoVo", OrderInfoVo.class);

        String resource = "mapper/OrderInfoMapper.xml";
        try (InputStream inputStream = OrderInfoMapperSqlTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(inputStream);
            new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments()).parse();
        }
    }

    @Test
    void frozenStatusReturnsFrozenAndPendingOrders() {
        assertCombinedStatusQuery("1");
    }

    @Test
    void pendingStatusReturnsFrozenAndPendingOrders() {
        assertCombinedStatusQuery("2");
    }

    @Test
    void otherStatusStillUsesExactMatch() {
        String sql = sqlForStatus("0");

        assertTrue(sql.contains("AND o.status = ?"));
        assertFalse(sql.contains("AND o.status IN (1, 2)"));
    }

    @Test
    void missingStatusStillReturnsAllStatuses() {
        String sql = sqlForStatus(null);

        assertFalse(sql.contains("AND o.status"));
    }

    private void assertCombinedStatusQuery(String status) {
        String sql = sqlForStatus(status);

        assertTrue(sql.contains("AND o.status IN (1, 2)"));
        assertFalse(sql.contains("AND o.status = ?"));
    }

    private String sqlForStatus(String status) {
        OrderInfo query = new OrderInfo();
        query.setUserId(7L);
        query.setStatus(status);
        query.setHasOrderGoodsHotel(false);

        BoundSql boundSql = configuration
                .getMappedStatement("com.brushing.member.mapper.OrderInfoMapper.selectOrderInfosByUser")
                .getBoundSql(query);
        return boundSql.getSql().replaceAll("\\s+", " ").trim();
    }
}
