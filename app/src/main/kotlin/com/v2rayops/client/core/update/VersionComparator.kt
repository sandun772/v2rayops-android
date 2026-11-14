package com.v2rayops.client.core.update

object VersionComparator {

    /**
     * Standard semantic version comparator.
     * Returns:
     * - Negative value if [v1] is older than [v2] (v1 < v2)
     * - Zero if [v1] and [v2] are semantically identical (v1 == v2)
     * - Positive value if [v1] is newer than [v2] (v1 > v2)
     */
    fun compare(v1: String, v2: String): Int {
        val clean1 = normalizeVersion(v1)
        val clean2 = normalizeVersion(v2)

        if (clean1.isEmpty() && clean2.isEmpty()) return 0
        if (clean1.isEmpty()) return -1
        if (clean2.isEmpty()) return 1

        val parts1 = clean1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = clean2.split(".").map { it.toIntOrNull() ?: 0 }

        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) {
                return p1.compareTo(p2)
            }
        }
        return 0
    }

    /**
     * Returns true if [remoteVersion] is strictly newer than [currentVersion].
     * Supports formats like:
     * - "1.0.1" vs "1.0.0"
     * - "v1.1.0" vs "1.0.0"
     * - "v1.0.0-beta.2" vs "1.0.0"
     */
    fun isNewer(currentVersion: String, remoteVersion: String): Boolean {
        return compare(remoteVersion, currentVersion) > 0
    }

    fun normalizeVersion(raw: String): String {
        return raw.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore("-")
            .substringBefore("+")
            .trim()
    }
}
