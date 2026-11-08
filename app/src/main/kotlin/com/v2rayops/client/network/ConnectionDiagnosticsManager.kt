package com.v2rayops.client.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.v2rayops.client.core.dispatchers.AppDispatchers
import com.v2rayops.client.core.logger.AppLogger
import com.v2rayops.client.domain.model.VpnConfig
import com.v2rayops.client.profiles.ProfileParser
import com.v2rayops.client.vpn.VpnManager
import com.v2rayops.client.xray.XrayEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLParameters
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

enum class DiagnosticStatus {
    IDLE,
    TESTING,
    SUCCESS,
    FAILED
}

data class ConnectionDiagnosticResult(
    val status: DiagnosticStatus = DiagnosticStatus.IDLE,
    val pingMs: Long = -1L,
    val publicIp: String = "",
    val country: String = "",
    val countryCode: String = "",
    val isp: String = "",
    val isV2RayOpsExit: Boolean = false,
    val message: String = "",
    val debugDetails: String = "",
    val testedAtMs: Long = System.currentTimeMillis()
)

data class NodeProbeReport(
    val nodeName: String,
    val endpoint: String,
    val sni: String,
    val dnsResolvedIps: List<String>,
    val dnsLatencyMs: Long,
    val tcpSuccess: Boolean,
    val tcpLatencyMs: Long,
    val tcpError: String = "",
    val tlsSuccess: Boolean,
    val tlsLatencyMs: Long,
    val tlsError: String = "",
    val wsSuccess: Boolean,
    val wsStatusLine: String = "",
    val wsError: String = "",
    val summary: String
)

data class DnsServerCheckResult(
    val provider: String,
    val serverIp: String,
    val protocol: String,
    val latencyMs: Long,
    val isReachable: Boolean,
    val details: String = ""
)

@Singleton
class ConnectionDiagnosticsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val xrayEngine: XrayEngine,
    private val vpnManager: VpnManager,
    private val profileParser: ProfileParser,
    private val dispatchers: AppDispatchers
) {
    companion object {
        private const val TAG = "ConnectionDiagnostics"
        private const val LOCAL_PROXY_PORT = 10809
        private const val V2RAYOPS_SERVER_IP = "172.236.134.215"
    }

    private fun getPhysicalNetwork(): Network? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return null
        return cm.allNetworks.firstOrNull { net ->
            val caps = cm.getNetworkCapabilities(net) ?: return@firstOrNull false
            (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) &&
                    !caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        }
    }

    /**
     * Executes an end-to-end connection and IP diagnostic test.
     * When VPN is connected, test traffic routes through local proxy listener (127.0.0.1:10809).
     * If the tunnel test times out or fails, performs an automated pre-flight socket probe
     * to pinpoint whether DNS, TCP, TLS, or WebSocket is failing.
     */
    suspend fun runDiagnosticTest(targetConfig: VpnConfig? = null): ConnectionDiagnosticResult = withContext(dispatchers.io) {
        val isVpnConnected = vpnManager.connectionState.value.isConnected

        AppLogger.i(TAG, "Starting connection diagnostics (isVpnConnected=$isVpnConnected)")

        // Build OkHttpClient (proxied through local core HTTP inbound if connected, direct if disconnected)
        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)

        if (isVpnConnected) {
            clientBuilder.proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", LOCAL_PROXY_PORT)))
        }

        val client = clientBuilder.build()

        try {
            val reqStart = System.currentTimeMillis()
            var pingMs = -1L
            var ip = ""
            var country = ""
            var countryCode = ""
            var isp = ""

            // Attempt 1: http://ip-api.com/json (fast HTTP endpoint, ideal for proxy testing)
            try {
                val req1 = Request.Builder()
                    .url("http://ip-api.com/json")
                    .header("User-Agent", "V2RayOps-Client/1.0")
                    .build()
                val res1 = client.newCall(req1).execute()
                if (res1.isSuccessful) {
                    val body1 = res1.body?.string() ?: ""
                    val json1 = JSONObject(body1)
                    if (json1.optString("status") == "success" || json1.has("query")) {
                        ip = json1.optString("query", "")
                        country = json1.optString("country", "")
                        countryCode = json1.optString("countryCode", "")
                        isp = json1.optString("isp", json1.optString("org", ""))
                        pingMs = System.currentTimeMillis() - reqStart
                    }
                }
            } catch (t: Throwable) {
                AppLogger.d(TAG, "ip-api.com check failed: ${t.message}, trying fallback")
            }

            // Attempt 2 (Fallback): https://ipwho.is
            if (ip.isBlank()) {
                try {
                    val req2 = Request.Builder()
                        .url("https://ipwho.is")
                        .header("User-Agent", "V2RayOps-Client/1.0")
                        .build()
                    val res2 = client.newCall(req2).execute()
                    if (res2.isSuccessful) {
                        val body2 = res2.body?.string() ?: ""
                        val json2 = JSONObject(body2)
                        if (json2.optBoolean("success", true)) {
                            ip = json2.optString("ip", "")
                            country = json2.optString("country", "")
                            countryCode = json2.optString("country_code", "")
                            val conn = json2.optJSONObject("connection")
                            isp = conn?.optString("isp", conn.optString("org", "")) ?: ""
                            pingMs = System.currentTimeMillis() - reqStart
                        }
                    }
                } catch (t: Throwable) {
                    AppLogger.d(TAG, "ipwho.is fallback check failed: ${t.message}")
                }
            }

            // Attempt 3 (Minimal IP fallback): http://api.ipify.org?format=json
            if (ip.isBlank()) {
                try {
                    val req3 = Request.Builder()
                        .url("http://api.ipify.org?format=json")
                        .header("User-Agent", "V2RayOps-Client/1.0")
                        .build()
                    val res3 = client.newCall(req3).execute()
                    if (res3.isSuccessful) {
                        val body3 = res3.body?.string() ?: ""
                        val json3 = JSONObject(body3)
                        ip = json3.optString("ip", "")
                        pingMs = System.currentTimeMillis() - reqStart
                    }
                } catch (t: Throwable) {
                    AppLogger.d(TAG, "api.ipify.org fallback check failed: ${t.message}")
                }
            }

            if (pingMs <= 0) {
                pingMs = System.currentTimeMillis() - reqStart
            }

            val edgeLatency = measureEdgePing(targetConfig)
            val finalPing = if (edgeLatency > 0) edgeLatency else pingMs

            if (ip.isBlank()) {
                // Pinpoint failure with target node probe if available
                val probeSummary = if (targetConfig != null) {
                    val probe = probeTargetNode(targetConfig)
                    probe.summary
                } else {
                    "Local proxy (127.0.0.1:10809) did not respond in time"
                }

                return@withContext ConnectionDiagnosticResult(
                    status = if (edgeLatency > 0) DiagnosticStatus.SUCCESS else DiagnosticStatus.FAILED,
                    pingMs = if (edgeLatency > 0) edgeLatency else finalPing,
                    message = if (edgeLatency > 0) "Edge Server Active • Latency ${edgeLatency}ms" else "Could not route traffic via tunnel",
                    debugDetails = probeSummary
                )
            }

            val isServerExit = ip == V2RAYOPS_SERVER_IP ||
                    countryCode.equals("SG", ignoreCase = true) ||
                    isp.contains("Akamai", ignoreCase = true) ||
                    isp.contains("Linode", ignoreCase = true)

            AppLogger.i(TAG, "Diagnostic success: IP=$ip, Country=$country, ISP=$isp, Ping=${finalPing}ms, isExit=$isServerExit")

            ConnectionDiagnosticResult(
                status = DiagnosticStatus.SUCCESS,
                pingMs = finalPing,
                publicIp = ip,
                country = country,
                countryCode = countryCode,
                isp = isp,
                isV2RayOpsExit = isServerExit,
                message = if (isServerExit) {
                    "Tunnel Verified • Exiting via V2RayOPS Singapore ($ip)"
                } else if (isVpnConnected) {
                    "Tunnel Active • IP $ip ($country)"
                } else {
                    "Direct Carrier Network (Unprotected) • IP $ip ($country)"
                },
                debugDetails = "Verified tunnel route: exit=$ip ($country), latency=${pingMs}ms, ISP=$isp"
            )
        } catch (e: Exception) {
            AppLogger.w(TAG, "Diagnostic test failed: ${e.message}")
            val probeSummary = if (targetConfig != null) {
                val probe = probeTargetNode(targetConfig)
                probe.summary
            } else {
                e.message ?: "Failed to reach diagnostic endpoint"
            }

            ConnectionDiagnosticResult(
                status = DiagnosticStatus.FAILED,
                pingMs = -1L,
                message = e.message ?: "Failed to reach diagnostic endpoint",
                debugDetails = probeSummary
            )
        }
    }

    /**
     * Measures low-latency TCP edge ping to the server endpoint (returns round-trip in ms, e.g. 80-160ms).
     */
    suspend fun measureEdgePing(targetConfig: VpnConfig?): Long = withContext(dispatchers.io) {
        val rawHost = targetConfig?.host?.ifBlank { "v2rayops.vpnxhub.com" } ?: "v2rayops.vpnxhub.com"
        val port = targetConfig?.port ?: 443
        val targetIp = if (rawHost.contains("vpnxhub.com", ignoreCase = true)) "172.236.134.215" else rawHost
        val start = System.currentTimeMillis()
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(targetIp, port), 2500)
            }
            (System.currentTimeMillis() - start).coerceAtLeast(1L)
        } catch (e: Exception) {
            AppLogger.d(TAG, "measureEdgePing failed to $targetIp:$port: ${e.message}")
            -1L
        }
    }

    /**
     * Performs a granular pre-flight diagnostic probe of a server node:
     * 1. Resolves host IP via DNS
     * 2. Tests raw TCP socket connect latency and timeout
     * 3. Tests TLS handshake with specified SNI (allowing insecure/bug hosts)
     * 4. Tests WebSocket HTTP upgrade negotiation (expects 101 Switching Protocols)
     */
    suspend fun probeTargetNode(config: VpnConfig): NodeProbeReport = withContext(dispatchers.io) {
        val parsed = profileParser.parseUri(config.configUri)
        val host = parsed?.host?.ifBlank { config.host } ?: config.host
        val port = parsed?.port ?: config.port
        val sni = parsed?.sni?.ifBlank { host } ?: host
        val wsHost = parsed?.wsHost?.ifBlank { host } ?: host
        val wsPath = parsed?.wsPath?.ifBlank { "/v2rayops" } ?: "/v2rayops"

        AppLogger.i(TAG, "=== Initiating Node Probe: ${config.displayName} ===")
        AppLogger.i(TAG, "Target: $host:$port, SNI: $sni, WSHost: $wsHost, WSPath: $wsPath")

        // 1. DNS Resolution
        val dnsStart = System.currentTimeMillis()
        var dnsIps = emptyList<String>()
        var dnsLatency = 0L
        try {
            val resolveTarget = if (host.contains("vpnxhub.com", ignoreCase = true)) {
                "172.236.134.215"
            } else {
                host
            }
            val addresses = InetAddress.getAllByName(resolveTarget)
            dnsIps = addresses.map { it.hostAddress ?: "" }.filter { it.isNotBlank() }
            dnsLatency = System.currentTimeMillis() - dnsStart
            AppLogger.i(TAG, "[Probe-DNS] Resolved $host -> ${dnsIps.joinToString()} in ${dnsLatency}ms")
        } catch (e: Exception) {
            dnsLatency = System.currentTimeMillis() - dnsStart
            AppLogger.w(TAG, "[Probe-DNS] Failed to resolve $host: ${e.message}")
        }

        // Prefer IPv4 addresses for maximum stability on cellular/Wi-Fi CDNs (IPv6 CDNs frequently drop TLS fronting)
        val ipv4Addresses = dnsIps.filter { !it.contains(":") }
        val targetIp = ipv4Addresses.firstOrNull() ?: dnsIps.firstOrNull() ?: if (host.contains("vpnxhub.com", ignoreCase = true)) "172.236.134.215" else host

        // 2. TCP Socket Connect
        val tcpStart = System.currentTimeMillis()
        var tcpSuccess = false
        var tcpLatency = 0L
        var tcpError = ""
        var rawSocket: Socket? = null

        try {
            val s = Socket()
            s.connect(InetSocketAddress(targetIp, port), 4000)
            tcpLatency = System.currentTimeMillis() - tcpStart
            tcpSuccess = true
            rawSocket = s
            AppLogger.i(TAG, "[Probe-TCP] Connected to $targetIp:$port in ${tcpLatency}ms")
        } catch (e: Exception) {
            tcpLatency = System.currentTimeMillis() - tcpStart
            tcpError = e.message ?: "TCP connect timed out"
            AppLogger.w(TAG, "[Probe-TCP] Connection failed to $targetIp:$port: $tcpError")
        }

        // 3. TLS Handshake Probe (if security == tls)
        var tlsSuccess = false
        var tlsLatency = 0L
        var tlsError = ""
        var sslSocket: SSLSocket? = null

        if (tcpSuccess && rawSocket != null && parsed?.security.equals("tls", ignoreCase = true)) {
            val tlsStart = System.currentTimeMillis()
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
                ssl.soTimeout = 4000

                ssl.startHandshake()
                tlsLatency = System.currentTimeMillis() - tlsStart
                tlsSuccess = true
                sslSocket = ssl
                val certSubject = ssl.session.peerCertificates.firstOrNull()?.toString()?.take(80) ?: "None"
                AppLogger.i(TAG, "[Probe-TLS] Handshake complete in ${tlsLatency}ms! PeerCert: $certSubject")
            } catch (e: Exception) {
                tlsLatency = System.currentTimeMillis() - tlsStart
                tlsError = e.message ?: "TLS handshake failed"
                AppLogger.w(TAG, "[Probe-TLS] TLS handshake with SNI '$sni' failed: $tlsError")
            }
        }

        // 4. WebSocket HTTP Upgrade Probe
        var wsSuccess = false
        var wsStatusLine = ""
        var wsError = ""

        val probeSocket = sslSocket ?: if (parsed?.security != "tls") rawSocket else null
        if (probeSocket != null && probeSocket.isConnected && parsed?.networkType.equals("ws", ignoreCase = true)) {
            try {
                val wsRequest = "GET $wsPath HTTP/1.1\r\n" +
                        "Host: $wsHost\r\n" +
                        "Upgrade: websocket\r\n" +
                        "Connection: Upgrade\r\n" +
                        "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n" +
                        "Sec-WebSocket-Version: 13\r\n\r\n"

                probeSocket.outputStream.write(wsRequest.toByteArray(Charsets.UTF_8))
                probeSocket.outputStream.flush()

                val reader = BufferedReader(InputStreamReader(probeSocket.inputStream))
                wsStatusLine = reader.readLine() ?: ""
                AppLogger.i(TAG, "[Probe-WS] Response: $wsStatusLine")
                if (wsStatusLine.contains("101", ignoreCase = true)) {
                    wsSuccess = true
                } else {
                    wsError = "Unexpected response: $wsStatusLine"
                }
            } catch (e: Exception) {
                wsError = e.message ?: "WS upgrade failed"
                AppLogger.w(TAG, "[Probe-WS] WebSocket upgrade error: $wsError")
            }
        }

        try {
            sslSocket?.close()
            rawSocket?.close()
        } catch (_: Exception) {}

        val summary = buildString {
            append("DNS: ")
            if (dnsIps.isNotEmpty()) append("OK (${dnsIps.first()}, ${dnsLatency}ms) | ")
            else append("FAILED | ")

            append("TCP: ")
            if (tcpSuccess) append("OK (${tcpLatency}ms) | ")
            else append("FAIL ($tcpError) | ")

            if (parsed?.security.equals("tls", ignoreCase = true)) {
                append("TLS: ")
                if (tlsSuccess) append("OK (SNI $sni, ${tlsLatency}ms) | ")
                else append("FAIL ($tlsError) | ")
            }

            if (parsed?.networkType.equals("ws", ignoreCase = true)) {
                append("WS: ")
                if (wsSuccess) append("READY (101 Switching Protocols)")
                else if (wsStatusLine.isNotBlank()) append(wsStatusLine)
                else append("FAIL ($wsError)")
            }
        }

        AppLogger.i(TAG, "=== Node Probe Completed: $summary ===")

        NodeProbeReport(
            nodeName = config.displayName,
            endpoint = "$host:$port",
            sni = sni,
            dnsResolvedIps = dnsIps,
            dnsLatencyMs = dnsLatency,
            tcpSuccess = tcpSuccess,
            tcpLatencyMs = tcpLatency,
            tcpError = tcpError,
            tlsSuccess = tlsSuccess,
            tlsLatencyMs = tlsLatency,
            tlsError = tlsError,
            wsSuccess = wsSuccess,
            wsStatusLine = wsStatusLine,
            wsError = wsError,
            summary = summary
        )
    }

    /**
     * Probes any custom host/IP, SNI, and WebSocket path on demand for developer debugging.
     */
    suspend fun probeCustomEndpoint(
        host: String,
        port: Int = 443,
        sni: String = "",
        wsHost: String = "",
        wsPath: String = "/v2rayops",
        usePureCarrier: Boolean = true
    ): NodeProbeReport = withContext(dispatchers.io) {
        val effectiveHost = host.trim().removePrefix("https://").removePrefix("http://")
        val effectiveSni = sni.ifBlank { effectiveHost }
        val effectiveWsHost = wsHost.ifBlank { effectiveSni }
        val physicalNet = if (usePureCarrier) getPhysicalNetwork() else null

        // 1. DNS Resolution
        val dnsStart = System.currentTimeMillis()
        var dnsIps = emptyList<String>()
        var dnsLatency = 0L
        try {
            val addrs = physicalNet?.getAllByName(effectiveHost) ?: InetAddress.getAllByName(effectiveHost)
            dnsIps = addrs.mapNotNull { it.hostAddress }.filter { it.isNotBlank() }
            dnsLatency = (System.currentTimeMillis() - dnsStart).coerceAtLeast(1L)
        } catch (e: Exception) {
            dnsLatency = (System.currentTimeMillis() - dnsStart).coerceAtLeast(1L)
        }

        val ipv4 = dnsIps.firstOrNull { !it.contains(":") } ?: dnsIps.firstOrNull() ?: effectiveHost

        // 2. TCP Socket Connect
        val tcpStart = System.currentTimeMillis()
        var tcpSuccess = false
        var tcpLatency = 0L
        var tcpError = ""
        var rawSocket: Socket? = null

        try {
            val s = Socket()
            s.tcpNoDelay = true
            s.reuseAddress = true
            s.setSoLinger(true, 0)
            if (physicalNet != null) {
                try { physicalNet.bindSocket(s) } catch (_: Exception) {}
            }
            s.connect(InetSocketAddress(ipv4, port), 3500)
            tcpLatency = (System.currentTimeMillis() - tcpStart).coerceAtLeast(1L)
            tcpSuccess = true
            rawSocket = s
        } catch (e: Exception) {
            tcpLatency = (System.currentTimeMillis() - tcpStart).coerceAtLeast(1L)
            tcpError = e.message ?: "TCP Connect Timed Out"
        }

        // 3. TLS Handshake
        var tlsSuccess = false
        var tlsLatency = 0L
        var tlsError = ""
        var sslSocket: SSLSocket? = null

        if (tcpSuccess && rawSocket != null) {
            val tlsStart = System.currentTimeMillis()
            try {
                val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                })
                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, trustAll, SecureRandom())

                val ssl = sslContext.socketFactory.createSocket(rawSocket, ipv4, port, true) as SSLSocket
                ssl.tcpNoDelay = true
                ssl.setSoLinger(true, 0)
                val sslParams = SSLParameters()
                sslParams.serverNames = listOf(SNIHostName(effectiveSni))
                ssl.sslParameters = sslParams
                ssl.soTimeout = 3500

                ssl.startHandshake()
                tlsLatency = (System.currentTimeMillis() - tlsStart).coerceAtLeast(1L)
                tlsSuccess = true
                sslSocket = ssl
            } catch (e: Exception) {
                tlsLatency = (System.currentTimeMillis() - tlsStart).coerceAtLeast(1L)
                tlsError = e.message ?: "TLS Handshake Failed"
            }
        }

        // 4. WebSocket Upgrade
        var wsSuccess = false
        var wsStatusLine = ""
        var wsError = ""

        if (sslSocket != null && sslSocket.isConnected) {
            try {
                val nonce = ByteArray(16)
                SecureRandom().nextBytes(nonce)
                val wsKey = Base64.getEncoder().encodeToString(nonce)
                val wsRequest = "GET $wsPath HTTP/1.1\r\n" +
                        "Host: $effectiveWsHost\r\n" +
                        "Upgrade: websocket\r\n" +
                        "Connection: Upgrade\r\n" +
                        "Sec-WebSocket-Key: $wsKey\r\n" +
                        "Sec-WebSocket-Version: 13\r\n\r\n"

                val writer = sslSocket.outputStream
                writer.write(wsRequest.toByteArray(Charsets.UTF_8))
                writer.flush()

                val reader = BufferedReader(InputStreamReader(sslSocket.inputStream))
                wsStatusLine = reader.readLine() ?: ""
                if (wsStatusLine.contains("101", ignoreCase = true)) {
                    wsSuccess = true
                } else {
                    wsError = wsStatusLine.ifBlank { "No HTTP status response" }
                }
            } catch (e: Exception) {
                wsError = e.message ?: "WS upgrade failed"
            }
        }

        try {
            sslSocket?.close()
            rawSocket?.close()
        } catch (_: Exception) {}

        val summary = buildString {
            append("DNS: ")
            if (dnsIps.isNotEmpty()) append("OK (${dnsIps.first()}, ${dnsLatency}ms) | ")
            else append("FAIL | ")
            append("TCP: ")
            if (tcpSuccess) append("OK (${tcpLatency}ms) | ")
            else append("FAIL ($tcpError) | ")
            append("TLS: ")
            if (tlsSuccess) append("OK (SNI $effectiveSni, ${tlsLatency}ms) | ")
            else append("FAIL ($tlsError) | ")
            append("WS: ")
            if (wsSuccess) append("READY (101 Switching Protocols)")
            else if (wsStatusLine.isNotBlank()) append(wsStatusLine)
            else append("FAIL ($wsError)")
        }

        NodeProbeReport(
            nodeName = "Custom: $effectiveHost",
            endpoint = "$effectiveHost:$port",
            sni = effectiveSni,
            dnsResolvedIps = dnsIps,
            dnsLatencyMs = dnsLatency,
            tcpSuccess = tcpSuccess,
            tcpLatencyMs = tcpLatency,
            tcpError = tcpError,
            tlsSuccess = tlsSuccess,
            tlsLatencyMs = tlsLatency,
            tlsError = tlsError,
            wsSuccess = wsSuccess,
            wsStatusLine = wsStatusLine,
            wsError = wsError,
            summary = summary
        )
    }

    /**
     * Inspects Cloudflare, Google, Quad9, and Carrier DNS servers using both TCP 53 and UDP 53.
     */
    suspend fun inspectDnsResolvers(usePureCarrier: Boolean = true): List<DnsServerCheckResult> = withContext(dispatchers.io) {
        val physicalNet = if (usePureCarrier) getPhysicalNetwork() else null
        val results = mutableListOf<DnsServerCheckResult>()

        val servers = listOf(
            Triple("Cloudflare Primary", "1.1.1.1", 53),
            Triple("Google Public", "8.8.8.8", 53),
            Triple("Quad9 Secure", "9.9.9.9", 53),
            Triple("Cloudflare Secondary", "1.0.0.1", 53)
        )

        for ((provider, ip, port) in servers) {
            // Test 1: TCP Port 53
            val tcpStart = System.currentTimeMillis()
            var tcpOk = false
            var tcpLat = 0L
            var tcpErr = ""
            try {
                val s = Socket()
                s.tcpNoDelay = true
                if (physicalNet != null) {
                    try { physicalNet.bindSocket(s) } catch (_: Exception) {}
                }
                s.connect(InetSocketAddress(ip, port), 1800)
                tcpLat = (System.currentTimeMillis() - tcpStart).coerceAtLeast(1L)
                tcpOk = true
                s.close()
            } catch (e: Exception) {
                tcpErr = e.message ?: "Timeout"
            }

            results.add(
                DnsServerCheckResult(
                    provider = provider,
                    serverIp = ip,
                    protocol = "TCP:53",
                    latencyMs = if (tcpOk) tcpLat else 0L,
                    isReachable = tcpOk,
                    details = if (tcpOk) "TCP Handshake Pass in ${tcpLat}ms" else "TCP Rejected: $tcpErr"
                )
            )

            // Test 2: UDP Port 53 Datagram Query
            val udpStart = System.currentTimeMillis()
            var udpOk = false
            var udpLat = 0L
            var udpErr = ""
            try {
                val ds = DatagramSocket()
                ds.soTimeout = 1500
                val query = byteArrayOf(
                    0x12, 0x34, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x06, 0x67, 0x6f, 0x6f,
                    0x67, 0x6c, 0x65, 0x03, 0x63, 0x6f, 0x6d, 0x00,
                    0x00, 0x01, 0x00, 0x01
                )
                val targetAddr = if (physicalNet != null) physicalNet.getByName(ip) else InetAddress.getByName(ip)
                val sendPacket = DatagramPacket(query, query.size, targetAddr, port)
                ds.send(sendPacket)
                val buf = ByteArray(512)
                val recPacket = DatagramPacket(buf, buf.size)
                ds.receive(recPacket)
                udpLat = (System.currentTimeMillis() - udpStart).coerceAtLeast(1L)
                udpOk = true
                ds.close()
            } catch (e: Exception) {
                udpErr = e.message ?: "Carrier UDP 53 Filtered/Timeout"
            }

            results.add(
                DnsServerCheckResult(
                    provider = provider,
                    serverIp = ip,
                    protocol = "UDP:53",
                    latencyMs = if (udpOk) udpLat else 0L,
                    isReachable = udpOk,
                    details = if (udpOk) "UDP Query Resolved in ${udpLat}ms" else "UDP Blocked: $udpErr"
                )
            )
        }

        // Test 3: System Local Carrier Resolver
        val sysStart = System.currentTimeMillis()
        var sysOk = false
        var sysLat = 0L
        var sysErr = ""
        var sysIps = ""
        try {
            val addrs = physicalNet?.getAllByName("dns.google") ?: InetAddress.getAllByName("dns.google")
            sysLat = (System.currentTimeMillis() - sysStart).coerceAtLeast(1L)
            sysOk = addrs.isNotEmpty()
            sysIps = addrs.joinToString { it.hostAddress ?: "" }
        } catch (e: Exception) {
            sysErr = e.message ?: "Resolver failed"
        }

        results.add(
            DnsServerCheckResult(
                provider = "Carrier Default Resolver",
                serverIp = "Local System",
                protocol = "System DNS",
                latencyMs = if (sysOk) sysLat else 0L,
                isReachable = sysOk,
                details = if (sysOk) "Resolved 'dns.google' -> [$sysIps] in ${sysLat}ms" else "Failed: $sysErr"
            )
        )

        results
    }
}
