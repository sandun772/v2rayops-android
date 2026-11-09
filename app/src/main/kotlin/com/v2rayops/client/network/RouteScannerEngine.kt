package com.v2rayops.client.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.v2rayops.client.core.dispatchers.AppDispatchers
import com.v2rayops.client.core.logger.AppLogger
import com.v2rayops.client.core.result.AppResult
import com.v2rayops.client.domain.model.VpnConfig
import com.v2rayops.client.domain.repository.SubscriptionRepository
import com.v2rayops.client.profiles.ProfileParser
import com.v2rayops.client.vpn.VpnManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
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
    NETFLIX("Netflix Video"),
    TIKTOK("TikTok"),
    WHATSAPP_SOCIAL("WhatsApp & Social"),
    TELEGRAM("Telegram Messenger"),
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

object CommonProxyPorts {
    val POPULAR_TLS_PORTS = listOf(443, 8443, 2053, 2083, 2087, 2096)
    val POPULAR_HTTP_PORTS = listOf(80, 8080, 8880, 2052, 2082, 2086, 2095)
    val QUICK_MULTI_PORTS = listOf(443, 8443, 2053, 2083, 80, 8080)
    val ALL_PORTS = listOf(443, 8443, 2053, 2083, 2087, 2096, 80, 8080, 8880, 2052, 2082, 2086, 2095)

    fun isTlsPort(port: Int): Boolean = port in POPULAR_TLS_PORTS
}

enum class PortProtocol(val label: String) {
    TLS("TLS / HTTPS"),
    HTTP("HTTP / Cleartext")
}

data class PortScanResult(
    val port: Int,
    val protocol: PortProtocol,
    val isOpen: Boolean,
    val isWsReady: Boolean = false,
    val latencyMs: Long = 0L,
    val error: String = ""
) {
    val portName: String get() = when (port) {
        443 -> "443 (Standard HTTPS)"
        8443 -> "8443 (Cloudflare SSL Alt)"
        2053 -> "2053 (Cloudflare SSL)"
        2083 -> "2083 (cPanel / Cloudflare SSL)"
        2087 -> "2087 (WHM / Cloudflare SSL)"
        2096 -> "2096 (Webmail / Cloudflare SSL)"
        80 -> "80 (Standard HTTP)"
        8080 -> "8080 (HTTP Alt Proxy)"
        8880 -> "8880 (Cloudflare HTTP)"
        2052 -> "2052 (Cloudflare HTTP)"
        2082 -> "2082 (cPanel HTTP)"
        2086 -> "2086 (WHM HTTP)"
        2095 -> "2095 (Webmail HTTP)"
        else -> "$port (${protocol.name})"
    }
}

data class ThreeXUiInboundSpec(
    val remark: String,
    val protocol: String = "vless",
    val port: Int,
    val isTls: Boolean,
    val security: String,
    val listen: String = "0.0.0.0",
    val network: String = "ws",
    val wsPath: String = "/v2rayops",
    val wsHost: String = "sg-v2rayops.vpnxhub.com",
    val sni: String = "sg-v2rayops.vpnxhub.com",
    val clientUuid: String = "",
    val panelUrl: String = "https://v2rayops.vpnxhub.com:2083/",
    val fullJsonSnippet: String = "",
    val instructions: List<String> = emptyList()
) {
    val summaryText: String
        get() = """
            === 3X-UI INBOUND SPECIFICATION ===
            Remark: $remark
            Protocol: $protocol
            Port: $port
            Listening IP: $listen
            Network (Transmission): $network
            WS Path: $wsPath
            WS Host Header: $wsHost
            Security: $security
            ${if (isTls) "TLS SNI: $sni" else "TLS SNI: (None - Plaintext WS)"}
            Client UUID: ${clientUuid.ifBlank { "Auto/New" }}
            3X-UI Panel URL: $panelUrl
        """.trimIndent()
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
    val isSustainedPass: Boolean = false,     // True if 100% pass without loss
    val verifiedPorts: List<Int> = listOf(443),
    val portAuditResults: List<PortScanResult> = emptyList()
) {
    val isRockSolid: Boolean get() = status == SniStatus.WS_READY && stabilityScore >= 85 && packetLossPercent == 0
}

/**
 * Route Scanner Engine:
 * Benchmarks carrier bug host SNIs and fronting Addr-SNI routes to discover the fastest,
 * zero-timeout tunneling path for the client's current mobile carrier (Dialog, Mobitel, etc.) or Wi-Fi.
 * Supports Pure Direct Carrier Mode (binding raw sockets to physical cellular/Wi-Fi interface, bypassing VPN).
 */
@Singleton
class RouteScannerEngine @Inject constructor(
    @ApplicationContext private val context: Context,
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
            "aka.ms" to SniCategory.ZOOM_TEAMS,

            // Netflix Packages
            "fast.com" to SniCategory.NETFLIX,
            "netflix.com" to SniCategory.NETFLIX,
            "api-global.netflix.com" to SniCategory.NETFLIX,
            "assets.nflxext.com" to SniCategory.NETFLIX,
            "nflxvideo.net" to SniCategory.NETFLIX,

            // TikTok Packages
            "tiktok.com" to SniCategory.TIKTOK,
            "p16-va.tiktokcdn.com" to SniCategory.TIKTOK,
            "v16m.tiktokcdn.com" to SniCategory.TIKTOK,
            "api.tiktokv.com" to SniCategory.TIKTOK,
            "v16-webapp-prime.tiktok.com" to SniCategory.TIKTOK,
            "byteoversea.com" to SniCategory.TIKTOK,

            // WhatsApp & Social
            "web.whatsapp.com" to SniCategory.WHATSAPP_SOCIAL,
            "media.whatsapp.net" to SniCategory.WHATSAPP_SOCIAL,
            "g.whatsapp.net" to SniCategory.WHATSAPP_SOCIAL,
            "v.whatsapp.net" to SniCategory.WHATSAPP_SOCIAL,
            "facebook.com" to SniCategory.WHATSAPP_SOCIAL,
            "instagram.com" to SniCategory.WHATSAPP_SOCIAL,

            // Telegram Messenger
            "t.me" to SniCategory.TELEGRAM,
            "telegram.org" to SniCategory.TELEGRAM,
            "web.telegram.org" to SniCategory.TELEGRAM,
            "cdn1.telesco.pe" to SniCategory.TELEGRAM,

            // YouTube / Google
            "googlevideo.com" to SniCategory.CUSTOM,
            "youtube.com" to SniCategory.CUSTOM,

            // Cloudflare / Universal Edges
            "cloudflare.com" to SniCategory.CLOUDFLARE_EDGE,
            "one.one.one.one" to SniCategory.CLOUDFLARE_EDGE
        )

        val DEFAULT_CANDIDATES: List<SniRouteCandidate> = RAW_BUG_DOMAINS.flatMap { (domain, cat) ->
            listOf(
                // 1. Addr SNI Strategy (Fronting):
                // Destination address connects to bug host (e.g. support.zoom.us), TLS SNI & Host are VPS domain
                SniRouteCandidate(
                    domain = domain,
                    strategy = RouteStrategy.ADDR_SNI,
                    category = cat,
                    targetHost = domain,
                    targetPort = 443,
                    sni = DEFAULT_VPS_DOMAIN,
                    wsHost = DEFAULT_VPS_DOMAIN,
                    wsPath = "/v2rayops"
                ),
                // 2. Standard SNI Strategy:
                // Destination connects to VPS IP, TLS SNI is bug host domain
                SniRouteCandidate(
                    domain = domain,
                    strategy = RouteStrategy.STANDARD_SNI,
                    category = cat,
                    targetHost = DEFAULT_VPS_IP,
                    targetPort = 443,
                    sni = domain,
                    wsHost = DEFAULT_VPS_DOMAIN,
                    wsPath = "/v2rayops"
                )
            )
        }

        /**
         * Word-Based SNI Generator:
         * Expands user keywords (e.g. "zoom", "dialog", "tiktok", "teams", "slt")
         * into domain candidates and creates both ADDR_SNI and STANDARD_SNI candidates.
         */
        fun generateCandidatesFromWords(words: List<String>): List<SniRouteCandidate> {
            val domainTemplates = listOf(
                { w: String -> "support.$w.us" },
                { w: String -> "news.$w.us" },
                { w: String -> "$w.us" },
                { w: String -> "$w.com" },
                { w: String -> "api.$w.com" },
                { w: String -> "static.$w.com" },
                { w: String -> "m.$w.com" },
                { w: String -> "portal.$w.lk" },
                { w: String -> "learn.$w.lk" },
                { w: String -> "$w.lk" },
                { w: String -> "${w}cdn.com" },
                { w: String -> "p16-va.${w}cdn.com" },
                { w: String -> "v16m.${w}cdn.com" },
                { w: String -> "cdn.$w.net" },
                { w: String -> "web.$w.com" },
                { w: String -> "media.$w.net" }
            )

            val cleanWords = words
                .flatMap { it.split(",", " ", ";", "\n", "\t") }
                .map { it.trim().lowercase().removePrefix("https://").removePrefix("http://").removePrefix("www.") }
                .filter { it.isNotBlank() }
                .distinct()

            val generatedDomains = mutableSetOf<String>()
            for (w in cleanWords) {
                if (w.contains(".")) {
                    generatedDomains.add(w)
                } else {
                    domainTemplates.forEach { t -> generatedDomains.add(t(w)) }
                    if (w == "netflix" || w == "nflx") {
                        generatedDomains.addAll(listOf("fast.com", "netflix.com", "api-global.netflix.com", "assets.nflxext.com", "nflxvideo.net"))
                    } else if (w == "telegram" || w == "tg") {
                        generatedDomains.addAll(listOf("t.me", "telegram.org", "web.telegram.org", "cdn1.telesco.pe"))
                    }
                }
            }

            return generatedDomains.flatMap { d ->
                val cat = when {
                    d.contains("netflix") || d.contains("fast.com") || d.contains("nflx") -> SniCategory.NETFLIX
                    d.contains("telegram") || d.contains("t.me") || d.contains("telesco.pe") -> SniCategory.TELEGRAM
                    d.contains("zoom") || d.contains("teams") || d.contains("learn") || d.contains("office") -> SniCategory.ZOOM_TEAMS
                    d.contains("tiktok") || d.contains("byte") -> SniCategory.TIKTOK
                    d.contains("whatsapp") || d.contains("face") || d.contains("insta") -> SniCategory.WHATSAPP_SOCIAL
                    d.contains("cloudflare") || d.contains("one") -> SniCategory.CLOUDFLARE_EDGE
                    else -> SniCategory.CUSTOM
                }
                listOf(
                    SniRouteCandidate(
                        domain = d,
                        strategy = RouteStrategy.ADDR_SNI,
                        category = cat,
                        targetHost = d,
                        targetPort = 443,
                        sni = DEFAULT_VPS_DOMAIN,
                        wsHost = DEFAULT_VPS_DOMAIN,
                        wsPath = "/v2rayops"
                    ),
                    SniRouteCandidate(
                        domain = d,
                        strategy = RouteStrategy.STANDARD_SNI,
                        category = cat,
                        targetHost = DEFAULT_VPS_IP,
                        targetPort = 443,
                        sni = d,
                        wsHost = DEFAULT_VPS_DOMAIN,
                        wsPath = "/v2rayops"
                    )
                )
            }
        }

        fun findBestSpeedCandidate(candidates: List<SniRouteCandidate>): SniRouteCandidate? {
            val passing = candidates.filter { it.status == SniStatus.WS_READY || it.status == SniStatus.TLS_OK }
            if (passing.isEmpty()) return candidates.firstOrNull()

            // Prioritize WS_READY passing candidates with lowest average latency (whether ADDR_SNI edge or STANDARD_SNI)
            val wsPassing = passing.filter { it.status == SniStatus.WS_READY }
            if (wsPassing.isNotEmpty()) {
                return wsPassing.minByOrNull { it.avgLatencyMs.takeIf { lat -> lat > 0 } ?: 9999L }
            }
            return passing.minByOrNull { it.avgLatencyMs.takeIf { lat -> lat > 0 } ?: 9999L }
        }

        fun findBestStabilityCandidate(candidates: List<SniRouteCandidate>): SniRouteCandidate? {
            val passing = candidates.filter { it.status == SniStatus.WS_READY || it.status == SniStatus.TLS_OK }
            if (passing.isEmpty()) return candidates.firstOrNull()

            // Prioritize candidates with 100% pass rate (0% packet loss), lowest jitter, and highest stability score
            return passing.sortedWith(
                compareByDescending<SniRouteCandidate> { it.status == SniStatus.WS_READY }
                    .thenByDescending { it.stabilityScore }
                    .thenBy { it.packetLossPercent }
                    .thenBy { it.jitterMs }
                    .thenBy { it.avgLatencyMs.takeIf { lat -> lat > 0 } ?: Long.MAX_VALUE }
            ).firstOrNull()
        }

        fun findBestBalancedCandidate(candidates: List<SniRouteCandidate>): SniRouteCandidate? {
            val passing = candidates.filter { it.status == SniStatus.WS_READY || it.status == SniStatus.TLS_OK }
            if (passing.isEmpty()) return candidates.firstOrNull()
            fun score(c: SniRouteCandidate): Double {
                val latClamped = c.avgLatencyMs.coerceIn(10L, 500L)
                val latScore = (500L - latClamped).toDouble() / 500.0 * 40.0
                val stabScore = c.stabilityScore.toDouble() * 0.6
                val wsBonus = if (c.status == SniStatus.WS_READY) 15.0 else 0.0
                return stabScore + latScore + wsBonus
            }
            return passing.maxByOrNull { score(it) }
        }
    }

    /**
     * Finds the underlying physical network (Cellular radio or Wi-Fi),
     * ignoring any active Android VPN interface so probes test the pure carrier connection.
     */
    fun getUnderlyingPhysicalNetwork(): Network? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return null
        return cm.allNetworks.firstOrNull { net ->
            val caps = cm.getNetworkCapabilities(net) ?: return@firstOrNull false
            (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) &&
                    !caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        }
    }

    fun getPhysicalNetworkType(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return "UNKNOWN"
        val net = getUnderlyingPhysicalNetwork() ?: return "NO PHYSICAL NETWORK"
        val caps = cm.getNetworkCapabilities(net) ?: return "CONNECTED"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR (RADIO DIRECT)"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI (DIRECT)"
            else -> "PHYSICAL LINK"
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
            val physicalNet = getUnderlyingPhysicalNetwork()

            // Resolve targetHost to IPv4 using physical network resolver if possible
            val resolvedIps = try {
                if (physicalNet != null) {
                    physicalNet.getAllByName(targetHost).toList()
                } else {
                    java.net.InetAddress.getAllByName(targetHost).toList()
                }
            } catch (e: Exception) {
                emptyList()
            }
            val targetIp = resolvedIps.firstOrNull { it is java.net.Inet4Address }?.hostAddress
                ?: resolvedIps.firstOrNull()?.hostAddress
                ?: targetHost

            val s = Socket()
            s.tcpNoDelay = true
            s.reuseAddress = true
            s.setSoLinger(true, 0)

            // CRITICAL: Bind socket directly to physical cellular / Wi-Fi network interface
            // This ensures pure scanning that completely bypasses any active VPN tunnel
            try {
                physicalNet?.bindSocket(s)
            } catch (e: Exception) {
                AppLogger.d(TAG, "Binding socket to physical network skipped: ${e.message}")
            }

            s.connect(InetSocketAddress(targetIp, port), timeoutMs)
            rawSocket = s
            val tcpLatency = (System.currentTimeMillis() - startTcp).coerceAtLeast(1L)

            val isTls = CommonProxyPorts.isTlsPort(port)
            val activeSocket: Socket = if (isTls) {
                val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                })
                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, trustAll, SecureRandom())

                val ssl = sslContext.socketFactory.createSocket(s, targetIp, port, true) as SSLSocket
                ssl.tcpNoDelay = true
                ssl.setSoLinger(true, 0)
                val sslParams = SSLParameters()
                sslParams.serverNames = listOf(SNIHostName(sni))
                ssl.sslParameters = sslParams
                ssl.soTimeout = timeoutMs

                ssl.startHandshake()
                sslSocket = ssl
                ssl
            } else {
                s.soTimeout = timeoutMs
                s
            }

            var wsPassed = false
            var wsError = ""
            try {
                val writer = OutputStreamWriter(activeSocket.outputStream, "UTF-8")
                val reader = BufferedReader(InputStreamReader(activeSocket.inputStream, "UTF-8"))

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

            val totalLatency = (System.currentTimeMillis() - startTcp).coerceAtLeast(tcpLatency)
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

        // Multi-port check: Discover open ports on candidate (e.g. 443, 8443, 2053, 2083, 80, 8080)
        val verifiedPorts = mutableListOf<Int>()
        val portAuditList = mutableListOf<PortScanResult>()

        val mainIsOpen = successCount > 0
        val mainIsTls = CommonProxyPorts.isTlsPort(port)
        if (mainIsOpen) {
            verifiedPorts.add(port)
        }
        portAuditList.add(
            PortScanResult(
                port = port,
                protocol = if (mainIsTls) PortProtocol.TLS else PortProtocol.HTTP,
                isOpen = mainIsOpen,
                isWsReady = wsSuccessCount > 0,
                latencyMs = avgLatency,
                error = if (mainIsOpen) "" else lastError
            )
        )

        // If candidate host is responsive, probe other ports in QUICK_MULTI_PORTS
        val otherPorts = CommonProxyPorts.QUICK_MULTI_PORTS.filter { it != port }
        if (mainIsOpen) {
            for (p in otherPorts) {
                val pTls = CommonProxyPorts.isTlsPort(p)
                val pResult = probeSingleSample(targetHost, p, sni, wsHost, wsPath, timeoutMs = 1200)
                val pIsOpen = pResult.success || pResult.isWsReady
                if (pIsOpen) {
                    verifiedPorts.add(p)
                }
                portAuditList.add(
                    PortScanResult(
                        port = p,
                        protocol = if (pTls) PortProtocol.TLS else PortProtocol.HTTP,
                        isOpen = pIsOpen,
                        isWsReady = pResult.isWsReady,
                        latencyMs = if (pIsOpen) pResult.latencyMs else 0L,
                        error = if (pIsOpen) "" else pResult.error
                    )
                )
            }
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
            verifiedPorts = if (verifiedPorts.isNotEmpty()) verifiedPorts.distinct().sorted() else listOf(port),
            portAuditResults = portAuditList.sortedBy { it.port },
            errorDetail = when {
                wsSuccessCount == totalRounds -> "Rock-Solid: $totalRounds/$totalRounds Pass (0% Loss, ±${jitter}ms Jitter)"
                wsSuccessCount > 0 -> "Intermittent WS: $wsSuccessCount/$totalRounds Pass ($packetLossPercent% Loss)"
                successCount > 0 -> "TLS Only ($lastError)"
                else -> "Blocked by carrier: ${lastError.ifBlank { "Handshake timed out" }}"
            }
        )
    }

    /**
     * Deep port audit: Probes all 13 Cloudflare / proxy ports on the candidate host.
     */
    suspend fun auditAllPorts(candidate: SniRouteCandidate): List<PortScanResult> = withContext(dispatchers.io) {
        val targetHost = candidate.targetHost
        val sni = candidate.sni
        val wsHost = candidate.wsHost
        val wsPath = candidate.wsPath

        CommonProxyPorts.ALL_PORTS.map { p ->
            val pTls = CommonProxyPorts.isTlsPort(p)
            val res = probeSingleSample(targetHost, p, sni, wsHost, wsPath, timeoutMs = 1500)
            val isOpen = res.success || res.isWsReady
            PortScanResult(
                port = p,
                protocol = if (pTls) PortProtocol.TLS else PortProtocol.HTTP,
                isOpen = isOpen,
                isWsReady = res.isWsReady,
                latencyMs = if (isOpen) res.latencyMs else 0L,
                error = if (isOpen) "" else res.error
            )
        }
    }

    suspend fun scanAll(
        candidates: List<SniRouteCandidate> = DEFAULT_CANDIDATES,
        profile: ScanProfile = ScanProfile.QUICK_AUDIT,
        onProgress: (scanned: Int, total: Int, latest: SniRouteCandidate) -> Unit = { _, _, _ -> }
    ): List<SniRouteCandidate> = coroutineScope {
        val semaphore = Semaphore(2) // Controlled concurrency to prevent mobile carrier burst throttling
        val total = candidates.size

        // Deduplicate probes by candidate key (strategy + domain) to probe BOTH Standard SNI and Addr Fronting
        val candidateKeys = candidates.map { "${it.strategy.name}:${it.domain}" }.distinct()
        val candidateResults = ConcurrentHashMap<String, SniRouteCandidate>()
        var scannedCount = 0

        val deferredList = candidateKeys.map { key ->
            val sampleCandidate = candidates.first { "${it.strategy.name}:${it.domain}" == key }
            async(dispatchers.io) {
                semaphore.withPermit {
                    val result = scanCandidate(sampleCandidate, profile)
                    candidateResults[key] = result
                    synchronized(this@RouteScannerEngine) {
                        val matching = candidates.filter { "${it.strategy.name}:${it.domain}" == key }
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
            val key = "${candidate.strategy.name}:${candidate.domain}"
            val probe = candidateResults[key]
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
                    it.status == SniStatus.WS_READY && it.strategy == RouteStrategy.STANDARD_SNI && it.stabilityScore >= 80 -> 0 // 1. Top Verified Rock-Solid Standard SNI Routes
                    it.status == SniStatus.WS_READY && it.stabilityScore >= 80 -> 1                                              // 2. Verified Rock-Solid WS Routes
                    it.status == SniStatus.WS_READY && it.strategy == RouteStrategy.STANDARD_SNI -> 2                             // 3. WS Ready Standard SNI with minor jitter
                    it.status == SniStatus.WS_READY -> 3                                                                           // 4. WS Ready Fronting
                    it.status == SniStatus.TLS_OK && it.strategy == RouteStrategy.STANDARD_SNI && it.stabilityScore >= 80 -> 4   // 5. Stable TLS Standard SNI
                    it.status == SniStatus.TLS_OK && it.stabilityScore >= 80 -> 5                                                 // 6. Stable TLS
                    it.status == SniStatus.TLS_OK -> 6                                                                            // 7. TLS Pass
                    it.status == SniStatus.BLOCKED_TIMEOUT -> 7                                                                   // 8. Blocked
                    else -> 8                                                                                                     // 9. Port closed
                }
            }.thenByDescending { it.stabilityScore }
             .thenBy { it.packetLossPercent }
             .thenBy { it.jitterMs }
             .thenBy { it.avgLatencyMs.takeIf { lat -> lat > 0 } ?: Long.MAX_VALUE }
        )
    }

    /**
     * Triple-Mode Presets Candidate Evaluators:
     * Discovers 3 distinct best candidates for SPEED, STABILITY, and BALANCED modes.
     */
    fun findBestSpeedCandidate(candidates: List<SniRouteCandidate>): SniRouteCandidate? =
        Companion.findBestSpeedCandidate(candidates)

    fun findBestStabilityCandidate(candidates: List<SniRouteCandidate>): SniRouteCandidate? =
        Companion.findBestStabilityCandidate(candidates)

    fun findBestBalancedCandidate(candidates: List<SniRouteCandidate>): SniRouteCandidate? =
        Companion.findBestBalancedCandidate(candidates)

    fun buildTunedPresetConfig(
        base: VpnConfig,
        candidate: SniRouteCandidate,
        mode: String,
        customMtu: Int,
        customTcpBufferKb: Int,
        customDns: String = "1.1.1.1",
        dpiDelayFix: Boolean = true,
        smartQoS: Boolean = true
    ): VpnConfig {
        var updatedUri = base.configUri
        val targetPort = candidate.targetPort
        val isTls = CommonProxyPorts.isTlsPort(targetPort)
        val securityVal = if (isTls) "tls" else "none"

        if (base.isVmess) {
            try {
                val base64Content = updatedUri.removePrefix("vmess://").trim()
                val jsonStr = String(Base64.getUrlDecoder().decode(base64Content), Charsets.UTF_8)
                val json = JSONObject(jsonStr)

                if (candidate.strategy == RouteStrategy.ADDR_SNI) {
                    json.put("add", candidate.domain)
                    json.put("port", targetPort)
                    json.put("sni", if (isTls) candidate.wsHost else "")
                    json.put("host", candidate.wsHost)
                    json.put("tls", securityVal)
                    json.put("insecure", isTls)
                } else {
                    json.put("add", DEFAULT_VPS_DOMAIN)
                    json.put("port", targetPort)
                    json.put("sni", if (isTls) candidate.domain else "")
                    json.put("host", candidate.wsHost)
                    json.put("tls", securityVal)
                    json.put("insecure", isTls)
                }
                val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(json.toString().toByteArray(Charsets.UTF_8))
                updatedUri = "vmess://$encoded"
            } catch (e: Exception) {
                AppLogger.w(TAG, "Failed to update VMess URI for preset $mode: ${e.message}")
            }
        } else {
            if (candidate.strategy == RouteStrategy.ADDR_SNI) {
                updatedUri = replaceAddressAndPort(updatedUri, candidate.domain, targetPort)
                updatedUri = replaceQueryParam(updatedUri, "security", securityVal)
                if (isTls) {
                    updatedUri = replaceQueryParam(updatedUri, "sni", candidate.wsHost)
                    updatedUri = replaceQueryParam(updatedUri, "allowInsecure", "true")
                } else {
                    updatedUri = removeQueryParam(updatedUri, "sni")
                    updatedUri = removeQueryParam(updatedUri, "allowInsecure")
                }
                updatedUri = replaceQueryParam(updatedUri, "host", candidate.wsHost)
            } else {
                updatedUri = replaceAddressAndPort(updatedUri, DEFAULT_VPS_DOMAIN, targetPort)
                updatedUri = replaceQueryParam(updatedUri, "security", securityVal)
                if (isTls) {
                    updatedUri = replaceQueryParam(updatedUri, "sni", candidate.domain)
                    updatedUri = replaceQueryParam(updatedUri, "allowInsecure", "true")
                } else {
                    updatedUri = removeQueryParam(updatedUri, "sni")
                    updatedUri = removeQueryParam(updatedUri, "allowInsecure")
                }
                updatedUri = replaceQueryParam(updatedUri, "host", candidate.wsHost)
            }
        }

        val parsed = profileParser.parseUri(updatedUri)
        val cleanName = base.displayName
            .substringBefore(" [Balanced]")
            .substringBefore(" [Speed]")
            .substringBefore(" [Stability]")
            .substringBefore(" [")

        val modeTag = when (mode.uppercase()) {
            "SPEED" -> "Speed"
            "STABILITY" -> "Stability"
            else -> "Balanced"
        }
        val presetName = "$cleanName [$modeTag]"

        val modeOffset = when (mode.uppercase()) {
            "SPEED" -> 1000L
            "STABILITY" -> 2000L
            else -> 0L
        }
        val presetId = if (base.id > 1000000000L) base.id + modeOffset else base.id * 1000L + modeOffset

        return base.copy(
            id = presetId,
            configUri = updatedUri,
            host = parsed?.host ?: base.host,
            port = parsed?.port ?: base.port,
            sni = parsed?.sni ?: base.sni,
            wsHost = parsed?.wsHost ?: base.wsHost,
            wsPath = parsed?.wsPath ?: base.wsPath,
            configName = presetName,
            remark = presetName,
            tuningMode = mode.uppercase(),
            customMtu = customMtu,
            customTcpBufferKb = customTcpBufferKb,
            customDns = customDns,
            dpiDelayFix = dpiDelayFix,
            smartQoS = smartQoS,
            measuredPingMs = candidate.avgLatencyMs.takeIf { it > 0 } ?: candidate.latencyMs,
            stabilityScore = candidate.stabilityScore
        )
    }

    /**
     * Applies the selected candidate to the active V2Ray configuration.
     * Rewrites destination address, target port, SNI, and WS Host parameters and cleanly reloads the tunnel.
     */
    suspend fun applySniRoute(candidate: SniRouteCandidate): AppResult<VpnConfig> = withContext(dispatchers.io) {
        val current = subscriptionRepository.observeSelectedConfig().first()
            ?: return@withContext AppResult.Failure(com.v2rayops.client.core.error.AppError.Validation("No active config selected to tune"))

        var updatedUri = current.configUri
        val targetPort = candidate.targetPort
        val isTls = CommonProxyPorts.isTlsPort(targetPort)
        val securityVal = if (isTls) "tls" else "none"

        if (current.isVmess) {
            try {
                val base64Content = updatedUri.removePrefix("vmess://").trim()
                val jsonStr = String(Base64.getUrlDecoder().decode(base64Content), Charsets.UTF_8)
                val json = JSONObject(jsonStr)

                if (candidate.strategy == RouteStrategy.ADDR_SNI) {
                    json.put("add", candidate.domain)
                    json.put("port", targetPort)
                    json.put("sni", if (isTls) candidate.wsHost else "")
                    json.put("host", candidate.wsHost)
                    json.put("tls", securityVal)
                    json.put("insecure", isTls)
                } else {
                    json.put("add", DEFAULT_VPS_DOMAIN)
                    json.put("port", targetPort)
                    json.put("sni", if (isTls) candidate.domain else "")
                    json.put("host", candidate.wsHost)
                    json.put("tls", securityVal)
                    json.put("insecure", isTls)
                }
                val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(json.toString().toByteArray(Charsets.UTF_8))
                updatedUri = "vmess://$encoded"
            } catch (e: Exception) {
                AppLogger.w(TAG, "Failed to update VMess URI: ${e.message}")
            }
        } else {
            if (candidate.strategy == RouteStrategy.ADDR_SNI) {
                // Strategy 2 (Addr SNI Fronting):
                // Destination address becomes candidate.domain (e.g. support.zoom.us:port)
                // SNI becomes VPS domain (sg-v2rayops.vpnxhub.com) if TLS
                // Host becomes VPS domain (sg-v2rayops.vpnxhub.com)
                updatedUri = replaceAddressAndPort(updatedUri, candidate.domain, targetPort)
                updatedUri = replaceQueryParam(updatedUri, "security", securityVal)
                if (isTls) {
                    updatedUri = replaceQueryParam(updatedUri, "sni", candidate.wsHost)
                    updatedUri = replaceQueryParam(updatedUri, "allowInsecure", "true")
                } else {
                    updatedUri = removeQueryParam(updatedUri, "sni")
                    updatedUri = removeQueryParam(updatedUri, "allowInsecure")
                }
                updatedUri = replaceQueryParam(updatedUri, "host", candidate.wsHost)
            } else {
                // Strategy 1 (Standard SNI):
                // Destination address is VPS IP / domain
                // SNI becomes candidate.domain if TLS
                // Host remains VPS domain
                updatedUri = replaceAddressAndPort(updatedUri, DEFAULT_VPS_DOMAIN, targetPort)
                updatedUri = replaceQueryParam(updatedUri, "security", securityVal)
                if (isTls) {
                    updatedUri = replaceQueryParam(updatedUri, "sni", candidate.domain)
                    updatedUri = replaceQueryParam(updatedUri, "allowInsecure", "true")
                } else {
                    updatedUri = removeQueryParam(updatedUri, "sni")
                    updatedUri = removeQueryParam(updatedUri, "allowInsecure")
                }
                updatedUri = replaceQueryParam(updatedUri, "host", candidate.wsHost)
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
            remark = "$cleanName [$strategyTag: ${candidate.domain}:$targetPort]"
        )

        AppLogger.i(TAG, "Applied winning route [${candidate.strategy.name}] '${candidate.domain}:$targetPort' to config #${updatedConfig.id} (Addr: ${updatedConfig.effectiveAddr}, SNI: ${updatedConfig.effectiveSni})")
        subscriptionRepository.selectConfig(updatedConfig)

        // If currently running, cleanly reconnect with the newly tuned route
        if (vpnManager.connectionState.value.isConnected) {
            AppLogger.i(TAG, "Reconnecting tunnel with newly tuned route (${candidate.strategy.shortTag}: ${candidate.domain}:$targetPort)")
            vpnManager.connect(updatedConfig)
        }

        AppResult.Success(updatedConfig)
    }

    /**
     * Generates a complete 3X-UI inbound configuration specification for the given port and candidate.
     */
    suspend fun generate3xUiInbound(
        candidate: SniRouteCandidate,
        port: Int = candidate.targetPort
    ): ThreeXUiInboundSpec = withContext(dispatchers.io) {
        val current = subscriptionRepository.observeSelectedConfig().first()
        val parsed = current?.configUri?.let { profileParser.parseUri(it) }
        val clientUuid = parsed?.uuidOrPassword ?: java.util.UUID.randomUUID().toString()

        val isTls = CommonProxyPorts.isTlsPort(port)
        val security = if (isTls) "tls" else "none"
        val remark = "V2RayOps-VLESS-WS-$port"
        val wsHost = candidate.wsHost
        val wsPath = candidate.wsPath
        val sni = wsHost

        val jsonSnippet = if (isTls) {
            """
            {
              "port": $port,
              "protocol": "vless",
              "listen": "0.0.0.0",
              "tag": "inbound-$port",
              "settings": {
                "clients": [
                  {
                    "id": "$clientUuid",
                    "flow": ""
                  }
                ],
                "decryption": "none",
                "fallbacks": []
              },
              "streamSettings": {
                "network": "ws",
                "security": "tls",
                "tlsSettings": {
                  "serverName": "$sni",
                  "certificates": []
                },
                "wsSettings": {
                  "path": "$wsPath",
                  "headers": {
                    "Host": "$wsHost"
                  }
                }
              },
              "sniffing": {
                "enabled": true,
                "destOverride": ["http", "tls"]
              }
            }
            """.trimIndent()
        } else {
            """
            {
              "port": $port,
              "protocol": "vless",
              "listen": "0.0.0.0",
              "tag": "inbound-$port",
              "settings": {
                "clients": [
                  {
                    "id": "$clientUuid",
                    "flow": ""
                  }
                ],
                "decryption": "none",
                "fallbacks": []
              },
              "streamSettings": {
                "network": "ws",
                "security": "none",
                "wsSettings": {
                  "path": "$wsPath",
                  "headers": {
                    "Host": "$wsHost"
                  }
                }
              },
              "sniffing": {
                "enabled": true,
                "destOverride": ["http", "tls"]
              }
            }
            """.trimIndent()
        }

        val instructions = listOf(
            "1. Open 3X-UI Web Panel: https://v2rayops.vpnxhub.com:2083/",
            "2. Navigate to 'Inbounds' tab -> Click '+ Add Inbound'",
            "3. Set Remark: '$remark'",
            "4. Protocol: 'vless' | Listen: '0.0.0.0' | Port: '$port'",
            "5. Transmission / Network: 'ws' (WebSocket)",
            "6. Path: '$wsPath'",
            "7. Header Host: '$wsHost'",
            if (isTls) "8. Security: 'tls' | SNI: '$sni'" else "8. Security: 'none' (Cleartext HTTP / WS)",
            "9. Client UUID: '$clientUuid'",
            "10. Click 'Save' / 'Create' to activate the inbound on port $port!"
        )

        ThreeXUiInboundSpec(
            remark = remark,
            protocol = "vless",
            port = port,
            isTls = isTls,
            security = security,
            listen = "0.0.0.0",
            network = "ws",
            wsPath = wsPath,
            wsHost = wsHost,
            sni = sni,
            clientUuid = clientUuid,
            panelUrl = "https://v2rayops.vpnxhub.com:2083/",
            fullJsonSnippet = jsonSnippet,
            instructions = instructions
        )
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

    private fun removeQueryParam(rawUri: String, key: String): String {
        return rawUri.replace(Regex("""[?&]$key=[^&#]*"""), "")
            .replace(Regex("""\?&"""), "?")
            .replace(Regex("""\?$"""), "")
    }
}
