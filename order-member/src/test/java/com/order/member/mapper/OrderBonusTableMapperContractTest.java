package com.order.member.mapper;

import com.order.member.domain.OrderBonusTable;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderBonusTableMapperContractTest {

    @Test
    void mapperXmlParsesWithMyBatis() throws IOException {
        Configuration configuration = new Configuration();
        configuration.getTypeAliasRegistry().registerAlias(
                "OrderBonusTable", OrderBonusTable.class);
        try (InputStream input = getClass().getResourceAsStream(
                "/mapper/OrderBonusTableMapper.xml")) {
            assertNotNull(input, "Missing OrderBonusTableMapper.xml");
            new XMLMapperBuilder(
                    input,
                    configuration,
                    "mapper/OrderBonusTableMapper.xml",
                    configuration.getSqlFragments())
                    .parse();
        }
        assertTrue(configuration.hasStatement(
                "com.order.member.mapper.OrderBonusTableMapper.selectOrderBonusTableList"));
    }

    @Test
    void administrationListResolvesPushUsernamesWithoutChangingBusinessQueries()
            throws IOException {
        String xml = mapper();
        assertTrue(xml.contains("property=\"tousernames\" column=\"to_usernames\""));

        String listProjection = statement(xml, "sql", "selectorderbonustablelistvo");
        assertTrue(listProjection.contains("group_concat("));
        assertTrue(listProjection.contains("from order_user ou"));
        assertTrue(listProjection.contains("find_in_set("));
        assertTrue(listProjection.contains("order by find_in_set("));
        assertTrue(listProjection.contains("as to_usernames"));

        String businessProjection = statement(xml, "sql", "selectorderbonustablevo");
        assertFalse(businessProjection.contains("group_concat("));
        assertFalse(businessProjection.contains("order_user"));

        String list = statement(xml, "select", "selectorderbonustablelist");
        assertTrue(list.contains("refid=\"selectorderbonustablelistvo\""));
    }

    private String mapper() throws IOException {
        try (InputStream input = getClass().getResourceAsStream(
                "/mapper/OrderBonusTableMapper.xml")) {
            assertNotNull(input, "Missing OrderBonusTableMapper.xml");
            return new String(input.readAllBytes(), StandardCharsets.UTF_8)
                    .toLowerCase(Locale.ROOT)
                    .replaceAll("\\s+", " ")
                    .trim();
        }
    }

    private String statement(String xml, String tag, String id) {
        String startTag = "<" + tag + " id=\"" + id + "\"";
        int start = xml.indexOf(startTag);
        assertTrue(start >= 0, "Missing " + id);
        int end = xml.indexOf("</" + tag + ">", start);
        assertTrue(end > start, "Unclosed " + id);
        return xml.substring(start, end);
    }
}
