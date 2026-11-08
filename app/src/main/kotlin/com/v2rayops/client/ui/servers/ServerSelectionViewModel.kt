package com.v2rayops.client.ui.servers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.v2rayops.client.domain.model.VpnConfig
import com.v2rayops.client.domain.repository.AuthRepository
import com.v2rayops.client.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.v2rayops.client.network.ConnectionDiagnosticsManager
import javax.inject.Inject

@HiltViewModel
class ServerSelectionViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val authRepository: AuthRepository,
    private val diagnosticsManager: ConnectionDiagnosticsManager
) : ViewModel() {

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
    }

    fun testAllNodes() {
        viewModelScope.launch {
            _isTestingAll.value = true
            val currentConfigs = configs.value
            val newMap = _pingMap.value.toMutableMap()
            for (cfg in currentConfigs) {
                val ping = diagnosticsManager.measureEdgePing(cfg)
                newMap[cfg.id] = ping
                _pingMap.value = newMap.toMap()
            }
            _isTestingAll.value = false
        }
    }

    fun selectConfig(config: VpnConfig) {
        subscriptionRepository.selectConfig(config)
    }

    fun importConfig(rawUri: String): String? {
        return when (val res = subscriptionRepository.importManualConfig(rawUri)) {
            is com.v2rayops.client.core.result.AppResult.Success -> null
            is com.v2rayops.client.core.result.AppResult.Failure -> res.error.message
            else -> null
        }
    }

    fun deleteConfig(configId: Long) {
        subscriptionRepository.deleteConfig(configId)
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
