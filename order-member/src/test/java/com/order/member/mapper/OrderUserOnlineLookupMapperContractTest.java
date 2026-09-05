package com.order.member.mapper;

import com.order.member.domain.GoodsMemberLevel;
import com.order.member.domain.OrderUser;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
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

class OrderUserOnlineLookupMapperContractTest
{
    @Test
    void usernameBatchLookupDoesNotReuseTheAdministrationReportingQuery() throws IOException
    {
        Configuration configuration = mapperConfiguration();

        BoundSql boundSql = configuration
                .getMappedStatement(OrderUserMapper.class.getName() + ".selectUsernamesByIds")
                .getBoundSql(Map.of("userIds", List.of(7L, 8L)));
        String sql = boundSql.getSql().replaceAll("\\s+", " ").toLowerCase();

        assertTrue(sql.contains("select username from order_user"));
        assertTrue(sql.contains("where id in"));
        assertEquals(2, boundSql.getParameterMappings().size());
        assertFalse(sql.contains("with "));
        assertFalse(sql.contains("goods_transaction_flow"));
        assertFalse(sql.contains(" join "));
    }

    @Test
    void usernameBatchLookupProducesValidSqlForAnEmptyCollection() throws IOException
    {
        Configuration configuration = mapperConfiguration();

        BoundSql boundSql = configuration
                .getMappedStatement(OrderUserMapper.class.getName() + ".selectUsernamesByIds")
                .getBoundSql(Map.of("userIds", List.of()));
        String sql = boundSql.getSql().replaceAll("\\s+", " ");

        assertTrue(sql.contains("where 1 = 0"));
        assertEquals(0, boundSql.getParameterMappings().size());
    }

    private Configuration mapperConfiguration() throws IOException
    {
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
        return configuration;
    }
}
