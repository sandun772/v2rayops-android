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
import com.v2rayops.client.xray.XrayState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
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
    companion object {
        private const val TAG = "VpnManager"
    }

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _trafficStats = MutableStateFlow(TrafficStats())
    val trafficStats: StateFlow<TrafficStats> = _trafficStats.asStateFlow()

    private var currentConfig: VpnConfig? = null
    private var statsJob: Job? = null
    private var isConnectInProgress = false

    init {
        // Observe Xray engine state reactively
        scope.launch {
            xrayEngine.state.collect { xrayState ->
                when (xrayState) {
                    is XrayState.Running -> {
                        val cfg = currentConfig
                        val now = xrayState.startedAtMs
                        _connectionState.value = ConnectionState.Connected(
                            serverName = cfg?.displayName ?: "V2RayOps Client Config",
                            serverEndpoint = cfg?.displayEndpoint ?: "sg-v2rayops.vpnxhub.com:443",
                            connectedSinceMs = now
                        )
                        startTrafficPolling()
                    }
                    is XrayState.Starting -> {
                        val cfg = currentConfig
                        _connectionState.value = ConnectionState.Connecting(
                            serverName = cfg?.displayName ?: "V2RayOps Client Config"
                        )
                    }
                    is XrayState.Stopped -> {
                        stopTrafficPolling()
                        if (_connectionState.value !is ConnectionState.Disconnected) {
                            _connectionState.value = ConnectionState.Disconnected
                        }
                    }
                    is XrayState.Error -> {
                        stopTrafficPolling()
                        _connectionState.value = ConnectionState.Error(xrayState.error)
                    }
                    is XrayState.BinaryPending -> {
                        stopTrafficPolling()
                        _connectionState.value = ConnectionState.Unavailable(xrayState.message)
                    }
                }
            }
        }
    }

    fun connect(config: VpnConfig) {
        scope.launch {
            if (isConnectInProgress) {
                AppLogger.d(TAG, "Connection initiation already in progress, debouncing duplicate request")
                return@launch
            }
            isConnectInProgress = true
            currentConfig = config
            _connectionState.value = ConnectionState.Connecting(config.displayName)

            try {
                val settings = settingsRepository.observeSettings().first()
                val configJson = configGenerator.generateJson(config, settings)

                AppLogger.i(TAG, "Requesting VPN Service start with config: ${config.displayName}")
                vpnController.startService(config.displayName, config.displayEndpoint, configJson)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to initiate VPN connection", e)
                _connectionState.value = ConnectionState.Error(e.message ?: "Failed to generate tunnel configuration")
                vpnController.stopService()
            } finally {
                delay(600)
                isConnectInProgress = false
            }
        }
    }

    fun connect(server: ServerNode, config: VpnConfig) {
        connect(config)
    }

    fun disconnect() {
        scope.launch {
            AppLogger.i(TAG, "Disconnect requested")
            _connectionState.value = ConnectionState.Disconnecting
            isConnectInProgress = false
            stopTrafficPolling()
            vpnController.stopService()
            xrayEngine.stop()
            _connectionState.value = ConnectionState.Disconnected
            _trafficStats.value = TrafficStats()
            currentConfig = null
        }
    }

    private fun startTrafficPolling() {
        statsJob?.cancel()
        statsJob = scope.launch(dispatchers.io) {
            while (isActive) {
                try {
                    val stats = xrayEngine.queryTrafficStats()
                    _trafficStats.value = stats
                } catch (t: Throwable) {
                    AppLogger.w(TAG, "Error polling traffic stats: ${t.message}")
                }
                delay(1000L)
            }
        }
    }

    private fun stopTrafficPolling() {
        statsJob?.cancel()
        statsJob = null
    }
}
