package com.order.framework.migration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyBootstrapDecisionTest
{
    @Test
    void baselinesOnlyWhenAllRequiredLegacyStructuresExist()
    {
        assertEquals("2026.08.17.0", ControlledFlywayMigrationStrategy.LEGACY_BASELINE_VERSION);
        assertEquals(69, LegacyBootstrapDecision.REQUIRED_COLUMNS.size());
        assertEquals(LegacyBootstrapDecision.Action.BASELINE,
                LegacyBootstrapDecision.decide(false, completeSchema()));
    }

    @Test
    void tableFingerprintExactlyMatchesControlledSnapshot() throws IOException
    {
        String sql = Files.readString(locateControlledSnapshot(), StandardCharsets.UTF_8);
        Matcher matcher = Pattern.compile("(?im)^CREATE TABLE `([^`]+)`").matcher(sql);
        Set<String> snapshotTables = new LinkedHashSet<>();
        while (matcher.find())
        {
            snapshotTables.add(matcher.group(1));
        }
        assertEquals(LegacyBootstrapDecision.CONTROLLED_SNAPSHOT_TABLES, snapshotTables);
    }

    @Test
    void refusesEmptyOrIncompleteDatabase()
    {
        IllegalStateException empty = assertThrows(IllegalStateException.class,
                () -> LegacyBootstrapDecision.decide(false, LegacySchemaSnapshot.empty()));
        assertTrue(empty.getMessage().contains("controlled baseline"));

        Map<String, Set<String>> incompleteColumns = mutableCopy(LegacyBootstrapDecision.REQUIRED_COLUMNS);
        incompleteColumns.remove("order_login_log");
        IllegalStateException missing = assertThrows(IllegalStateException.class,
                () -> LegacyBootstrapDecision.decide(false,
                        new LegacySchemaSnapshot(incompleteColumns, LegacyBootstrapDecision.REQUIRED_PRIMARY_KEYS)));
        assertTrue(missing.getMessage().contains("order_login_log"));
    }

    @Test
    void refusesMissingLateBaselineColumnOrPrimaryKey()
    {
        Map<String, Set<String>> incompleteColumns = mutableCopy(LegacyBootstrapDecision.REQUIRED_COLUMNS);
        incompleteColumns.get("order_user").remove("identity_status");
        IllegalStateException missingColumn = assertThrows(IllegalStateException.class,
                () -> LegacyBootstrapDecision.decide(false,
                        new LegacySchemaSnapshot(incompleteColumns, LegacyBootstrapDecision.REQUIRED_PRIMARY_KEYS)));
        assertTrue(missingColumn.getMessage().contains("order_user.identity_status"));

        Map<String, Set<String>> incompleteKeys = mutableCopy(LegacyBootstrapDecision.REQUIRED_PRIMARY_KEYS);
        incompleteKeys.get("sys_role_menu").remove("menu_id");
        IllegalStateException missingKey = assertThrows(IllegalStateException.class,
                () -> LegacyBootstrapDecision.decide(false,
                        new LegacySchemaSnapshot(LegacyBootstrapDecision.REQUIRED_COLUMNS, incompleteKeys)));
        assertTrue(missingKey.getMessage().contains("sys_role_menu.menu_id"));
    }

    @Test
    void existingHistoryIsNeverAutomaticallyBaselined()
    {
        assertEquals(LegacyBootstrapDecision.Action.KEEP_EXISTING_HISTORY,
                LegacyBootstrapDecision.decide(true, LegacySchemaSnapshot.empty()));
    }

    private static LegacySchemaSnapshot completeSchema()
    {
        return new LegacySchemaSnapshot(LegacyBootstrapDecision.REQUIRED_COLUMNS,
                LegacyBootstrapDecision.REQUIRED_PRIMARY_KEYS);
    }

    private static Map<String, Set<String>> mutableCopy(Map<String, Set<String>> source)
    {
        Map<String, Set<String>> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(key, new LinkedHashSet<>(value)));
        return copy;
    }

    private static Path locateControlledSnapshot()
    {
        for (Path candidate : List.of(
                Path.of("sql", "init", "order_base_20260803.sql"),
                Path.of("..", "sql", "init", "order_base_20260803.sql")))
        {
            if (Files.isRegularFile(candidate))
            {
                return candidate;
            }
        }
        throw new AssertionError("Cannot locate controlled legacy snapshot");
    }
}
