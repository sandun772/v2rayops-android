package com.v2rayops.client.network

import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import com.v2rayops.client.BuildConfig
import com.v2rayops.client.core.dispatchers.AppDispatchers
import com.v2rayops.client.core.error.AppError
import com.v2rayops.client.core.logger.AppLogger
import com.v2rayops.client.core.result.AppResult
import com.v2rayops.client.data.api.V2RayOpsClientApi
import com.v2rayops.client.data.api.dto.DevReportActiveConfigDto
import com.v2rayops.client.data.api.dto.DevReportDeviceInfoDto
import com.v2rayops.client.data.api.dto.DevReportDiagnosticsDto
import com.v2rayops.client.data.api.dto.DevReportLogsDto
import com.v2rayops.client.data.api.dto.DevReportNetworkInfoDto
import com.v2rayops.client.data.api.dto.DevReportProbeReportDto
import com.v2rayops.client.data.api.dto.DevReportRequestDto
import com.v2rayops.client.data.api.dto.DevReportResponseDto
import com.v2rayops.client.data.api.dto.DevReportTrafficStatsDto
import com.v2rayops.client.data.api.dto.DevReportVpnStateDto
import com.v2rayops.client.domain.model.ConnectionState
import com.v2rayops.client.domain.model.VpnConfig
import com.v2rayops.client.domain.repository.AuthRepository
import com.v2rayops.client.domain.repository.SettingsRepository
import com.v2rayops.client.domain.repository.SubscriptionRepository
import com.v2rayops.client.vpn.VpnManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DevReportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: V2RayOpsClientApi,
    private val authRepository: AuthRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val settingsRepository: SettingsRepository,
    private val vpnManager: VpnManager,
    private val diagnosticsManager: ConnectionDiagnosticsManager,
    private val networkMonitor: NetworkMonitor,
    private val dispatchers: AppDispatchers
) {
    companion object {
        private const val TAG = "DevReportManager"
    }

    suspend fun collectAndUploadReport(userNote: String = ""): AppResult<DevReportResponseDto> =
        withContext(dispatchers.io) {
            try {
                AppLogger.i(TAG, "Compiling full live telemetry report...")

                // 1. Customer Code
                val customerCode = authRepository.getSavedCustomerCode() ?: "anonymous"

                // 2. Device Info
                val deviceInfo = DevReportDeviceInfoDto(
                    manufacturer = Build.MANUFACTURER ?: "Unknown",
                    model = Build.MODEL ?: "Device",
                    product = Build.PRODUCT ?: "Android",
                    androidVersion = Build.VERSION.RELEASE ?: "N/A",
                    sdkInt = Build.VERSION.SDK_INT,
                    appVersion = BuildConfig.VERSION_NAME
                )

                // 3. Network Info
                val netStatus = networkMonitor.getCurrentStatus()
                val telephonyManager = try {
                    context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                } catch (_: Exception) { null }
                val carrierName = telephonyManager?.networkOperatorName?.ifBlank { "Unknown" } ?: "Unknown"

                val networkInfo = DevReportNetworkInfoDto(
                    type = netStatus.type.name,
                    isMetered = netStatus.isMetered,
                    carrierName = carrierName,
                    isWifi = netStatus.type == NetworkType.WIFI,
                    isCellular = netStatus.type == NetworkType.CELLULAR
                )

                // 4. VPN State & Traffic Stats
                val connState = vpnManager.connectionState.value
                val trafficStats = vpnManager.trafficStats.value
                val now = System.currentTimeMillis()

                val (statusStr, connectedSince, durationSec, uptimeFmt) = when (connState) {
                    is ConnectionState.Connected -> {
                        val duration = ((now - connState.connectedSinceMs) / 1000).coerceAtLeast(0)
                        val h = duration / 3600
                        val m = (duration % 3600) / 60
                        val s = duration % 60
                        Quad(
                            "CONNECTED",
                            connState.connectedSinceMs,
                            duration,
                            String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
                        )
                    }
                    is ConnectionState.Connecting -> Quad("CONNECTING", now, 0L, "00:00:00")
                    is ConnectionState.Disconnecting -> Quad("DISCONNECTING", 0L, 0L, "00:00:00")
                    is ConnectionState.Error -> Quad("ERROR: ${connState.message}", 0L, 0L, "00:00:00")
                    is ConnectionState.Unavailable -> Quad("UNAVAILABLE: ${connState.reason}", 0L, 0L, "00:00:00")
                    ConnectionState.Disconnected -> Quad("DISCONNECTED", 0L, 0L, "00:00:00")
                }

                val trafficDto = DevReportTrafficStatsDto(
                    uplinkBytes = trafficStats.totalUploadBytes,
                    downlinkBytes = trafficStats.totalDownloadBytes,
                    uplinkFormatted = formatBytes(trafficStats.totalUploadBytes),
                    downlinkFormatted = formatBytes(trafficStats.totalDownloadBytes),
                    uplinkSpeedBps = trafficStats.uploadBytesPerSec,
                    downlinkSpeedBps = trafficStats.downloadBytesPerSec
                )

                val vpnStateDto = DevReportVpnStateDto(
                    status = statusStr,
                    connectedSinceMs = connectedSince,
                    durationSeconds = durationSec,
                    uptimeFormatted = uptimeFmt,
                    trafficStats = trafficDto
                )

                // 5. Active Configuration & Settings
                val activeConfig: VpnConfig? = subscriptionRepository.observeSelectedConfig().first()
                    ?: subscriptionRepository.observeConfigs().first().firstOrNull()

                val settings = settingsRepository.observeSettings().first()

                val activeConfigDto = if (activeConfig != null) {
                    DevReportActiveConfigDto(
                        id = activeConfig.id,
                        protocol = activeConfig.protocol,
                        name = activeConfig.displayName,
                        remark = activeConfig.remark,
                        host = activeConfig.host,
                        port = activeConfig.port,
                        sni = activeConfig.activeSni,
                        wsHost = activeConfig.wsHost,
                        wsPath = activeConfig.wsPath,
                        isAddrFronting = activeConfig.isAddrFronting,
                        effectiveAddr = activeConfig.effectiveAddr,
                        effectiveSni = activeConfig.effectiveSni,
                        smartQoSEnabled = settings.smartQoSEnabled,
                        dpiDelayFixEnabled = settings.dpiDelayFixEnabled,
                        customDns = settings.customDns,
                        routingMode = settings.routingMode.name
                    )
                } else {
                    DevReportActiveConfigDto(
                        id = 0,
                        protocol = "none",
                        name = "No Config Selected",
                        remark = "",
                        host = "",
                        port = 0,
                        sni = "",
                        wsHost = "",
                        wsPath = "",
                        isAddrFronting = false,
                        effectiveAddr = "N/A",
                        effectiveSni = "N/A",
                        smartQoSEnabled = settings.smartQoSEnabled,
                        dpiDelayFixEnabled = settings.dpiDelayFixEnabled,
                        customDns = settings.customDns,
                        routingMode = settings.routingMode.name
                    )
                }

                // 6. Pre-flight Node Diagnostics Probe
                val probeReportDto = if (activeConfig != null) {
                    try {
                        val report = diagnosticsManager.probeTargetNode(activeConfig)
                        DevReportProbeReportDto(
                            tcpSuccess = report.tcpSuccess,
                            tlsSuccess = report.tlsSuccess,
                            wsSuccess = report.wsSuccess,
                            tcpLatencyMs = report.tcpLatencyMs,
                            tlsLatencyMs = report.tlsLatencyMs,
                            dnsLatencyMs = report.dnsLatencyMs,
                            error = report.tcpError.ifBlank { report.tlsError }.ifBlank { report.wsError },
                            summary = report.summary
                        )
                    } catch (e: Exception) {
                        DevReportProbeReportDto(
                            tcpSuccess = false,
                            tlsSuccess = false,
                            wsSuccess = false,
                            tcpLatencyMs = 0L,
                            tlsLatencyMs = 0L,
                            dnsLatencyMs = 0L,
                            error = e.message ?: "Probe failed",
                            summary = "Probe error: ${e.message}"
                        )
                    }
                } else null

                val qosMode = vpnManager.qosStatus.value.level.name
                val diagnosticsDto = DevReportDiagnosticsDto(
                    probeReport = probeReportDto,
                    qosMode = qosMode,
                    recentRouteCandidate = activeConfig?.effectiveSni
                )

                // 7. Developer In-App Logs & Recent Logcat
                val inAppLogs = AppLogger.getFormattedLogs()
                val logcat = readRecentLogcat(150)

                val logsDto = DevReportLogsDto(
                    inAppLogs = inAppLogs,
                    logcat = logcat
                )

                val requestDto = DevReportRequestDto(
                    customerCode = customerCode,
                    deviceInfo = deviceInfo,
                    networkInfo = networkInfo,
                    vpnState = vpnStateDto,
                    activeConfig = activeConfigDto,
                    diagnostics = diagnosticsDto,
                    logs = logsDto,
                    userNote = userNote
                )

                AppLogger.i(TAG, "Uploading dev report to VPS API (/api/client/dev-report)...")
                val response = api.uploadDevReport(requestDto)

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    AppLogger.i(TAG, "Report uploaded successfully! Report ID: ${body.reportId}")
                    AppResult.Success(body)
                } else {
                    val errMsg = "HTTP ${response.code()}: ${response.errorBody()?.string() ?: response.message()}"
                    AppLogger.e(TAG, "Failed to upload dev report to VPS: $errMsg")
                    AppResult.Failure(AppError.Server(response.code(), errMsg))
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error generating or uploading dev report", e)
                AppResult.Failure(AppError.Network(e.message ?: "Network error during report upload"))
            }
        }

    private fun readRecentLogcat(lines: Int): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-v", "time", "-t", lines.toString()))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.appendLine(line)
            }
            process.waitFor()
            sb.toString()
        } catch (t: Throwable) {
            "Could not read logcat: ${t.message}"
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
        val pre = "KMGTPE"[exp - 1]
        return String.format(Locale.US, "%.1f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
