package com.v2rayops.client.network

import com.v2rayops.client.ui.routefinder.CandidateBenchmarkType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkStressSuiteTest {

    @Test
    fun stabilityProgress_scoreCalculationLogic() {
        // Case 1: Perfect connection (0% loss, low jitter 10ms, avg latency 50ms)
        val lossPercent1 = 0.0
        val jitter1 = 10L
        val avgLat1 = 50L
        val score1 = (100.0 - (lossPercent1 * 2.5) - (jitter1.coerceAtMost(150) * 0.25) - (if (avgLat1 > 400) 15.0 else 0.0))
            .coerceIn(0.0, 100.0).toInt()
        assertEquals(97, score1)
        assertTrue("Must be Rock Solid", score1 >= 90)

        // Case 2: High jitter connection (0% loss, 120ms jitter, 150ms avg latency)
        val jitter2 = 120L
        val score2 = (100.0 - (0.0 * 2.5) - (jitter2.coerceAtMost(150) * 0.25) - (if (150L > 400) 15.0 else 0.0))
            .coerceIn(0.0, 100.0).toInt()
        assertEquals(70, score2)
        assertTrue("Must be Moderate/Stable", score2 in 65..75)

        // Case 3: High packet loss connection (25% loss, 30ms jitter)
        val lossPercent3 = 25.0
        val score3 = (100.0 - (lossPercent3 * 2.5) - (30L.coerceAtMost(150) * 0.25) - (if (100L > 400) 15.0 else 0.0))
            .coerceIn(0.0, 100.0).toInt()
        assertEquals(30, score3)
        assertTrue("Must be degraded", score3 < 45)
    }

    @Test
    fun download100Mb_progressCalculations() {
        val totalTarget = 104_857_600L // 100MB
        val bytesRead = 52_428_800L   // 50MB
        val elapsedSec = 10.0

        val progressPercent = (bytesRead.toFloat() / totalTarget.toFloat()).coerceIn(0f, 1f)
        assertEquals(0.5f, progressPercent, 0.001f)

        val avgMbps = (bytesRead * 8.0) / (elapsedSec * 1_000_000.0)
        assertEquals(41.94, avgMbps, 0.01)

        val remainingBytes = totalTarget - bytesRead
        val etaSec = ((remainingBytes * 8.0) / (avgMbps * 1_000_000.0)).toLong()
        assertEquals(10L, etaSec)
    }

    @Test
    fun congestionStress_bufferbloatGrading() {
        fun computeGrade(delta: Long, loss: Double): String {
            return when {
                loss >= 10.0 -> "GRADE F (Bufferbloat Saturated / Drops)"
                delta <= 15 && loss <= 1.0 -> "GRADE A+ (Zero Inflation)"
                delta <= 35 && loss <= 3.0 -> "GRADE A (Minimal Delay)"
                delta <= 70 && loss <= 6.0 -> "GRADE B (Mild Bloat)"
                delta <= 150 -> "GRADE C (Noticeable Delay)"
                delta <= 300 -> "GRADE D (Heavy Queuing)"
                else -> "GRADE F (Bufferbloat Saturated / Drops)"
            }
        }

        assertEquals("GRADE A+ (Zero Inflation)", computeGrade(8, 0.0))
        assertEquals("GRADE A (Minimal Delay)", computeGrade(28, 1.5))
        assertEquals("GRADE B (Mild Bloat)", computeGrade(55, 4.0))
        assertEquals("GRADE C (Noticeable Delay)", computeGrade(110, 2.0))
        assertEquals("GRADE D (Heavy Queuing)", computeGrade(220, 0.0))
        assertEquals("GRADE F (Bufferbloat Saturated / Drops)", computeGrade(450, 8.0))
        assertEquals("GRADE F (Bufferbloat Saturated / Drops)", computeGrade(10, 15.0))
    }

    @Test
    fun speedTest_ratingClassification() {
        fun computeRating(dlMbps: Double, ulMbps: Double, ping: Long): String {
            return when {
                dlMbps >= 25.0 && ulMbps >= 8.0 && ping < 80 -> "4K UHD Streaming & Low-Latency Gaming Ready"
                dlMbps >= 10.0 && ulMbps >= 3.0 -> "1080p FHD Video & High-Speed Browsing Ready"
                dlMbps >= 3.0 -> "720p HD Standard Streaming"
                else -> "Basic Web & Messaging Connection"
            }
        }

        assertEquals("4K UHD Streaming & Low-Latency Gaming Ready", computeRating(55.0, 15.0, 42))
        assertEquals("1080p FHD Video & High-Speed Browsing Ready", computeRating(18.0, 5.0, 95))
        assertEquals("720p HD Standard Streaming", computeRating(6.5, 1.2, 120))
        assertEquals("Basic Web & Messaging Connection", computeRating(1.5, 0.5, 250))
    }

    @Test
    fun candidateBenchmarkTypes_allExist() {
        val types = CandidateBenchmarkType.values()
        assertEquals(4, types.size)
        assertTrue(types.contains(CandidateBenchmarkType.SPEEDTEST))
        assertTrue(types.contains(CandidateBenchmarkType.STABILITY_2MIN))
        assertTrue(types.contains(CandidateBenchmarkType.DOWNLOAD_100MB))
        assertTrue(types.contains(CandidateBenchmarkType.CONGESTION_STRESS))
    }
}
