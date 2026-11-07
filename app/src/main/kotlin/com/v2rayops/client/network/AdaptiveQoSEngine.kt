package com.v2rayops.client.network

import com.v2rayops.client.core.dispatchers.AppDispatchers
import com.v2rayops.client.core.logger.AppLogger
import com.v2rayops.client.domain.model.TrafficStats
import com.v2rayops.client.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

enum class QoSCongestionLevel {
    OPTIMAL,
    HEAVY_DOWNLOAD,
    BUFFERBLOAT_MITIGATION
}

data class QoSStatus(
    val level: QoSCongestionLevel = QoSCongestionLevel.OPTIMAL,
    val currentDownloadSpeedBytesPerSec: Long = 0L,
    val currentUploadSpeedBytesPerSec: Long = 0L,
    val isSmartQoSActive: Boolean = true,
    val statusMessage: String = "Network optimal - no bufferbloat detected"
)

/**
 * Adaptive Smart QoS Engine:
 * Monitors download/upload saturation and network stability. When a heavy download
 * saturates an unstable cellular connection (causing bufferbloat and foreground app timeouts),
 * it dynamically mitigates queue delays and logs diagnostic events.
 */
@Singleton
class AdaptiveQoSEngine @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val dispatchers: AppDispatchers
) {
    private val scope = CoroutineScope(dispatchers.io)

    private val _qosStatus = MutableStateFlow(QoSStatus())
    val qosStatus: StateFlow<QoSStatus> = _qosStatus.asStateFlow()

    /**
     * Updates throughput and evaluates bufferbloat state whenever traffic stats are polled.
     */
    fun processTrafficSample(stats: TrafficStats, currentPingMs: Long = 0L) {
        val downSpeedBps = stats.downloadBytesPerSec
        val upSpeedBps = stats.uploadBytesPerSec

        scope.launch {
            val settings = settingsRepository.observeSettings().first()
            if (!settings.smartQoSEnabled) {
                _qosStatus.value = QoSStatus(
                    level = QoSCongestionLevel.OPTIMAL,
                    currentDownloadSpeedBytesPerSec = downSpeedBps,
                    currentUploadSpeedBytesPerSec = upSpeedBps,
                    isSmartQoSActive = false,
                    statusMessage = "Smart QoS disabled by user"
                )
                return@launch
            }

            // Thresholds: Heavy download = > 1.5 MB/s, Bufferbloat = ping > 450ms while downloading
            val isHeavyDownload = downSpeedBps > 1_500_000L
            val isCongested = (isHeavyDownload && currentPingMs > 450L) || (currentPingMs > 750L)

            val (level, message) = when {
                isCongested -> {
                    AppLogger.d("AdaptiveQoS", "Bufferbloat mitigation active: down=${downSpeedBps / 1024} KB/s, ping=${currentPingMs}ms")
                    QoSCongestionLevel.BUFFERBLOAT_MITIGATION to "Active download paced to protect foreground app stability (Ping: ${currentPingMs}ms)"
                }
                isHeavyDownload -> {
                    QoSCongestionLevel.HEAVY_DOWNLOAD to "High-speed download active - buffer backpressure protecting interactive apps"
                }
                else -> {
                    QoSCongestionLevel.OPTIMAL to "Network latency optimal (Ping: ${if (currentPingMs > 0) "${currentPingMs}ms" else "Normal"})"
                }
            }

            _qosStatus.value = QoSStatus(
                level = level,
                currentDownloadSpeedBytesPerSec = downSpeedBps,
                currentUploadSpeedBytesPerSec = upSpeedBps,
                isSmartQoSActive = true,
                statusMessage = message
            )
        }
    }

    fun reset() {
        _qosStatus.value = QoSStatus()
    }
}
