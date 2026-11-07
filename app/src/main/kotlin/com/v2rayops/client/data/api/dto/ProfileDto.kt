package com.v2rayops.client.data.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ProfileResponseDto(
    @Json(name = "user") val user: UserDto,
    @Json(name = "wallet") val wallet: WalletDto,
    @Json(name = "subscriptions") val subscriptions: List<SubscriptionPlanDto> = emptyList(),
    @Json(name = "trial") val trial: TrialDto? = null
)

@JsonClass(generateAdapter = true)
data class UserDto(
    @Json(name = "customer_code") val customerCode: String,
    @Json(name = "name") val name: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "status") val status: String = "active",
    @Json(name = "billing_mode") val billingMode: String? = "payg"
)

@JsonClass(generateAdapter = true)
data class WalletDto(
    @Json(name = "balance_lkr") val balanceLkr: Double? = 0.0
)

@JsonClass(generateAdapter = true)
data class SubscriptionPlanDto(
    @Json(name = "id") val id: Long,
    @Json(name = "status") val status: String,
    @Json(name = "start_date") val startDate: String? = null,
    @Json(name = "expiry_date") val expiryDate: String? = null,
    @Json(name = "plan_name") val planName: String? = null,
    @Json(name = "data_gb") val dataGb: Double? = 0.0
)

@JsonClass(generateAdapter = true)
data class TrialDto(
    @Json(name = "data_gb") val dataGb: Double? = 0.0,
    @Json(name = "duration_hours") val durationHours: Int? = 0,
    @Json(name = "status") val status: String = "inactive",
    @Json(name = "started_at") val startedAt: String? = null,
    @Json(name = "expires_at") val expiresAt: String? = null,
    @Json(name = "used_bytes") val usedBytes: Long? = 0
)
