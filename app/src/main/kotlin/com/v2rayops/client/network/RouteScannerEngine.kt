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
    val errorDetail: String = ""
)

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

    suspend fun scanCandidate(
        candidate: SniRouteCandidate,
        timeoutMs: Int = 3000
    ): SniRouteCandidate = withContext(dispatchers.io) {
        val targetHost = candidate.targetHost
        val port = candidate.targetPort
        val sni = candidate.sni
        val wsHost = candidate.wsHost
        val wsPath = candidate.wsPath

        val startTcp = System.currentTimeMillis()

        // 1. TCP Socket Connect (Resolves domain via system DNS if targetHost is domain)
        val rawSocket: Socket = try {
            val s = Socket()
            s.connect(InetSocketAddress(targetHost, port), timeoutMs)
            s
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTcp
            return@withContext candidate.copy(
                status = SniStatus.TCP_FAILED,
                latencyMs = elapsed,
                errorDetail = e.message ?: "TCP Connect timeout"
            )
        }

        // 2. TLS Handshake Probe with SNI
        val startTls = System.currentTimeMillis()
        val sslSocket: SSLSocket = try {
            val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAll, SecureRandom())

            val ssl = sslContext.socketFactory.createSocket(rawSocket, targetHost, port, true) as SSLSocket
            val sslParams = SSLParameters()
            sslParams.serverNames = listOf(SNIHostName(sni))
            ssl.sslParameters = sslParams
            ssl.soTimeout = timeoutMs

            ssl.startHandshake()
            ssl
        } catch (e: Exception) {
            try { rawSocket.close() } catch (_: Exception) {}
            val elapsed = System.currentTimeMillis() - startTls
            return@withContext candidate.copy(
                status = SniStatus.BLOCKED_TIMEOUT,
                latencyMs = elapsed,
                errorDetail = "Blocked by carrier: ${e.message ?: "SSL handshake timed out"}"
            )
        }

        val tlsLatency = System.currentTimeMillis() - startTls

        // 3. WebSocket HTTP Upgrade Check
        var wsPassed = false
        var wsError = ""
        try {
            val writer = OutputStreamWriter(sslSocket.outputStream, "UTF-8")
            val reader = BufferedReader(InputStreamReader(sslSocket.inputStream, "UTF-8"))

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
        } finally {
            try {
                sslSocket.close()
                rawSocket.close()
            } catch (_: Exception) {}
        }

        val totalLatency = (System.currentTimeMillis() - startTcp).coerceAtLeast(tlsLatency)

        if (wsPassed) {
            candidate.copy(
                status = SniStatus.WS_READY,
                latencyMs = totalLatency,
                errorDetail = "HTTP 101 Switching Protocols"
            )
        } else {
            candidate.copy(
                status = SniStatus.TLS_OK,
                latencyMs = totalLatency,
                errorDetail = if (wsError.isNotBlank()) "TLS OK ($wsError)" else "TLS OK"
            )
        }
    }

    suspend fun scanAll(
        candidates: List<SniRouteCandidate> = DEFAULT_CANDIDATES,
        onProgress: (scanned: Int, total: Int, latest: SniRouteCandidate) -> Unit = { _, _, _ -> }
    ): List<SniRouteCandidate> = coroutineScope {
        val semaphore = Semaphore(4)
        val total = candidates.size
        var scannedCount = 0

        val deferredList = candidates.map { candidate ->
            async(dispatchers.io) {
                semaphore.withPermit {
                    val result = scanCandidate(candidate)
                    synchronized(this@RouteScannerEngine) {
                        scannedCount++
                        onProgress(scannedCount, total, result)
                    }
                    result
                }
            }
        }

        deferredList.awaitAll().sortedWith(
            compareBy<SniRouteCandidate> {
                when (it.status) {
                    SniStatus.WS_READY -> 0
                    SniStatus.TLS_OK -> 1
                    SniStatus.BLOCKED_TIMEOUT -> 2
                    SniStatus.TCP_FAILED -> 3
                }
            }.thenBy { it.latencyMs }
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

        val parsed = profileParser.parseUri(updatedUri)
            ?: return@withContext AppResult.Failure(com.v2rayops.client.core.error.AppError.Validation("Failed to re-parse updated config URI"))

        val strategyTag = candidate.strategy.shortTag
        val cleanName = current.displayName.substringBefore(" [")
        val updatedConfig = current.copy(
            configUri = updatedUri,
            remark = "$cleanName [$strategyTag: ${candidate.domain}]"
        )

        AppLogger.i(TAG, "Applied winning route [${candidate.strategy.name}] '${candidate.domain}' to config #${updatedConfig.id} (Addr: ${parsed.host}, SNI: ${parsed.sni})")
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
