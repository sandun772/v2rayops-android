package com.v2rayops.client.vpn

import com.v2rayops.client.core.dispatchers.AppDispatchers
import com.v2rayops.client.core.logger.AppLogger
import com.v2rayops.client.domain.model.ConnectionState
import com.v2rayops.client.domain.model.ServerNode
import com.v2rayops.client.domain.model.TrafficStats
import com.v2rayops.client.domain.model.VpnConfig
import com.v2rayops.client.domain.repository.SettingsRepository
import com.v2rayops.client.xray.XrayConfigGenerator
import com.v2rayops.client.xray.XrayEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VpnManager @Inject constructor(
    private val xrayEngine: XrayEngine,
    private val vpnController: VpnServiceController,
    private val configGenerator: XrayConfigGenerator,
    private val settingsRepository: SettingsRepository,
    private val dispatchers: AppDispatchers
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _trafficStats = MutableStateFlow(TrafficStats())
    val trafficStats: StateFlow<TrafficStats> = _trafficStats.asStateFlow()

    fun connect(server: ServerNode, config: VpnConfig) {
        scope.launch {
            _connectionState.value = ConnectionState.Connecting(server.displayName)

            // Honest check: If the native engine binary is pending (Phase 2), do not fake connection
            if (!xrayEngine.isBinaryAvailable) {
                AppLogger.i("VpnManager", "Engine binary is pending compilation (Phase 2)")
                _connectionState.value = ConnectionState.Unavailable(
                    "Xray Core native engine (libxray.so) will be compiled and linked in Phase 2. Foundation verified."
                )
                return@launch
            }

            try {
                val settings = settingsRepository.observeSettings().first()
                val configJson = configGenerator.generateJson(config, settings)

                vpnController.startService(server)
                val result = xrayEngine.start(configJson)

                if (result.isSuccess) {
                    val now = System.currentTimeMillis()
                    _connectionState.value = ConnectionState.Connected(
                        serverName = server.displayName,
                        serverEndpoint = server.displayEndpoint,
                        connectedSinceMs = now
                    )
                } else {
                    val err = result.exceptionOrNull()?.message ?: "Failed to start Xray tunnel"
                    _connectionState.value = ConnectionState.Error(err)
                    vpnController.stopService()
                }
            } catch (e: Exception) {
                AppLogger.e("VpnManager", "Failed to connect", e)
                _connectionState.value = ConnectionState.Error(e.message ?: "Connection error")
                vpnController.stopService()
            }
        }
    }

    fun disconnect() {
        scope.launch {
            _connectionState.value = ConnectionState.Disconnecting
            xrayEngine.stop()
            vpnController.stopService()
            _connectionState.value = ConnectionState.Disconnected
            _trafficStats.value = TrafficStats()
        }
    }
}
