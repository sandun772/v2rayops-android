package com.v2rayops.client.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.v2rayops.client.domain.model.RoutingMode
import com.v2rayops.client.domain.model.UserSettings
import com.v2rayops.client.domain.repository.AuthRepository
import com.v2rayops.client.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsRepository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    fun toggleKillSwitch(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setKillSwitchEnabled(enabled)
        }
    }

    fun toggleAutoConnectWifi(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoConnectWifi(enabled)
        }
    }

    fun toggleAutoConnectCellular(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoConnectCellular(enabled)
        }
    }

    fun setRoutingMode(mode: RoutingMode) {
        viewModelScope.launch {
            settingsRepository.setRoutingMode(mode)
        }
    }

    fun toggleSmartQoS(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSmartQoSEnabled(enabled)
        }
    }

    fun setDownloadSpeedLimit(limitMbps: Int) {
        viewModelScope.launch {
            settingsRepository.setDownloadSpeedLimitMbps(limitMbps)
        }
    }

    fun toggleCellularMtuOptimized(optimized: Boolean) {
        viewModelScope.launch {
            settingsRepository.setCellularMtuOptimized(optimized)
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onSuccess()
        }
    }
}
