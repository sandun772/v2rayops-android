package com.v2rayops.client.data.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HeartbeatRequestDto(
    @Json(name = "code") val code: String,
    @Json(name = "uploadBytes") val uploadBytes: Long,
    @Json(name = "downloadBytes") val downloadBytes: Long,
    @Json(name = "deviceName") val deviceName: String? = null,
    @Json(name = "currentSettingsVersion") val currentSettingsVersion: Long? = null
)

@JsonClass(generateAdapter = true)
data class HeartbeatResponseDto(
    @Json(name = "ok") val ok: Boolean = true,
    @Json(name = "recorded") val recorded: String? = null,
    @Json(name = "error") val error: String? = null,
    @Json(name = "remoteSettingsVersion") val remoteSettingsVersion: Long? = null,
    @Json(name = "hasPendingSettingsUpdate") val hasPendingSettingsUpdate: Boolean = false
)
