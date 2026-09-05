package com.order.framework.migration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigrationResourceContractTest
{
    private static final String LOCATION = "classpath*:db/migration/mysql/*.sql";

    private static final Pattern FILE_NAME = Pattern.compile("V\\d{4}_\\d{2}_\\d{2}_\\d{2}__[a-z0-9_]+\\.sql");

    private static final Pattern DANGEROUS_SQL = Pattern.compile(
            "(?im)^\\s*(?:DROP\\s+(?:DATABASE|SCHEMA|TABLE)\\b|CREATE\\s+(?:DATABASE|SCHEMA)\\b|"
                    + "TRUNCATE\\s+TABLE\\b|LOAD\\s+DATA\\b|SOURCE\\s+\\S|USE\\s+[`a-zA-Z])");

    @Test
    void allAppendOnlyMigrationsArePackagedAndSafelyNamed() throws IOException
    {
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources(LOCATION);
        List<String> names = Arrays.stream(resources)
                .map(Resource::getFilename)
                .sorted()
                .toList();

        assertEquals(List.of(
                "V2026_08_04_00__remove_legacy_branding.sql",
                "V2026_08_08_00__member_sensitive_permissions.sql",
                "V2026_08_18_00__application_version.sql",
                "V2026_08_18_01__login_audit_user_id_nullable.sql",
                "V2026_08_18_02__order_status_alignment.sql",
                "V2026_08_18_03__restore_timezone_management_menu.sql",
                "V2026_08_18_04__remove_legacy_branding_compat.sql",
                "V2026_08_18_05__member_sensitive_permissions_compat.sql",
                "V2026_09_01_00__user_table_column_config.sql"), names);
        for (Resource resource : resources)
        {
            assertTrue(FILE_NAME.matcher(resource.getFilename()).matches(), resource.getFilename());
            String sql = resource.getContentAsString(StandardCharsets.UTF_8);
            assertFalse(DANGEROUS_SQL.matcher(stripComments(sql)).find(), resource.getFilename());
        }
    }

    @Test
    void tableColumnConfigMigrationDefinesPerUserTableState() throws IOException
    {
        String sql = resourceText("db/migration/mysql/V2026_09_01_00__user_table_column_config.sql");

        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS sys_user_table_column_config"));
        assertTrue(Pattern.compile("(?is)config_id\\s+BIGINT\\s+NOT\\s+NULL\\s+AUTO_INCREMENT")
                .matcher(sql).find());
        assertTrue(Pattern.compile("(?is)user_id\\s+BIGINT\\s+NOT\\s+NULL")
                .matcher(sql).find());
        assertTrue(Pattern.compile("(?is)table_key\\s+VARCHAR\\s*\\(\\s*128\\s*\\)\\s+NOT\\s+NULL")
                .matcher(sql).find());
        assertTrue(Pattern.compile("(?is)config_content\\s+TEXT\\s+NOT\\s+NULL")
                .matcher(sql).find());
        assertTrue(Pattern.compile("(?is)PRIMARY\\s+KEY\\s*\\(\\s*config_id\\s*\\)")
                .matcher(sql).find());
        assertTrue(Pattern.compile("(?is)UNIQUE\\s+KEY\\s+uk_sys_user_table_column_config_user_table\\s*"
                        + "\\(\\s*user_id\\s*,\\s*table_key\\s*\\)")
                .matcher(sql).find());
        assertTrue(Pattern.compile("(?is)create_time\\s+DATETIME\\s*\\(\\s*3\\s*\\)")
                .matcher(sql).find());
        assertTrue(Pattern.compile("(?is)update_time\\s+DATETIME\\s*\\(\\s*3\\s*\\).*"
                        + "ON\\s+UPDATE\\s+CURRENT_TIMESTAMP\\s*\\(\\s*3\\s*\\)")
                .matcher(sql).find());
        assertTrue(Pattern.compile("(?is)ENGINE\\s*=\\s*InnoDB.*DEFAULT\\s+CHARSET\\s*=\\s*utf8mb4")
                .matcher(sql).find());
        assertFalse(Pattern.compile("(?i)FOREIGN\\s+KEY|REFERENCES\\s+sys_user")
                .matcher(sql).find());
    }

    @Test
    void versionMigrationOnlyCreatesTheInternalVersionTable() throws IOException
    {
        String sql = resourceText("db/migration/mysql/V2026_08_18_00__application_version.sql");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS sys_application_version"));
        assertTrue(sql.contains("last_migrated_at"));
        assertTrue(Pattern.compile("(?is)PRIMARY\\s+KEY\\s*\\(\\s*id\\s*\\)").matcher(sql).find());
        assertFalse(sql.contains("sys_menu"));
        assertFalse(sql.contains("sys_role"));
        assertFalse(sql.contains("system:version:list"));

        String compatibilityMarker = resourceText(
                "db/migration/mysql/V2026_08_18_04__remove_legacy_branding_compat.sql");
        assertTrue(Pattern.compile("(?im)^\\s*SELECT\\s+1\\s*;\\s*$").matcher(compatibilityMarker).find());
        assertFalse(Pattern.compile("(?i)\\b(?:DELETE|UPDATE|INSERT|ALTER|DROP|TRUNCATE)\\b")
                .matcher(stripComments(compatibilityMarker)).find());
        assertFalse(compatibilityMarker.contains("sys_job"));
        assertFalse(compatibilityMarker.contains("sys_dept"));

        String permissionSuccessor = resourceText(
                "db/migration/mysql/V2026_08_18_05__member_sensitive_permissions_compat.sql");
        assertFalse(Pattern.compile("(?i)\\b(?:DELETE|UPDATE|ALTER|DROP|TRUNCATE)\\b")
                .matcher(stripComments(permissionSuccessor)).find());
        assertTrue(permissionSuccessor.contains("existing.menu_id IS NULL"));
        assertEquals(3, Pattern.compile("(?i)NOT\\s+EXISTS\\s*\\(")
                .matcher(permissionSuccessor).results().count());
    }

    @Test
    void packagedChangeScriptsAreImmutableCopiesOfReviewedRootScripts() throws IOException
    {
        Map<String, String> copies = Map.of(
                "V2026_08_04_00__remove_legacy_branding.sql",
                "2026-08-04_remove_legacy_branding.sql",
                "V2026_08_08_00__member_sensitive_permissions.sql",
                "2026-08-08_member_sensitive_permissions.sql",
                "V2026_08_18_01__login_audit_user_id_nullable.sql",
                "2026-08-18_login_audit_user_id_nullable.sql",
                "V2026_08_18_02__order_status_alignment.sql",
                "2026-08-18_order_status_alignment.sql",
                "V2026_08_18_03__restore_timezone_management_menu.sql",
                "2026-08-18_restore_timezone_management_menu.sql",
                "V2026_08_18_05__member_sensitive_permissions_compat.sql",
                "2026-08-08_member_sensitive_permissions.sql");

        Path rootSqlDirectory = locateRootSqlDirectory();
        for (Map.Entry<String, String> copy : copies.entrySet())
        {
            String packaged = normalize(resourceText("db/migration/mysql/" + copy.getKey()));
            String reviewed = normalize(Files.readString(rootSqlDirectory.resolve(copy.getValue()), StandardCharsets.UTF_8));
            assertEquals(reviewed, packaged, copy.getKey() + " must remain an exact copy");
        }
    }

    @Test
    void multiStatementDmlMigrationsAreTransactional() throws IOException
    {
        for (String resource : List.of(
                "V2026_08_04_00__remove_legacy_branding.sql",
                "V2026_08_08_00__member_sensitive_permissions.sql",
                "V2026_08_18_03__restore_timezone_management_menu.sql",
                "V2026_08_18_05__member_sensitive_permissions_compat.sql"))
        {
            String sql = resourceText("db/migration/mysql/" + resource);
            assertTrue(Pattern.compile("(?im)^\\s*START\\s+TRANSACTION\\s*;").matcher(sql).find(), resource);
            assertTrue(Pattern.compile("(?im)^\\s*COMMIT\\s*;\\s*$").matcher(sql).find(), resource);
        }
    }

    @Test
    void releaseRulesReadmeIsPackaged() throws IOException
    {
        String readme = resourceText("db/migration/mysql/README.md");
        assertTrue(readme.contains("order.migration-version"));
        assertTrue(readme.contains("`order.version` 是对外展示版本"));
        assertTrue(readme.contains("VYYYY_MM_DD_NN__description.sql"));
        assertTrue(readme.contains("禁止修改、重命名或删除"));
        assertTrue(readme.contains("根目录的 `sql/` 不会被自动执行"));
        assertTrue(readme.contains("禁止自动 `repair`"));
        assertTrue(readme.contains("受控基线"));
        assertTrue(readme.contains("兼容 successor"));
        assertTrue(readme.contains("baseline 到 `2026.08.17.0`"));
        assertTrue(readme.contains("不会自动重放"));
        assertTrue(readme.contains("不修改 legacy 任务和部门数据"));
    }

    private static String resourceText(String name) throws IOException
    {
        try (var stream = MigrationResourceContractTest.class.getClassLoader().getResourceAsStream(name))
        {
            assertNotNull(stream, name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Path locateRootSqlDirectory()
    {
        for (Path candidate : List.of(Path.of("sql"), Path.of("..", "sql")))
        {
            if (Files.isDirectory(candidate))
            {
                return candidate;
            }
        }
        throw new AssertionError("Cannot locate repository sql directory");
    }

    private static String normalize(String text)
    {
        return text.replace("\r\n", "\n").strip();
    }

    private static String stripComments(String sql)
    {
        return Arrays.stream(sql.split("\\R"))
                .map(line -> line.replaceFirst("--.*$", ""))
                .reduce("", (left, right) -> left + "\n" + right)
                .toUpperCase(Locale.ROOT);
    }
}
