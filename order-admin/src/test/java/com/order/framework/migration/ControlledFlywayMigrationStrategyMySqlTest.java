package com.order.framework.migration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class ControlledFlywayMigrationStrategyMySqlTest
{
    @Container
    static final MySQLContainer BASELINE_03_DATABASE = new MySQLContainer("mysql:8.0.42")
            .withDatabaseName("flyway_baseline_03_test")
            .withUsername("order")
            .withPassword("order");

    @Container
    static final MySQLContainer BASELINE_17_DATABASE = new MySQLContainer("mysql:8.0.42")
            .withDatabaseName("flyway_baseline_17_test")
            .withUsername("order")
            .withPassword("order");

    @BeforeAll
    static void createControlledLegacySchema() throws Exception
    {
        createControlledLegacySchema(BASELINE_03_DATABASE);
        createControlledLegacySchema(BASELINE_17_DATABASE);
    }

    private static void createControlledLegacySchema(MySQLContainer database) throws Exception
    {
        List<String> statements = List.of(
                """
                CREATE TABLE sys_menu (
                    menu_id BIGINT NOT NULL, menu_name VARCHAR(64), parent_id BIGINT, order_num INT,
                    path VARCHAR(128), component VARCHAR(255), `query` VARCHAR(255), route_name VARCHAR(128),
                    is_frame INT, is_cache INT, menu_type CHAR(1), visible CHAR(1), status CHAR(1),
                    perms VARCHAR(255), icon VARCHAR(128), create_by VARCHAR(64), create_time DATETIME,
                    update_by VARCHAR(64), update_time DATETIME, remark VARCHAR(500), PRIMARY KEY (menu_id)
                ) ENGINE=InnoDB
                """,
                """
                CREATE TABLE sys_config (
                    config_id INT NOT NULL, config_key VARCHAR(100), config_value VARCHAR(500),
                    PRIMARY KEY (config_id)
                ) ENGINE=InnoDB
                """,
                """
                CREATE TABLE sys_dept (
                    dept_id BIGINT NOT NULL, leader VARCHAR(64), email VARCHAR(128), PRIMARY KEY (dept_id)
                ) ENGINE=InnoDB
                """,
                """
                CREATE TABLE sys_dict_data (
                    dict_code BIGINT NOT NULL AUTO_INCREMENT, dict_sort INT, dict_label VARCHAR(100),
                    dict_value VARCHAR(100), dict_type VARCHAR(100), css_class VARCHAR(100),
                    list_class VARCHAR(100), is_default CHAR(1), status CHAR(1), create_by VARCHAR(64),
                    create_time DATETIME, update_by VARCHAR(64), update_time DATETIME, remark VARCHAR(500),
                    PRIMARY KEY (dict_code)
                ) ENGINE=InnoDB
                """,
                """
                CREATE TABLE sys_role (
                    role_id BIGINT NOT NULL, role_key VARCHAR(100), status CHAR(1), PRIMARY KEY (role_id)
                ) ENGINE=InnoDB
                """,
                """
                CREATE TABLE sys_role_menu (
                    role_id BIGINT NOT NULL, menu_id BIGINT NOT NULL, PRIMARY KEY (role_id, menu_id)
                ) ENGINE=InnoDB
                """,
                """
                CREATE TABLE sys_job (
                    job_id BIGINT NOT NULL, job_name VARCHAR(64), job_group VARCHAR(64), invoke_target VARCHAR(500),
                    cron_expression VARCHAR(255), misfire_policy VARCHAR(20), concurrent CHAR(1), status CHAR(1),
                    create_by VARCHAR(64), create_time DATETIME, remark VARCHAR(500), PRIMARY KEY (job_id)
                ) ENGINE=InnoDB
                """,
                """
                CREATE TABLE order_user (
                    id BIGINT NOT NULL, username VARCHAR(100), parent_id BIGINT, invite_code VARCHAR(50),
                    password VARCHAR(100), withdrawal_password_fail_count INT, max_single_withdrawal DECIMAL(20,2),
                    identity_status CHAR(1), today_sign_count INT, PRIMARY KEY (id)
                ) ENGINE=InnoDB
                """,
                """
                CREATE TABLE order_login_log (
                    id BIGINT NOT NULL, user_id BIGINT NOT NULL, success CHAR(1), request_headers TEXT,
                    PRIMARY KEY (id)
                ) ENGINE=InnoDB
                """,
                "INSERT INTO sys_dept (dept_id, leader, email) VALUES (100, 'old', 'old@example.com')",
                "INSERT INTO sys_role (role_id, role_key, status) VALUES (1, 'admin', '0')",
                """
                INSERT INTO sys_job (job_id, job_name, job_group, invoke_target, cron_expression, misfire_policy,
                    concurrent, status, create_by, create_time, remark)
                VALUES (1, '系统默认测试任务', 'DEFAULT', 'test.run', '* * * * * ?', '3', '1', '0', 'admin', NOW(), '')
                """,
                """
                INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, `query`, route_name,
                    is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by,
                    update_time, remark) VALUES
                    (2002, '客户管理', 0, 1, 'member', NULL, NULL, '', 1, 0, 'M', '0', '0', '', '#', 'admin', NOW(), '', NULL, ''),
                    (2023, '会员管理', 2002, 1, 'orderuser', 'member/orderuser/index', NULL, '', 1, 0, 'C', '0', '0', 'member:orderuser:list', '#', 'admin', NOW(), '', NULL, ''),
                    (2067, '网站管理', 2002, 2, 'website', NULL, NULL, '', 1, 0, 'M', '0', '0', '', '#', 'admin', NOW(), '', NULL, ''),
                    (2093, '时区管理', 5002, 4, 'zone', 'system/zone/index', NULL, '', 1, 0, 'C', '0', '0', 'system:zone:list', '#', 'admin', NOW(), '', NULL, '')
                """);

        try (Connection connection = connection(database); Statement statement = connection.createStatement())
        {
            for (String sql : statements)
            {
                statement.execute(sql);
            }
            for (String table : LegacyBootstrapDecision.CONTROLLED_SNAPSHOT_TABLES)
            {
                statement.execute("CREATE TABLE IF NOT EXISTS `" + table
                        + "` (id BIGINT NOT NULL, PRIMARY KEY (id)) ENGINE=InnoDB");
            }
        }
    }

    @Test
    void baseline03EnvironmentWithAppliedLowMigrationsUpgradesAndKeepsHistory() throws Exception
    {
        Flyway previousRelease = flyway(BASELINE_03_DATABASE, "2026.08.03.0", "2026.08.18.03",
                "classpath:db/migration/mysql");
        previousRelease.baseline();

        ControlledFlywayMigrationStrategy current = new ControlledFlywayMigrationStrategy("1.2.2");
        IllegalStateException pendingLegacyData = assertThrows(IllegalStateException.class,
                () -> current.migrate(previousRelease));
        assertTrue(pendingLegacyData.getMessage().contains("must not be auto-replayed"));
        assertEquals(1, number(BASELINE_03_DATABASE, "SELECT COUNT(*) FROM sys_job WHERE job_id = 1"));
        assertEquals("old", text(BASELINE_03_DATABASE, "SELECT leader FROM sys_dept WHERE dept_id = 100"));

        previousRelease.migrate();
        execute(BASELINE_03_DATABASE, "INSERT INTO sys_job (job_id, job_name, job_group, invoke_target, "
                + "cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark) "
                + "VALUES (1, '系统默认用户恢复任务', 'CUSTOM', 'custom.run', '* * * * * ?', '3', '1', '0', "
                + "'operator', NOW(), 'must survive successor')");
        execute(BASELINE_03_DATABASE, "UPDATE sys_dept SET leader = 'custom-owner', "
                + "email = 'custom@example.com' WHERE dept_id = 100");
        execute(BASELINE_03_DATABASE, "INSERT INTO sys_application_version "
                + "(id, application_version, schema_version, last_migrated_at) "
                + "VALUES (1, '1.2.0', '2026.08.18.03', CURRENT_TIMESTAMP(3))");

        assertEquals(7, number(BASELINE_03_DATABASE, "SELECT COUNT(*) FROM flyway_schema_history"));
        assertEquals(1, number(BASELINE_03_DATABASE, "SELECT COUNT(*) FROM flyway_schema_history "
                + "WHERE type = 'BASELINE' AND version = '2026.08.03.0'"));
        assertEquals(2, number(BASELINE_03_DATABASE, "SELECT COUNT(*) FROM flyway_schema_history "
                + "WHERE version IN ('2026.08.04.00', '2026.08.08.00') AND success = 1"));
        assertEquals("1.2.0", text(BASELINE_03_DATABASE,
                "SELECT application_version FROM sys_application_version WHERE id = 1"));

        IllegalStateException targeted = assertThrows(IllegalStateException.class,
                () -> current.migrate(previousRelease));
        assertTrue(targeted.getMessage().contains("would be ignored"));

        Flyway flyway = flyway(BASELINE_03_DATABASE);
        current.migrate(flyway);

        assertEquals(10, number(BASELINE_03_DATABASE, "SELECT COUNT(*) FROM flyway_schema_history"));
        assertEquals(2, number(BASELINE_03_DATABASE, "SELECT COUNT(*) FROM flyway_schema_history "
                + "WHERE version IN ('2026.08.18.04', '2026.08.18.05') AND success = 1"));
        assertEquals("1.2.2", text(BASELINE_03_DATABASE,
                "SELECT application_version FROM sys_application_version WHERE id = 1"));
        assertEquals("2026.09.01.00", text(BASELINE_03_DATABASE,
                "SELECT schema_version FROM sys_application_version WHERE id = 1"));
        assertEquals(1, number(BASELINE_03_DATABASE, "SELECT COUNT(*) FROM information_schema.key_column_usage "
                + "WHERE table_schema = DATABASE() AND table_name = 'sys_application_version' "
                + "AND constraint_name = 'PRIMARY' AND column_name = 'id'"));
        assertEquals("YES", text(BASELINE_03_DATABASE, "SELECT is_nullable FROM information_schema.columns "
                + "WHERE table_schema = DATABASE() AND table_name = 'order_login_log' AND column_name = 'user_id'"));
        assertEquals("已取消", text(BASELINE_03_DATABASE, "SELECT dict_label FROM sys_dict_data "
                + "WHERE dict_type = 'order_status' AND dict_value = '3'"));
        assertEquals(2067, number(BASELINE_03_DATABASE, "SELECT parent_id FROM sys_menu WHERE menu_id = 2093"));
        assertEquals(1, number(BASELINE_03_DATABASE, "SELECT COUNT(*) FROM sys_role_menu grants JOIN sys_menu menu "
                + "ON menu.menu_id = grants.menu_id WHERE grants.role_id = 1 AND menu.perms = 'system:zone:active'"));
        assertEquals(1, number(BASELINE_03_DATABASE, "SELECT COUNT(*) FROM sys_job WHERE job_id = 1"));
        assertEquals("custom-owner", text(BASELINE_03_DATABASE,
                "SELECT leader FROM sys_dept WHERE dept_id = 100"));
        assertEquals("custom@example.com", text(BASELINE_03_DATABASE,
                "SELECT email FROM sys_dept WHERE dept_id = 100"));
        assertEquals(8, number(BASELINE_03_DATABASE, "SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE table_schema = DATABASE() AND table_name = 'sys_user_table_column_config'"));
        assertEquals("bigint", text(BASELINE_03_DATABASE, "SELECT data_type FROM information_schema.columns "
                + "WHERE table_schema = DATABASE() AND table_name = 'sys_user_table_column_config' "
                + "AND column_name = 'config_id'"));
        assertEquals("auto_increment", text(BASELINE_03_DATABASE, "SELECT extra FROM information_schema.columns "
                + "WHERE table_schema = DATABASE() AND table_name = 'sys_user_table_column_config' "
                + "AND column_name = 'config_id'"));
        assertEquals(128, number(BASELINE_03_DATABASE, "SELECT character_maximum_length "
                + "FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'sys_user_table_column_config' AND column_name = 'table_key'"));
        assertEquals("text", text(BASELINE_03_DATABASE, "SELECT data_type FROM information_schema.columns "
                + "WHERE table_schema = DATABASE() AND table_name = 'sys_user_table_column_config' "
                + "AND column_name = 'config_content'"));
        assertEquals("user_id,table_key", text(BASELINE_03_DATABASE,
                "SELECT GROUP_CONCAT(column_name ORDER BY seq_in_index) FROM information_schema.statistics "
                        + "WHERE table_schema = DATABASE() AND table_name = 'sys_user_table_column_config' "
                        + "AND index_name = 'uk_sys_user_table_column_config_user_table' AND non_unique = 0"));
        assertEquals(0, number(BASELINE_03_DATABASE, "SELECT COUNT(*) FROM information_schema.referential_constraints "
                + "WHERE constraint_schema = DATABASE() AND table_name = 'sys_user_table_column_config'"));

        int installedBefore = number(BASELINE_03_DATABASE, "SELECT COUNT(*) FROM flyway_schema_history");
        LocalDateTime migratedAtBefore = timestamp(BASELINE_03_DATABASE,
                "SELECT last_migrated_at FROM sys_application_version WHERE id = 1");
        current.migrate(flyway);
        assertEquals(installedBefore, number(BASELINE_03_DATABASE, "SELECT COUNT(*) FROM flyway_schema_history"));
        assertEquals(migratedAtBefore,
                timestamp(BASELINE_03_DATABASE,
                        "SELECT last_migrated_at FROM sys_application_version WHERE id = 1"));

        IllegalStateException downgrade = assertThrows(IllegalStateException.class,
                () -> new ControlledFlywayMigrationStrategy("1.2.0").migrate(flyway));
        assertTrue(downgrade.getMessage().contains("downgrade is not allowed"));

        Flyway withIgnoredMigration = flyway(BASELINE_03_DATABASE, "2026.08.03.0", null,
                "classpath:db/migration/mysql", "classpath:db/migration/ignored");
        IllegalStateException ignored = assertThrows(IllegalStateException.class,
                () -> current.migrate(withIgnoredMigration));
        assertTrue(ignored.getMessage().contains("would be ignored"));

        execute(BASELINE_03_DATABASE, "ALTER TABLE sys_application_version DROP PRIMARY KEY");
        IllegalStateException invalidVersionTable = assertThrows(IllegalStateException.class,
                () -> current.migrate(flyway));
        assertTrue(invalidVersionTable.getMessage().contains("primary key must contain only id"));
    }

    @Test
    void baseline17AdoptionAndEarlyEnvironmentPreserveLegacyData() throws Exception
    {
        Flyway adoption = flyway(BASELINE_17_DATABASE);
        ControlledFlywayMigrationStrategy current = new ControlledFlywayMigrationStrategy("1.2.2");
        current.migrate(adoption);

        assertEquals(8, number(BASELINE_17_DATABASE, "SELECT COUNT(*) FROM flyway_schema_history"));
        assertEquals(1, number(BASELINE_17_DATABASE, "SELECT COUNT(*) FROM flyway_schema_history "
                + "WHERE type = 'BASELINE' AND version = '2026.08.17.0'"));
        assertEquals(0, number(BASELINE_17_DATABASE, "SELECT COUNT(*) FROM flyway_schema_history "
                + "WHERE version IN ('2026.08.04.00', '2026.08.08.00')"));
        assertEquals(1, number(BASELINE_17_DATABASE, "SELECT COUNT(*) FROM sys_job WHERE job_id = 1"));
        assertEquals("old", text(BASELINE_17_DATABASE, "SELECT leader FROM sys_dept WHERE dept_id = 100"));
        assertEquals("old@example.com", text(BASELINE_17_DATABASE,
                "SELECT email FROM sys_dept WHERE dept_id = 100"));

        // Recreate the early 1.2.0 history shape observed in local environments.
        execute(BASELINE_17_DATABASE, "DROP TABLE flyway_schema_history");
        execute(BASELINE_17_DATABASE, "DROP TABLE sys_application_version");
        Flyway earlyRelease = flyway(BASELINE_17_DATABASE, "2026.08.17.0", "2026.08.18.03",
                "classpath:db/migration/mysql");
        earlyRelease.baseline();
        earlyRelease.migrate();
        execute(BASELINE_17_DATABASE, "INSERT INTO sys_application_version "
                + "(id, application_version, schema_version, last_migrated_at) "
                + "VALUES (1, '1.2.0', '2026.08.18.03', CURRENT_TIMESTAMP(3))");

        assertEquals(5, number(BASELINE_17_DATABASE, "SELECT COUNT(*) FROM flyway_schema_history"));
        assertEquals(1, number(BASELINE_17_DATABASE, "SELECT COUNT(*) FROM flyway_schema_history "
                + "WHERE type = 'BASELINE' AND version = '2026.08.17.0'"));
        assertEquals(0, number(BASELINE_17_DATABASE, "SELECT COUNT(*) FROM flyway_schema_history "
                + "WHERE version IN ('2026.08.04.00', '2026.08.08.00')"));

        Flyway flyway = flyway(BASELINE_17_DATABASE);
        current.migrate(flyway);

        assertEquals(8, number(BASELINE_17_DATABASE, "SELECT COUNT(*) FROM flyway_schema_history"));
        assertEquals(2, number(BASELINE_17_DATABASE, "SELECT COUNT(*) FROM flyway_schema_history "
                + "WHERE version IN ('2026.08.18.04', '2026.08.18.05') AND success = 1"));
        assertEquals("1.2.2", text(BASELINE_17_DATABASE,
                "SELECT application_version FROM sys_application_version WHERE id = 1"));
        assertEquals("2026.09.01.00", text(BASELINE_17_DATABASE,
                "SELECT schema_version FROM sys_application_version WHERE id = 1"));
        assertEquals(1, number(BASELINE_17_DATABASE, "SELECT COUNT(*) FROM sys_job WHERE job_id = 1"));
        assertEquals("old", text(BASELINE_17_DATABASE, "SELECT leader FROM sys_dept WHERE dept_id = 100"));
        assertEquals("old@example.com", text(BASELINE_17_DATABASE,
                "SELECT email FROM sys_dept WHERE dept_id = 100"));

        LocalDateTime migratedAt = timestamp(BASELINE_17_DATABASE,
                "SELECT last_migrated_at FROM sys_application_version WHERE id = 1");
        current.migrate(flyway);
        assertEquals(8, number(BASELINE_17_DATABASE, "SELECT COUNT(*) FROM flyway_schema_history"));
        assertEquals(migratedAt, timestamp(BASELINE_17_DATABASE,
                "SELECT last_migrated_at FROM sys_application_version WHERE id = 1"));

        Flyway unexpectedBelowBaseline = flyway(BASELINE_17_DATABASE, "2026.08.17.0", null,
                "classpath:db/migration/mysql", "classpath:db/migration/unexpected_below");
        IllegalStateException rejected = assertThrows(IllegalStateException.class,
                () -> current.migrate(unexpectedBelowBaseline));
        assertTrue(rejected.getMessage().contains("would be ignored"));
    }

    private static Flyway flyway(MySQLContainer database)
    {
        return flyway(database, "2026.08.17.0", null, "classpath:db/migration/mysql");
    }

    private static Flyway flyway(MySQLContainer database, String baselineVersion, String target,
            String... locations)
    {
        FluentConfiguration configuration = Flyway.configure()
                .dataSource(database.getJdbcUrl(), database.getUsername(), database.getPassword())
                .locations(locations)
                .table("flyway_schema_history")
                .baselineOnMigrate(false)
                .baselineVersion(baselineVersion)
                .baselineDescription("2026.08.03.0".equals(baselineVersion)
                        ? "Controlled snapshot order_base_20260803"
                        : "Controlled legacy adoption through 20260817")
                .validateOnMigrate(true)
                .validateMigrationNaming(true)
                .outOfOrder(false)
                .cleanDisabled(true)
                .placeholderReplacement(false)
                .failOnMissingLocations(true)
                .lockRetryCount(10);
        if (target != null)
        {
            configuration.target(target);
        }
        return configuration.load();
    }

    private static void execute(MySQLContainer database, String sql) throws Exception
    {
        try (Connection connection = connection(database); Statement statement = connection.createStatement())
        {
            statement.execute(sql);
        }
    }

    private static Connection connection(MySQLContainer database) throws Exception
    {
        return DriverManager.getConnection(database.getJdbcUrl(), database.getUsername(), database.getPassword());
    }

    private static int number(MySQLContainer database, String sql) throws Exception
    {
        return ((Number) scalar(database, sql)).intValue();
    }

    private static String text(MySQLContainer database, String sql) throws Exception
    {
        Object value = scalar(database, sql);
        return value == null ? null : value.toString();
    }

    private static LocalDateTime timestamp(MySQLContainer database, String sql) throws Exception
    {
        return (LocalDateTime) scalar(database, sql);
    }

    private static Object scalar(MySQLContainer database, String sql) throws Exception
    {
        try (Connection connection = connection(database);
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql))
        {
            assertTrue(resultSet.next(), sql);
            return resultSet.getObject(1);
        }
    }
}
