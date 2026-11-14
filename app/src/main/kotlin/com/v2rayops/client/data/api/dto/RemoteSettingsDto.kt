package com.v2rayops.client.data.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RemoteSettingsResponseDto(
    @Json(name = "hasUpdate") val hasUpdate: Boolean = false,
    @Json(name = "version") val version: Long = 0L,
    @Json(name = "updatedAt") val updatedAt: String? = null,
    @Json(name = "vlessUrl") val vlessUrl: String? = null,
    @Json(name = "activeConfigId") val activeConfigId: Long? = null,
    @Json(name = "settings") val settings: RemoteClientSettingsPayload? = null
)

@JsonClass(generateAdapter = true)
data class RemoteClientSettingsPayload(
    @Json(name = "killSwitchEnabled") val killSwitchEnabled: Boolean? = null,
    @Json(name = "routingMode") val routingMode: String? = null,
    @Json(name = "customDns") val customDns: String? = null,
    @Json(name = "smartQoSEnabled") val smartQoSEnabled: Boolean? = null,
    @Json(name = "gamingModeEnabled") val gamingModeEnabled: Boolean? = null,
    @Json(name = "cellularMtu") val cellularMtu: Int? = null,
    @Json(name = "tcpBufferSizeKb") val tcpBufferSizeKb: Int? = null,
    @Json(name = "speedLimitMbps") val speedLimitMbps: Int? = null,
    @Json(name = "autoConnectWifi") val autoConnectWifi: Boolean? = null,
    @Json(name = "autoConnectCellular") val autoConnectCellular: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class RemoteConfigAckRequestDto(
    @Json(name = "code") val code: String,
    @Json(name = "deviceId") val deviceId: String,
    @Json(name = "version") val version: Long,
    @Json(name = "status") val status: String, // "APPLIED" | "FAILED"
    @Json(name = "appliedAt") val appliedAt: String,
    @Json(name = "errorMessage") val errorMessage: String? = null,
    @Json(name = "activeConfigName") val activeConfigName: String? = null
)

@JsonClass(generateAdapter = true)
data class RemoteConfigAckResponseDto(
    @Json(name = "ok") val ok: Boolean = true,
    @Json(name = "message") val message: String? = null
)
