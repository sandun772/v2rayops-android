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
import com.v2rayops.client.network.AdaptiveQoSEngine
import com.v2rayops.client.network.QoSStatus
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import com.v2rayops.client.network.FailureDiagnosticsTracker
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
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VpnManager @Inject constructor(
    private val xrayEngine: XrayEngine,
    private val vpnController: VpnServiceController,
    private val configGenerator: XrayConfigGenerator,
    private val settingsRepository: SettingsRepository,
    private val adaptiveQoSEngine: AdaptiveQoSEngine,
    private val failureDiagnosticsTracker: FailureDiagnosticsTracker,
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

    private val _isNetworkWorkingFine = MutableStateFlow(true)
    val isNetworkWorkingFine: StateFlow<Boolean> = _isNetworkWorkingFine.asStateFlow()

    val qosStatus: StateFlow<QoSStatus> = adaptiveQoSEngine.qosStatus

    private var currentConfig: VpnConfig? = null
    private var statsJob: Job? = null
    private var healthMonitorJob: Job? = null
    private var savedConnectedSinceMs: Long = 0L
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
                        startHealthMonitoring()
                        failureDiagnosticsTracker.recordSuccess()
                    }
                    is XrayState.Starting -> {
                        val cfg = currentConfig
                        _connectionState.value = ConnectionState.Connecting(
                            serverName = cfg?.displayName ?: "V2RayOps Client Config"
                        )
                    }
                    is XrayState.Stopped -> {
                        stopTrafficPolling()
                        stopHealthMonitoring()
                        if (_connectionState.value !is ConnectionState.Disconnected) {
                            _connectionState.value = ConnectionState.Disconnected
                        }
                    }
                    is XrayState.Error -> {
                        stopTrafficPolling()
                        stopHealthMonitoring()
                        _connectionState.value = ConnectionState.Error(xrayState.error)
                        failureDiagnosticsTracker.recordFailure(xrayState.error, currentConfig)
                    }
                    is XrayState.BinaryPending -> {
                        stopTrafficPolling()
                        stopHealthMonitoring()
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
                val effectiveMtu = if (settings.gamingModeEnabled) {
                    1360
                } else {
                    config.customMtu ?: if (settings.cellularMtuOptimized) 1360 else 1500
                }

                AppLogger.i(TAG, "Requesting VPN Service start with config: ${config.displayName} (MTU: $effectiveMtu, SmartQoS: ${settings.smartQoSEnabled})")
                vpnController.startService(config.displayName, config.displayEndpoint, configJson, effectiveMtu)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to initiate VPN connection", e)
                _connectionState.value = ConnectionState.Error(e.message ?: "Failed to generate tunnel configuration")
                failureDiagnosticsTracker.recordFailure(e.message ?: "TunnelConfigGenerationFailed", config, e)
                vpnController.stopService()
            } finally {
                delay(600)
                isConnectInProgress = false
            }
        }
    }

    /**
     * Hot-reloads running Xray engine with updated remote settings or VLESS URL
     * without tearing down the Android VPN interface or requiring OS re-authorization.
     */
    fun hotReloadTunnel(updatedConfig: VpnConfig? = null) {
        scope.launch {
            val state = _connectionState.value
            if (state !is ConnectionState.Connected) {
                AppLogger.d(TAG, "Hot-reload skipped: tunnel is not currently connected")
                return@launch
            }
            val config = updatedConfig ?: currentConfig ?: return@launch
            try {
                currentConfig = config
                val settings = settingsRepository.observeSettings().first()
                val configJson = configGenerator.generateJson(config, settings)
                val effectiveMtu = if (settings.gamingModeEnabled) {
                    1360
                } else {
                    config.customMtu ?: if (settings.cellularMtuOptimized) 1360 else 1500
                }

                AppLogger.i(TAG, "Hot-reloading active tunnel with remote settings update: ${config.displayName} (MTU: $effectiveMtu)")
                vpnController.startService(config.displayName, config.displayEndpoint, configJson, effectiveMtu)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to hot-reload active tunnel", e)
            }
        }
    }

    fun connect(server: ServerNode, config: VpnConfig) {
        connect(config)
    }

    fun reconnect() {
        val cfg = currentConfig ?: return
        connect(cfg)
    }

    fun measureDelay(url: String = "https://www.google.com/generate_204"): Long {
        return xrayEngine.measureDelay(url)
    }

    fun disconnect() {
        scope.launch {
            AppLogger.i(TAG, "Disconnect requested")
            _connectionState.value = ConnectionState.Disconnecting
            isConnectInProgress = false
            stopTrafficPolling()
            stopHealthMonitoring()
            adaptiveQoSEngine.reset()
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
                    adaptiveQoSEngine.processTrafficSample(stats)
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
        adaptiveQoSEngine.reset()
    }

    /**
     * Active Tunnel Health & Timeout Monitor:
     * - Continuously verifies live data flow through the tunnel.
     * - If traffic is active (> 256 bytes delta), connection is confirmed healthy.
     * - If idle, sends non-blocking lightweight HTTP 204 probes via local proxy (127.0.0.1:10809).
     * - When all requests time out (2 consecutive timeouts): transitions state to Connecting.
     * - When requests succeed again: automatically restores state to Connected.
     */
    private fun startHealthMonitoring() {
        healthMonitorJob?.cancel()
        healthMonitorJob = scope.launch(dispatchers.io) {
            // Allow tunnel socket & route tables 2.5s to establish handshakes
            delay(2500L)

            val probeUrls = listOf(
                "http://cp.cloudflare.com/generate_204",
                "http://www.google.com/generate_204"
            )
            var consecutiveTimeouts = 0
            var lastRx = _trafficStats.value.totalDownloadBytes

            while (isActive) {
                val currentRx = _trafficStats.value.totalDownloadBytes
                val rxDelta = currentRx - lastRx
                lastRx = currentRx

                // If real user traffic is accumulating (> 256 bytes), networking is working fine
                if (rxDelta > 256) {
                    consecutiveTimeouts = 0
                    _isNetworkWorkingFine.value = true
                    restoreConnectedIfDegraded()
                    delay(4000L)
                    continue
                }

                // Traffic is idle: run active probe through local proxy inbound 127.0.0.1:10809
                val isOk = testProxyProbe(probeUrls)

                if (isOk) {
                    consecutiveTimeouts = 0
                    _isNetworkWorkingFine.value = true
                    restoreConnectedIfDegraded()
                    delay(5000L)
                } else {
                    consecutiveTimeouts++
                    AppLogger.w(TAG, "Active tunnel request timed out (count=$consecutiveTimeouts)")

                    if (consecutiveTimeouts >= 2) {
                        _isNetworkWorkingFine.value = false
                        val state = _connectionState.value
                        if (state is ConnectionState.Connected) {
                            savedConnectedSinceMs = state.connectedSinceMs
                            AppLogger.w(TAG, "All requests timed out. Transitioning state to Connecting.")
                            _connectionState.value = ConnectionState.Connecting(
                                serverName = state.serverName
                            )
                        }
                    }

                    if (consecutiveTimeouts >= 6) {
                        AppLogger.w(TAG, "Tunnel stalled for 25s+. Triggering reconnect to restore socket.")
                        reconnect()
                        break
                    }

                    delay(3000L)
                }
            }
        }
    }

    private fun restoreConnectedIfDegraded() {
        val state = _connectionState.value
        if (state is ConnectionState.Connecting && currentConfig != null && xrayEngine.state.value is XrayState.Running) {
            val cfg = currentConfig
            val since = if (savedConnectedSinceMs > 0L) savedConnectedSinceMs else System.currentTimeMillis()
            AppLogger.i(TAG, "Tunnel requests healthy again. Restoring Connected state.")
            _connectionState.value = ConnectionState.Connected(
                serverName = cfg?.displayName ?: "V2RayOps Client Config",
                serverEndpoint = cfg?.displayEndpoint ?: "sg-v2rayops.vpnxhub.com:443",
                connectedSinceMs = since
            )
        }
    }

    private fun stopHealthMonitoring() {
        healthMonitorJob?.cancel()
        healthMonitorJob = null
        savedConnectedSinceMs = 0L
        _isNetworkWorkingFine.value = true
    }

    private fun testProxyProbe(urls: List<String>): Boolean {
        val client = OkHttpClient.Builder()
            .proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", 10809)))
            .connectTimeout(2000, TimeUnit.MILLISECONDS)
            .readTimeout(2000, TimeUnit.MILLISECONDS)
            .callTimeout(2300, TimeUnit.MILLISECONDS)
            .build()

        for (url in urls) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "V2RayOps-Client/1.0")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful || response.code == 204) {
                        return true
                    }
                }
            } catch (_: Exception) {
                // Timeout or network unreachable
            }
        }
        return false
    }
}
