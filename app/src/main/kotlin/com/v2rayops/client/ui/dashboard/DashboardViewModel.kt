package com.v2rayops.client.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.v2rayops.client.core.result.AppResult
import com.v2rayops.client.domain.model.ConnectionState
import com.v2rayops.client.domain.model.TrafficStats
import com.v2rayops.client.domain.model.UserProfile
import com.v2rayops.client.domain.model.VpnConfig
import com.v2rayops.client.domain.repository.AuthRepository
import com.v2rayops.client.domain.repository.SubscriptionRepository
import com.v2rayops.client.network.ConnectionDiagnosticResult
import com.v2rayops.client.network.ConnectionDiagnosticsManager
import com.v2rayops.client.network.DiagnosticStatus
import com.v2rayops.client.vpn.VpnManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val vpnManager: VpnManager,
    private val diagnosticsManager: ConnectionDiagnosticsManager
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = vpnManager.connectionState
    val trafficStats: StateFlow<TrafficStats> = vpnManager.trafficStats

    val selectedConfig: StateFlow<VpnConfig?> = subscriptionRepository.observeSelectedConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val configs: StateFlow<List<VpnConfig>> = subscriptionRepository.observeConfigs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _diagnosticResult = MutableStateFlow<ConnectionDiagnosticResult?>(null)
    val diagnosticResult: StateFlow<ConnectionDiagnosticResult?> = _diagnosticResult.asStateFlow()

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    init {
        refreshDashboard()
        observeConnectionForDiagnostics()
    }

    private fun observeConnectionForDiagnostics() {
        viewModelScope.launch {
            connectionState.collect { state ->
                if (state is ConnectionState.Connected) {
                    // Small delay to ensure tunnel socket is fully warmed up
                    delay(1200)
                    runConnectionTest()
                } else if (state is ConnectionState.Disconnected) {
                    _diagnosticResult.value = null
                }
            }
        }
    }

    fun runConnectionTest() {
        viewModelScope.launch {
            _isTestingConnection.value = true
            _diagnosticResult.value = _diagnosticResult.value?.copy(status = DiagnosticStatus.TESTING)
                ?: ConnectionDiagnosticResult(status = DiagnosticStatus.TESTING)
            val result = diagnosticsManager.runDiagnosticTest(selectedConfig.value)
            _diagnosticResult.value = result
            _isTestingConnection.value = false
        }
    }

    fun runNodeProbe() {
        viewModelScope.launch {
            val config = selectedConfig.value ?: return@launch
            _isTestingConnection.value = true
            val probe = diagnosticsManager.probeTargetNode(config)
            _diagnosticResult.value = ConnectionDiagnosticResult(
                status = if (probe.tcpSuccess) DiagnosticStatus.SUCCESS else DiagnosticStatus.FAILED,
                pingMs = if (probe.tlsLatencyMs > 0) probe.tlsLatencyMs else probe.tcpLatencyMs,
                message = probe.summary,
                debugDetails = "Endpoint: ${probe.endpoint} • SNI: ${probe.sni}"
            )
            _isTestingConnection.value = false
        }
    }

    fun refreshDashboard() {
        viewModelScope.launch {
            _isRefreshing.value = true
            when (val profileRes = authRepository.fetchProfile()) {
                is AppResult.Success -> _userProfile.value = profileRes.data
                else -> Unit
            }

            val code = authRepository.getSavedCustomerCode()
            if (!code.isNullOrBlank()) {
                subscriptionRepository.syncConfigs(code)
            }
            _isRefreshing.value = false
        }
    }

    fun selectConfig(config: VpnConfig) {
        subscriptionRepository.selectConfig(config)
    }

    fun importConfig(rawUri: String): String? {
        return when (val res = subscriptionRepository.importManualConfig(rawUri)) {
            is AppResult.Success -> null
            is AppResult.Failure -> res.error.message
            else -> null
        }
    }

    fun onConnectToggle() {
        val currentState = connectionState.value
        if (currentState.isConnected || currentState.isConnecting) {
            vpnManager.disconnect()
        } else {
            viewModelScope.launch {
                var config = selectedConfig.value
                if (config == null) {
                    val code = authRepository.getSavedCustomerCode()
                    if (!code.isNullOrBlank()) {
                        subscriptionRepository.syncConfigs(code)
                    }
                    config = selectedConfig.value ?: subscriptionRepository.observeConfigs().first().firstOrNull()
                }

                if (config != null) {
                    vpnManager.connect(config)
                }
            }
        }
    }
}
