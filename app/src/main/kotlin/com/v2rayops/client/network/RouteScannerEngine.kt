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

enum class SniCategory(val displayName: String) {
    ZOOM_TEAMS("Zoom & Teams"),
    TIKTOK("TikTok"),
    WHATSAPP_SOCIAL("WhatsApp & Social"),
    CLOUDFLARE_EDGE("Cloudflare Edge"),
    CUSTOM("Custom SNI")
}

enum class SniStatus {
    WS_READY,       // Handshake passed & HTTP 101 WS Upgrade OK
    TLS_OK,         // Handshake passed, WS not tested or not 101
    BLOCKED_TIMEOUT,// Carrier DPI dropped TLS handshake (timed out)
    TCP_FAILED      // Port connection rejected
}

data class SniRouteCandidate(
    val sni: String,
    val category: SniCategory,
    val targetHost: String = "172.236.134.215",
    val targetPort: Int = 443,
    val wsHost: String = "sg-v2rayops.vpnxhub.com",
    val wsPath: String = "/v2rayops",
    val status: SniStatus = SniStatus.TCP_FAILED,
    val latencyMs: Long = 0L,
    val errorDetail: String = ""
)

/**
 * Route Scanner Engine:
 * Benchmarks carrier bug host SNIs and frontend CDN routes to discover the fastest,
 * zero-timeout tunneling path for the client's current mobile carrier or Wi-Fi ISP.
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

        val DEFAULT_CANDIDATES = listOf(
            // Zoom / Education / Work & Learn
            SniRouteCandidate(sni = "support.zoom.us", category = SniCategory.ZOOM_TEAMS),
            SniRouteCandidate(sni = "news.zoom.us", category = SniCategory.ZOOM_TEAMS),
            SniRouteCandidate(sni = "zoom.us", category = SniCategory.ZOOM_TEAMS),
            SniRouteCandidate(sni = "microsoft.com", category = SniCategory.ZOOM_TEAMS),
            SniRouteCandidate(sni = "teams.microsoft.com", category = SniCategory.ZOOM_TEAMS),

            // TikTok Packages
            SniRouteCandidate(sni = "tiktok.com", category = SniCategory.TIKTOK),
            SniRouteCandidate(sni = "p16-va.tiktokcdn.com", category = SniCategory.TIKTOK),
            SniRouteCandidate(sni = "v16m.tiktokcdn.com", category = SniCategory.TIKTOK),
            SniRouteCandidate(sni = "api.tiktokv.com", category = SniCategory.TIKTOK),

            // WhatsApp & Social
            SniRouteCandidate(sni = "web.whatsapp.com", category = SniCategory.WHATSAPP_SOCIAL),
            SniRouteCandidate(sni = "media.whatsapp.net", category = SniCategory.WHATSAPP_SOCIAL),

            // Universal CDN Edges
            SniRouteCandidate(sni = "cloudflare.com", category = SniCategory.CLOUDFLARE_EDGE),
            SniRouteCandidate(sni = "one.one.one.one", category = SniCategory.CLOUDFLARE_EDGE)
        )
    }

    suspend fun scanCandidate(
        candidate: SniRouteCandidate,
        timeoutMs: Int = 3000
    ): SniRouteCandidate = withContext(dispatchers.io) {
        val targetIp = candidate.targetHost
        val port = candidate.targetPort
        val sni = candidate.sni
        val wsHost = candidate.wsHost
        val wsPath = candidate.wsPath

        var rawSocket: Socket? = null
        val startTcp = System.currentTimeMillis()

        // 1. TCP Socket Connect
        try {
            val s = Socket()
            s.connect(InetSocketAddress(targetIp, port), timeoutMs)
            rawSocket = s
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
        var sslSocket: SSLSocket? = null
        try {
            val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAll, SecureRandom())

            val ssl = sslContext.socketFactory.createSocket(rawSocket, targetIp, port, true) as SSLSocket
            val sslParams = SSLParameters()
            sslParams.serverNames = listOf(SNIHostName(sni))
            ssl.sslParameters = sslParams
            ssl.soTimeout = timeoutMs

            ssl.startHandshake()
            sslSocket = ssl
        } catch (e: Exception) {
            rawSocket?.close()
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

            val wsKey = Base64.getEncoder().encodeToString("V2RayOpsProbeKey123".toByteArray())
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
                rawSocket?.close()
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
     * Applies the selected SNI to the active V2Ray configuration.
     * Rewrites query parameters and reloads the tunnel.
     */
    suspend fun applySniRoute(candidate: SniRouteCandidate): AppResult<VpnConfig> = withContext(dispatchers.io) {
        val current = subscriptionRepository.observeSelectedConfig().first()
            ?: return@withContext AppResult.Failure(com.v2rayops.client.core.error.AppError.Validation("No active config selected to tune"))

        val updatedUri = replaceQueryParam(current.configUri, "sni", candidate.sni)
        val parsed = profileParser.parseUri(updatedUri)
            ?: return@withContext AppResult.Failure(com.v2rayops.client.core.error.AppError.Validation("Failed to re-parse updated config URI"))

        val updatedConfig = current.copy(
            configUri = updatedUri,
            remark = "${current.displayName.substringBefore(" [")} [SNI: ${candidate.sni}]"
        )

        AppLogger.i(TAG, "Applied winning SNI '${candidate.sni}' to config #${updatedConfig.id}")
        subscriptionRepository.selectConfig(updatedConfig)

        // If currently running, cleanly reconnect with the winning SNI
        if (vpnManager.connectionState.value.isConnected) {
            AppLogger.i(TAG, "Reconnecting tunnel with newly tuned SNI route: ${candidate.sni}")
            vpnManager.connect(updatedConfig)
        }

        AppResult.Success(updatedConfig)
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
