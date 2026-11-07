package com.v2rayops.client.domain.model

data class UserProfile(
    val customerCode: String,
    val name: String,
    val email: String,
    val status: String,
    val billingMode: String,
    val walletBalanceLkr: Double,
    val activeSubscriptions: List<UserSubscription>,
    val trial: TrialInfo?
)

data class UserSubscription(
    val id: Long,
    val planName: String,
    val status: String,
    val dataGb: Double,
    val startDate: String?,
    val expiryDate: String?
)

data class TrialInfo(
    val dataGb: Double,
    val durationHours: Int,
    val status: String,
    val startedAt: String?,
    val expiresAt: String?,
    val usedBytes: Long
) {
    val totalBytes: Long get() = (dataGb * 1024 * 1024 * 1024).toLong()
    val isExpired: Boolean get() = status != "active"
}
