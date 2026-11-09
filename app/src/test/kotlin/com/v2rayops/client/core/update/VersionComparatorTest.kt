package com.v2rayops.client.core.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparatorTest {

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
