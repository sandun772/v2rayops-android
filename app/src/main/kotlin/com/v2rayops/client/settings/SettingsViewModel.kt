package com.v2rayops.client.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.v2rayops.client.data.storage.UserPreferencesDataStore
import com.v2rayops.client.domain.model.RoutingMode
import com.v2rayops.client.domain.model.UserSettings
import com.v2rayops.client.domain.repository.AuthRepository
import com.v2rayops.client.domain.repository.SettingsRepository
import com.v2rayops.client.network.RemoteConfigSyncManager
import com.v2rayops.client.network.RemoteSyncState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val remoteConfigSyncManager: RemoteConfigSyncManager
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsRepository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    val remoteManagementEnabled: StateFlow<Boolean> = preferencesDataStore.remoteManagementEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val remoteSettingsVersion: StateFlow<Long> = preferencesDataStore.remoteSettingsVersionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val lastRemoteSyncStatus: StateFlow<String> = preferencesDataStore.lastRemoteSyncStatusFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Not Synced")

    val remoteSyncState: StateFlow<RemoteSyncState> = remoteConfigSyncManager.syncState

    fun toggleRemoteManagement(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.setRemoteManagementEnabled(enabled)
        }
    }

    fun syncWithCloud() {
        remoteConfigSyncManager.triggerSync()
    }

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

    fun toggleDpiDelayFix(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDpiDelayFixEnabled(enabled)
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onSuccess()
        }
    }
}
