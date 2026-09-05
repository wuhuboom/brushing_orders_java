package com.order.system.mapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import com.order.system.domain.SysUserTableColumnConfig;

class SysUserTableColumnConfigMapperContractTest
{
    @Test
    void mapperXmlParsesAndExposesAllSelfServiceStatements() throws IOException
    {
        Configuration configuration = new Configuration();
        configuration.getTypeAliasRegistry().registerAlias(
                "SysUserTableColumnConfig", SysUserTableColumnConfig.class);
        String resource = "mapper/system/SysUserTableColumnConfigMapper.xml";
        try (InputStream input = getClass().getResourceAsStream("/" + resource))
        {
            assertNotNull(input, "Missing mapper resource " + resource);
            new XMLMapperBuilder(
                    input,
                    configuration,
                    resource,
                    configuration.getSqlFragments())
                    .parse();
        }

        String namespace = SysUserTableColumnConfigMapper.class.getName();
        assertTrue(configuration.hasStatement(namespace + ".selectByUserIdAndTableKey"));
        assertTrue(configuration.hasStatement(namespace + ".upsert"));
        assertTrue(configuration.hasStatement(namespace + ".deleteByUserIdAndTableKey"));
    }

    @Test
    void persistenceIsAtomicallyUpsertedAndEveryReadWriteIsUserScoped() throws IOException
    {
        String xml;
        try (InputStream input = getClass().getResourceAsStream(
                "/mapper/system/SysUserTableColumnConfigMapper.xml"))
        {
            assertNotNull(input);
            xml = new String(input.readAllBytes(), StandardCharsets.UTF_8)
                    .toLowerCase(Locale.ROOT)
                    .replaceAll("\\s+", " ")
                    .trim();
        }

        assertTrue(xml.contains("on duplicate key update"));
        assertTrue(xml.contains("where user_id = #{userid} and table_key = #{tablekey}"));
    }
}
