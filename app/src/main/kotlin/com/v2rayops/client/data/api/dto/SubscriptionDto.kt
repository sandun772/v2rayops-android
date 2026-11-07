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
    @Json(name = "protocol") val protocol: String,
    @Json(name = "config_uri") val configUri: String,
    @Json(name = "server_id") val serverId: Long,
    @Json(name = "status") val status: String
)
