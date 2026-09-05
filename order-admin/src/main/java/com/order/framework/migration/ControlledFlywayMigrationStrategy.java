package com.order.framework.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.flywaydb.core.api.MigrationVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.stereotype.Component;

/**
 * Applies Flyway migrations with a guarded bootstrap for the legacy schema and
 * a separate database migration release-version gate.
 */
@Component
public class ControlledFlywayMigrationStrategy implements FlywayMigrationStrategy
{
    static final String LEGACY_BASELINE_VERSION = "2026.08.17.0";

    static final String VERSION_TABLE = "sys_application_version";

    static final Map<String, VersionColumnDefinition> VERSION_TABLE_COLUMNS = Map.of(
            "id", new VersionColumnDefinition("tinyint", false),
            "application_version", new VersionColumnDefinition("varchar", false),
            "schema_version", new VersionColumnDefinition("varchar", true),
            "last_migrated_at", new VersionColumnDefinition("datetime", false));

    private static final Map<MigrationVersion, MigrationVersion> COMPATIBLE_BELOW_BASELINE_SUCCESSORS = Map.of(
            MigrationVersion.fromVersion("2026.08.04.00"), MigrationVersion.fromVersion("2026.08.18.04"),
            MigrationVersion.fromVersion("2026.08.08.00"), MigrationVersion.fromVersion("2026.08.18.05"));

    private static final Logger log = LoggerFactory.getLogger(ControlledFlywayMigrationStrategy.class);

    private final String migrationVersion;

    public ControlledFlywayMigrationStrategy(
            @Value("${order.migration-version:${order.version}}") String migrationVersion)
    {
        this.migrationVersion = ReleaseVersion.parse(migrationVersion).toString();
    }

    @Override
    public void migrate(Flyway flyway)
    {
        DataSource dataSource = flyway.getConfiguration().getDataSource();
        try (Connection lockConnection = dataSource.getConnection())
        {
            String lockName = releaseLockName(lockConnection);
            acquireReleaseLock(lockConnection, lockName, flyway.getConfiguration().getLockRetryCount());
            try
            {
                migrateWhileLocked(flyway, dataSource);
            }
            finally
            {
                releaseReleaseLock(lockConnection, lockName);
            }
        }
        catch (SQLException exception)
        {
            throw new IllegalStateException("Unable to coordinate database migration", exception);
        }
    }

    private void migrateWhileLocked(Flyway flyway, DataSource dataSource)
    {
        String historyTable = flyway.getConfiguration().getTable();

        boolean historyTableExists = tableExists(dataSource, historyTable);
        LegacySchemaSnapshot legacySchema = historyTableExists
                ? LegacySchemaSnapshot.empty() : inspectLegacySchema(dataSource);
        LegacyBootstrapDecision.Action bootstrapAction = LegacyBootstrapDecision.decide(
                historyTableExists, legacySchema);
        if (bootstrapAction == LegacyBootstrapDecision.Action.BASELINE)
        {
            String configuredBaseline = flyway.getConfiguration().getBaselineVersion().getVersion();
            if (!LEGACY_BASELINE_VERSION.equals(configuredBaseline))
            {
                throw new IllegalStateException("Controlled legacy baseline must be configured as "
                        + LEGACY_BASELINE_VERSION + " but was " + configuredBaseline);
            }
            log.info("No Flyway history found; baselining verified legacy schema at {}", LEGACY_BASELINE_VERSION);
            flyway.baseline();
        }

        MigrationInfo[] migrationInfos = flyway.info().all();
        rejectPendingLegacyDataMigrations(migrationInfos);
        rejectIgnoredResolvedMigrations(migrationInfos);
        int pendingCount = (int) Arrays.stream(migrationInfos)
                .filter(migration -> migration.getState() == MigrationState.PENDING)
                .count();
        String databaseVersion = readDatabaseApplicationVersion(dataSource);
        MigrationVersionDecision.Action action = MigrationVersionDecision.decide(
                migrationVersion, databaseVersion, pendingCount);

        if (action == MigrationVersionDecision.Action.SKIP)
        {
            // A no-op startup has no legitimate pending migration, so any Flyway validation error is fatal.
            flyway.validate();
            log.info("Database and migration release are already consistent at {}; skipping migration",
                    migrationVersion);
            return;
        }

        log.info("Applying {} pending database migration(s) for migration release {}", pendingCount,
                migrationVersion);
        flyway.migrate();

        MigrationInfo[] remaining = flyway.info().pending();
        if (remaining.length != 0)
        {
            throw new IllegalStateException("Flyway completed with " + remaining.length + " migration(s) still pending");
        }
        // migrate() performs Flyway's normal pre-migration validation. Once pending migrations are applied,
        // validate strictly again so every checksum/missing/failed/ignored error blocks the version write.
        flyway.validate();
        requireVersionTableStructure(dataSource);

        MigrationInfo current = flyway.info().current();
        String schemaVersion = current == null || current.getVersion() == null
                ? null : current.getVersion().getVersion();
        writeDatabaseApplicationVersion(dataSource, migrationVersion, schemaVersion);
        log.info("Database migration version advanced to {} (schema {})", migrationVersion, schemaVersion);
    }

    static void rejectIgnoredResolvedMigrations(MigrationInfo[] migrations)
    {
        Set<MigrationVersion> compatibleSuccessorVersions = Arrays.stream(migrations)
                .filter(migration -> migration.getVersion() != null
                        && (migration.getState() == MigrationState.PENDING
                                || migration.getState() == MigrationState.SUCCESS
                                || migration.getState() == MigrationState.OUT_OF_ORDER))
                .map(MigrationInfo::getVersion)
                .collect(java.util.stream.Collectors.toSet());
        java.util.List<String> ignored = Arrays.stream(migrations)
                .filter(migration -> migration.getState() == MigrationState.IGNORED
                        || migration.getState() == MigrationState.BELOW_BASELINE
                        || migration.getState() == MigrationState.BASELINE_IGNORED
                        || migration.getState() == MigrationState.ABOVE_TARGET)
                .filter(migration -> !isCompatibleBelowBaselineMigration(migration, compatibleSuccessorVersions))
                .map(migration -> (migration.getVersion() == null ? "repeatable" : migration.getVersion().getVersion())
                        + " (" + migration.getDescription() + ", " + migration.getState().getDisplayName() + ")")
                .toList();
        if (!ignored.isEmpty())
        {
            throw new IllegalStateException("Resolved migration(s) would be ignored and cannot be auto-migrated: "
                    + String.join(", ", ignored));
        }
    }

    static void rejectPendingLegacyDataMigrations(MigrationInfo[] migrations)
    {
        List<String> pendingLegacyDataMigrations = Arrays.stream(migrations)
                .filter(migration -> migration.getState() == MigrationState.PENDING
                        && migration.getVersion() != null)
                .map(MigrationInfo::getVersion)
                .filter(COMPATIBLE_BELOW_BASELINE_SUCCESSORS::containsKey)
                .map(MigrationVersion::getVersion)
                .toList();
        if (!pendingLegacyDataMigrations.isEmpty())
        {
            throw new IllegalStateException("Legacy data migration(s) "
                    + String.join(", ", pendingLegacyDataMigrations)
                    + " are pending and must not be auto-replayed; preserve existing legacy data and use the "
                    + LEGACY_BASELINE_VERSION + " controlled adoption boundary");
        }
    }

    private static boolean isCompatibleBelowBaselineMigration(MigrationInfo migration,
            Set<MigrationVersion> compatibleSuccessorVersions)
    {
        if (migration.getState() != MigrationState.BELOW_BASELINE || migration.getVersion() == null)
        {
            return false;
        }
        MigrationVersion successor = COMPATIBLE_BELOW_BASELINE_SUCCESSORS.get(migration.getVersion());
        if (successor == null || !compatibleSuccessorVersions.contains(successor))
        {
            return false;
        }
        log.warn("Allowing known below-baseline compatibility migration {} because successor {} is packaged",
                migration.getVersion(), successor);
        return true;
    }

    private static String releaseLockName(Connection connection) throws SQLException
    {
        String catalog = connection.getCatalog();
        String candidate = "order:flyway-release:" + (catalog == null ? "default" : catalog);
        if (candidate.length() <= 64)
        {
            return candidate;
        }
        return "order:flyway-release:" + Integer.toUnsignedString(candidate.hashCode(), 16);
    }

    private static void acquireReleaseLock(Connection connection, String lockName, int timeoutSeconds)
            throws SQLException
    {
        try (PreparedStatement statement = connection.prepareStatement("SELECT GET_LOCK(?, ?)"))
        {
            statement.setString(1, lockName);
            statement.setInt(2, Math.max(timeoutSeconds, 0));
            try (ResultSet resultSet = statement.executeQuery())
            {
                if (!resultSet.next() || resultSet.getInt(1) != 1)
                {
                    throw new IllegalStateException("Timed out waiting for database migration lock " + lockName);
                }
            }
        }
    }

    private static void releaseReleaseLock(Connection connection, String lockName)
    {
        SQLException releaseFailure = null;
        try (PreparedStatement statement = connection.prepareStatement("SELECT RELEASE_LOCK(?)"))
        {
            statement.setString(1, lockName);
            try (ResultSet resultSet = statement.executeQuery())
            {
                if (!resultSet.next() || resultSet.getInt(1) != 1)
                {
                    throw new SQLException("RELEASE_LOCK did not confirm release of " + lockName);
                }
            }
            return;
        }
        catch (SQLException exception)
        {
            releaseFailure = exception;
        }

        try (PreparedStatement statement = connection.prepareStatement("SELECT RELEASE_ALL_LOCKS()");
                ResultSet ignored = statement.executeQuery())
        {
            log.warn("RELEASE_LOCK failed for {}; released all named locks held by the migration session instead",
                    lockName, releaseFailure);
            return;
        }
        catch (SQLException releaseAllFailure)
        {
            releaseFailure.addSuppressed(releaseAllFailure);
        }

        try
        {
            // A pooled close may keep the physical MySQL session alive, so abort it after both release calls fail.
            connection.abort(Runnable::run);
        }
        catch (SQLException abortFailure)
        {
            releaseFailure.addSuppressed(abortFailure);
        }
        log.warn("Unable to release database migration lock {}; aborted the physical JDBC session", lockName,
                releaseFailure);
    }

    static LegacySchemaSnapshot inspectLegacySchema(DataSource dataSource)
    {
        Set<String> tableNames = LegacyBootstrapDecision.REQUIRED_COLUMNS.keySet();
        String placeholders = String.join(", ", tableNames.stream().map(name -> "?").toList());
        String columnsSql = "SELECT LOWER(table_name), LOWER(column_name) FROM information_schema.columns "
                + "WHERE table_schema = DATABASE() AND LOWER(table_name) IN (" + placeholders + ")";
        String primaryKeysSql = "SELECT LOWER(table_name), LOWER(column_name) "
                + "FROM information_schema.key_column_usage WHERE table_schema = DATABASE() "
                + "AND constraint_name = 'PRIMARY' AND LOWER(table_name) IN (" + placeholders + ") "
                + "ORDER BY table_name, ordinal_position";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement columns = connection.prepareStatement(columnsSql);
                PreparedStatement primaryKeys = connection.prepareStatement(primaryKeysSql))
        {
            bindTableNames(columns, tableNames);
            bindTableNames(primaryKeys, tableNames);
            return new LegacySchemaSnapshot(readSchemaItems(columns), readSchemaItems(primaryKeys));
        }
        catch (SQLException exception)
        {
            throw new IllegalStateException("Unable to inspect legacy database structure before baseline", exception);
        }
    }

    private static void bindTableNames(PreparedStatement statement, Set<String> tableNames) throws SQLException
    {
        int index = 1;
        for (String tableName : tableNames)
        {
            statement.setString(index++, tableName);
        }
    }

    private static java.util.Map<String, Set<String>> readSchemaItems(PreparedStatement statement) throws SQLException
    {
        java.util.Map<String, Set<String>> items = new java.util.LinkedHashMap<>();
        try (ResultSet resultSet = statement.executeQuery())
        {
            while (resultSet.next())
            {
                items.computeIfAbsent(resultSet.getString(1).toLowerCase(Locale.ROOT), key -> new LinkedHashSet<>())
                        .add(resultSet.getString(2).toLowerCase(Locale.ROOT));
            }
        }
        return items;
    }

    static boolean tableExists(DataSource dataSource, String tableName)
    {
        return existingTables(dataSource, Set.of(tableName)).contains(tableName.toLowerCase(Locale.ROOT));
    }

    static Set<String> existingTables(DataSource dataSource, Set<String> tableNames)
    {
        String placeholders = String.join(", ", tableNames.stream().map(name -> "?").toList());
        String sql = "SELECT LOWER(table_name) FROM information_schema.tables "
                + "WHERE table_schema = DATABASE() AND LOWER(table_name) IN (" + placeholders + ")";
        Set<String> normalizedNames = tableNames.stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql))
        {
            int index = 1;
            for (String tableName : normalizedNames)
            {
                statement.setString(index++, tableName);
            }
            try (ResultSet resultSet = statement.executeQuery())
            {
                Set<String> existing = new LinkedHashSet<>();
                while (resultSet.next())
                {
                    existing.add(resultSet.getString(1).toLowerCase(Locale.ROOT));
                }
                return existing;
            }
        }
        catch (SQLException exception)
        {
            throw new IllegalStateException("Unable to inspect database schema before migration", exception);
        }
    }

    private static String readDatabaseApplicationVersion(DataSource dataSource)
    {
        if (!tableExists(dataSource, VERSION_TABLE))
        {
            return null;
        }
        requireVersionTableStructure(dataSource);
        String sql = "SELECT application_version FROM " + VERSION_TABLE + " WHERE id = 1";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery())
        {
            return resultSet.next() ? resultSet.getString(1) : null;
        }
        catch (SQLException exception)
        {
            throw new IllegalStateException("Unable to read database application version", exception);
        }
    }

    static void requireVersionTableStructure(DataSource dataSource)
    {
        String columnsSql = "SELECT LOWER(column_name), LOWER(data_type), UPPER(is_nullable) "
                + "FROM information_schema.columns "
                + "WHERE table_schema = DATABASE() AND LOWER(table_name) = ?";
        String primaryKeySql = "SELECT LOWER(column_name) FROM information_schema.key_column_usage "
                + "WHERE table_schema = DATABASE() AND LOWER(table_name) = ? AND constraint_name = 'PRIMARY' "
                + "ORDER BY ordinal_position";
        Map<String, VersionColumnDefinition> actualColumns = new LinkedHashMap<>();
        List<String> primaryKeyColumns = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement columns = connection.prepareStatement(columnsSql);
                PreparedStatement primaryKey = connection.prepareStatement(primaryKeySql))
        {
            columns.setString(1, VERSION_TABLE);
            try (ResultSet resultSet = columns.executeQuery())
            {
                while (resultSet.next())
                {
                    actualColumns.put(resultSet.getString(1).toLowerCase(Locale.ROOT),
                            new VersionColumnDefinition(resultSet.getString(2).toLowerCase(Locale.ROOT),
                                    "YES".equalsIgnoreCase(resultSet.getString(3))));
                }
            }
            primaryKey.setString(1, VERSION_TABLE);
            try (ResultSet resultSet = primaryKey.executeQuery())
            {
                while (resultSet.next())
                {
                    primaryKeyColumns.add(resultSet.getString(1).toLowerCase(Locale.ROOT));
                }
            }
        }
        catch (SQLException exception)
        {
            throw new IllegalStateException("Unable to inspect application version table", exception);
        }

        Set<String> missingColumns = new LinkedHashSet<>(VERSION_TABLE_COLUMNS.keySet());
        missingColumns.removeAll(actualColumns.keySet());
        if (!missingColumns.isEmpty())
        {
            throw new IllegalStateException("Application version table is absent or incomplete; missing column(s): "
                    + String.join(", ", missingColumns));
        }

        List<String> incompatibleColumns = VERSION_TABLE_COLUMNS.entrySet().stream()
                .filter(entry -> !entry.getValue().equals(actualColumns.get(entry.getKey())))
                .map(entry -> entry.getKey() + " expected " + entry.getValue().display()
                        + " but was " + actualColumns.get(entry.getKey()).display())
                .toList();
        if (!incompatibleColumns.isEmpty())
        {
            throw new IllegalStateException("Application version table has incompatible column definition(s): "
                    + String.join(", ", incompatibleColumns));
        }
        if (!primaryKeyColumns.equals(List.of("id")))
        {
            throw new IllegalStateException("Application version table primary key must contain only id but was: "
                    + (primaryKeyColumns.isEmpty() ? "<none>" : String.join(", ", primaryKeyColumns)));
        }
    }

    private static void writeDatabaseApplicationVersion(DataSource dataSource, String applicationVersion,
            String schemaVersion)
    {
        String sql = "INSERT INTO " + VERSION_TABLE
                + " (id, application_version, schema_version, last_migrated_at) VALUES (1, ?, ?, CURRENT_TIMESTAMP(3)) "
                + "ON DUPLICATE KEY UPDATE application_version = VALUES(application_version), "
                + "schema_version = VALUES(schema_version), last_migrated_at = VALUES(last_migrated_at)";
        try (Connection connection = dataSource.getConnection())
        {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql))
            {
                statement.setString(1, applicationVersion);
                statement.setString(2, schemaVersion);
                int updatedRows = statement.executeUpdate();
                if (updatedRows < 1)
                {
                    throw new IllegalStateException("Database application version was not updated");
                }
                connection.commit();
            }
            catch (SQLException | RuntimeException exception)
            {
                try
                {
                    connection.rollback();
                }
                catch (SQLException rollbackException)
                {
                    exception.addSuppressed(rollbackException);
                }
                throw exception;
            }
            finally
            {
                connection.setAutoCommit(originalAutoCommit);
            }
        }
        catch (SQLException exception)
        {
            throw new IllegalStateException("Unable to update database application version", exception);
        }
    }

    record VersionColumnDefinition(String dataType, boolean nullable)
    {
        String display()
        {
            return dataType + (nullable ? " NULL" : " NOT NULL");
        }
    }
}
