package com.v2rayops.client.xray

import android.content.Context
import com.v2rayops.client.domain.model.TrafficStats
import kotlinx.coroutines.flow.StateFlow

sealed interface XrayState {
    data object Stopped : XrayState
    data object Starting : XrayState
    data class Running(val startedAtMs: Long) : XrayState
    data class Error(val error: String) : XrayState
    data class BinaryPending(val message: String) : XrayState
}

interface XrayEngine {
    val state: StateFlow<XrayState>
    val isBinaryAvailable: Boolean
    val version: String

    fun initialize(context: Context): Boolean
    fun start(configJson: String, tunFd: Int = 0): Result<Unit>
    fun stop(): Result<Unit>
    fun queryTrafficStats(): TrafficStats
    fun measureDelay(url: String = "https://cp.cloudflare.com/generate_204"): Long
}
