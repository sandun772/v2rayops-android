package com.v2rayops.client.network

import com.v2rayops.client.core.dispatchers.AppDispatchers
import com.v2rayops.client.core.logger.AppLogger
import com.v2rayops.client.vpn.VpnManager
import com.v2rayops.client.xray.XrayEngine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

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
    val testedAtMs: Long = System.currentTimeMillis()
)

@Singleton
class ConnectionDiagnosticsManager @Inject constructor(
    private val xrayEngine: XrayEngine,
    private val vpnManager: VpnManager,
    private val dispatchers: AppDispatchers
) {
    companion object {
        private const val TAG = "ConnectionDiagnostics"
        private const val LOCAL_PROXY_PORT = 10809
        private const val V2RAYOPS_SERVER_IP = "172.236.134.215"
    }

    /**
     * Executes an end-to-end connection and IP diagnostic test.
     * When VPN is connected, test traffic routes through local proxy listener (127.0.0.1:10809).
     */
    suspend fun runDiagnosticTest(): ConnectionDiagnosticResult = withContext(dispatchers.io) {
        val isVpnConnected = vpnManager.connectionState.value.isConnected

        AppLogger.i(TAG, "Starting connection diagnostics (isVpnConnected=$isVpnConnected)")

        // 1. Measure Ping / Delay with 2.5s safety timeout so it never blocks UI
        var pingMs = -1L
        if (isVpnConnected) {
            val measured = withTimeoutOrNull(2500L) {
                xrayEngine.measureDelay("https://cp.cloudflare.com/generate_204")
            } ?: -1L
            if (measured > 0) {
                pingMs = measured
            }
        }

        // 2. Build OkHttpClient (proxied through local core HTTP inbound if connected, direct if disconnected)
        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .writeTimeout(4, TimeUnit.SECONDS)

        if (isVpnConnected) {
            clientBuilder.proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", LOCAL_PROXY_PORT)))
        }

        val client = clientBuilder.build()

        try {
            val reqStart = System.currentTimeMillis()
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
                        }
                    }
                } catch (t: Throwable) {
                    AppLogger.d(TAG, "ipwho.is fallback check failed: ${t.message}")
                }
            }

            // Attempt 3 (Minimal IP fallback): https://api.ipify.org?format=json
            if (ip.isBlank()) {
                try {
                    val req3 = Request.Builder()
                        .url("https://api.ipify.org?format=json")
                        .header("User-Agent", "V2RayOps-Client/1.0")
                        .build()
                    val res3 = client.newCall(req3).execute()
                    if (res3.isSuccessful) {
                        val body3 = res3.body?.string() ?: ""
                        val json3 = JSONObject(body3)
                        ip = json3.optString("ip", "")
                    }
                } catch (t: Throwable) {
                    AppLogger.d(TAG, "api.ipify.org fallback check failed: ${t.message}")
                }
            }

            val latency = System.currentTimeMillis() - reqStart
            if (pingMs <= 0) {
                pingMs = latency
            }

            if (ip.isBlank()) {
                return@withContext ConnectionDiagnosticResult(
                    status = DiagnosticStatus.FAILED,
                    pingMs = pingMs,
                    message = "Could not resolve exit IP via tunnel"
                )
            }

            val isServerExit = ip == V2RAYOPS_SERVER_IP ||
                    countryCode.equals("SG", ignoreCase = true) ||
                    isp.contains("Akamai", ignoreCase = true) ||
                    isp.contains("Linode", ignoreCase = true)

            AppLogger.i(TAG, "Diagnostic success: IP=$ip, Country=$country, ISP=$isp, Ping=${pingMs}ms, isExit=$isServerExit")

            ConnectionDiagnosticResult(
                status = DiagnosticStatus.SUCCESS,
                pingMs = pingMs,
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
                }
            )
        } catch (e: Exception) {
            AppLogger.w(TAG, "Diagnostic test failed: ${e.message}")
            ConnectionDiagnosticResult(
                status = DiagnosticStatus.FAILED,
                pingMs = pingMs,
                message = e.message ?: "Failed to reach diagnostic endpoint"
            )
        }
    }
}
