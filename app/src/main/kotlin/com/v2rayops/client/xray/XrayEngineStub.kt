package com.v2rayops.client.xray

import android.content.Context
import com.v2rayops.client.core.logger.AppLogger
import com.v2rayops.client.domain.model.TrafficStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Foundation Phase Stub for Xray Core Engine.
 *
 * Implements the full XrayEngine contract without faking an active tunnel.
 * Honestly reports BinaryPending until native libxray.so JNI bindings are compiled in Phase 2.
 */
@Singleton
class XrayEngineStub @Inject constructor() : XrayEngine {

    private val _state = MutableStateFlow<XrayState>(
        XrayState.BinaryPending("Xray Core binary integration scheduled for Phase 2")
    )
    override val state: StateFlow<XrayState> = _state.asStateFlow()

    override val isBinaryAvailable: Boolean = false

    override fun initialize(context: Context): Boolean {
        AppLogger.i("XrayEngineStub", "Initialized Xray Engine abstraction (binary compilation pending)")
        return true
    }

    override fun start(configJson: String): Result<Unit> {
        AppLogger.w("XrayEngineStub", "Attempted to start Xray Engine before native binary was linked")
        _state.value = XrayState.BinaryPending("Xray Core binary integration scheduled for Phase 2")
        return Result.failure(
            IllegalStateException("Xray Core native engine binary (libxray.so) will be linked in Phase 2.")
        )
    }

    override fun stop(): Result<Unit> {
        _state.value = XrayState.Stopped
        return Result.success(Unit)
    }

    override fun queryTrafficStats(): TrafficStats {
        return TrafficStats(
            uploadBytesPerSec = 0,
            downloadBytesPerSec = 0,
            totalUploadBytes = 0,
            totalDownloadBytes = 0
        )
    }
}
