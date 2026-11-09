package com.v2rayops.client.core.update

object VersionComparator {

    /**
     * Returns true if [remoteVersion] is strictly newer than [currentVersion].
     * Supports formats like:
     * - "1.0.1" vs "1.0.0"
     * - "v1.1.0" vs "1.0.0"
     * - "v1.0.0-beta.2" vs "1.0.0"
     */
    fun isNewer(currentVersion: String, remoteVersion: String): Boolean {
        val cleanCurrent = normalizeVersion(currentVersion)
        val cleanRemote = normalizeVersion(remoteVersion)

        if (cleanCurrent.isEmpty() || cleanRemote.isEmpty()) {
            return false
        }

        val currentParts = cleanCurrent.split(".").map { it.toIntOrNull() ?: 0 }
        val remoteParts = cleanRemote.split(".").map { it.toIntOrNull() ?: 0 }

        val maxLen = maxOf(currentParts.size, remoteParts.size)

        for (i in 0 until maxLen) {
            val curr = currentParts.getOrElse(i) { 0 }
            val rem = remoteParts.getOrElse(i) { 0 }
            if (rem > curr) return true
            if (rem < curr) return false
        }

        return false
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
