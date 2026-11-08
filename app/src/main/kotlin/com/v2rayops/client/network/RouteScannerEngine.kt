package com.v2rayops.client.network

import com.v2rayops.client.core.dispatchers.AppDispatchers
import com.v2rayops.client.core.logger.AppLogger
import com.v2rayops.client.core.result.AppResult
import com.v2rayops.client.domain.model.VpnConfig
import com.v2rayops.client.domain.repository.SubscriptionRepository
import com.v2rayops.client.profiles.ProfileParser
import com.v2rayops.client.vpn.VpnManager
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLParameters
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

enum class RouteStrategy(val displayName: String, val shortTag: String) {
    STANDARD_SNI("Standard SNI Bug Host", "SNI"),
    ADDR_SNI("Addr SNI (Fronting)", "ADDR")
}

enum class SniCategory(val displayName: String) {
    ZOOM_TEAMS("Zoom & Teams"),
    TIKTOK("TikTok"),
    WHATSAPP_SOCIAL("WhatsApp & Social"),
    CLOUDFLARE_EDGE("Cloudflare Edge"),
    CUSTOM("Custom")
}

enum class SniStatus {
    WS_READY,       // Handshake passed & HTTP 101 WS Upgrade OK
    TLS_OK,         // Handshake passed, WS not tested or not 101
    BLOCKED_TIMEOUT,// Carrier DPI dropped TLS handshake (timed out)
    TCP_FAILED      // Port connection rejected
}

enum class ScanProfile(
    val displayName: String,
    val shortLabel: String,
    val rounds: Int,
    val sampleDelayMs: Long,
    val timeoutPerSampleMs: Int
) {
    DEEP_STABILITY("Deep Stability (Multi-Sample)", "DEEP STABILITY", rounds = 3, sampleDelayMs = 350L, timeoutPerSampleMs = 3000),
    ULTRA_ENDURANCE("Ultra Endurance (5-Round Audit)", "ULTRA 5-ROUND", rounds = 5, sampleDelayMs = 500L, timeoutPerSampleMs = 3500),
    QUICK_AUDIT("Quick Audit (1-Sample)", "QUICK AUDIT", rounds = 1, sampleDelayMs = 0L, timeoutPerSampleMs = 2500)
}

data class SniRouteCandidate(
    val domain: String,                       // Bug host domain (e.g. support.zoom.us)
    val strategy: RouteStrategy,              // STANDARD_SNI or ADDR_SNI
    val category: SniCategory,
    val targetHost: String,                   // Connect IP/host (for STANDARD_SNI: VPS IP; for ADDR_SNI: domain)
    val targetPort: Int = 443,
    val sni: String,                          // TLS SNI header (for STANDARD_SNI: domain; for ADDR_SNI: sg-v2rayops.vpnxhub.com)
    val wsHost: String = "sg-v2rayops.vpnxhub.com",
    val wsPath: String = "/v2rayops",
    val status: SniStatus = SniStatus.TCP_FAILED,
    val latencyMs: Long = 0L,
    val errorDetail: String = "",
    val samplesCount: Int = 0,                // Total probe attempts
    val successCount: Int = 0,                // Successful passes
    val packetLossPercent: Int = 0,           // Packet loss rate (0 - 100%)
    val jitterMs: Long = 0L,                  // Latency variance across rounds
    val minLatencyMs: Long = 0L,              // Minimum recorded latency
    val maxLatencyMs: Long = 0L,              // Maximum recorded latency
    val avgLatencyMs: Long = 0L,              // Average latency across successful samples
    val stabilityScore: Int = 0,              // Stability index (0 - 100%)
    val isSustainedPass: Boolean = false      // True if 100% pass without loss
) {
    val isRockSolid: Boolean get() = status == SniStatus.WS_READY && stabilityScore >= 85 && packetLossPercent == 0
}

/**
 * Route Scanner Engine:
 * Benchmarks carrier bug host SNIs and fronting Addr-SNI routes to discover the fastest,
 * zero-timeout tunneling path for the client's current mobile carrier (Dialog, Mobitel, etc.) or Wi-Fi.
 */
@Singleton
class RouteScannerEngine @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val profileParser: ProfileParser,
    private val vpnManager: VpnManager,
    private val dispatchers: AppDispatchers
) {
    companion object {
        private const val TAG = "RouteScanner"
        const val DEFAULT_VPS_IP = "172.236.134.215"
        const val DEFAULT_VPS_DOMAIN = "sg-v2rayops.vpnxhub.com"

        private val RAW_BUG_DOMAINS = listOf(
            // Zoom / Education / Work & Learn
            "support.zoom.us" to SniCategory.ZOOM_TEAMS,
            "news.zoom.us" to SniCategory.ZOOM_TEAMS,
            "zoom.us" to SniCategory.ZOOM_TEAMS,
            "microsoft.com" to SniCategory.ZOOM_TEAMS,
            "teams.microsoft.com" to SniCategory.ZOOM_TEAMS,
            "office.com" to SniCategory.ZOOM_TEAMS,

            // TikTok Packages
            "tiktok.com" to SniCategory.TIKTOK,
            "p16-va.tiktokcdn.com" to SniCategory.TIKTOK,
            "v16m.tiktokcdn.com" to SniCategory.TIKTOK,
            "api.tiktokv.com" to SniCategory.TIKTOK,

            // WhatsApp & Social
            "web.whatsapp.com" to SniCategory.WHATSAPP_SOCIAL,
            "media.whatsapp.net" to SniCategory.WHATSAPP_SOCIAL,

            // YouTube / Google
            "googlevideo.com" to SniCategory.CUSTOM,
            "youtube.com" to SniCategory.CUSTOM,

            // Cloudflare / Universal Edges
            "cloudflare.com" to SniCategory.CLOUDFLARE_EDGE,
            "one.one.one.one" to SniCategory.CLOUDFLARE_EDGE
        )

        val DEFAULT_CANDIDATES: List<SniRouteCandidate> = RAW_BUG_DOMAINS.flatMap { (domain, cat) ->
            listOf(
                // 1. Standard SNI Strategy: Connect to VPS, send domain in TLS SNI
                SniRouteCandidate(
                    domain = domain,
                    strategy = RouteStrategy.STANDARD_SNI,
                    category = cat,
                    targetHost = DEFAULT_VPS_IP,
                    targetPort = 443,
                    sni = domain,
                    wsHost = DEFAULT_VPS_DOMAIN,
                    wsPath = "/v2rayops"
                ),
                // 2. Addr SNI Strategy (Fronting): URI address is bug host domain, dials VPS cluster with zero-rated SNI
                SniRouteCandidate(
                    domain = domain,
                    strategy = RouteStrategy.ADDR_SNI,
                    category = cat,
                    targetHost = DEFAULT_VPS_IP,
                    targetPort = 443,
                    sni = domain,
                    wsHost = DEFAULT_VPS_DOMAIN,
                    wsPath = "/v2rayops"
                )
            )
        }
    }

    private data class SampleProbeResult(
        val success: Boolean,
        val isWsReady: Boolean,
        val latencyMs: Long,
        val error: String
    )

    private fun probeSingleSample(
        targetHost: String,
        port: Int,
        sni: String,
        wsHost: String,
        wsPath: String,
        timeoutMs: Int
    ): SampleProbeResult {
        val startTcp = System.currentTimeMillis()
        var rawSocket: Socket? = null
        var sslSocket: SSLSocket? = null
        try {
            val s = Socket()
            s.tcpNoDelay = true
            s.reuseAddress = true
            s.setSoLinger(true, 0)
            s.connect(InetSocketAddress(targetHost, port), timeoutMs)
            rawSocket = s

            val startTls = System.currentTimeMillis()
            val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAll, SecureRandom())

            val ssl = sslContext.socketFactory.createSocket(s, targetHost, port, true) as SSLSocket
            ssl.tcpNoDelay = true
            ssl.setSoLinger(true, 0)
            val sslParams = SSLParameters()
            sslParams.serverNames = listOf(SNIHostName(sni))
            ssl.sslParameters = sslParams
            ssl.soTimeout = timeoutMs

            ssl.startHandshake()
            sslSocket = ssl
            val tlsLatency = System.currentTimeMillis() - startTls

            var wsPassed = false
            var wsError = ""
            try {
                val writer = OutputStreamWriter(ssl.outputStream, "UTF-8")
                val reader = BufferedReader(InputStreamReader(ssl.inputStream, "UTF-8"))

                val nonce = ByteArray(16)
                SecureRandom().nextBytes(nonce)
                val wsKey = Base64.getEncoder().encodeToString(nonce)
                val request = "GET $wsPath HTTP/1.1\r\n" +
                        "Host: $wsHost\r\n" +
                        "Upgrade: websocket\r\n" +
                        "Connection: Upgrade\r\n" +
                        "Sec-WebSocket-Key: $wsKey\r\n" +
                        "Sec-WebSocket-Version: 13\r\n\r\n"

                writer.write(request)
                writer.flush()

                val statusLine = reader.readLine()
                if (statusLine != null && statusLine.contains("101")) {
                    wsPassed = true
                } else {
                    wsError = statusLine ?: "No HTTP status response"
                }
            } catch (e: Exception) {
                wsError = e.message ?: "WS upgrade failed"
            }

            val totalLatency = (System.currentTimeMillis() - startTcp).coerceAtLeast(tlsLatency)
            return if (wsPassed) {
                SampleProbeResult(true, true, totalLatency, "")
            } else if (wsError.isNotBlank() && (wsError.contains("400") || wsError.contains("HTTP"))) {
                SampleProbeResult(true, false, totalLatency, wsError)
            } else {
                SampleProbeResult(false, false, totalLatency, wsError.ifBlank { "Upgrade failed" })
            }
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTcp
            return SampleProbeResult(false, false, elapsed, e.message ?: "Connection failed")
        } finally {
            try { sslSocket?.close() } catch (_: Exception) {}
            try { rawSocket?.close() } catch (_: Exception) {}
        }
    }

    suspend fun scanCandidate(
        candidate: SniRouteCandidate,
        profile: ScanProfile = ScanProfile.DEEP_STABILITY
    ): SniRouteCandidate = withContext(dispatchers.io) {
        val targetHost = candidate.targetHost
        val port = candidate.targetPort
        val sni = candidate.sni
        val wsHost = candidate.wsHost
        val wsPath = candidate.wsPath

        val latencies = mutableListOf<Long>()
        var wsSuccessCount = 0
        var lastError = ""

        val totalRounds = profile.rounds

        for (round in 1..totalRounds) {
            if (round > 1 && profile.sampleDelayMs > 0) {
                kotlinx.coroutines.delay(profile.sampleDelayMs)
            }

            val result = probeSingleSample(targetHost, port, sni, wsHost, wsPath, profile.timeoutPerSampleMs)
            if (result.success) {
                latencies.add(result.latencyMs)
                if (result.isWsReady) {
                    wsSuccessCount++
                }
            } else {
                lastError = result.error
            }
        }

        val successCount = latencies.size
        val packetLossPercent = (((totalRounds - successCount).toFloat() / totalRounds.toFloat()) * 100).toInt()

        val avgLatency = if (latencies.isNotEmpty()) latencies.average().toLong() else 0L
        val minLatency = latencies.minOrNull() ?: 0L
        val maxLatency = latencies.maxOrNull() ?: 0L
        val jitter = if (latencies.size > 1) (maxLatency - minLatency) else 0L

        val status = when {
            wsSuccessCount > 0 -> SniStatus.WS_READY
            successCount > 0 -> SniStatus.TLS_OK
            lastError.contains("Connect timeout", ignoreCase = true) || lastError.contains("refused", ignoreCase = true) -> SniStatus.TCP_FAILED
            else -> SniStatus.BLOCKED_TIMEOUT
        }

        // Calculate Reliability & Stability Score (0 - 100%)
        // 60% based on success rate, 25% on low jitter consistency, 15% on latency tier
        val successRatio = successCount.toFloat() / totalRounds.toFloat()
        val basePoints = (successRatio * 60.0).toInt()

        val jitterPoints = when {
            jitter <= 15L -> 25
            jitter <= 40L -> 20
            jitter <= 80L -> 14
            jitter <= 150L -> 8
            else -> 3
        }

        val latencyPoints = when {
            avgLatency in 1L..150L -> 15
            avgLatency in 151L..250L -> 12
            avgLatency in 251L..400L -> 8
            avgLatency > 400L -> 4
            else -> 0
        }

        val stabilityScore = if (successCount == 0) 0 else (basePoints + jitterPoints + latencyPoints).coerceIn(0, 100)
        val isSustainedPass = successCount == totalRounds && wsSuccessCount == totalRounds && jitter <= 60L

        candidate.copy(
            status = status,
            latencyMs = avgLatency,
            avgLatencyMs = avgLatency,
            minLatencyMs = minLatency,
            maxLatencyMs = maxLatency,
            jitterMs = jitter,
            samplesCount = totalRounds,
            successCount = successCount,
            packetLossPercent = packetLossPercent,
            stabilityScore = stabilityScore,
            isSustainedPass = isSustainedPass,
            errorDetail = when {
                wsSuccessCount == totalRounds -> "Rock-Solid: $totalRounds/$totalRounds Pass (0% Loss, ±${jitter}ms Jitter)"
                wsSuccessCount > 0 -> "Intermittent WS: $wsSuccessCount/$totalRounds Pass ($packetLossPercent% Loss)"
                successCount > 0 -> "TLS Only ($lastError)"
                else -> "Blocked by carrier: ${lastError.ifBlank { "Handshake timed out" }}"
            }
        )
    }

    suspend fun scanAll(
        candidates: List<SniRouteCandidate> = DEFAULT_CANDIDATES,
        profile: ScanProfile = ScanProfile.QUICK_AUDIT,
        onProgress: (scanned: Int, total: Int, latest: SniRouteCandidate) -> Unit = { _, _, _ -> }
    ): List<SniRouteCandidate> = coroutineScope {
        val semaphore = Semaphore(2) // Controlled concurrency to prevent mobile carrier burst throttling
        val total = candidates.size

        // Deduplicate probes by domain to prevent socket flooding & carrier DPI throttling
        val uniqueDomains = candidates.map { it.domain }.distinct()
        val domainResults = ConcurrentHashMap<String, SniRouteCandidate>()
        var scannedCount = 0

        val deferredList = uniqueDomains.map { domain ->
            val sampleCandidate = candidates.first { it.domain == domain }
            async(dispatchers.io) {
                semaphore.withPermit {
                    val result = scanCandidate(sampleCandidate, profile)
                    domainResults[domain] = result
                    synchronized(this@RouteScannerEngine) {
                        val matching = candidates.filter { it.domain == domain }
                        scannedCount += matching.size
                        matching.forEach { match ->
                            onProgress(scannedCount, total, match.copy(
                                status = result.status,
                                latencyMs = result.latencyMs,
                                avgLatencyMs = result.avgLatencyMs,
                                minLatencyMs = result.minLatencyMs,
                                maxLatencyMs = result.maxLatencyMs,
                                jitterMs = result.jitterMs,
                                samplesCount = result.samplesCount,
                                successCount = result.successCount,
                                packetLossPercent = result.packetLossPercent,
                                stabilityScore = result.stabilityScore,
                                isSustainedPass = result.isSustainedPass,
                                errorDetail = result.errorDetail
                            ))
                        }
                    }
                    result
                }
            }
        }

        deferredList.awaitAll()

        candidates.map { candidate ->
            val probe = domainResults[candidate.domain]
            if (probe != null) {
                candidate.copy(
                    status = probe.status,
                    latencyMs = probe.latencyMs,
                    avgLatencyMs = probe.avgLatencyMs,
                    minLatencyMs = probe.minLatencyMs,
                    maxLatencyMs = probe.maxLatencyMs,
                    jitterMs = probe.jitterMs,
                    samplesCount = probe.samplesCount,
                    successCount = probe.successCount,
                    packetLossPercent = probe.packetLossPercent,
                    stabilityScore = probe.stabilityScore,
                    isSustainedPass = probe.isSustainedPass,
                    errorDetail = probe.errorDetail
                )
            } else {
                candidate
            }
        }.sortedWith(
            compareBy<SniRouteCandidate> {
                when {
                    it.status == SniStatus.WS_READY && it.stabilityScore >= 80 -> 0 // 1. Top Verified Rock-Solid WS Routes
                    it.status == SniStatus.WS_READY -> 1                           // 2. WS Ready with minor jitter
                    it.status == SniStatus.TLS_OK && it.stabilityScore >= 80 -> 2  // 3. Stable TLS
                    it.status == SniStatus.TLS_OK -> 3                             // 4. TLS Pass
                    it.status == SniStatus.BLOCKED_TIMEOUT -> 4                    // 5. Blocked
                    else -> 5                                                      // 6. Port closed
                }
            }.thenByDescending { it.stabilityScore }
             .thenBy { it.packetLossPercent }
             .thenBy { it.jitterMs }
             .thenBy { it.avgLatencyMs.takeIf { lat -> lat > 0 } ?: Long.MAX_VALUE }
        )
    }

    /**
     * Applies the selected candidate to the active V2Ray configuration.
     * Rewrites destination address, SNI, and WS Host parameters and cleanly reloads the tunnel.
     */
    suspend fun applySniRoute(candidate: SniRouteCandidate): AppResult<VpnConfig> = withContext(dispatchers.io) {
        val current = subscriptionRepository.observeSelectedConfig().first()
            ?: return@withContext AppResult.Failure(com.v2rayops.client.core.error.AppError.Validation("No active config selected to tune"))

        var updatedUri = current.configUri

        if (current.isVmess) {
            try {
                val base64Content = updatedUri.removePrefix("vmess://").trim()
                val jsonStr = String(Base64.getUrlDecoder().decode(base64Content), Charsets.UTF_8)
                val json = JSONObject(jsonStr)

                if (candidate.strategy == RouteStrategy.ADDR_SNI) {
                    json.put("add", candidate.domain)
                    json.put("port", candidate.targetPort)
                    json.put("sni", candidate.wsHost)
                    json.put("host", candidate.wsHost)
                    json.put("insecure", true)
                } else {
                    json.put("add", DEFAULT_VPS_DOMAIN)
                    json.put("port", 443)
                    json.put("sni", candidate.domain)
                    json.put("host", candidate.wsHost)
                    json.put("insecure", true)
                }
                val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(json.toString().toByteArray(Charsets.UTF_8))
                updatedUri = "vmess://$encoded"
            } catch (e: Exception) {
                AppLogger.w(TAG, "Failed to update VMess URI: ${e.message}")
            }
        } else {
            if (candidate.strategy == RouteStrategy.ADDR_SNI) {
                // Strategy 2 (Addr SNI Fronting):
                // Destination address becomes candidate.domain (e.g. support.zoom.us:443)
                // SNI becomes VPS domain (sg-v2rayops.vpnxhub.com)
                // Host becomes VPS domain (sg-v2rayops.vpnxhub.com)
                // allowInsecure is enabled for zero-rated certificate fronting
                updatedUri = replaceAddressAndPort(updatedUri, candidate.domain, candidate.targetPort)
                updatedUri = replaceQueryParam(updatedUri, "sni", candidate.wsHost)
                updatedUri = replaceQueryParam(updatedUri, "host", candidate.wsHost)
                updatedUri = replaceQueryParam(updatedUri, "allowInsecure", "true")
            } else {
                // Strategy 1 (Standard SNI):
                // Destination address is VPS IP / domain
                // SNI becomes candidate.domain
                // Host remains VPS domain
                updatedUri = replaceAddressAndPort(updatedUri, DEFAULT_VPS_DOMAIN, 443)
                updatedUri = replaceQueryParam(updatedUri, "sni", candidate.domain)
                updatedUri = replaceQueryParam(updatedUri, "host", candidate.wsHost)
                updatedUri = replaceQueryParam(updatedUri, "allowInsecure", "true")
            }
        }

        val parsed = profileParser.parseUri(updatedUri)
            ?: return@withContext AppResult.Failure(com.v2rayops.client.core.error.AppError.Validation("Failed to re-parse updated config URI"))

        val strategyTag = candidate.strategy.shortTag
        val cleanName = current.displayName.substringBefore(" [")
        val updatedConfig = current.copy(
            configUri = updatedUri,
            host = parsed.host,
            port = parsed.port,
            sni = parsed.sni,
            wsHost = parsed.wsHost,
            wsPath = parsed.wsPath,
            remark = "$cleanName [$strategyTag: ${candidate.domain}]"
        )

        AppLogger.i(TAG, "Applied winning route [${candidate.strategy.name}] '${candidate.domain}' to config #${updatedConfig.id} (Addr: ${updatedConfig.effectiveAddr}, SNI: ${updatedConfig.effectiveSni})")
        subscriptionRepository.selectConfig(updatedConfig)

        // If currently running, cleanly reconnect with the newly tuned route
        if (vpnManager.connectionState.value.isConnected) {
            AppLogger.i(TAG, "Reconnecting tunnel with newly tuned route (${candidate.strategy.shortTag}: ${candidate.domain})")
            vpnManager.connect(updatedConfig)
        }

        AppResult.Success(updatedConfig)
    }

    private fun replaceAddressAndPort(rawUri: String, newHost: String, newPort: Int): String {
        val regex = Regex("""@([^:/?#]+)(:[0-9]+)?""")
        return if (regex.containsMatchIn(rawUri)) {
            rawUri.replace(regex, "@$newHost:$newPort")
        } else {
            rawUri
        }
    }

    private fun replaceQueryParam(rawUri: String, key: String, newValue: String): String {
        val regex = Regex("""([?&])$key=[^&#]*""")
        return if (regex.containsMatchIn(rawUri)) {
            rawUri.replace(regex, "$1$key=$newValue")
        } else {
            val hashIdx = rawUri.indexOf('#')
            if (hashIdx >= 0) {
                val base = rawUri.substring(0, hashIdx)
                val fragment = rawUri.substring(hashIdx)
                val sep = if (base.contains("?")) "&" else "?"
                "$base$sep$key=$newValue$fragment"
            } else {
                val sep = if (rawUri.contains("?")) "&" else "?"
                "$rawUri$sep$key=$newValue"
            }
        }
    }
}
