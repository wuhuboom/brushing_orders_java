package db.migration.mysql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.zip.CRC32;
import java.io.IOException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Preflight all identity conflicts before MySQL's auto-committing DDL. Never deletes member data. */
public class V2026_11_09_00__identity_indexes_and_link_batch_sequence extends BaseJavaMigration {
    // Java migrations also need a stable checksum so deployed changes cannot pass validation silently.
    @Override
    public Integer getChecksum() {
        try (var input = getClass().getResourceAsStream(getClass().getSimpleName() + ".class")) {
            if (input == null) throw new IllegalStateException("Missing migration bytecode");
            CRC32 checksum = new CRC32();
            checksum.update(input.readAllBytes());
            return (int) checksum.getValue();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to checksum migration", ex);
        }
    }

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        for (String column : List.of("username", "phone_number", "invite_code")) {
            try (Statement statement = connection.createStatement();
                    ResultSet rows = statement.executeQuery("SELECT 1 FROM order_user WHERE " + column
                            + " IS NOT NULL GROUP BY " + column + " HAVING COUNT(*) > 1 LIMIT 1")) {
                if (rows.next()) {
                    throw new SQLException("Duplicate order_user." + column
                            + "; reconcile identities before retrying this migration. No records were deleted.");
                }
            }
        }
        addIndex(connection, "order_user", "uk_order_user_username", "UNIQUE", "username");
        addIndex(connection, "order_user", "uk_order_user_phone", "UNIQUE", "phone_number");
        addIndex(connection, "order_user", "uk_order_user_invite", "UNIQUE", "invite_code");
        addIndex(connection, "order_user", "idx_order_user_parent", "", "parent_id");
        addIndex(connection, "order_login_log", "idx_login_user_time", "", "user_id,create_time,id");
        addIndex(connection, "order_info", "idx_order_info_user_created", "", "user_id,create_time,id");
        addIndex(connection, "order_withdrawal", "idx_withdrawal_user_created", "", "user_id,create_time,id");
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS order_link_batch_sequence (
                        id TINYINT NOT NULL PRIMARY KEY,
                        current_value BIGINT NOT NULL
                    ) ENGINE=InnoDB
                    """);
            // Seed above historical IDs and never move an already-used allocator backwards.
            statement.executeUpdate("""
                    INSERT INTO order_link_batch_sequence (id, current_value)
                    SELECT 1, GREATEST(COALESCE(MAX(link_order_id), 0), 0) FROM order_link
                    ON DUPLICATE KEY UPDATE current_value = GREATEST(current_value, VALUES(current_value))
                    """);
        }
    }

    private static void addIndex(Connection connection, String table, String name,
            String kind, String columns) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT NON_UNIQUE, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS cols
                FROM information_schema.statistics
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?
                GROUP BY NON_UNIQUE
                """)) {
            statement.setString(1, table);
            statement.setString(2, name);
            try (ResultSet rows = statement.executeQuery()) {
                if (rows.next()) {
                    if (!columns.equalsIgnoreCase(rows.getString("cols"))
                            || ("UNIQUE".equals(kind) && rows.getInt("NON_UNIQUE") != 0)) {
                        throw new SQLException("Existing index has unexpected definition: " + table + "." + name);
                    }
                    return;
                }
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + table + " ADD " + kind + " INDEX " + name + " (" + columns + ")");
        }
    }
}
