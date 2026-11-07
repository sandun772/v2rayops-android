package com.v2rayops.client.data.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SubscriptionResponseDto(
    @Json(name = "customer_code") val customerCode: String,
    @Json(name = "count") val count: Int = 0,
    @Json(name = "configs") val configs: List<VpnConfigDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class VpnConfigDto(
    @Json(name = "id") val id: Long,
    @Json(name = "protocol") val protocol: String = "vless",
    @Json(name = "config_name") val configName: String? = null,
    @Json(name = "config_uri") val configUri: String? = null,
    @Json(name = "config_data") val configData: String? = null,
    @Json(name = "server_id") val serverId: Long = 0,
    @Json(name = "status") val status: String = "active",
    @Json(name = "expires_at") val expiresAt: String? = null,
    @Json(name = "server_name") val serverName: String? = null,
    @Json(name = "server_country") val serverCountry: String? = null,
    @Json(name = "server_flag") val serverFlag: String? = null
)
