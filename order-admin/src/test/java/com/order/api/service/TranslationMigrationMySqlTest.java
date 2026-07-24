package com.order.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers(disabledWithoutDocker = true)
class TranslationMigrationMySqlTest {
    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.42")
            .withDatabaseName("translation_test")
            .withUsername("order")
            .withPassword("order");

    @BeforeEach
    void createLegacySchema() throws Exception {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("SET FOREIGN_KEY_CHECKS = 0");
            statement.execute("DROP TABLE IF EXISTS order_config");
            statement.execute("DROP TABLE IF EXISTS goods_member_level");
            statement.execute("DROP TABLE IF EXISTS goods_customer_service");
            statement.execute("DROP TABLE IF EXISTS sys_notice");
            statement.execute("DROP TABLE IF EXISTS translations");
            statement.execute("SET FOREIGN_KEY_CHECKS = 1");
            statement.execute("""
                    CREATE TABLE translations (
                        id BIGINT NOT NULL AUTO_INCREMENT,
                        zh_CN TEXT,
                        zh_TW TEXT,
                        ko_KR TEXT,
                        th_TH TEXT,
                        ja_JP TEXT,
                        pt_PT TEXT,
                        en_US TEXT,
                        PRIMARY KEY (id)
                    ) ENGINE=InnoDB
                    """);
            statement.execute("""
                    CREATE TABLE order_config (
                        id BIGINT PRIMARY KEY,
                        translations_id BIGINT NULL
                    ) ENGINE=InnoDB
                    """);
            statement.execute("""
                    CREATE TABLE goods_member_level (
                        id BIGINT PRIMARY KEY
                    ) ENGINE=InnoDB
                    """);
            statement.execute("""
                    CREATE TABLE goods_customer_service (
                        id VARCHAR(64) PRIMARY KEY
                    ) ENGINE=InnoDB
                    """);
            statement.execute("""
                    CREATE TABLE sys_notice (
                        notice_id BIGINT PRIMARY KEY
                    ) ENGINE=InnoDB
                    """);
            statement.execute("""
                    INSERT INTO translations(id, en_US, zh_CN)
                    VALUES (10, '{"name":"English"}', '{"name":"中文"}')
                    """);
            statement.execute("INSERT INTO order_config(id, translations_id) VALUES (1, 10)");
        }
    }

    @Test
    void migrationIsRepeatableAndPreservesValidTranslationLinks() throws Exception {
        Path migration = findMigration();
        executeScript(migration);

        assertEquals(1L, queryLong(
                "SELECT COUNT(*) FROM order_config WHERE id = 1 AND translations_id = 10"));
        assertEquals(21L, queryLong("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'translations' AND column_name <> 'id'
                """));
        execute("INSERT INTO goods_member_level(id, translations_id) VALUES (2, 10)");
        execute("INSERT INTO goods_customer_service(id, translations_id) VALUES ('3', 10)");
        execute("INSERT INTO sys_notice(notice_id, translations_id) VALUES (4, 10)");

        executeScript(migration);

        assertEquals(4L, queryLong("""
                SELECT
                    (SELECT COUNT(*) FROM order_config WHERE translations_id = 10)
                  + (SELECT COUNT(*) FROM goods_member_level WHERE translations_id = 10)
                  + (SELECT COUNT(*) FROM goods_customer_service WHERE translations_id = 10)
                  + (SELECT COUNT(*) FROM sys_notice WHERE translations_id = 10)
                """));
        assertEquals(1L, queryLong("""
                SELECT COUNT(*) FROM translations
                WHERE id = 10
                  AND en_US = '{"name":"English"}'
                  AND zh_CN = '{"name":"中文"}'
                """));
        assertEquals(4L, queryLong("""
                SELECT COUNT(*) FROM information_schema.referential_constraints
                WHERE constraint_schema = DATABASE()
                  AND constraint_name IN (
                    'fk_order_config_translations',
                    'fk_member_level_translations',
                    'fk_customer_service_translations',
                    'fk_sys_notice_translations'
                  )
                """));
        assertEquals(4L, queryLong("""
                SELECT COUNT(DISTINCT CONCAT(table_name, ':', index_name))
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND index_name IN (
                    'idx_order_config_translations_id',
                    'idx_member_level_translations_id',
                    'idx_customer_service_translations_id',
                    'idx_sys_notice_translations_id'
                  )
                """));
    }

    private void executeScript(Path scriptPath) throws Exception {
        String delimiter = ";";
        StringBuilder sql = new StringBuilder();
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            for (String line : Files.readAllLines(scriptPath)) {
                String trimmed = line.trim();
                if (trimmed.startsWith("DELIMITER ")) {
                    delimiter = trimmed.substring("DELIMITER ".length()).trim();
                    continue;
                }
                if (trimmed.startsWith("--") || (trimmed.isEmpty() && sql.length() == 0)) {
                    continue;
                }
                sql.append(line).append('\n');
                if (trimmed.endsWith(delimiter)) {
                    int delimiterIndex = sql.lastIndexOf(delimiter);
                    String statementSql = sql.substring(0, delimiterIndex).trim();
                    if (!statementSql.isEmpty()) {
                        statement.execute(statementSql);
                    }
                    sql.setLength(0);
                }
            }
        }
        if (!sql.toString().isBlank()) {
            throw new IllegalStateException("Unterminated SQL statement in " + scriptPath);
        }
    }

    private Path findMigration() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve("sql")
                    .resolve("2026-07-01_translation_languages.sql");
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Translation migration script was not found");
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
    }

    private void execute(String sql) throws Exception {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private long queryLong(String sql) throws Exception {
        try (Connection connection = connection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }
}
