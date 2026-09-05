package com.order.framework.migration;

/**
 * Centralises the release-version gate independently of Flyway and JDBC.
 */
public final class MigrationVersionDecision
{
    private MigrationVersionDecision()
    {
    }

    public enum Action
    {
        SKIP,
        MIGRATE_AND_UPDATE
    }

    public static Action decide(String applicationVersion, String databaseVersion, int pendingCount)
    {
        if (pendingCount < 0)
        {
            throw new IllegalArgumentException("Pending migration count must not be negative");
        }

        ReleaseVersion application = ReleaseVersion.parse(applicationVersion);
        if (databaseVersion == null || databaseVersion.isBlank())
        {
            return Action.MIGRATE_AND_UPDATE;
        }

        ReleaseVersion database = ReleaseVersion.parse(databaseVersion);
        int comparison = database.compareTo(application);
        if (comparison > 0)
        {
            throw new IllegalStateException("Database application version " + database
                    + " is newer than running application " + application + "; downgrade is not allowed");
        }
        if (comparison == 0 && pendingCount > 0)
        {
            throw new IllegalStateException("Application and database are both at version " + application
                    + " but " + pendingCount
                    + " migration(s) are pending; increase order.migration-version before release");
        }
        return comparison == 0 ? Action.SKIP : Action.MIGRATE_AND_UPDATE;
    }
}
