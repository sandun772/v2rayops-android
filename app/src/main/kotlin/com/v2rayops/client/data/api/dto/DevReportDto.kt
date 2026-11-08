package com.v2rayops.client.data.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DevReportDeviceInfoDto(
    @Json(name = "manufacturer") val manufacturer: String,
    @Json(name = "model") val model: String,
    @Json(name = "product") val product: String,
    @Json(name = "androidVersion") val androidVersion: String,
    @Json(name = "sdkInt") val sdkInt: Int,
    @Json(name = "appVersion") val appVersion: String
)

@JsonClass(generateAdapter = true)
data class DevReportNetworkInfoDto(
    @Json(name = "type") val type: String,
    @Json(name = "isMetered") val isMetered: Boolean,
    @Json(name = "carrierName") val carrierName: String,
    @Json(name = "isWifi") val isWifi: Boolean,
    @Json(name = "isCellular") val isCellular: Boolean
)

@JsonClass(generateAdapter = true)
data class DevReportTrafficStatsDto(
    @Json(name = "uplinkBytes") val uplinkBytes: Long,
    @Json(name = "downlinkBytes") val downlinkBytes: Long,
    @Json(name = "uplinkFormatted") val uplinkFormatted: String,
    @Json(name = "downlinkFormatted") val downlinkFormatted: String,
    @Json(name = "uplinkSpeedBps") val uplinkSpeedBps: Long,
    @Json(name = "downlinkSpeedBps") val downlinkSpeedBps: Long
)

@JsonClass(generateAdapter = true)
data class DevReportVpnStateDto(
    @Json(name = "status") val status: String,
    @Json(name = "connectedSinceMs") val connectedSinceMs: Long,
    @Json(name = "durationSeconds") val durationSeconds: Long,
    @Json(name = "uptimeFormatted") val uptimeFormatted: String,
    @Json(name = "trafficStats") val trafficStats: DevReportTrafficStatsDto? = null
)

@JsonClass(generateAdapter = true)
data class DevReportActiveConfigDto(
    @Json(name = "id") val id: Long,
    @Json(name = "protocol") val protocol: String,
    @Json(name = "name") val name: String,
    @Json(name = "remark") val remark: String,
    @Json(name = "host") val host: String,
    @Json(name = "port") val port: Int,
    @Json(name = "sni") val sni: String,
    @Json(name = "wsHost") val wsHost: String,
    @Json(name = "wsPath") val wsPath: String,
    @Json(name = "isAddrFronting") val isAddrFronting: Boolean,
    @Json(name = "effectiveAddr") val effectiveAddr: String,
    @Json(name = "effectiveSni") val effectiveSni: String,
    @Json(name = "smartQoSEnabled") val smartQoSEnabled: Boolean,
    @Json(name = "dpiDelayFixEnabled") val dpiDelayFixEnabled: Boolean,
    @Json(name = "customDns") val customDns: String,
    @Json(name = "routingMode") val routingMode: String
)

@JsonClass(generateAdapter = true)
data class DevReportProbeReportDto(
    @Json(name = "tcpSuccess") val tcpSuccess: Boolean,
    @Json(name = "tlsSuccess") val tlsSuccess: Boolean,
    @Json(name = "wsSuccess") val wsSuccess: Boolean,
    @Json(name = "tcpLatencyMs") val tcpLatencyMs: Long,
    @Json(name = "tlsLatencyMs") val tlsLatencyMs: Long,
    @Json(name = "dnsLatencyMs") val dnsLatencyMs: Long,
    @Json(name = "error") val error: String,
    @Json(name = "summary") val summary: String
)

@JsonClass(generateAdapter = true)
data class DevReportDiagnosticsDto(
    @Json(name = "probeReport") val probeReport: DevReportProbeReportDto? = null,
    @Json(name = "qosMode") val qosMode: String? = null,
    @Json(name = "recentRouteCandidate") val recentRouteCandidate: String? = null
)

@JsonClass(generateAdapter = true)
data class DevReportLogsDto(
    @Json(name = "inAppLogs") val inAppLogs: String,
    @Json(name = "logcat") val logcat: String
)

@JsonClass(generateAdapter = true)
data class DevReportRequestDto(
    @Json(name = "customerCode") val customerCode: String,
    @Json(name = "deviceInfo") val deviceInfo: DevReportDeviceInfoDto,
    @Json(name = "networkInfo") val networkInfo: DevReportNetworkInfoDto,
    @Json(name = "vpnState") val vpnState: DevReportVpnStateDto,
    @Json(name = "activeConfig") val activeConfig: DevReportActiveConfigDto,
    @Json(name = "diagnostics") val diagnostics: DevReportDiagnosticsDto,
    @Json(name = "logs") val logs: DevReportLogsDto,
    @Json(name = "userNote") val userNote: String = ""
)

@JsonClass(generateAdapter = true)
data class DevReportResponseDto(
    @Json(name = "ok") val ok: Boolean = true,
    @Json(name = "reportId") val reportId: String? = null,
    @Json(name = "savedAt") val savedAt: String? = null,
    @Json(name = "downloadUrl") val downloadUrl: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "error") val error: String? = null
)
