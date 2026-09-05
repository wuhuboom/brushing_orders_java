package com.order.member.mapper;

import com.order.member.domain.GoodsMemberLevel;
import com.order.member.domain.OrderUser;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderUserKeywordMapperContractTest
{
    @Test
    void keywordListStatementParsesAndRendersThroughMyBatis() throws IOException
    {
        Configuration configuration = new Configuration();
        configuration.getTypeAliasRegistry().registerAlias("OrderUser", OrderUser.class);
        configuration.getTypeAliasRegistry().registerAlias("GoodsMemberLevel", GoodsMemberLevel.class);

        try (InputStream input = mapperStream()) {
            new XMLMapperBuilder(
                    input,
                    configuration,
                    "mapper/OrderUserMapper.xml",
                    configuration.getSqlFragments()).parse();
        }

        OrderUser query = new OrderUser();
        query.setKeyword("IF2XQ");
        BoundSql boundSql = configuration
                .getMappedStatement(OrderUserMapper.class.getName() + ".selectOrderUserList")
                .getBoundSql(query);
        String sql = boundSql.getSql().replaceAll("\\s+", " ");

        assertTrue(sql.contains("ou.invite_code = ?"));
        assertTrue(sql.contains("pu.invite_code = ?"));
        assertTrue(sql.contains("not exists ("));
        assertTrue(sql.contains("keyword_ou.invite_code = ?"));
        assertTrue(sql.contains("ou.invite_code like concat('%', ?, '%')"));
        assertTrue(sql.contains("pu.invite_code like concat('%', ?, '%')"));
    }

    @Test
    void keywordSearchUsesExactMatchesBeforeFallingBackToPartialMatches() throws IOException
    {
        String xml = mapper();

        assertTrue(xml.contains("ou.username = #{keyword}"));
        assertTrue(xml.contains("ou.phone_number = #{keyword}"));
        assertTrue(xml.contains("ou.invite_code = #{keyword}"));
        assertTrue(xml.contains("pu.invite_code = #{keyword}"));
        assertTrue(xml.contains("keyword_ou.invite_code = #{keyword}"));
        assertTrue(xml.contains("ll.ip = #{keyword}"));
        assertTrue(xml.contains("not exists ("));
        assertTrue(xml.contains("ou.username like concat('%', #{keyword}, '%')"));
    }

    @Test
    void keywordInviteCodeMatchesBothMemberAndDisplayedParentInviteCodes() throws IOException
    {
        String xml = mapper();

        assertTrue(xml.contains("ou.invite_code like concat('%', #{keyword}, '%')"));
        assertTrue(xml.contains("pu.invite_code like concat('%', #{keyword}, '%')"));
    }

    private String mapper() throws IOException
    {
        try (InputStream input = mapperStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private InputStream mapperStream()
    {
        InputStream input = getClass().getResourceAsStream("/mapper/OrderUserMapper.xml");
        assertNotNull(input, "Missing OrderUserMapper.xml");
        return input;
    }
}
