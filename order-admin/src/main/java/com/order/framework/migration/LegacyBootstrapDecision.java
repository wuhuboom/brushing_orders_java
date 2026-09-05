package com.order.framework.migration;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Pure decision for the one-time adoption of an existing legacy database.
 */
public final class LegacyBootstrapDecision
{
    public enum Action
    {
        BASELINE,
        KEEP_EXISTING_HISTORY
    }

    static final Set<String> CONTROLLED_SNAPSHOT_TABLES = orderedSet(
            "activity_account", "activity_account_prize", "activity_info", "activity_partner", "activity_prize",
            "gen_table", "gen_table_column", "goods", "goods_banner", "goods_customer_service",
            "goods_extra_commission_setting", "goods_member_level", "goods_recharge_record",
            "goods_transaction_flow", "goods_type", "goods_withdrawal_account", "legacy_auth_record",
            "legacy_bulletin", "legacy_recruitment", "legacy_wallet", "legacy_yuebao_account",
            "legacy_yuebao_flow", "order_api_request", "order_bonus_table", "order_config", "order_info",
            "order_link", "order_login_log", "order_sequence_manager", "order_site_message", "order_user",
            "order_withdrawal", "order_withdrawal_type", "points_account", "points_flow", "points_gift",
            "points_gift_order", "sys_config", "sys_dept", "sys_dict_data", "sys_dict_type", "sys_file",
            "sys_file_reference", "sys_group_menu", "sys_group_strategy", "sys_job", "sys_job_log",
            "sys_logininfor", "sys_menu", "sys_notice", "sys_oper_log", "sys_permission_group",
            "sys_permission_strategy", "sys_post", "sys_post_role", "sys_role", "sys_role_data_rule",
            "sys_role_dept", "sys_role_menu", "sys_role_strategy", "sys_strategy_menu", "sys_time_zone",
            "sys_user", "sys_user_group", "sys_user_post", "sys_user_role", "translations",
            "website_customer", "withdrawal_daily_quota");

    static final Map<String, Set<String>> REQUIRED_COLUMNS = requiredColumns();

    static final Map<String, Set<String>> REQUIRED_PRIMARY_KEYS = requiredPrimaryKeys();

    private LegacyBootstrapDecision()
    {
    }

    public static Action decide(boolean historyTableExists, LegacySchemaSnapshot schema)
    {
        if (historyTableExists)
        {
            return Action.KEEP_EXISTING_HISTORY;
        }

        if (schema == null)
        {
            throw new IllegalStateException("Legacy schema inspection did not return a result");
        }

        Set<String> missingTables = new LinkedHashSet<>(REQUIRED_COLUMNS.keySet());
        missingTables.removeAll(schema.columns().keySet());
        Set<String> missingColumns = missingItems(REQUIRED_COLUMNS, schema.columns(), missingTables);
        Set<String> invalidPrimaryKeys = missingItems(REQUIRED_PRIMARY_KEYS, schema.primaryKeys(), Set.of());
        if (!missingTables.isEmpty() || !missingColumns.isEmpty() || !invalidPrimaryKeys.isEmpty())
        {
            throw new IllegalStateException("Flyway history is absent and the database is not a complete legacy schema; "
                    + describeFailures(missingTables, missingColumns, invalidPrimaryKeys)
                    + ". Empty databases must be loaded from the controlled baseline before startup");
        }
        return Action.BASELINE;
    }

    private static Set<String> missingItems(Map<String, Set<String>> required, Map<String, Set<String>> actual,
            Set<String> skippedTables)
    {
        Set<String> missing = new LinkedHashSet<>();
        for (Map.Entry<String, Set<String>> requirement : required.entrySet())
        {
            if (skippedTables.contains(requirement.getKey()))
            {
                continue;
            }
            Set<String> actualItems = actual.getOrDefault(requirement.getKey(), Set.of());
            for (String item : requirement.getValue())
            {
                if (!actualItems.contains(item))
                {
                    missing.add(requirement.getKey() + "." + item);
                }
            }
        }
        return missing;
    }

    private static String describeFailures(Set<String> missingTables, Set<String> missingColumns,
            Set<String> invalidPrimaryKeys)
    {
        List<String> failures = new java.util.ArrayList<>();
        if (!missingTables.isEmpty())
        {
            failures.add("missing table(s): " + String.join(", ", missingTables));
        }
        if (!missingColumns.isEmpty())
        {
            failures.add("missing column(s): " + String.join(", ", missingColumns));
        }
        if (!invalidPrimaryKeys.isEmpty())
        {
            failures.add("missing primary-key column(s): " + String.join(", ", invalidPrimaryKeys));
        }
        return String.join("; ", failures);
    }

    private static Map<String, Set<String>> requiredColumns()
    {
        Map<String, Set<String>> required = new LinkedHashMap<>();
        CONTROLLED_SNAPSHOT_TABLES.forEach(table -> required.put(table, Set.of()));
        required.put("sys_menu", orderedSet("menu_id", "menu_name", "parent_id", "order_num", "path",
                "component", "query", "route_name", "is_frame", "is_cache", "menu_type", "visible", "status",
                "perms", "icon", "create_by", "create_time", "update_by", "update_time", "remark"));
        required.put("sys_config", orderedSet("config_id", "config_key", "config_value"));
        required.put("sys_dept", orderedSet("dept_id", "leader", "email"));
        required.put("sys_dict_data", orderedSet("dict_code", "dict_sort", "dict_label", "dict_value", "dict_type",
                "css_class", "list_class", "is_default", "status", "create_by", "create_time", "update_by",
                "update_time", "remark"));
        required.put("sys_role", orderedSet("role_id", "role_key", "status"));
        required.put("sys_role_menu", orderedSet("role_id", "menu_id"));
        required.put("sys_job", orderedSet("job_id", "job_name", "job_group", "invoke_target", "cron_expression",
                "misfire_policy", "concurrent", "status", "create_by", "create_time", "remark"));
        required.put("order_user", orderedSet("id", "username", "parent_id", "invite_code", "password",
                "withdrawal_password_fail_count", "max_single_withdrawal", "identity_status",
                "today_sign_count"));
        required.put("order_login_log", orderedSet("id", "user_id", "success", "request_headers"));
        return Collections.unmodifiableMap(required);
    }

    private static Map<String, Set<String>> requiredPrimaryKeys()
    {
        Map<String, Set<String>> required = new LinkedHashMap<>();
        required.put("sys_menu", orderedSet("menu_id"));
        required.put("sys_config", orderedSet("config_id"));
        required.put("sys_dept", orderedSet("dept_id"));
        required.put("sys_dict_data", orderedSet("dict_code"));
        required.put("sys_role", orderedSet("role_id"));
        required.put("sys_role_menu", orderedSet("role_id", "menu_id"));
        required.put("sys_job", orderedSet("job_id"));
        required.put("order_user", orderedSet("id"));
        required.put("order_login_log", orderedSet("id"));
        return Collections.unmodifiableMap(required);
    }

    private static Set<String> orderedSet(String... values)
    {
        Set<String> result = new LinkedHashSet<>();
        for (String value : values)
        {
            result.add(value.toLowerCase(Locale.ROOT));
        }
        return Collections.unmodifiableSet(result);
    }
}

record LegacySchemaSnapshot(Map<String, Set<String>> columns, Map<String, Set<String>> primaryKeys)
{
    LegacySchemaSnapshot
    {
        columns = normalize(columns);
        primaryKeys = normalize(primaryKeys);
    }

    static LegacySchemaSnapshot empty()
    {
        return new LegacySchemaSnapshot(Map.of(), Map.of());
    }

    private static Map<String, Set<String>> normalize(Map<String, Set<String>> source)
    {
        Map<String, Set<String>> normalized = new LinkedHashMap<>();
        if (source != null)
        {
            source.forEach((table, items) -> normalized.put(table.toLowerCase(Locale.ROOT),
                    items.stream().map(item -> item.toLowerCase(Locale.ROOT))
                            .collect(java.util.stream.Collectors.toUnmodifiableSet())));
        }
        return Collections.unmodifiableMap(normalized);
    }
}
