package com.v2rayops.client.domain.repository

import com.v2rayops.client.domain.model.RoutingMode
import com.v2rayops.client.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<UserSettings>
    suspend fun setKillSwitchEnabled(enabled: Boolean)
    suspend fun setAutoConnectWifi(enabled: Boolean)
    suspend fun setAutoConnectCellular(enabled: Boolean)
    suspend fun setRoutingMode(mode: RoutingMode)
    suspend fun setCustomDns(dns: String)
    suspend fun setSelectedServerId(serverId: Long)
    suspend fun setPreferredProtocol(protocol: String)
}
