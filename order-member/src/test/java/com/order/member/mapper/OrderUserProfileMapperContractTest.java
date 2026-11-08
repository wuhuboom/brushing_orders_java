package com.order.member.mapper;

import com.order.member.domain.GoodsMemberLevel;
import com.order.member.domain.OrderUser;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderUserProfileMapperContractTest {

    @Test
    void profilePreservesDistributedTotalAndIncludesClaimedUndistributedBonus() throws IOException {
        Configuration configuration = new Configuration();
        configuration.getTypeAliasRegistry().registerAlias("OrderUser", OrderUser.class);
        configuration.getTypeAliasRegistry().registerAlias("GoodsMemberLevel", GoodsMemberLevel.class);

        String resource = "mapper/OrderUserMapper.xml";
        try (InputStream input = getClass().getResourceAsStream("/" + resource)) {
            assertNotNull(input, "Missing " + resource);
            new XMLMapperBuilder(
                    input,
                    configuration,
                    resource,
                    configuration.getSqlFragments()).parse();
        }

        BoundSql boundSql = configuration
                .getMappedStatement(OrderUserMapper.class.getName() + ".selectUserProfileById")
                .getBoundSql(7L);
        String sql = boundSql.getSql().replaceAll("\\s+", " ");

        assertTrue(sql.contains("SELECT obt.amount FROM order_bonus_table obt"));
        assertTrue(sql.contains("obt.user_id = ou.id"));
        assertTrue(sql.contains("obt.is_received = '1'"));
        assertTrue(sql.contains("obt.is_received = '0' OR ( obt.is_received = '1'"));
        assertTrue(sql.contains("obt.is_distributed = '1'"));
        assertTrue(sql.contains("obt.order_num = COALESCE(ou.task_progress, 0) + 1"));
        assertTrue(sql.contains("obt.expiry_time IS NULL OR obt.expiry_time > NOW()"));
        assertTrue(sql.contains("NOT EXISTS ( SELECT 1 FROM order_info oi"));
        assertTrue(sql.contains("oi.user_id = ou.id AND oi.status = '1'"));
        assertTrue(sql.contains("ORDER BY CASE WHEN obt.is_received = '0' THEN 0 ELSE 1 END,"));
        assertTrue(sql.contains("obt.expiry_time ASC, obt.create_time ASC, obt.id ASC LIMIT 1"));
        assertTrue(sql.contains("0) AS lucky_bonus"));
        assertTrue(sql.contains("SELECT SUM(obt.amount) FROM order_bonus_table obt"));
        assertTrue(sql.contains("obt.is_distributed = '0'"));
        assertTrue(sql.contains("0) AS pending_bonus_amount"));
    }
}
