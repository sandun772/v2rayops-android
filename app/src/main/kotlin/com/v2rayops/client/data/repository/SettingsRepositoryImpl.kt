package com.v2rayops.client.data.repository

import com.v2rayops.client.data.storage.UserPreferencesDataStore
import com.v2rayops.client.domain.model.RoutingMode
import com.v2rayops.client.domain.model.UserSettings
import com.v2rayops.client.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val preferencesDataStore: UserPreferencesDataStore
) : SettingsRepository {

    override fun observeSettings(): Flow<UserSettings> = preferencesDataStore.userSettingsFlow

    override suspend fun setKillSwitchEnabled(enabled: Boolean) {
        preferencesDataStore.setKillSwitchEnabled(enabled)
    }

    override suspend fun setAutoConnectWifi(enabled: Boolean) {
        preferencesDataStore.setAutoConnectWifi(enabled)
    }

    override suspend fun setAutoConnectCellular(enabled: Boolean) {
        preferencesDataStore.setAutoConnectCellular(enabled)
    }

    override suspend fun setRoutingMode(mode: RoutingMode) {
        preferencesDataStore.setRoutingMode(mode)
    }

    override suspend fun setCustomDns(dns: String) {
        preferencesDataStore.setCustomDns(dns)
    }

    override suspend fun setSelectedServerId(serverId: Long) {
        preferencesDataStore.setSelectedServerId(serverId)
    }

    override suspend fun setPreferredProtocol(protocol: String) {
        preferencesDataStore.setPreferredProtocol(protocol)
    }

    override suspend fun setSmartQoSEnabled(enabled: Boolean) {
        preferencesDataStore.setSmartQoSEnabled(enabled)
    }

    override suspend fun setDownloadSpeedLimitMbps(limitMbps: Int) {
        preferencesDataStore.setDownloadSpeedLimitMbps(limitMbps)
    }

    override suspend fun setCellularMtuOptimized(optimized: Boolean) {
        preferencesDataStore.setCellularMtuOptimized(optimized)
    }

    override suspend fun setTcpBufferSizeKb(sizeKb: Int) {
        preferencesDataStore.setTcpBufferSizeKb(sizeKb)
    }
}
