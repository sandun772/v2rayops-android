package com.v2rayops.client.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.v2rayops.client.core.logger.AppLogger
import com.v2rayops.client.core.result.AppResult
import com.v2rayops.client.domain.repository.SubscriptionRepository
import com.v2rayops.client.network.ConnectionDiagnosticsManager
import com.v2rayops.client.network.DevReportManager
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
    private val subscriptionRepository: SubscriptionRepository,
    private val devReportManager: DevReportManager
) : ViewModel() {

    private val _isProbing = MutableStateFlow(false)
    val isProbing: StateFlow<Boolean> = _isProbing.asStateFlow()

    private val _lastProbeReport = MutableStateFlow<NodeProbeReport?>(null)
    val lastProbeReport: StateFlow<NodeProbeReport?> = _lastProbeReport.asStateFlow()

    private val _isUploadingReport = MutableStateFlow(false)
    val isUploadingReport: StateFlow<Boolean> = _isUploadingReport.asStateFlow()

    private val _uploadResult = MutableStateFlow<String?>(null)
    val uploadResult: StateFlow<String?> = _uploadResult.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError: StateFlow<String?> = _uploadError.asStateFlow()

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

    fun uploadDevReport(userNote: String = "") {
        if (_isUploadingReport.value) return
        viewModelScope.launch {
            _isUploadingReport.value = true
            _uploadResult.value = null
            _uploadError.value = null

            when (val res = devReportManager.collectAndUploadReport(userNote)) {
                is AppResult.Success -> {
                    _uploadResult.value = "Dev report successfully uploaded to VPS!\nReport ID: ${res.data.reportId}"
                    AppLogger.i("DevLogs", "Uploaded dev report: ${res.data.reportId}")
                }
                is AppResult.Failure -> {
                    _uploadError.value = res.error.message
                    AppLogger.e("DevLogs", "Dev report upload failed: ${res.error.message}")
                }
                is AppResult.Loading -> Unit
            }
            _isUploadingReport.value = false
        }
    }

    fun clearUploadFeedback() {
        _uploadResult.value = null
        _uploadError.value = null
    }

    fun importConfig(rawUri: String): String? {
        return when (val res = subscriptionRepository.importManualConfig(rawUri)) {
            is AppResult.Success -> null
            is AppResult.Failure -> res.error.message
            else -> null
        }
    }
}
