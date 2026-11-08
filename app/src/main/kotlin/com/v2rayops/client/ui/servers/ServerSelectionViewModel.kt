package com.v2rayops.client.ui.servers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.v2rayops.client.domain.model.ConnectionState
import com.v2rayops.client.domain.model.VpnConfig
import com.v2rayops.client.domain.repository.AuthRepository
import com.v2rayops.client.domain.repository.SubscriptionRepository
import com.v2rayops.client.network.ConnectionDiagnosticsManager
import com.v2rayops.client.network.FineTuneState
import com.v2rayops.client.network.OneClickFineTuneManager
import com.v2rayops.client.vpn.VpnManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServerSelectionViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val authRepository: AuthRepository,
    private val diagnosticsManager: ConnectionDiagnosticsManager,
    private val vpnManager: VpnManager,
    private val fineTuneManager: OneClickFineTuneManager
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = vpnManager.connectionState
    val fineTuneState: StateFlow<FineTuneState> = fineTuneManager.state

    val configs: StateFlow<List<VpnConfig>> = subscriptionRepository.observeConfigs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedConfig: StateFlow<VpnConfig?> = subscriptionRepository.observeSelectedConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _pingMap = MutableStateFlow<Map<Long, Long>>(emptyMap())
    val pingMap: StateFlow<Map<Long, Long>> = _pingMap.asStateFlow()

    private val _isTestingAll = MutableStateFlow(false)
    val isTestingAll: StateFlow<Boolean> = _isTestingAll.asStateFlow()

    init {
        refreshConfigs()
        observeConfigsForTesting()
    }

    private fun observeConfigsForTesting() {
        viewModelScope.launch {
            configs.collect { list ->
                if (list.isNotEmpty() && _pingMap.value.isEmpty()) {
                    testAllNodes()
                }
            }
        }
    }

    fun startOneClickFineTune() {
        val target = selectedConfig.value ?: configs.value.firstOrNull()
        fineTuneManager.startFineTune(target)
    }

    fun stopOneClickFineTune() {
        fineTuneManager.stopFineTune()
    }

    fun dismissFineTuneReport() {
        fineTuneManager.dismissReport()
    }

    /**
     * Executes real, parallel TCP connect latency measurements across all assigned server profiles.
     */
    fun testAllNodes() {
        if (_isTestingAll.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isTestingAll.value = true
            val currentConfigs = configs.value
            val results = currentConfigs.map { cfg ->
                async {
                    val ping = diagnosticsManager.measureEdgePing(cfg)
                    cfg.id to ping
                }
            }.awaitAll()
            _pingMap.value = results.toMap()
            _isTestingAll.value = false
        }
    }

    fun selectConfig(config: VpnConfig) {
        subscriptionRepository.selectConfig(config)
    }

    fun refreshConfigs() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val code = authRepository.getSavedCustomerCode()
            if (!code.isNullOrBlank()) {
                subscriptionRepository.syncConfigs(code)
            }
            _isRefreshing.value = false
        }
    }
}
