package com.v2rayops.client.core.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparatorTest {

    @Test
    fun compare_ordersVersionsCorrectly() {
        assertTrue(VersionComparator.compare("1.0.0", "1.0.1") < 0)
        assertTrue(VersionComparator.compare("1.0.1", "1.0.0") > 0)
        assertEquals(0, VersionComparator.compare("1.0.0", "1.0.0"))
        assertEquals(0, VersionComparator.compare("v1.0.3", "1.0.3"))
        assertTrue(VersionComparator.compare("v1.0.3", "v1.0.2") > 0)
        assertTrue(VersionComparator.compare("v1.0.2", "v1.0.3") < 0)

        // Verify maxWithOrNull picks highest release
        val releases = listOf("v1.0.0", "v1.0.3", "v1.0.1", "v1.0.4", "v1.0.2")
        val max = releases.maxWithOrNull { a, b -> VersionComparator.compare(a, b) }
        assertEquals("v1.0.4", max)
        assertTrue(VersionComparator.isNewer("1.0.3", "1.0.4"))
    }

    @Test
    fun isNewer_patchVersionBump_returnsTrue() {
        assertTrue(VersionComparator.isNewer("1.0.0", "1.0.1"))
        assertTrue(VersionComparator.isNewer("1.0.0", "v1.0.1"))
        assertTrue(VersionComparator.isNewer("v1.0.0", "v1.0.1"))
    }

    @Test
    fun isNewer_minorVersionBump_returnsTrue() {
        assertTrue(VersionComparator.isNewer("1.0.9", "1.1.0"))
        assertTrue(VersionComparator.isNewer("1.0.0", "v1.1.0"))
    }

    @Test
    fun isNewer_majorVersionBump_returnsTrue() {
        assertTrue(VersionComparator.isNewer("1.9.9", "2.0.0"))
    }

    @Test
    fun isNewer_sameVersion_returnsFalse() {
        assertFalse(VersionComparator.isNewer("1.0.0", "1.0.0"))
        assertFalse(VersionComparator.isNewer("1.0.0", "v1.0.0"))
        assertFalse(VersionComparator.isNewer("v1.0.0", "1.0.0"))
        assertFalse(VersionComparator.isNewer("1.0.0", "v1.0.0-beta"))
    }

    @Test
    fun isNewer_olderVersion_returnsFalse() {
        assertFalse(VersionComparator.isNewer("1.0.1", "1.0.0"))
        assertFalse(VersionComparator.isNewer("2.0.0", "1.9.9"))
    }

    @Test
    fun isNewer_varyingSegmentLengths_handlesCorrectly() {
        assertTrue(VersionComparator.isNewer("1.0", "1.0.1"))
        assertFalse(VersionComparator.isNewer("1.0.1", "1.0"))
        assertFalse(VersionComparator.isNewer("1.0", "1.0.0"))
    }
}
