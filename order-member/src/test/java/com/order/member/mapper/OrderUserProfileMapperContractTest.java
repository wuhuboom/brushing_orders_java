package com.order.member.mapper;

import com.order.member.domain.GoodsMemberLevel;
import com.order.member.domain.OrderUser;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderUserProfileMapperContractTest {

    @Test
    void profileSumsOnlyDistributedLuckyBonusesAndDefaultsToZero() throws IOException {
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

        assertTrue(sql.contains("SELECT SUM(obt.amount) FROM order_bonus_table obt"));
        assertTrue(sql.contains("obt.user_id = ou.id"));
        assertTrue(sql.contains("obt.is_distributed = '0'"));
        assertTrue(sql.contains("0) AS lucky_bonus"));
        assertFalse(sql.contains("obt.is_distributed = '1'"));
        assertFalse(sql.contains("obt.is_received"));
    }
}
