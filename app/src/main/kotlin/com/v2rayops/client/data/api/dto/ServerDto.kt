package com.v2rayops.client.data.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ServersResponseDto(
    @Json(name = "servers") val servers: List<ServerNodeDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ServerNodeDto(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "hostname") val hostname: String,
    @Json(name = "port") val port: Int? = 443
)
