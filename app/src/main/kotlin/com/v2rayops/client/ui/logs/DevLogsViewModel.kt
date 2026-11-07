package com.v2rayops.client.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.v2rayops.client.core.logger.AppLogger
import com.v2rayops.client.domain.repository.SubscriptionRepository
import com.v2rayops.client.network.ConnectionDiagnosticsManager
import com.v2rayops.client.network.NodeProbeReport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DevLogsViewModel @Inject constructor(
    private val diagnosticsManager: ConnectionDiagnosticsManager,
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    private val _isProbing = MutableStateFlow(false)
    val isProbing: StateFlow<Boolean> = _isProbing.asStateFlow()

    private val _lastProbeReport = MutableStateFlow<NodeProbeReport?>(null)
    val lastProbeReport: StateFlow<NodeProbeReport?> = _lastProbeReport.asStateFlow()

    fun runProbe() {
        viewModelScope.launch {
            _isProbing.value = true
            val config = subscriptionRepository.observeSelectedConfig().first()
                ?: subscriptionRepository.observeConfigs().first().firstOrNull()

            if (config != null) {
                AppLogger.i("DevLogs", "Starting on-demand socket probe for node: ${config.displayName}")
                val report = diagnosticsManager.probeTargetNode(config)
                _lastProbeReport.value = report
                AppLogger.i("DevLogs", "Probe result: ${report.summary}")
            } else {
                AppLogger.w("DevLogs", "No config selected to probe")
            }
            _isProbing.value = false
        }
    }

    fun importConfig(rawUri: String): String? {
        return when (val res = subscriptionRepository.importManualConfig(rawUri)) {
            is com.v2rayops.client.core.result.AppResult.Success -> null
            is com.v2rayops.client.core.result.AppResult.Failure -> res.error.message
            else -> null
        }
    }
}
