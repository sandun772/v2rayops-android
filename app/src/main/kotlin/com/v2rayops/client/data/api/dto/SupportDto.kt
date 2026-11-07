package com.v2rayops.client.data.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SupportResponseDto(
    @Json(name = "ok") val ok: Boolean = true,
    @Json(name = "contacts") val contacts: List<SupportContactDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SupportContactDto(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "roleTitle") val roleTitle: String? = null,
    @Json(name = "telegram") val telegram: String? = null,
    @Json(name = "telegramUrl") val telegramUrl: String? = null,
    @Json(name = "whatsapp") val whatsapp: String? = null,
    @Json(name = "whatsappUrl") val whatsappUrl: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "availability") val availability: String? = null
)
