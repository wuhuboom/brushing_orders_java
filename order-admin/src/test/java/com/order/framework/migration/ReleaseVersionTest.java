package com.order.framework.migration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReleaseVersionTest
{
    @Test
    void comparesNumericSegmentsInsteadOfLexically()
    {
        assertTrue(ReleaseVersion.parse("1.10.0").compareTo(ReleaseVersion.parse("1.2.9")) > 0);
        assertEquals(0, ReleaseVersion.parse("1.2").compareTo(ReleaseVersion.parse("1.2.0")));
        assertTrue(ReleaseVersion.parse("2.0").compareTo(ReleaseVersion.parse("1.99.99")) > 0);
    }

    @Test
    void trimsInputAndRejectsNonNumericVersions()
    {
        assertEquals("1.2.0", ReleaseVersion.parse(" 1.2.0 ").toString());
        assertThrows(IllegalArgumentException.class, () -> ReleaseVersion.parse("1.2.0-SNAPSHOT"));
        assertThrows(IllegalArgumentException.class, () -> ReleaseVersion.parse("1..2"));
        assertThrows(IllegalArgumentException.class, () -> ReleaseVersion.parse(" "));
    }
}
