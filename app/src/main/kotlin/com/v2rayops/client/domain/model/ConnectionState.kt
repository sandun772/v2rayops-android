package com.v2rayops.client.domain.model

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data class Connecting(val serverName: String) : ConnectionState
    data class Connected(
        val serverName: String,
        val serverEndpoint: String,
        val connectedSinceMs: Long,
        val durationFormatted: String = "00:00:00"
    ) : ConnectionState
    data object Disconnecting : ConnectionState
    data class Error(val message: String) : ConnectionState
    data class Unavailable(val reason: String) : ConnectionState

    val isConnected: Boolean get() = this is Connected
    val isConnecting: Boolean get() = this is Connecting
    val isDisconnected: Boolean get() = this is Disconnected
    val isBusy: Boolean get() = this is Connecting || this is Disconnecting
}

data class TrafficStats(
    val uploadBytesPerSec: Long = 0,
    val downloadBytesPerSec: Long = 0,
    val totalUploadBytes: Long = 0,
    val totalDownloadBytes: Long = 0
) {
    val totalBytes: Long get() = totalUploadBytes + totalDownloadBytes
}

enum class SmartHuntStage {
    IDLE,
    CHECKING_VPN,
    WAITING_AIRPLANE_ON,
    AIRPLANE_ON,
    STABILIZING_NETWORK,
    CONNECTING_VPN,
    SUCCESS
}

data class SmartHuntProgress(
    val stage: SmartHuntStage = SmartHuntStage.IDLE,
    val countdownSeconds: Int = 0,
    val currentCycle: Int = 0,
    val assignedIp: String? = null
) {
    val isActive: Boolean get() = stage != SmartHuntStage.IDLE && stage != SmartHuntStage.SUCCESS
}
