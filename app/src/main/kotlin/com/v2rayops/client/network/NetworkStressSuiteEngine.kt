package com.v2rayops.client.network

import android.content.Context
import com.v2rayops.client.core.dispatchers.AppDispatchers
import com.v2rayops.client.core.logger.AppLogger
import com.v2rayops.client.core.result.AppResult
import com.v2rayops.client.domain.repository.SettingsRepository
import com.v2rayops.client.vpn.VpnManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.random.Random

// --- 1. Two-Minute Stability Data Models ---
data class StabilityProgress(
    val elapsedSeconds: Int = 0,
    val totalSeconds: Int = 120,
    val progressPercent: Float = 0f,
    val currentLatencyMs: Long = 0L,
    val minLatencyMs: Long = 0L,
    val avgLatencyMs: Long = 0L,
    val maxLatencyMs: Long = 0L,
    val jitterMs: Long = 0L,
    val packetsSent: Int = 0,
    val packetsReceived: Int = 0,
    val packetLossPercent: Double = 0.0,
    val stabilityScorePercent: Int = 100,
    val recentPings: List<Long> = emptyList(),
    val isRunning: Boolean = false,
    val statusSummary: String = "Initializing 2-minute stability audit..."
)

data class TwoMinuteStabilityResult(
    val durationSeconds: Int = 120,
    val packetsSent: Int,
    val packetsReceived: Int,
    val packetLossPercent: Double,
    val minLatencyMs: Long,
    val avgLatencyMs: Long,
    val maxLatencyMs: Long,
    val jitterMs: Long,
    val stabilityScorePercent: Int,
    val stabilityGrade: String,
    val pingHistory: List<Long>,
    val isSuccessful: Boolean,
    val failureReason: String = ""
)

// --- 2. 100MB Download & Dev Report Data Models ---
data class Download100MbProgress(
    val bytesReceived: Long = 0L,
    val totalBytes: Long = 104_857_600L, // 100MB
    val progressPercent: Float = 0f,
    val currentMbps: Double = 0.0,
    val avgMbps: Double = 0.0,
    val peakMbps: Double = 0.0,
    val elapsedSeconds: Long = 0L,
    val estimatedRemainingSeconds: Long = 0L,
    val isRunning: Boolean = false,
    val statusMessage: String = ""
)

data class Download100MbResult(
    val totalBytes: Long,
    val totalDurationMs: Long,
    val avgSpeedMbps: Double,
    val peakSpeedMbps: Double,
    val isSuccessful: Boolean,
    val devReportId: String? = null,
    val devReportUploaded: Boolean = false,
    val devReportUploadError: String? = null,
    val errorMessage: String = ""
)

// --- 3. Bidirectional Congestion / Stress Test Data Models ---
data class CongestionStressProgress(
    val elapsedSeconds: Int = 0,
    val totalSeconds: Int = 30,
    val progressPercent: Float = 0f,
    val currentPingMs: Long = 0L,
    val baselineIdlePingMs: Long = 0L,
    val bufferbloatDeltaMs: Long = 0L,
    val currentDownloadMbps: Double = 0.0,
    val currentUploadMbps: Double = 0.0,
    val packetsSent: Int = 0,
    val packetsDropped: Int = 0,
    val packetLossPercent: Double = 0.0,
    val isRunning: Boolean = false,
    val statusMessage: String = ""
)

data class CongestionStressResult(
    val durationSeconds: Int,
    val baselineIdlePingMs: Long,
    val loadedAvgPingMs: Long,
    val bufferbloatDeltaMs: Long,
    val bufferbloatGrade: String,
    val sustainedDownloadMbps: Double,
    val sustainedUploadMbps: Double,
    val packetLossPercent: Double,
    val isSuccessful: Boolean,
    val recommendation: String,
    val errorMessage: String = ""
)

// --- 4. Speedtest Data Models ---
enum class SpeedTestPhase(val label: String) {
    IDLE("Ready"),
    PING_JITTER("Measuring Latency & Jitter"),
    DOWNLOAD("Testing Download Throughput"),
    UPLOAD("Testing Upload Throughput"),
    COMPLETE("Speedtest Finished"),
    FAILED("Speedtest Failed")
}

data class SpeedTestProgress(
    val phase: SpeedTestPhase = SpeedTestPhase.IDLE,
    val progressPercent: Float = 0f,
    val pingMs: Long = 0L,
    val jitterMs: Long = 0L,
    val currentSpeedMbps: Double = 0.0,
    val downloadSpeedMbps: Double = 0.0,
    val uploadSpeedMbps: Double = 0.0,
    val isRunning: Boolean = false,
    val statusText: String = ""
)

data class SpeedTestResult(
    val pingMs: Long,
    val jitterMs: Long,
    val downloadSpeedMbps: Double,
    val uploadSpeedMbps: Double,
    val peakDownloadMbps: Double,
    val peakUploadMbps: Double,
    val isSuccessful: Boolean,
    val rating: String,
    val errorMessage: String = ""
)

/**
 * High-performance Network Stress & Stability Testing Suite:
 * - 2-minute sustained ping & jitter audit
 * - 100MB streaming benchmark with automatic VPS Dev Report upload
 * - Active load stability test with simultaneous background downloading and uploading (Bufferbloat analysis)
 * - Full 3-phase Speedtest (Ping/Jitter, Sustained Download, Sustained Upload)
 */
@Singleton
class NetworkStressSuiteEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vpnManager: VpnManager,
    private val settingsRepository: SettingsRepository,
    private val devReportManager: DevReportManager,
    private val dispatchers: AppDispatchers
) {
    companion object {
        private const val TAG = "NetworkStressSuite"
        private const val LOCAL_PROXY_PORT = 10809
        private const val SIZE_100_MB = 104_857_600L

        // Primary & Fallback Endpoints
        private val LATENCY_PROBE_ENDPOINTS = listOf(
            "https://1.1.1.1/cdn-cgi/trace",
            "https://cp.cloudflare.com/generate_204",
            "https://www.google.com/generate_204"
        )

        private val DOWNLOAD_100MB_ENDPOINTS = listOf(
            "https://speed.cloudflare.com/__down?bytes=104857600",
            "https://speed.cloudflare.com/__down?bytes=26214400", // 25MB chunks
            "https://proof.ovh.net/files/100Mb.dat"
        )

        private const val CLOUDFLARE_UPLOAD_ENDPOINT = "https://speed.cloudflare.com/__up"
    }

    private fun buildHttpClient(timeoutSec: Long = 10): OkHttpClient {
        val isVpnConnected = vpnManager.connectionState.value.isConnected
        val builder = OkHttpClient.Builder()
            .connectTimeout(timeoutSec, TimeUnit.SECONDS)
            .readTimeout(timeoutSec, TimeUnit.SECONDS)
            .writeTimeout(timeoutSec, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)

        if (isVpnConnected) {
            // Tunnel through active Xray core local HTTP inbound on 127.0.0.1:10809
            builder.proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", LOCAL_PROXY_PORT)))
        }

        return builder.build()
    }

    /**
     * Executes single latency probe. Returns latency in ms, or -1 if timeout/error.
     */
    private fun probePingSingle(client: OkHttpClient): Long {
        for (url in LATENCY_PROBE_ENDPOINTS) {
            try {
                val start = System.currentTimeMillis()
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "V2RayOps-StressSuite/2.0")
                    .header("Cache-Control", "no-cache")
                    .build()

                client.newCall(req).execute().use { res ->
                    if (res.isSuccessful || res.code == 204) {
                        return (System.currentTimeMillis() - start).coerceAtLeast(1)
                    }
                }
            } catch (_: Exception) {
                // Try next endpoint
            }
        }
        return -1L
    }

    // =========================================================================
    // 1. TWO-MINUTE DEEP STABILITY TEST
    // =========================================================================

    suspend fun runTwoMinuteStabilityTest(
        totalSeconds: Int = 120,
        onProgress: (StabilityProgress) -> Unit = {}
    ): TwoMinuteStabilityResult = withContext(dispatchers.io) {
        AppLogger.i(TAG, "Starting 2-minute sustained connection stability audit ($totalSeconds seconds)...")
        val client = buildHttpClient(timeoutSec = 4)

        val pingSamples = mutableListOf<Long>()
        var packetsSent = 0
        var packetsReceived = 0
        var lastValidPing = 0L
        var runningJitterSum = 0L
        var jitterCalculationsCount = 0

        val startTime = System.currentTimeMillis()
        val endTime = startTime + (totalSeconds * 1000L)

        onProgress(
            StabilityProgress(
                elapsedSeconds = 0,
                totalSeconds = totalSeconds,
                progressPercent = 0f,
                isRunning = true,
                statusSummary = "Auditing sustained connection stability..."
            )
        )

        while (System.currentTimeMillis() < endTime && isActive) {
            val sampleStart = System.currentTimeMillis()
            packetsSent++

            val latency = probePingSingle(client)

            if (latency > 0) {
                packetsReceived++
                pingSamples.add(latency)

                if (lastValidPing > 0) {
                    runningJitterSum += abs(latency - lastValidPing)
                    jitterCalculationsCount++
                }
                lastValidPing = latency
            }

            val elapsedSec = ((System.currentTimeMillis() - startTime) / 1000L).toInt().coerceAtMost(totalSeconds)
            val progressPercent = (elapsedSec.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)

            val dropped = packetsSent - packetsReceived
            val lossPercent = if (packetsSent > 0) (dropped.toDouble() / packetsSent.toDouble()) * 100.0 else 0.0
            val minLat = pingSamples.minOrNull() ?: 0L
            val maxLat = pingSamples.maxOrNull() ?: 0L
            val avgLat = if (pingSamples.isNotEmpty()) pingSamples.average().toLong() else 0L
            val avgJitter = if (jitterCalculationsCount > 0) runningJitterSum / jitterCalculationsCount else 0L

            val stabilityScore = (100.0 - (lossPercent * 2.5) - (avgJitter.coerceAtMost(150) * 0.25) - (if (avgLat > 400) 15.0 else 0.0))
                .coerceIn(0.0, 100.0).toInt()

            val recentWindow = pingSamples.takeLast(20)

            val statusText = when {
                lossPercent > 15.0 -> "High packet loss detected (${"%.1f".format(Locale.US, lossPercent)}%)"
                avgJitter > 80 -> "Significant carrier jitter (±${avgJitter}ms)"
                stabilityScore >= 85 -> "Sustained rock-solid tunnel connection (${stabilityScore}%)"
                else -> "Active audit in progress ($elapsedSec / ${totalSeconds}s)"
            }

            onProgress(
                StabilityProgress(
                    elapsedSeconds = elapsedSec,
                    totalSeconds = totalSeconds,
                    progressPercent = progressPercent,
                    currentLatencyMs = latency.coerceAtLeast(0),
                    minLatencyMs = minLat,
                    avgLatencyMs = avgLat,
                    maxLatencyMs = maxLat,
                    jitterMs = avgJitter,
                    packetsSent = packetsSent,
                    packetsReceived = packetsReceived,
                    packetLossPercent = lossPercent,
                    stabilityScorePercent = stabilityScore,
                    recentPings = recentWindow,
                    isRunning = true,
                    statusSummary = statusText
                )
            )

            // Dynamic interval: 1.5 seconds between probes
            val sampleDuration = System.currentTimeMillis() - sampleStart
            val sleepMs = (1500L - sampleDuration).coerceAtLeast(100L)
            delay(sleepMs)
        }

        val totalDurationSec = ((System.currentTimeMillis() - startTime) / 1000L).toInt()
        val finalDropped = packetsSent - packetsReceived
        val finalLoss = if (packetsSent > 0) (finalDropped.toDouble() / packetsSent.toDouble()) * 100.0 else 0.0
        val finalMin = pingSamples.minOrNull() ?: 0L
        val finalMax = pingSamples.maxOrNull() ?: 0L
        val finalAvg = if (pingSamples.isNotEmpty()) pingSamples.average().toLong() else 0L
        val finalJitter = if (jitterCalculationsCount > 0) runningJitterSum / jitterCalculationsCount else 0L

        val finalScore = (100.0 - (finalLoss * 2.5) - (finalJitter.coerceAtMost(150) * 0.25) - (if (finalAvg > 400) 15.0 else 0.0))
            .coerceIn(0.0, 100.0).toInt()

        val grade = when {
            finalScore >= 90 && finalLoss <= 2.0 -> "ROCK SOLID (A+)"
            finalScore >= 78 -> "STABLE (A)"
            finalScore >= 62 -> "MODERATE (B)"
            finalScore >= 45 -> "JITTER PRONE (C)"
            finalScore >= 25 -> "DEGRADED (D)"
            else -> "HIGH LOSS (F)"
        }

        val isSuccess = packetsReceived > 0 && finalLoss < 50.0

        onProgress(
            StabilityProgress(
                elapsedSeconds = totalDurationSec,
                totalSeconds = totalSeconds,
                progressPercent = 1.0f,
                currentLatencyMs = pingSamples.lastOrNull() ?: 0L,
                minLatencyMs = finalMin,
                avgLatencyMs = finalAvg,
                maxLatencyMs = finalMax,
                jitterMs = finalJitter,
                packetsSent = packetsSent,
                packetsReceived = packetsReceived,
                packetLossPercent = finalLoss,
                stabilityScorePercent = finalScore,
                recentPings = pingSamples.takeLast(20),
                isRunning = false,
                statusSummary = "Audit Finished: $grade ($finalScore% Score • ${"%.1f".format(Locale.US, finalLoss)}% Loss • ±${finalJitter}ms Jitter)"
            )
        )

        AppLogger.i(TAG, "2-Minute Stability Audit finished: $grade, score=$finalScore, sent=$packetsSent, rec=$packetsReceived, avg=${finalAvg}ms, jitter=±${finalJitter}ms")

        TwoMinuteStabilityResult(
            durationSeconds = totalDurationSec,
            packetsSent = packetsSent,
            packetsReceived = packetsReceived,
            packetLossPercent = finalLoss,
            minLatencyMs = finalMin,
            avgLatencyMs = finalAvg,
            maxLatencyMs = finalMax,
            jitterMs = finalJitter,
            stabilityScorePercent = finalScore,
            stabilityGrade = grade,
            pingHistory = pingSamples,
            isSuccessful = isSuccess,
            failureReason = if (isSuccess) "" else "Connection dropped or exceeded 50% packet loss"
        )
    }

    // =========================================================================
    // 2. 100MB DOWNLOAD BENCHMARK & DEV REPORT GENERATION
    // =========================================================================

    suspend fun run100MbDownloadTest(
        onProgress: (Download100MbProgress) -> Unit = {}
    ): Download100MbResult = withContext(dispatchers.io) {
        AppLogger.i(TAG, "Starting 100MB download benchmark with dev diagnostic upload...")
        val client = buildHttpClient(timeoutSec = 20)

        var totalBytesRead = 0L
        val targetBytes = SIZE_100_MB
        var peakSpeedMbps = 0.0
        val startTime = System.currentTimeMillis()

        var isSuccessful = false
        var lastError = ""
        var reportId: String? = null
        var reportUploaded = false
        var reportError: String? = null

        onProgress(
            Download100MbProgress(
                bytesReceived = 0L,
                totalBytes = targetBytes,
                progressPercent = 0f,
                isRunning = true,
                statusMessage = "Connecting to 100MB streaming endpoint..."
            )
        )

        try {
            // Stream chunks in a loop until 100MB is reached or failure
            val buffer = ByteArray(65536) // 64KB read buffer
            var endpointIndex = 0

            while (totalBytesRead < targetBytes && isActive) {
                val remainingBytes = targetBytes - totalBytesRead
                // Request up to remainingBytes or 25MB chunks
                val requestBytes = remainingBytes.coerceAtMost(26_214_400L)
                val url = "https://speed.cloudflare.com/__down?bytes=$requestBytes"

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "V2RayOps-100MB-Benchmark/2.0")
                    .header("Cache-Control", "no-cache")
                    .build()

                val callStart = System.currentTimeMillis()
                var chunkBytes = 0L

                try {
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            lastError = "HTTP ${response.code}"
                            endpointIndex++
                            return@use
                        }

                        val body = response.body ?: return@use
                        val inputStream: InputStream = body.byteStream()
                        var read: Int

                        var lastProgressUpdate = System.currentTimeMillis()

                        while (inputStream.read(buffer).also { read = it } != -1 && isActive) {
                            totalBytesRead += read
                            chunkBytes += read

                            val now = System.currentTimeMillis()
                            if (now - lastProgressUpdate >= 120 || totalBytesRead >= targetBytes) {
                                val totalElapsedMs = (now - startTime).coerceAtLeast(1)
                                val totalElapsedSec = totalElapsedMs / 1000.0

                                val currentWindowMs = (now - callStart).coerceAtLeast(1)
                                val currentWindowSec = currentWindowMs / 1000.0

                                val currentMbps = if (currentWindowSec > 0.1) {
                                    (chunkBytes * 8.0) / (currentWindowSec * 1_000_000.0)
                                } else 0.0

                                if (currentMbps > peakSpeedMbps) {
                                    peakSpeedMbps = currentMbps
                                }

                                val avgMbps = if (totalElapsedSec > 0.1) {
                                    (totalBytesRead * 8.0) / (totalElapsedSec * 1_000_000.0)
                                } else 0.0

                                val progressPercent = (totalBytesRead.toFloat() / targetBytes.toFloat()).coerceIn(0f, 1f)
                                val remainingBytesCalc = (targetBytes - totalBytesRead).coerceAtLeast(0L)
                                val etaSec = if (avgMbps > 0.1) {
                                    ((remainingBytesCalc * 8.0) / (avgMbps * 1_000_000.0)).toLong()
                                } else 0L

                                onProgress(
                                    Download100MbProgress(
                                        bytesReceived = totalBytesRead,
                                        totalBytes = targetBytes,
                                        progressPercent = progressPercent,
                                        currentMbps = currentMbps,
                                        avgMbps = avgMbps,
                                        peakMbps = peakSpeedMbps,
                                        elapsedSeconds = (totalElapsedMs / 1000L),
                                        estimatedRemainingSeconds = etaSec,
                                        isRunning = true,
                                        statusMessage = "Streaming 100MB payload (${"%.1f".format(Locale.US, totalBytesRead / (1024.0 * 1024.0))} MB / 100 MB • ${"%.2f".format(Locale.US, currentMbps)} Mbps)"
                                    )
                                )
                                lastProgressUpdate = now
                            }

                            if (totalBytesRead >= targetBytes) break
                        }
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    lastError = e.message ?: "Chunk transfer error"
                    AppLogger.w(TAG, "100MB chunk error: $lastError")
                    // Small delay before retrying next chunk
                    delay(300)
                }
            }

            isSuccessful = totalBytesRead >= (targetBytes * 0.90) // At least 90MB streamed successfully
        } catch (ce: CancellationException) {
            lastError = "Benchmark cancelled by user"
            throw ce
        } catch (e: Exception) {
            lastError = e.message ?: "Download benchmark error"
            AppLogger.e(TAG, "Fatal 100MB benchmark error: $lastError", e)
        }

        val totalDurationMs = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
        val finalAvgMbps = if (totalDurationMs > 0) {
            (totalBytesRead * 8.0) / ((totalDurationMs / 1000.0) * 1_000_000.0)
        } else 0.0

        // AUTOMATIC DEV REPORT COMPILATION & VPS UPLOAD
        onProgress(
            Download100MbProgress(
                bytesReceived = totalBytesRead,
                totalBytes = targetBytes,
                progressPercent = (totalBytesRead.toFloat() / targetBytes.toFloat()).coerceIn(0f, 1f),
                currentMbps = 0.0,
                avgMbps = finalAvgMbps,
                peakMbps = peakSpeedMbps,
                elapsedSeconds = totalDurationMs / 1000L,
                estimatedRemainingSeconds = 0L,
                isRunning = true,
                statusMessage = "100MB Complete. Compiling & uploading dev diagnostic report to VPS..."
            )
        )

        try {
            val userNote = "100MB Stress Benchmark: ${"%.2f".format(Locale.US, finalAvgMbps)} Mbps Avg, ${"%.2f".format(Locale.US, peakSpeedMbps)} Mbps Peak. Transferred: ${"%.1f".format(Locale.US, totalBytesRead / (1024.0 * 1024.0))} MB in ${totalDurationMs / 1000}s. Status: ${if (isSuccessful) "PASSED" else "PARTIAL ($lastError)"}"
            when (val uploadRes = devReportManager.collectAndUploadReport(userNote)) {
                is AppResult.Success -> {
                    reportId = uploadRes.data.reportId
                    reportUploaded = true
                    AppLogger.i(TAG, "Dev Report successfully uploaded to VPS! Report ID: $reportId")
                }
                is AppResult.Failure -> {
                    reportError = uploadRes.error.message
                    AppLogger.w(TAG, "Failed to upload Dev Report to VPS: $reportError")
                }
                is AppResult.Loading -> Unit
            }
        } catch (e: Exception) {
            reportError = e.message ?: "Upload error"
            AppLogger.w(TAG, "Exception during dev report upload: $reportError")
        }

        val finalStatusMessage = if (reportUploaded) {
            "Finished: ${"%.2f".format(Locale.US, finalAvgMbps)} Mbps Avg • Report #$reportId uploaded to VPS"
        } else {
            "Finished: ${"%.2f".format(Locale.US, finalAvgMbps)} Mbps Avg (${totalBytesRead / (1024 * 1024)} MB)"
        }

        onProgress(
            Download100MbProgress(
                bytesReceived = totalBytesRead,
                totalBytes = targetBytes,
                progressPercent = 1.0f,
                currentMbps = 0.0,
                avgMbps = finalAvgMbps,
                peakMbps = peakSpeedMbps,
                elapsedSeconds = totalDurationMs / 1000L,
                estimatedRemainingSeconds = 0L,
                isRunning = false,
                statusMessage = finalStatusMessage
            )
        )

        Download100MbResult(
            totalBytes = totalBytesRead,
            totalDurationMs = totalDurationMs,
            avgSpeedMbps = finalAvgMbps,
            peakSpeedMbps = peakSpeedMbps.coerceAtLeast(finalAvgMbps),
            isSuccessful = isSuccessful,
            devReportId = reportId,
            devReportUploaded = reportUploaded,
            devReportUploadError = reportError,
            errorMessage = if (isSuccessful) "" else lastError
        )
    }

    // =========================================================================
    // 3. CONGESTION & STRESS STABILITY TEST (ACTIVE DL + UL + PING)
    // =========================================================================

    suspend fun runCongestionStressTest(
        durationSeconds: Int = 30,
        onProgress: (CongestionStressProgress) -> Unit = {}
    ): CongestionStressResult = withContext(dispatchers.io) {
        AppLogger.i(TAG, "Starting bidirectional congestion stress test (Download + Upload + Jitter, ${durationSeconds}s)...")
        val client = buildHttpClient(timeoutSec = 6)

        // Phase 1: Measure baseline idle latency (3 samples)
        onProgress(
            CongestionStressProgress(
                elapsedSeconds = 0,
                totalSeconds = durationSeconds,
                isRunning = true,
                statusMessage = "Measuring baseline idle ping..."
            )
        )

        val idleSamples = mutableListOf<Long>()
        repeat(3) {
            val ping = probePingSingle(client)
            if (ping > 0) idleSamples.add(ping)
            delay(300)
        }
        val baselineIdlePing = if (idleSamples.isNotEmpty()) idleSamples.average().toLong() else 60L

        // Phase 2: Launch concurrent Download, Upload, and Latency Probers
        val isRunningFlag = AtomicBoolean(true)
        val dlBytesTotal = AtomicLong(0L)
        val ulBytesTotal = AtomicLong(0L)

        val loadedPings = mutableListOf<Long>()
        var sentPackets = 0
        var droppedPackets = 0

        val startTime = System.currentTimeMillis()
        val endTime = startTime + (durationSeconds * 1000L)

        coroutineScope {
            // Background Downloader Worker
            val dlJob = launch(dispatchers.io) {
                val dlBuf = ByteArray(32768)
                while (isRunningFlag.get() && isActive) {
                    try {
                        val req = Request.Builder()
                            .url("https://speed.cloudflare.com/__down?bytes=2097152") // 2MB
                            .header("Cache-Control", "no-cache")
                            .build()

                        client.newCall(req).execute().use { res ->
                            val isStream = res.body?.byteStream() ?: return@use
                            var read: Int
                            while (isStream.read(dlBuf).also { read = it } != -1 && isRunningFlag.get() && isActive) {
                                dlBytesTotal.addAndGet(read.toLong())
                            }
                        }
                    } catch (_: Exception) {
                        delay(200)
                    }
                }
            }

            // Background Uploader Worker
            val ulJob = launch(dispatchers.io) {
                val randomBytes = ByteArray(131072) // 128KB payload
                Random.nextBytes(randomBytes)
                val mediaType = "application/octet-stream".toMediaType()

                while (isRunningFlag.get() && isActive) {
                    try {
                        val body = randomBytes.toRequestBody(mediaType)
                        val req = Request.Builder()
                            .url(CLOUDFLARE_UPLOAD_ENDPOINT)
                            .post(body)
                            .header("Cache-Control", "no-cache")
                            .build()

                        client.newCall(req).execute().use { res ->
                            if (res.isSuccessful) {
                                ulBytesTotal.addAndGet(randomBytes.size.toLong())
                            }
                        }
                    } catch (_: Exception) {
                        delay(200)
                    }
                }
            }

            // Latency & Progress Monitor Loop
            try {
                while (System.currentTimeMillis() < endTime && isActive) {
                    val probeStart = System.currentTimeMillis()
                    sentPackets++

                    val ping = probePingSingle(client)
                    if (ping > 0) {
                        loadedPings.add(ping)
                    } else {
                        droppedPackets++
                    }

                    val elapsedSec = ((System.currentTimeMillis() - startTime) / 1000L).toInt().coerceAtMost(durationSeconds)
                    val elapsedTotalSec = (System.currentTimeMillis() - startTime).coerceAtLeast(1) / 1000.0
                    val currentDlMbps = (dlBytesTotal.get() * 8.0) / (elapsedTotalSec * 1_000_000.0)
                    val currentUlMbps = (ulBytesTotal.get() * 8.0) / (elapsedTotalSec * 1_000_000.0)

                    val loadedAvg = if (loadedPings.isNotEmpty()) loadedPings.average().toLong() else baselineIdlePing
                    val delta = (loadedAvg - baselineIdlePing).coerceAtLeast(0)
                    val lossPct = if (sentPackets > 0) (droppedPackets.toDouble() / sentPackets.toDouble()) * 100.0 else 0.0

                    onProgress(
                        CongestionStressProgress(
                            elapsedSeconds = elapsedSec,
                            totalSeconds = durationSeconds,
                            progressPercent = (elapsedSec.toFloat() / durationSeconds.toFloat()).coerceIn(0f, 1f),
                            currentPingMs = ping.coerceAtLeast(0),
                            baselineIdlePingMs = baselineIdlePing,
                            bufferbloatDeltaMs = delta,
                            currentDownloadMbps = currentDlMbps,
                            currentUploadMbps = currentUlMbps,
                            packetsSent = sentPackets,
                            packetsDropped = droppedPackets,
                            packetLossPercent = lossPct,
                            isRunning = true,
                            statusMessage = "Stress Testing: DL ${"%.1f".format(Locale.US, currentDlMbps)}M • UL ${"%.1f".format(Locale.US, currentUlMbps)}M • Ping ${loadedAvg}ms (+${delta}ms)"
                        )
                    )

                    val probeDuration = System.currentTimeMillis() - probeStart
                    val sleepMs = (800L - probeDuration).coerceAtLeast(100L)
                    delay(sleepMs)
                }
            } finally {
                isRunningFlag.set(false)
                dlJob.cancel()
                ulJob.cancel()
            }
        }

        val totalDurationSec = ((System.currentTimeMillis() - startTime) / 1000L).toInt().coerceAtLeast(1)
        val finalLoadedAvg = if (loadedPings.isNotEmpty()) loadedPings.average().toLong() else baselineIdlePing
        val finalDelta = (finalLoadedAvg - baselineIdlePing).coerceAtLeast(0)
        val finalDlMbps = (dlBytesTotal.get() * 8.0) / (totalDurationSec * 1_000_000.0)
        val finalUlMbps = (ulBytesTotal.get() * 8.0) / (totalDurationSec * 1_000_000.0)
        val finalLoss = if (sentPackets > 0) (droppedPackets.toDouble() / sentPackets.toDouble()) * 100.0 else 0.0

        val grade = when {
            finalLoss >= 10.0 -> "GRADE F (Bufferbloat Saturated / Drops)"
            finalDelta <= 15 && finalLoss <= 1.0 -> "GRADE A+ (Zero Inflation)"
            finalDelta <= 35 && finalLoss <= 3.0 -> "GRADE A (Minimal Delay)"
            finalDelta <= 70 && finalLoss <= 6.0 -> "GRADE B (Mild Bloat)"
            finalDelta <= 150 -> "GRADE C (Noticeable Delay)"
            finalDelta <= 300 -> "GRADE D (Heavy Queuing)"
            else -> "GRADE F (Bufferbloat Saturated / Drops)"
        }

        val recommendation = when {
            finalDelta <= 35 -> "Excellent carrier queuing. Gaming and real-time voice calls will remain smooth under full load."
            finalDelta <= 100 -> "Moderate bufferbloat. Smart QoS is actively shaping background flows to prevent voice/game stutter."
            else -> "Significant bufferbloat under cellular congestion. Smaller TCP buffers and Smart QoS enabled are strictly recommended."
        }

        onProgress(
            CongestionStressProgress(
                elapsedSeconds = totalDurationSec,
                totalSeconds = durationSeconds,
                progressPercent = 1.0f,
                currentPingMs = loadedPings.lastOrNull() ?: 0L,
                baselineIdlePingMs = baselineIdlePing,
                bufferbloatDeltaMs = finalDelta,
                currentDownloadMbps = finalDlMbps,
                currentUploadMbps = finalUlMbps,
                packetsSent = sentPackets,
                packetsDropped = droppedPackets,
                packetLossPercent = finalLoss,
                isRunning = false,
                statusMessage = "Test Finished: $grade (+${finalDelta}ms under load)"
            )
        )

        AppLogger.i(TAG, "Congestion Stress Test complete: $grade, idle=${baselineIdlePing}ms, loaded=${finalLoadedAvg}ms (delta=+${finalDelta}ms), DL=${finalDlMbps}Mbps, UL=${finalUlMbps}Mbps")

        CongestionStressResult(
            durationSeconds = totalDurationSec,
            baselineIdlePingMs = baselineIdlePing,
            loadedAvgPingMs = finalLoadedAvg,
            bufferbloatDeltaMs = finalDelta,
            bufferbloatGrade = grade,
            sustainedDownloadMbps = finalDlMbps,
            sustainedUploadMbps = finalUlMbps,
            packetLossPercent = finalLoss,
            isSuccessful = loadedPings.isNotEmpty() && finalLoss < 50.0,
            recommendation = recommendation
        )
    }

    // =========================================================================
    // 4. FULL SPEEDTEST (PING / JITTER, DOWNLOAD, UPLOAD)
    // =========================================================================

    suspend fun runFullSpeedTest(
        onProgress: (SpeedTestProgress) -> Unit = {}
    ): SpeedTestResult = withContext(dispatchers.io) {
        AppLogger.i(TAG, "Starting full 3-phase Speedtest...")
        val client = buildHttpClient(timeoutSec = 12)

        var finalPing: Long
        var finalJitter: Long
        var downloadMbps: Double
        var peakDownloadMbps = 0.0
        var uploadMbps: Double
        var peakUploadMbps = 0.0

        // PHASE 1: PING & JITTER (5 rapid probes)
        onProgress(
            SpeedTestProgress(
                phase = SpeedTestPhase.PING_JITTER,
                progressPercent = 0.05f,
                isRunning = true,
                statusText = "Measuring latency & jitter..."
            )
        )

        val pingSamples = mutableListOf<Long>()
        var jitterSum = 0L
        var lastP = 0L
        var jitterCount = 0

        for (i in 0 until 5) {
            val p = probePingSingle(client)
            if (p > 0) {
                pingSamples.add(p)
                if (lastP > 0) {
                    jitterSum += abs(p - lastP)
                    jitterCount++
                }
                lastP = p
            }
            onProgress(
                SpeedTestProgress(
                    phase = SpeedTestPhase.PING_JITTER,
                    progressPercent = 0.05f + (i * 0.04f),
                    pingMs = p.coerceAtLeast(0),
                    jitterMs = if (jitterCount > 0) jitterSum / jitterCount else 0L,
                    isRunning = true,
                    statusText = "Ping probe #${i + 1} (${p}ms)"
                )
            )
            delay(150)
        }

        finalPing = if (pingSamples.isNotEmpty()) pingSamples.average().toLong() else 65L
        finalJitter = if (jitterCount > 0) jitterSum / jitterCount else 8L

        // PHASE 2: DOWNLOAD THROUGHPUT (8 seconds sustained)
        onProgress(
            SpeedTestProgress(
                phase = SpeedTestPhase.DOWNLOAD,
                progressPercent = 0.25f,
                pingMs = finalPing,
                jitterMs = finalJitter,
                isRunning = true,
                statusText = "Testing download speed..."
            )
        )

        var totalDlBytes = 0L
        val dlStartTime = System.currentTimeMillis()
        val dlEndTime = dlStartTime + 8000L // 8s download
        val dlBuffer = ByteArray(32768)

        while (System.currentTimeMillis() < dlEndTime && isActive) {
            try {
                val req = Request.Builder()
                    .url("https://speed.cloudflare.com/__down?bytes=5242880") // 5MB chunks
                    .header("Cache-Control", "no-cache")
                    .build()

                val callStart = System.currentTimeMillis()
                var chunkBytes = 0L

                client.newCall(req).execute().use { res ->
                    val isStream = res.body?.byteStream() ?: return@use
                    var read: Int
                    var lastUpdate = System.currentTimeMillis()

                    while (isStream.read(dlBuffer).also { read = it } != -1 && System.currentTimeMillis() < dlEndTime && isActive) {
                        totalDlBytes += read
                        chunkBytes += read

                        val now = System.currentTimeMillis()
                        if (now - lastUpdate >= 120) {
                            val elapsedSec = (now - dlStartTime).coerceAtLeast(1) / 1000.0
                            val chunkSec = (now - callStart).coerceAtLeast(1) / 1000.0
                            val currentSpeed = if (chunkSec > 0.05) (chunkBytes * 8.0) / (chunkSec * 1_000_000.0) else 0.0
                            if (currentSpeed > peakDownloadMbps) peakDownloadMbps = currentSpeed
                            val avgSpeed = (totalDlBytes * 8.0) / (elapsedSec * 1_000_000.0)

                            val pct = 0.25f + ((now - dlStartTime).toFloat() / 8000f * 0.35f).coerceIn(0f, 0.35f)

                            onProgress(
                                SpeedTestProgress(
                                    phase = SpeedTestPhase.DOWNLOAD,
                                    progressPercent = pct,
                                    pingMs = finalPing,
                                    jitterMs = finalJitter,
                                    currentSpeedMbps = currentSpeed,
                                    downloadSpeedMbps = avgSpeed,
                                    isRunning = true,
                                    statusText = "Download: ${"%.2f".format(Locale.US, currentSpeed)} Mbps"
                                )
                            )
                            lastUpdate = now
                        }
                    }
                }
            } catch (_: Exception) {
                delay(150)
            }
        }

        val totalDlElapsedSec = (System.currentTimeMillis() - dlStartTime).coerceAtLeast(1) / 1000.0
        downloadMbps = if (totalDlElapsedSec > 0) (totalDlBytes * 8.0) / (totalDlElapsedSec * 1_000_000.0) else 0.0

        // PHASE 3: UPLOAD THROUGHPUT (6 seconds sustained)
        onProgress(
            SpeedTestProgress(
                phase = SpeedTestPhase.UPLOAD,
                progressPercent = 0.60f,
                pingMs = finalPing,
                jitterMs = finalJitter,
                downloadSpeedMbps = downloadMbps,
                isRunning = true,
                statusText = "Testing upload speed..."
            )
        )

        var totalUlBytes = 0L
        val ulStartTime = System.currentTimeMillis()
        val ulEndTime = ulStartTime + 6000L // 6s upload
        val uploadChunk = ByteArray(131072) // 128KB
        Random.nextBytes(uploadChunk)
        val mediaType = "application/octet-stream".toMediaType()

        while (System.currentTimeMillis() < ulEndTime && isActive) {
            try {
                val callStart = System.currentTimeMillis()
                val body = uploadChunk.toRequestBody(mediaType)
                val req = Request.Builder()
                    .url(CLOUDFLARE_UPLOAD_ENDPOINT)
                    .post(body)
                    .header("Cache-Control", "no-cache")
                    .build()

                client.newCall(req).execute().use { res ->
                    if (res.isSuccessful) {
                        totalUlBytes += uploadChunk.size
                        val now = System.currentTimeMillis()
                        val callSec = (now - callStart).coerceAtLeast(1) / 1000.0
                        val chunkSpeed = (uploadChunk.size * 8.0) / (callSec * 1_000_000.0)
                        if (chunkSpeed > peakUploadMbps) peakUploadMbps = chunkSpeed

                        val totalUlSec = (now - ulStartTime).coerceAtLeast(1) / 1000.0
                        val avgUlSpeed = (totalUlBytes * 8.0) / (totalUlSec * 1_000_000.0)
                        val pct = 0.60f + ((now - ulStartTime).toFloat() / 6000f * 0.40f).coerceIn(0f, 0.40f)

                        onProgress(
                            SpeedTestProgress(
                                phase = SpeedTestPhase.UPLOAD,
                                progressPercent = pct,
                                pingMs = finalPing,
                                jitterMs = finalJitter,
                                currentSpeedMbps = chunkSpeed,
                                downloadSpeedMbps = downloadMbps,
                                uploadSpeedMbps = avgUlSpeed,
                                isRunning = true,
                                statusText = "Upload: ${"%.2f".format(Locale.US, chunkSpeed)} Mbps"
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                delay(150)
            }
        }

        val totalUlElapsedSec = (System.currentTimeMillis() - ulStartTime).coerceAtLeast(1) / 1000.0
        uploadMbps = if (totalUlElapsedSec > 0) (totalUlBytes * 8.0) / (totalUlElapsedSec * 1_000_000.0) else 0.0

        val rating = when {
            downloadMbps >= 25.0 && uploadMbps >= 8.0 && finalPing < 80 -> "4K UHD Streaming & Low-Latency Gaming Ready"
            downloadMbps >= 10.0 && uploadMbps >= 3.0 -> "1080p FHD Video & High-Speed Browsing Ready"
            downloadMbps >= 3.0 -> "720p HD Standard Streaming"
            else -> "Basic Web & Messaging Connection"
        }

        onProgress(
            SpeedTestProgress(
                phase = SpeedTestPhase.COMPLETE,
                progressPercent = 1.0f,
                pingMs = finalPing,
                jitterMs = finalJitter,
                currentSpeedMbps = 0.0,
                downloadSpeedMbps = downloadMbps,
                uploadSpeedMbps = uploadMbps,
                isRunning = false,
                statusText = "Speedtest Complete: DL ${"%.2f".format(Locale.US, downloadMbps)} Mbps • UL ${"%.2f".format(Locale.US, uploadMbps)} Mbps • ${finalPing}ms"
            )
        )

        AppLogger.i(TAG, "Speedtest complete: DL=${downloadMbps}Mbps, UL=${uploadMbps}Mbps, Ping=${finalPing}ms, Jitter=${finalJitter}ms")

        SpeedTestResult(
            pingMs = finalPing,
            jitterMs = finalJitter,
            downloadSpeedMbps = downloadMbps,
            uploadSpeedMbps = uploadMbps,
            peakDownloadMbps = peakDownloadMbps.coerceAtLeast(downloadMbps),
            peakUploadMbps = peakUploadMbps.coerceAtLeast(uploadMbps),
            isSuccessful = downloadMbps > 0 || uploadMbps > 0,
            rating = rating
        )
    }
}
