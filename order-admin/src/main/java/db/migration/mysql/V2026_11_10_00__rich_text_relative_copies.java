package db.migration.mysql;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Adds stored relative image copies without changing any existing HTML or translation value. */
public class V2026_11_10_00__rich_text_relative_copies extends BaseJavaMigration {
    private static final int BATCH_SIZE = 100;
    // Freeze the released locale set instead of consulting the application's evolving registry.
    private static final List<String> LOCALES = List.of("zh_CN", "zh_TW", "ko_KR", "th_TH", "ja_JP", "pt_PT",
            "en_US", "ar_SA", "es_ES", "sv_SE", "it_IT", "de_DE", "no_NO", "ru_RU", "hu_HU", "pl_PL",
            "sk_SK", "fr_FR", "cs_CZ", "pt_BR", "hi_IN");
    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    public Integer getChecksum() {
        CRC32 checksum = new CRC32();
        List<Class<?>> sources = new ArrayList<>(List.of(getClass(), RichTextImagePathsV20261110.class));
        sources.addAll(List.of(RichTextImagePathsV20261110.class.getDeclaredClasses()));
        for (Class<?> source : sources) {
            try (var input = source.getResourceAsStream("/" + source.getName().replace('.', '/') + ".class")) {
                if (input == null) throw new IllegalStateException("Missing migration bytecode: " + source.getName());
                checksum.update(input.readAllBytes());
            } catch (IOException ex) {
                throw new IllegalStateException("Unable to checksum relative text migration", ex);
            }
        }
        return (int) checksum.getValue();
    }

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        Map<String, String> sources = new LinkedHashMap<>();
        sources.put("order_config", "content");
        sources.put("goods", "description");
        sources.put("goods_member_level", "description");
        sources.put("order_site_message", "content");
        // Check every dependency and any pre-existing destination before MySQL commits the first DDL.
        for (var entry : sources.entrySet()) {
            requireColumn(connection, entry.getKey(), "id");
            requireColumn(connection, entry.getKey(), entry.getValue());
            validateDestination(connection, entry.getKey(), entry.getValue() + "_relative");
        }
        requireColumn(connection, "translations", "id");
        for (String locale : LOCALES) {
            requireColumn(connection, "translations", locale);
        }
        validateDestination(connection, "translations", "relative_content");
        for (var entry : sources.entrySet()) {
            addColumn(connection, entry.getKey(), entry.getValue() + "_relative");
        }
        addColumn(connection, "translations", "relative_content");
        for (var entry : sources.entrySet()) {
            backfillContent(connection, entry.getKey(), entry.getValue());
        }
        backfillTranslations(connection);
    }

    private static void requireColumn(Connection connection, String table, String column) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1 FROM information_schema.columns
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?
                """)) {
            statement.setString(1, table);
            statement.setString(2, column);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) throw new SQLException("Missing migration dependency: " + table + "." + column);
            }
        }
    }

    private static boolean validateDestination(Connection connection, String table, String column)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT DATA_TYPE, IS_NULLABLE FROM information_schema.columns
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?
                """)) {
            statement.setString(1, table);
            statement.setString(2, column);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) return false;
                if (!"longtext".equalsIgnoreCase(rows.getString("DATA_TYPE"))
                        || !"YES".equalsIgnoreCase(rows.getString("IS_NULLABLE"))) {
                    throw new SQLException("Unexpected relative text column definition: " + table + "." + column);
                }
                return true;
            }
        }
    }

    private static void addColumn(Connection connection, String table, String column) throws SQLException {
        if (validateDestination(connection, table, column)) return;
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " LONGTEXT NULL");
        }
    }

    private static void backfillContent(Connection connection, String table, String source) throws Exception {
        Long afterId = null;
        while (true) {
            List<Map.Entry<Long, String>> batch = new ArrayList<>();
            String sql = "SELECT id, " + source + " FROM " + table + " WHERE " + source + "_relative IS NULL"
                    + (afterId == null ? "" : " AND id > ?") + " ORDER BY id LIMIT " + BATCH_SIZE;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                if (afterId != null) statement.setLong(1, afterId);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        batch.add(new java.util.AbstractMap.SimpleImmutableEntry<>(
                                rows.getLong("id"), rows.getString(source)));
                    }
                }
            }
            if (batch.isEmpty()) return;
            try (PreparedStatement statement = connection.prepareStatement("UPDATE " + table + " SET "
                    + source + "_relative = ? WHERE id = ? AND " + source + "_relative IS NULL")) {
                for (var row : batch) {
                    setNullableString(statement, 1, RichTextImagePathsV20261110.toRelative(row.getValue()));
                    statement.setLong(2, row.getKey());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            afterId = batch.get(batch.size() - 1).getKey();
        }
    }

    private static void backfillTranslations(Connection connection) throws Exception {
        Long afterId = null;
        String columns = String.join(", ", LOCALES);
        while (true) {
            List<Map.Entry<Long, String>> batch = new ArrayList<>();
            String sql = "SELECT id, " + columns + " FROM translations WHERE relative_content IS NULL"
                    + (afterId == null ? "" : " AND id > ?") + " ORDER BY id LIMIT " + BATCH_SIZE;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                if (afterId != null) statement.setLong(1, afterId);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        Map<String, String> relative = new LinkedHashMap<>();
                        for (String locale : LOCALES) {
                            String value = rows.getString(locale);
                            if (value != null) relative.put(locale, RichTextImagePathsV20261110.toRelative(value));
                        }
                        batch.add(Map.entry(rows.getLong("id"), JSON.writeValueAsString(relative)));
                    }
                }
            }
            if (batch.isEmpty()) return;
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE translations SET relative_content = ? WHERE id = ? AND relative_content IS NULL")) {
                for (var row : batch) {
                    statement.setString(1, row.getValue());
                    statement.setLong(2, row.getKey());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            afterId = batch.get(batch.size() - 1).getKey();
        }
    }

    private static void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null) statement.setNull(index, Types.LONGVARCHAR);
        else statement.setString(index, value);
    }
}
