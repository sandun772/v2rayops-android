package com.v2rayops.client.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.v2rayops.client.core.result.AppResult
import com.v2rayops.client.domain.model.ConnectionState
import com.v2rayops.client.domain.model.ServerNode
import com.v2rayops.client.domain.model.TrafficStats
import com.v2rayops.client.domain.model.UserProfile
import com.v2rayops.client.domain.repository.AuthRepository
import com.v2rayops.client.domain.repository.ServerRepository
import com.v2rayops.client.domain.repository.SubscriptionRepository
import com.v2rayops.client.vpn.VpnManager
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val serverRepository: ServerRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val vpnManager: VpnManager
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = vpnManager.connectionState
    val trafficStats: StateFlow<TrafficStats> = vpnManager.trafficStats

    val selectedServer: StateFlow<ServerNode?> = serverRepository.observeSelectedServer()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        refreshDashboard()
    }

    fun refreshDashboard() {
        viewModelScope.launch {
            _isRefreshing.value = true
            when (val profileRes = authRepository.fetchProfile()) {
                is AppResult.Success -> _userProfile.value = profileRes.data
                else -> Unit
            }

            serverRepository.refreshServers()

            val code = authRepository.getSavedCustomerCode()
            if (!code.isNullOrBlank()) {
                subscriptionRepository.syncConfigs(code)
            }
            _isRefreshing.value = false
        }
    }

    fun onConnectToggle() {
        val currentState = connectionState.value
        if (currentState.isConnected || currentState.isConnecting) {
            vpnManager.disconnect()
        } else {
            viewModelScope.launch {
                val server = selectedServer.value
                    ?: serverRepository.observeServers().first().firstOrNull()

                val configs = subscriptionRepository.observeConfigs().first()
                val matchedConfig = configs.find { it.serverId == server?.id } ?: configs.firstOrNull()

                if (server != null && matchedConfig != null) {
                    vpnManager.connect(server, matchedConfig)
                }
            }
        }
    }
}
