package com.order.member.service;

import com.alibaba.druid.pool.DruidDataSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.common.i18n.SupportedLocale;
import com.order.common.i18n.Translations;
import com.order.member.domain.OrderConfig;
import com.order.member.mapper.OrderConfigMapper;
import com.order.member.mapper.TranslationsMapper;
import com.order.member.service.impl.OrderConfigServiceImpl;
import com.order.member.service.impl.TranslationsServiceImpl;
import db.migration.mysql.V2026_11_10_00__rich_text_relative_copies;
import java.io.InputStream;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Opt-in integration fixture. Only the isolated localhost hardening database is accepted. */
class RichTextRelativeMigrationMySqlTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String HTML = "<p>Existing text</p><img src=\"https://old.example.com/profile/upload/a.jpg?size=2#image\">";
    private static final String RELATIVE = "/profile/upload/a.jpg?size=2#image";
    private DruidDataSource source;
    private JdbcTemplate jdbc;

    @BeforeEach
    void setup() throws Exception {
        String url = System.getProperty("hardening.jdbcUrl");
        assumeTrue(url != null, "Requires isolated codex_backend_hardening_test database");
        URI endpoint = URI.create(url.substring("jdbc:".length()));
        assertEquals("127.0.0.1", endpoint.getHost());
        assertEquals("/codex_backend_hardening_test", endpoint.getPath());
        assertEquals("127.0.0.1:" + endpoint.getPort(), endpoint.getRawAuthority());
        assertNull(endpoint.getRawQuery());
        assertNull(endpoint.getRawFragment());
        source = new DruidDataSource();
        source.setUrl(url);
        source.setUsername(System.getenv("HARDENING_DB_USER"));
        source.setPassword(System.getenv("HARDENING_DB_PASSWORD"));
        jdbc = new JdbcTemplate(source);
        assertEquals("codex_backend_hardening_test", jdbc.queryForObject("SELECT DATABASE()", String.class));
        jdbc.execute("SET FOREIGN_KEY_CHECKS = 0");
        for (String table : List.of("order_config", "goods", "goods_member_level", "order_site_message", "translations")) {
            jdbc.execute("DROP TABLE IF EXISTS " + table);
        }
        jdbc.execute("SET FOREIGN_KEY_CHECKS = 1");
        jdbc.execute("""
                CREATE TABLE order_config (id BIGINT AUTO_INCREMENT PRIMARY KEY, type VARCHAR(64),
                sort BIGINT, name VARCHAR(64), content LONGTEXT, translations_id BIGINT,
                create_time DATETIME, update_time DATETIME) ENGINE=InnoDB
                """);
        jdbc.execute("CREATE TABLE goods (id BIGINT PRIMARY KEY, description LONGTEXT) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE goods_member_level (id BIGINT PRIMARY KEY, description LONGTEXT) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE order_site_message (id BIGINT PRIMARY KEY, content LONGTEXT) ENGINE=InnoDB");
        String languages = String.join(", ", SupportedLocale.all().stream()
                .map(locale -> locale.getCode() + " LONGTEXT").toList());
        jdbc.execute("CREATE TABLE translations (id BIGINT AUTO_INCREMENT PRIMARY KEY, " + languages + ") ENGINE=InnoDB");
    }

    @AfterEach
    void close() {
        if (source != null) source.close();
    }

    @Test
    void migrationBackfillsAllCopiesAndLanguagesWithoutChangingOriginalsAndCanResume() throws Exception {
        String config = JSON.writeValueAsString(java.util.Map.of("certificateContent", HTML, "sort", 3));
        jdbc.update("INSERT INTO order_config(id, content) VALUES (1, ?), (2, NULL), (3, '')", config);
        jdbc.update("INSERT INTO goods_member_level(id, description) VALUES (1, ?)", HTML);
        jdbc.update("INSERT INTO order_site_message(id, content) VALUES (1, ?)", HTML);
        for (int id = 1; id <= 105; id++) {
            jdbc.update("INSERT INTO goods(id, description) VALUES (?, ?)", id, HTML);
        }
        jdbc.update("INSERT INTO translations(id, en_US, ja_JP, zh_CN) VALUES (1, ?, ?, '')", config, HTML);
        migrate();
        assertEquals(config, jdbc.queryForObject("SELECT content FROM order_config WHERE id=1", String.class));
        assertEquals(HTML, jdbc.queryForObject("SELECT description FROM goods WHERE id=105", String.class));
        assertEquals(HTML, jdbc.queryForObject("SELECT description FROM goods_member_level WHERE id=1", String.class));
        assertEquals(HTML, jdbc.queryForObject("SELECT content FROM order_site_message WHERE id=1", String.class));
        assertTrue(JSON.readTree(jdbc.queryForObject("SELECT content_relative FROM order_config WHERE id=1", String.class))
                .path("certificateContent").asText().contains(RELATIVE));
        for (String query : List.of("SELECT description_relative FROM goods WHERE id=105",
                "SELECT description_relative FROM goods_member_level WHERE id=1",
                "SELECT content_relative FROM order_site_message WHERE id=1")) {
            assertTrue(jdbc.queryForObject(query, String.class).contains(RELATIVE));
        }
        assertNull(jdbc.queryForObject("SELECT content_relative FROM order_config WHERE id=2", String.class));
        assertEquals("", jdbc.queryForObject("SELECT content_relative FROM order_config WHERE id=3", String.class));
        var languages = JSON.readTree(jdbc.queryForObject("SELECT relative_content FROM translations WHERE id=1", String.class));
        assertTrue(JSON.readTree(languages.path("en_US").asText()).path("certificateContent").asText().contains(RELATIVE));
        assertTrue(languages.path("ja_JP").asText().contains(RELATIVE));
        assertEquals("", languages.path("zh_CN").asText());
        assertFalse(languages.has("ko_KR"));
        jdbc.update("UPDATE goods SET description_relative='already stored' WHERE id=1");
        migrate();
        assertEquals("already stored", jdbc.queryForObject("SELECT description_relative FROM goods WHERE id=1", String.class));
        assertNotNull(new V2026_11_10_00__rich_text_relative_copies().getChecksum());
    }

    @Test
    void dependencyFailurePrecedesEveryDdlStatement() {
        jdbc.execute("ALTER TABLE goods DROP COLUMN description");
        assertThrows(SQLException.class, this::migrate);
        assertEquals(0, jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns WHERE TABLE_SCHEMA=DATABASE()
                AND COLUMN_NAME IN ('content_relative', 'description_relative', 'relative_content')
                """, Integer.class));
    }

    @Test
    void realMappersRoundTripBothCopiesAndMergeTranslationPartialUpdates() throws Exception {
        migrate();
        try (var session = sqlSessionFactory().openSession()) {
            var translationsMapper = session.getMapper(TranslationsMapper.class);
            var translations = new TranslationsServiceImpl();
            ReflectionTestUtils.setField(translations, "translationsMapper", translationsMapper);
            var service = new OrderConfigServiceImpl(session.getMapper(OrderConfigMapper.class), JSON, translations);
            var config = new OrderConfig();
            config.setType("certificate");
            config.setContent(JSON.writeValueAsString(java.util.Map.of("certificateContent", HTML)));
            var languages = new Translations();
            languages.setEnUs(HTML);
            languages.setJaJp(HTML);
            config.setTranslations(languages);
            service.insertOrderConfig(config);
            session.commit();
            var saved = service.selectOrderConfigById(config.getId());
            assertEquals(config.getContent(), saved.getContent());
            assertTrue(saved.getContentRelative().contains(RELATIVE));
            assertTrue(JSON.readTree(saved.getTranslations().getRelativeContent()).path("ja_JP").asText().contains(RELATIVE));
            var partial = new Translations();
            partial.setId(languages.getId());
            partial.setEnUs("");
            partial.setRelativeContent("forged");
            translations.updateTranslations(partial);
            session.commit();
            var updated = translations.selectTranslationsById(languages.getId());
            assertEquals("", updated.getEnUs());
            assertEquals(HTML, updated.getJaJp());
            var relative = JSON.readTree(updated.getRelativeContent());
            assertEquals("", relative.path("en_US").asText());
            assertTrue(relative.path("ja_JP").asText().contains(RELATIVE));
            var empty = new OrderConfig();
            empty.setId(config.getId());
            empty.setContent("");
            service.updateOrderConfig(empty);
            session.commit();
            saved = service.selectOrderConfigById(config.getId());
            assertEquals("", saved.getContent());
            assertEquals("", saved.getContentRelative());
        }
    }

    private SqlSessionFactory sqlSessionFactory() throws Exception {
        var configuration = new Configuration(new Environment("relative-copy-test", new JdbcTransactionFactory(), source));
        configuration.getTypeAliasRegistry().registerAlias("OrderConfig", OrderConfig.class);
        for (String name : List.of("OrderConfigMapper", "TranslationsMapper")) {
            String path = "mapper/" + name + ".xml";
            try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
                assertNotNull(stream);
                new XMLMapperBuilder(stream, configuration, path, configuration.getSqlFragments()).parse();
            }
        }
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    private void migrate() throws Exception {
        try (Connection connection = source.getConnection()) {
            new V2026_11_10_00__rich_text_relative_copies().migrate(new Context() {
                @Override public org.flywaydb.core.api.configuration.Configuration getConfiguration() { return null; }
                @Override public Connection getConnection() { return connection; }
            });
        }
    }
}
