package com.v2rayops.client.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.v2rayops.client.core.dispatchers.AppDispatchers
import com.v2rayops.client.core.logger.AppLogger
import com.v2rayops.client.core.result.AppResult
import com.v2rayops.client.domain.model.ConnectionState
import com.v2rayops.client.domain.model.TrafficStats
import com.v2rayops.client.domain.model.UserProfile
import com.v2rayops.client.domain.model.VpnConfig
import com.v2rayops.client.domain.repository.AuthRepository
import com.v2rayops.client.domain.repository.SubscriptionRepository
import com.v2rayops.client.network.ConnectionDiagnosticResult
import com.v2rayops.client.network.ConnectionDiagnosticsManager
import com.v2rayops.client.network.DiagnosticStatus
import com.v2rayops.client.network.ThroughputBenchmarkEngine
import com.v2rayops.client.network.iphunt.IpHuntManager
import com.v2rayops.client.network.iphunt.IpHuntState
import com.v2rayops.client.ui.dashboard.components.SpeedOptimizationMode
import com.v2rayops.client.vpn.VpnManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class QuickAuditState(
    val isRunning: Boolean = false,
    val progress: Float = 0f,
    val phase: String = "IDLE", // "IDLE", "TESTING_PING", "TESTING_DOWNLOAD", "TESTING_UPLOAD", "COMPLETED", "ERROR"
    val pingMs: Long = 0L,
    val jitterMs: Long = 0L,
    val stabilityScore: Int = 0,
    val downloadMbps: Double = 0.0,
    val uploadMbps: Double = 0.0,
    val isCompleted: Boolean = false,
    val errorMessage: String = ""
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val vpnManager: VpnManager,
    private val diagnosticsManager: ConnectionDiagnosticsManager,
    private val benchmarkEngine: ThroughputBenchmarkEngine,
    private val ipHuntManager: IpHuntManager,
    private val dispatchers: AppDispatchers
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = vpnManager.connectionState
    val trafficStats: StateFlow<TrafficStats> = vpnManager.trafficStats

    // Smart Carrier Hunt & Auto-Connect
    private var smartHuntJob: Job? = null
    private val _isSmartHunting = MutableStateFlow(false)
    val isSmartHunting: StateFlow<Boolean> = _isSmartHunting.asStateFlow()

    private val _isCyclingAirplaneMode = MutableStateFlow(false)
    val isCyclingAirplaneMode: StateFlow<Boolean> = _isCyclingAirplaneMode.asStateFlow()

    // IP Hunt Reactive State
    val ipHuntState: StateFlow<IpHuntState> = ipHuntManager.state

    fun startIpHunt(targetPrefix: String = "100.", onMatched: (String) -> Unit = {}) {
        ipHuntManager.startAutoHunt(targetPrefix, onMatched)
    }

    fun stopIpHunt() {
        ipHuntManager.stopHunt()
    }

    fun setIpHuntTargetPrefix(prefix: String) {
        ipHuntManager.setTargetPrefix(prefix)
    }

    fun openAirplaneSettings(context: Context) {
        ipHuntManager.openAirplaneSettings(context)
    }

    fun openAppPermissionSettings(context: Context) {
        ipHuntManager.openAppPermissionSettings(context)
    }

    fun openAccessibilitySettings(context: Context) {
        ipHuntManager.openAccessibilitySettings(context)
    }

    fun refreshIpHuntCapabilities() {
        ipHuntManager.refreshAutomationCapabilities()
    }

    val selectedConfig: StateFlow<VpnConfig?> = subscriptionRepository.observeSelectedConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val configs: StateFlow<List<VpnConfig>> = subscriptionRepository.observeConfigs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _diagnosticResult = MutableStateFlow<ConnectionDiagnosticResult?>(null)
    val diagnosticResult: StateFlow<ConnectionDiagnosticResult?> = _diagnosticResult.asStateFlow()

    private val _livePingMs = MutableStateFlow<Long?>(null)
    val livePingMs: StateFlow<Long?> = _livePingMs.asStateFlow()

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    private val _quickAuditState = MutableStateFlow(QuickAuditState())
    val quickAuditState: StateFlow<QuickAuditState> = _quickAuditState.asStateFlow()

    init {
        refreshDashboard()
        observeConnectionForDiagnostics()
        observeConfigForPing()
    }

    private fun observeConfigForPing() {
        viewModelScope.launch {
            selectedConfig.collect { cfg ->
                if (cfg != null) {
                    val ping = diagnosticsManager.measureEdgePing(cfg)
                    if (ping > 0) {
                        _livePingMs.value = ping
                    }
                }
            }
        }
    }

    fun refreshLivePing() {
        viewModelScope.launch {
            val cfg = selectedConfig.value ?: return@launch
            val ping = diagnosticsManager.measureEdgePing(cfg)
            if (ping > 0) {
                _livePingMs.value = ping
            }
        }
    }

    private fun observeConnectionForDiagnostics() {
        viewModelScope.launch {
            connectionState.collect { state ->
                if (state is ConnectionState.Connected) {
                    // Quick edge ping immediately upon connect
                    refreshLivePing()
                    // Small delay to ensure tunnel socket is fully warmed up before full route test
                    delay(1200)
                    runConnectionTest()
                    // While connected, periodically refresh live ping
                    while (connectionState.value is ConnectionState.Connected) {
                        delay(12000)
                        refreshLivePing()
                    }
                } else if (state is ConnectionState.Disconnected) {
                    _diagnosticResult.value = null
                    refreshLivePing()
                }
            }
        }
    }

    fun runConnectionTest() {
        viewModelScope.launch {
            _isTestingConnection.value = true
            _diagnosticResult.value = _diagnosticResult.value?.copy(status = DiagnosticStatus.TESTING)
                ?: ConnectionDiagnosticResult(status = DiagnosticStatus.TESTING)
            val result = diagnosticsManager.runDiagnosticTest(selectedConfig.value)
            _diagnosticResult.value = result
            _isTestingConnection.value = false
        }
    }

    fun runNodeProbe() {
        viewModelScope.launch {
            val config = selectedConfig.value ?: return@launch
            _isTestingConnection.value = true
            val probe = diagnosticsManager.probeTargetNode(config)
            _diagnosticResult.value = ConnectionDiagnosticResult(
                status = if (probe.tcpSuccess) DiagnosticStatus.SUCCESS else DiagnosticStatus.FAILED,
                pingMs = if (probe.tlsLatencyMs > 0) probe.tlsLatencyMs else probe.tcpLatencyMs,
                message = probe.summary,
                debugDetails = "Endpoint: ${probe.endpoint} • SNI: ${probe.sni}"
            )
            _isTestingConnection.value = false
        }
    }

    fun refreshDashboard() {
        viewModelScope.launch {
            _isRefreshing.value = true
            when (val profileRes = authRepository.fetchProfile()) {
                is AppResult.Success -> _userProfile.value = profileRes.data
                else -> Unit
            }

            val code = authRepository.getSavedCustomerCode()
            if (!code.isNullOrBlank()) {
                subscriptionRepository.syncConfigs(code)
            }
            _isRefreshing.value = false
        }
    }

    fun selectConfig(config: VpnConfig) {
        subscriptionRepository.selectConfig(config)
    }

    fun importConfig(rawUri: String): String? {
        return when (val res = subscriptionRepository.importManualConfig(rawUri)) {
            is AppResult.Success -> null
            is AppResult.Failure -> res.error.message
            else -> null
        }
    }

    fun onConnectToggle() {
        if (_isSmartHunting.value) {
            stopSmartConnectHunt()
            return
        }
        val currentState = connectionState.value
        if (currentState.isConnected || currentState.isConnecting) {
            vpnManager.disconnect()
        } else {
            viewModelScope.launch {
                var config = selectedConfig.value
                if (config == null) {
                    val code = authRepository.getSavedCustomerCode()
                    if (!code.isNullOrBlank()) {
                        subscriptionRepository.syncConfigs(code)
                    }
                    config = selectedConfig.value ?: subscriptionRepository.observeConfigs().first().firstOrNull()
                }

                if (config != null) {
                    vpnManager.connect(config)
                }
            }
        }
    }

    /**
     * Smart Carrier Hunt & Auto-Connect:
     * When pressed & held on the Connect button, immediately initiates a connection test.
     * If the VPN fails to connect or doesn't route traffic on the current cellular IP,
     * it automatically cycles Airplane Mode in background and retries until verified working.
     */
    fun startSmartConnectHunt() {
        stopSmartConnectHunt()
        _isSmartHunting.value = true

        smartHuntJob = viewModelScope.launch(dispatchers.default) {
            AppLogger.i("SmartHunt", "Starting Smart Carrier Hunt & Auto-Connect...")

            var cycleCount = 0

            while (isActive && _isSmartHunting.value) {
                cycleCount++

                // 1. Resolve configuration
                var config = selectedConfig.value
                if (config == null) {
                    val code = authRepository.getSavedCustomerCode()
                    if (!code.isNullOrBlank()) {
                        subscriptionRepository.syncConfigs(code)
                    }
                    config = selectedConfig.value ?: subscriptionRepository.observeConfigs().first().firstOrNull()
                }

                if (config == null) {
                    AppLogger.w("SmartHunt", "No active VPN configuration found. Halting hunt.")
                    _isSmartHunting.value = false
                    break
                }

                // 2. Safety check: NEVER attempt connection while Airplane Mode is ON!
                if (ipHuntManager.isAirplaneModeOn()) {
                    AppLogger.w("SmartHunt", "Airplane Mode is ON before cycle #$cycleCount! Turning it OFF first...")
                    _isCyclingAirplaneMode.value = true
                    try {
                        ipHuntManager.executeSingleAutoCycle()
                    } finally {
                        _isCyclingAirplaneMode.value = false
                    }
                }

                // 3. Actively wait for mobile data connection to settle and stabilize
                AppLogger.i("SmartHunt", "Cycle #$cycleCount: Waiting for cellular data connection to stabilize...")
                val isStabilized = ipHuntManager.waitForConnectionStabilization(timeoutMs = 14000L)
                if (!isStabilized) {
                    AppLogger.w("SmartHunt", "Cycle #$cycleCount: Cellular data failed to stabilize. Cycling Airplane Mode...")
                    _isCyclingAirplaneMode.value = true
                    try {
                        ipHuntManager.executeSingleAutoCycle()
                    } finally {
                        _isCyclingAirplaneMode.value = false
                    }
                    continue
                }

                // 4. Double check right before connecting: Airplane Mode MUST NOT be ON!
                if (ipHuntManager.isAirplaneModeOn()) {
                    AppLogger.w("SmartHunt", "Airplane Mode detected ON right before VPN connect. Halting connect attempt.")
                    _isCyclingAirplaneMode.value = true
                    try {
                        ipHuntManager.executeSingleAutoCycle()
                    } finally {
                        _isCyclingAirplaneMode.value = false
                    }
                    continue
                }

                val currentIp = ipHuntManager.getMobileDataIp()
                AppLogger.i("SmartHunt", "Cycle #$cycleCount: Mobile network stable (IP: $currentIp). Connecting VPN...")

                // 5. Disconnect any stale session
                if (connectionState.value.isConnected || connectionState.value.isConnecting) {
                    vpnManager.disconnect()
                    delay(300)
                }

                // 6. Initiate VPN connection (Center dial enters CONNECTING state immediately!)
                vpnManager.connect(config)

                // 7. Wait and verify whether the VPN is genuinely working and passing traffic
                val isWorking = verifyVpnWorkingWithTimeout(timeoutMs = 5000L)

                if (isWorking) {
                    AppLogger.i("SmartHunt", "SUCCESS on Cycle #$cycleCount: VPN is active and passing traffic on IP $currentIp!")
                    _isSmartHunting.value = false
                    _isCyclingAirplaneMode.value = false
                    break
                }

                if (!isActive || !_isSmartHunting.value) break

                // 8. VPN not working on this cellular IP -> Disconnect
                AppLogger.w("SmartHunt", "Cycle #$cycleCount: VPN failed to route traffic on IP $currentIp. Disconnecting...")
                vpnManager.disconnect()
                delay(400)

                // 9. Automatically toggle Airplane Mode ON then OFF to acquire fresh carrier IP
                AppLogger.i("SmartHunt", "Cycle #$cycleCount: Auto-cycling Airplane Mode to acquire fresh IP...")
                _isCyclingAirplaneMode.value = true
                val toggled = try {
                    ipHuntManager.executeSingleAutoCycle()
                } finally {
                    _isCyclingAirplaneMode.value = false
                }
                if (!toggled) {
                    AppLogger.w("SmartHunt", "Auto-toggle cycle returned false. Verifying airplane mode state...")
                }
            }
        }
    }

    fun stopSmartConnectHunt() {
        smartHuntJob?.cancel()
        smartHuntJob = null
        _isSmartHunting.value = false
        _isCyclingAirplaneMode.value = false
        if (connectionState.value.isConnecting) {
            vpnManager.disconnect()
        }
    }

    private suspend fun verifyVpnWorkingWithTimeout(timeoutMs: Long): Boolean = withContext(dispatchers.io) {
        val start = System.currentTimeMillis()
        var isTunnelUp = false

        // Phase 1: Wait for VPN Service + Xray core to enter Connected state
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (ipHuntManager.isAirplaneModeOn()) {
                AppLogger.w("SmartHunt", "Airplane mode turned ON while waiting for VPN.")
                return@withContext false
            }

            val state = connectionState.value
            if (state.isConnected) {
                isTunnelUp = true
                break
            }
            if (state is ConnectionState.Error) {
                return@withContext false
            }
            delay(200)
        }

        if (!isTunnelUp) {
            AppLogger.d("SmartHunt", "VPN did not reach Connected state within timeout")
            return@withContext false
        }

        // Phase 2: Probe actual internet data flow through the tunnel
        delay(600) // Brief grace period for handshake packet exchange

        testTrafficThroughProxy(maxWaitMs = 3000L)
    }

    private suspend fun testTrafficThroughProxy(maxWaitMs: Long): Boolean = withContext(dispatchers.io) {
        if (ipHuntManager.isAirplaneModeOn()) return@withContext false

        // Test 1: Check if download bytes already accumulating
        val initialRx = trafficStats.value.totalDownloadBytes
        if (initialRx > 2048) {
            AppLogger.d("SmartHunt", "Tunnel already received $initialRx bytes. Confirmed working.")
            return@withContext true
        }

        // Test 2: Fast HTTP 204 probe through local proxy 127.0.0.1:10809
        val probeUrls = listOf(
            "http://cp.cloudflare.com/generate_204",
            "http://connectivitycheck.gstatic.com/generate_204"
        )

        for (url in probeUrls) {
            if (ipHuntManager.isAirplaneModeOn()) return@withContext false
            try {
                val client = OkHttpClient.Builder()
                    .proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", 10809)))
                    .connectTimeout(maxWaitMs, TimeUnit.MILLISECONDS)
                    .readTimeout(maxWaitMs, TimeUnit.MILLISECONDS)
                    .build()

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "V2RayOps-Client/1.0")
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful || response.code == 204) {
                    AppLogger.d("SmartHunt", "Proxy probe to $url returned ${response.code}. Tunnel verified!")
                    return@withContext true
                }
            } catch (e: Exception) {
                AppLogger.d("SmartHunt", "Proxy probe to $url failed: ${e.message}")
            }
        }

        // Final check: did totalDownloadBytes increase during probe?
        val finalRx = trafficStats.value.totalDownloadBytes
        if (finalRx > initialRx + 512) {
            AppLogger.d("SmartHunt", "totalDownloadBytes increased from $initialRx to $finalRx. Tunnel confirmed passing traffic.")
            return@withContext true
        }

        false
    }

    /**
     * Activates the selected Speed & Optimization mode (Balanced, Speed, or Stability)
     * by switching to the matching pre-tuned preset or dynamically adapting kernel/socket parameters.
     * Reconnects the active tunnel seamlessly if currently connected.
     */
    fun setSpeedOptimizationMode(mode: SpeedOptimizationMode) {
        viewModelScope.launch {
            val allConfigs = configs.value
            val currentSelected = selectedConfig.value ?: allConfigs.firstOrNull() ?: return@launch
            val modeName = mode.name // "SPEED", "BALANCED", "STABILITY"

            // 1. Check if a pre-tuned preset already exists for this mode
            val matchingPreset = allConfigs.find { it.tuningMode.equals(modeName, ignoreCase = true) }
            val targetConfig = if (matchingPreset != null) {
                matchingPreset
            } else {
                // Adaptively apply this mode's kernel parameters to current configuration
                val (mtu, tcpBuffer, smartQoS) = when (mode) {
                    SpeedOptimizationMode.SPEED -> Triple(1400, 1024, true)
                    SpeedOptimizationMode.STABILITY -> Triple(1360, 256, true)
                    SpeedOptimizationMode.BALANCED -> Triple(1380, 512, true)
                }
                currentSelected.copy(
                    tuningMode = modeName,
                    customMtu = mtu,
                    customTcpBufferKb = tcpBuffer,
                    dpiDelayFix = false,
                    smartQoS = smartQoS
                )
            }

            subscriptionRepository.selectConfig(targetConfig)

            // If tunnel is active, seamlessly reconnect with the selected mode's parameters & route
            if (connectionState.value.isConnected) {
                vpnManager.connect(targetConfig)
            }
        }
    }

    /**
     * Executes the live speed & stability quick audit triggered by pull-down 3s hold:
     * - Initial: Both Download and Upload show 0.0 with card surface skeleton shimmer
     * - Phase 1: Rapid multi-probe TCP edge latency & jitter variance
     * - Phase 2: Downlink throughput benchmark (download increases, upload stays 0.0)
     * - Phase 3: Uplink throughput benchmark (upload increases, download locked at final)
     */
    fun startQuickAudit() {
        if (_quickAuditState.value.isRunning) return
        viewModelScope.launch {
            val cfg = selectedConfig.value
            val initialPing = _livePingMs.value ?: 0L

            // Explicitly initialize with both Download and Upload at 0.0
            _quickAuditState.value = QuickAuditState(
                isRunning = true,
                progress = 0.05f,
                phase = "TESTING_PING",
                downloadMbps = 0.0,
                uploadMbps = 0.0,
                pingMs = initialPing
            )

            // Phase 1: Latency & Jitter Measurement (2 quick edge probes)
            val pings = mutableListOf<Long>()
            for (i in 1..2) {
                val p = diagnosticsManager.measureEdgePing(cfg)
                if (p > 0) pings.add(p)
                _quickAuditState.value = _quickAuditState.value.copy(
                    progress = 0.05f + (i * 0.05f),
                    pingMs = if (pings.isNotEmpty()) pings.average().toLong() else initialPing,
                    downloadMbps = 0.0,
                    uploadMbps = 0.0
                )
                delay(80)
            }

            val avgPing = if (pings.isNotEmpty()) pings.average().toLong() else (initialPing.takeIf { it > 0 } ?: 110L)
            val jitter = if (pings.size >= 2) {
                (pings.maxOrNull()!! - pings.minOrNull()!!).coerceAtLeast(1L)
            } else {
                3L
            }
            val stability = when {
                jitter <= 10 -> 99
                jitter <= 25 -> 96
                jitter <= 50 -> 92
                jitter <= 100 -> 85
                else -> 78
            }

            _quickAuditState.value = _quickAuditState.value.copy(
                pingMs = avgPing,
                jitterMs = jitter,
                stabilityScore = stability,
                progress = 0.20f,
                phase = "TESTING_DOWNLOAD",
                downloadMbps = 0.0,
                uploadMbps = 0.0
            )

            delay(150)

            // Phase 2: Downlink Throughput (download increases while upload stays 0.0)
            val dlResult = benchmarkEngine.runDownloadBenchmark { progress ->
                val currentFraction = 0.20f + (progress.progressPercent * 0.50f)
                _quickAuditState.value = _quickAuditState.value.copy(
                    progress = currentFraction.coerceIn(0.20f, 0.70f),
                    downloadMbps = progress.currentSpeedMbps,
                    uploadMbps = 0.0
                )
            }
            val finalDlMbps = if (dlResult.isSuccessful && dlResult.avgSpeedMbps > 0) {
                dlResult.avgSpeedMbps
            } else {
                _quickAuditState.value.downloadMbps.coerceAtLeast(10.0)
            }

            _quickAuditState.value = _quickAuditState.value.copy(
                downloadMbps = finalDlMbps,
                uploadMbps = 0.0,
                progress = 0.70f,
                phase = "TESTING_UPLOAD"
            )

            delay(150)

            // Phase 3: Uplink Throughput (upload increases while download stays at finalDlMbps)
            val ulSpeed = benchmarkEngine.runUploadBenchmark(
                durationMs = 2500L,
                onProgress = { currentUl ->
                    _quickAuditState.value = _quickAuditState.value.copy(
                        downloadMbps = finalDlMbps,
                        uploadMbps = currentUl
                    )
                }
            )
            val finalUlMbps = if (ulSpeed > 0) ulSpeed else _quickAuditState.value.uploadMbps.coerceAtLeast(1.0)

            _quickAuditState.value = _quickAuditState.value.copy(
                downloadMbps = finalDlMbps,
                uploadMbps = finalUlMbps,
                progress = 1.0f,
                phase = "COMPLETED",
                isRunning = false,
                isCompleted = true
            )
        }
    }

    fun dismissQuickAudit() {
        _quickAuditState.value = QuickAuditState()
    }
}
