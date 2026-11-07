package com.v2rayops.client.autoconnect

import com.v2rayops.client.core.logger.AppLogger
import com.v2rayops.client.domain.repository.ServerRepository
import com.v2rayops.client.domain.repository.SettingsRepository
import com.v2rayops.client.domain.repository.SubscriptionRepository
import com.v2rayops.client.network.NetworkMonitor
import com.v2rayops.client.network.NetworkType
import com.v2rayops.client.vpn.VpnManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoConnectManager @Inject constructor(
    private val networkMonitor: NetworkMonitor,
    private val settingsRepository: SettingsRepository,
    private val serverRepository: ServerRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val vpnManager: VpnManager
) {
    fun startObserving(scope: CoroutineScope) {
        scope.launch {
            networkMonitor.networkStatusFlow.collectLatest { status ->
                if (!status.isConnected) {
                    AppLogger.d("AutoConnectManager", "Device is offline. Pausing auto-connect triggers.")
                    return@collectLatest
                }

                val settings = settingsRepository.observeSettings().first()
                val shouldAutoConnect = when (status.type) {
                    NetworkType.WIFI -> settings.autoConnectOnWifi
                    NetworkType.CELLULAR -> settings.autoConnectOnCellular
                    else -> false
                }

                if (shouldAutoConnect && vpnManager.connectionState.value.isDisconnected) {
                    AppLogger.i("AutoConnectManager", "Triggering smart auto-connect for ${status.type}")
                    val selectedServer = serverRepository.observeSelectedServer().first()
                    val configs = subscriptionRepository.observeConfigs().first()
                    val matchedConfig = configs.find { it.serverId == selectedServer?.id } ?: configs.firstOrNull()

                    if (selectedServer != null && matchedConfig != null) {
                        vpnManager.connect(selectedServer, matchedConfig)
                    }
                }
            }
        }
    }
}
