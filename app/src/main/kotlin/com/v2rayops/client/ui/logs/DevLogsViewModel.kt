package com.v2rayops.client.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.v2rayops.client.core.logger.AppLogger
import com.v2rayops.client.core.result.AppResult
import com.v2rayops.client.domain.repository.SubscriptionRepository
import com.v2rayops.client.network.ConnectionDiagnosticsManager
import com.v2rayops.client.network.DevReportManager
import com.v2rayops.client.network.DnsServerCheckResult
import com.v2rayops.client.network.NodeProbeReport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
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

    // --- Interactive Custom Socket Prober ---
    private val _probeTargetHost = MutableStateFlow("support.zoom.us")
    val probeTargetHost: StateFlow<String> = _probeTargetHost.asStateFlow()

    private val _probeTargetPort = MutableStateFlow(443)
    val probeTargetPort: StateFlow<Int> = _probeTargetPort.asStateFlow()

    private val _probeSni = MutableStateFlow("sg-v2rayops.vpnxhub.com")
    val probeSni: StateFlow<String> = _probeSni.asStateFlow()

    private val _probeWsHost = MutableStateFlow("sg-v2rayops.vpnxhub.com")
    val probeWsHost: StateFlow<String> = _probeWsHost.asStateFlow()

    private val _probeWsPath = MutableStateFlow("/v2rayops")
    val probeWsPath: StateFlow<String> = _probeWsPath.asStateFlow()

    private val _customProbeReport = MutableStateFlow<NodeProbeReport?>(null)
    val customProbeReport: StateFlow<NodeProbeReport?> = _customProbeReport.asStateFlow()

    private val _isCustomProbing = MutableStateFlow(false)
    val isCustomProbing: StateFlow<Boolean> = _isCustomProbing.asStateFlow()

    // --- DNS Multi-Resolver Inspector ---
    private val _dnsCheckResults = MutableStateFlow<List<DnsServerCheckResult>>(emptyList())
    val dnsCheckResults: StateFlow<List<DnsServerCheckResult>> = _dnsCheckResults.asStateFlow()

    private val _isInspectingDns = MutableStateFlow(false)
    val isInspectingDns: StateFlow<Boolean> = _isInspectingDns.asStateFlow()

    init {
        // Automatically run initial DNS inspector on load
        runDnsInspection()
    }

    fun setProbeTargetHost(host: String) { _probeTargetHost.value = host }
    fun setProbeTargetPort(port: Int) { _probeTargetPort.value = port }
    fun setProbeSni(sni: String) { _probeSni.value = sni }
    fun setProbeWsHost(wsHost: String) { _probeWsHost.value = wsHost }
    fun setProbeWsPath(wsPath: String) { _probeWsPath.value = wsPath }

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

    fun runCustomProbe() {
        if (_isCustomProbing.value) return
        viewModelScope.launch {
            _isCustomProbing.value = true
            try {
                val report = diagnosticsManager.probeCustomEndpoint(
                    host = _probeTargetHost.value,
                    port = _probeTargetPort.value,
                    sni = _probeSni.value,
                    wsHost = _probeWsHost.value,
                    wsPath = _probeWsPath.value,
                    usePureCarrier = true
                )
                _customProbeReport.value = report
                AppLogger.i("DevLogs", "Custom Probe: ${report.summary}")
            } catch (ce: CancellationException) {
                // Ignore
            } catch (e: Exception) {
                AppLogger.e("DevLogs", "Custom probe error: ${e.message}")
            } finally {
                _isCustomProbing.value = false
            }
        }
    }

    fun runDnsInspection() {
        if (_isInspectingDns.value) return
        viewModelScope.launch {
            _isInspectingDns.value = true
            try {
                val checks = diagnosticsManager.inspectDnsResolvers(usePureCarrier = true)
                _dnsCheckResults.value = checks
                AppLogger.i("DevLogs", "DNS Inspection completed (${checks.size} checks)")
            } catch (ce: CancellationException) {
                // Ignore
            } catch (e: Exception) {
                AppLogger.e("DevLogs", "DNS Inspection failed: ${e.message}")
            } finally {
                _isInspectingDns.value = false
            }
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
