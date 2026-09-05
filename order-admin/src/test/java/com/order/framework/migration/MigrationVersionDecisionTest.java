package com.order.framework.migration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigrationVersionDecisionTest
{
    @Test
    void missingOrOlderDatabaseVersionMigratesAndUpdates()
    {
        assertEquals(MigrationVersionDecision.Action.MIGRATE_AND_UPDATE,
                MigrationVersionDecision.decide("1.2.0", null, 4));
        assertEquals(MigrationVersionDecision.Action.MIGRATE_AND_UPDATE,
                MigrationVersionDecision.decide("1.2.0", "1.1.0", 0));
    }

    @Test
    void matchingVersionWithoutPendingMigrationSkipsSecondStartup()
    {
        assertEquals(MigrationVersionDecision.Action.SKIP,
                MigrationVersionDecision.decide("1.2.0", "1.2", 0));
    }

    @Test
    void matchingVersionWithPendingMigrationRejectsUnversionedRelease()
    {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> MigrationVersionDecision.decide("1.2.0", "1.2.0", 1));
        assertTrue(exception.getMessage().contains("increase order.migration-version"));
    }

    @Test
    void newerDatabaseVersionRejectsDowngrade()
    {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> MigrationVersionDecision.decide("1.2.0", "1.3.0", 0));
        assertTrue(exception.getMessage().contains("downgrade is not allowed"));
    }
}
