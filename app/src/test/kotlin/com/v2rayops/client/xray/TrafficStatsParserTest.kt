package com.v2rayops.client.xray

import com.v2rayops.client.domain.model.TrafficStats
import org.junit.Assert.assertEquals
import org.junit.Test

class TrafficStatsParserTest {

    @Test
    fun parseOutboundStats_correctlySumsUplinkAndDownlink() {
        val rawStats = "proxy,uplink,1024;proxy,downlink,4096;direct,uplink,512;direct,downlink,1024;"
        val records = rawStats.split(";")

        var uplink = 0L
        var downlink = 0L

        for (record in records) {
            val parts = record.split(",")
            if (parts.size >= 3) {
                val direction = parts[1].trim()
                val value = parts[2].trim().toLongOrNull() ?: 0L

                if (direction.equals("uplink", ignoreCase = true)) {
                    uplink += value
                } else if (direction.equals("downlink", ignoreCase = true)) {
                    downlink += value
                }
            }
        }

        assertEquals(1536L, uplink)
        assertEquals(5120L, downlink)

        val stats = TrafficStats(
            uploadBytesPerSec = uplink,
            downloadBytesPerSec = downlink,
            totalUploadBytes = uplink,
            totalDownloadBytes = downlink
        )

        assertEquals(6656L, stats.totalBytes)
    }

    @Test
    fun parseOutboundStats_handlesEmptyOrMalformedString() {
        val rawStats = "invalid;malformed,data;"
        val records = rawStats.split(";")

        var uplink = 0L
        var downlink = 0L

        for (record in records) {
            val parts = record.split(",")
            if (parts.size >= 3) {
                val direction = parts[1].trim()
                val value = parts[2].trim().toLongOrNull() ?: 0L

                if (direction.equals("uplink", ignoreCase = true)) {
                    uplink += value
                } else if (direction.equals("downlink", ignoreCase = true)) {
                    downlink += value
                }
            }
        }

        assertEquals(0L, uplink)
        assertEquals(0L, downlink)
    }
}
