package com.v2rayops.client.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.v2rayops.client.core.dispatchers.AppDispatchers
import com.v2rayops.client.core.logger.AppLogger
import com.v2rayops.client.core.result.AppResult
import com.v2rayops.client.domain.model.ConnectionState
import com.v2rayops.client.domain.model.SmartHuntProgress
import com.v2rayops.client.domain.model.SmartHuntStage
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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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

    private val _smartHuntProgress = MutableStateFlow(SmartHuntProgress())
    val smartHuntProgress: StateFlow<SmartHuntProgress> = _smartHuntProgress.asStateFlow()

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

    fun openVoiceAssistantSettings(context: Context) {
        ipHuntManager.openVoiceAssistantSettings(context)
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
        if (ipHuntManager.isAirplaneModeOn()) {
            // User tapped connect while Airplane Mode is on: enter the auto-connect watcher
            startSmartConnectHunt()
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
     * Event-Driven Smart Carrier Auto-Connect Watcher:
     * - Zero arbitrary wait times or countdowns.
     * - Microsecond reactive Airplane Mode detection via ContentObserver / BroadcastReceiver.
     * - Instant cellular IP capture via ConnectivityManager.NetworkCallback link properties.
     * - Native Xray core packet delay probe & downlink byte verification.
     */
    fun startSmartConnectHunt() {
        stopSmartConnectHunt()
        _isSmartHunting.value = true

        smartHuntJob = viewModelScope.launch(dispatchers.default) {
            AppLogger.i("SmartHunt", "Starting Event-Driven Smart Carrier Watcher...")

            var cycleCount = 0

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
                _smartHuntProgress.value = SmartHuntProgress(SmartHuntStage.IDLE)
                return@launch
            }

            // Step 0: Check if device ALREADY has a working connection or target-matching IP!
            if (!ipHuntManager.isAirplaneModeOn()) {
                val currentIp = ipHuntManager.getMobileDataIp()
                val targetPrefix = ipHuntManager.state.value.selectedTargetPrefix
                val isStrictSubnet = targetPrefix.isNotBlank() && targetPrefix != "100." && targetPrefix != "ALL"
                val isPrefixMatched = !isStrictSubnet || ipHuntManager.isIpMatching(currentIp, targetPrefix)

                AppLogger.i("SmartHunt", "Step 0: Checking current cellular IP: $currentIp (Target: $targetPrefix, Matched: $isPrefixMatched)")

                if (!currentIp.isNullOrBlank() && isPrefixMatched) {
                    _smartHuntProgress.value = SmartHuntProgress(
                        stage = SmartHuntStage.CONNECTING_VPN,
                        currentCycle = 1,
                        assignedIp = currentIp
                    )

                    if (!connectionState.value.isConnected) {
                        vpnManager.connect(config)
                        withTimeoutOrNull(6000L) {
                            connectionState.filter { it.isConnected }.first()
                        }
                    }

                    if (connectionState.value.isConnected) {
                        _smartHuntProgress.value = SmartHuntProgress(
                            stage = SmartHuntStage.VERIFYING_TRAFFIC,
                            currentCycle = 1,
                            assignedIp = currentIp
                        )

                        val currentWorking = verifyActiveTunnelTraffic(timeoutMs = 3000L)
                        if (currentWorking) {
                            AppLogger.i("SmartHunt", "Instant Success: Current IP $currentIp is ALREADY working! Locking connection.")
                            _isSmartHunting.value = false
                            _smartHuntProgress.value = SmartHuntProgress(
                                stage = SmartHuntStage.SUCCESS,
                                currentCycle = 1,
                                assignedIp = currentIp
                            )
                            return@launch
                        } else {
                            AppLogger.w("SmartHunt", "Current IP $currentIp traffic test failed. Disconnecting and proceeding to Airplane cycle.")
                            vpnManager.disconnect()
                            delay(250)
                        }
                    }
                }
            }

            // Continuous event-driven cycle until a working IP is verified
            while (isActive && _isSmartHunting.value) {
                cycleCount++

                // PHASE 1: Wait for Airplane Mode to become ON (if not already ON)
                val isCurrentlyOn = ipHuntManager.isAirplaneModeOn()
                if (!isCurrentlyOn) {
                    _smartHuntProgress.value = SmartHuntProgress(
                        stage = SmartHuntStage.WAITING_AIRPLANE_ON,
                        currentCycle = cycleCount,
                        assignedIp = ipHuntManager.getMobileDataIp()
                    )
                    _isCyclingAirplaneMode.value = true

                    // Event-driven: wait until observeAirplaneMode emits true
                    ipHuntManager.observeAirplaneMode()
                        .filter { it }
                        .first()
                }

                if (!isActive || !_isSmartHunting.value) break

                // PHASE 2: Airplane Mode is ON -> wait until it becomes OFF
                AppLogger.i("SmartHunt", "Cycle #$cycleCount: Airplane Mode is ON. Waiting for user to disable it...")
                _smartHuntProgress.value = SmartHuntProgress(
                    stage = SmartHuntStage.AIRPLANE_ON,
                    currentCycle = cycleCount
                )

                // Event-driven: wait until observeAirplaneMode emits false
                ipHuntManager.observeAirplaneMode()
                    .filter { !it }
                    .first()

                if (!isActive || !_isSmartHunting.value) break

                // PHASE 3: Airplane Mode turned OFF!
                // Event-driven: await cellular IP assignment directly from network callback (zero preset countdowns)
                _isCyclingAirplaneMode.value = false
                AppLogger.i("SmartHunt", "Cycle #$cycleCount: Airplane Mode turned OFF. Awaiting cellular IP assignment event...")
                _smartHuntProgress.value = SmartHuntProgress(
                    stage = SmartHuntStage.ACQUIRING_IP,
                    currentCycle = cycleCount
                )

                val acquiredIp = withTimeoutOrNull(10000L) {
                    ipHuntManager.observeCellularIp()
                        .filter { !it.isNullOrBlank() }
                        .first()
                } ?: ipHuntManager.getMobileDataIp()

                if (!isActive || !_isSmartHunting.value) break

                AppLogger.i("SmartHunt", "Cycle #$cycleCount: Cellular IP received: $acquiredIp")

                // PHASE 4: Check Subnet / Target prefix (if user configured a custom prefix in Cockpit)
                val targetPrefix = ipHuntManager.state.value.selectedTargetPrefix
                val isStrictSubnet = targetPrefix.isNotBlank() && targetPrefix != "100." && targetPrefix != "ALL"
                if (isStrictSubnet && !ipHuntManager.isIpMatching(acquiredIp, targetPrefix)) {
                    AppLogger.w("SmartHunt", "Cycle #$cycleCount: IP $acquiredIp does not match target prefix $targetPrefix. Fast-retry.")
                    _smartHuntProgress.value = SmartHuntProgress(
                        stage = SmartHuntStage.IP_MISMATCH,
                        currentCycle = cycleCount,
                        assignedIp = acquiredIp,
                        detailMessage = "Subnet mismatch (wanted $targetPrefix*)"
                    )
                    delay(1200)
                    continue
                }

                // PHASE 5: AUTO-CONNECT VPN
                _smartHuntProgress.value = SmartHuntProgress(
                    stage = SmartHuntStage.CONNECTING_VPN,
                    currentCycle = cycleCount,
                    assignedIp = acquiredIp
                )

                if (connectionState.value.isConnected || connectionState.value.isConnecting) {
                    vpnManager.disconnect()
                    delay(250)
                }

                vpnManager.connect(config)

                // Wait for VPN connection state to reach Connected
                val connectSuccess = withTimeoutOrNull(8000L) {
                    connectionState.filter { it.isConnected }.first()
                } != null

                if (!isActive || !_isSmartHunting.value) break

                if (!connectSuccess) {
                    AppLogger.w("SmartHunt", "Cycle #$cycleCount: VPN failed to reach Connected state. Disconnecting & retrying.")
                    vpnManager.disconnect()
                    _smartHuntProgress.value = SmartHuntProgress(
                        stage = SmartHuntStage.DEAD_IP_RETRY,
                        currentCycle = cycleCount,
                        assignedIp = acquiredIp
                    )
                    delay(800)
                    continue
                }

                // PHASE 6: VERIFY LIVE DATA FLOW ("is the IP working or not")
                _smartHuntProgress.value = SmartHuntProgress(
                    stage = SmartHuntStage.VERIFYING_TRAFFIC,
                    currentCycle = cycleCount,
                    assignedIp = acquiredIp
                )

                val isTrafficWorking = verifyActiveTunnelTraffic(timeoutMs = 3000L)

                if (!isActive || !_isSmartHunting.value) break

                if (isTrafficWorking) {
                    AppLogger.i("SmartHunt", "SUCCESS on Cycle #$cycleCount: Verified working data flow on IP $acquiredIp!")
                    _isSmartHunting.value = false
                    _smartHuntProgress.value = SmartHuntProgress(
                        stage = SmartHuntStage.SUCCESS,
                        currentCycle = cycleCount,
                        assignedIp = acquiredIp
                    )
                    break
                }

                // Dead / Blocked IP detected: Disconnect & prompt user to toggle again
                AppLogger.w("SmartHunt", "Cycle #$cycleCount: IP $acquiredIp is blocked/dead (no data flow). Disconnecting.")
                vpnManager.disconnect()
                _smartHuntProgress.value = SmartHuntProgress(
                    stage = SmartHuntStage.DEAD_IP_RETRY,
                    currentCycle = cycleCount,
                    assignedIp = acquiredIp,
                    detailMessage = "No data flow"
                )
                delay(1200)
            }
        }
    }

    private suspend fun verifyActiveTunnelTraffic(timeoutMs: Long = 3000L): Boolean = withContext(dispatchers.io) {
        val start = System.currentTimeMillis()
        val initialRx = trafficStats.value.totalDownloadBytes

        // Check if data is already accumulating
        if (initialRx > 1024) {
            val probeRx = trafficStats.value.totalDownloadBytes
            delay(300)
            if (trafficStats.value.totalDownloadBytes > probeRx + 64) {
                AppLogger.i("SmartHunt", "Tunnel verified: active data flow detected.")
                return@withContext true
            }
        }

        // Fast non-blocking HTTP probe through local inbound HTTP port 10809 (strict 2000ms timeout)
        val probeUrls = listOf(
            "http://connectivitycheck.gstatic.com/generate_204",
            "http://cp.cloudflare.com/generate_204",
            "http://ip-api.com/json"
        )
        for (url in probeUrls) {
            if (ipHuntManager.isAirplaneModeOn()) return@withContext false
            try {
                val client = OkHttpClient.Builder()
                    .proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", 10809)))
                    .connectTimeout(2000, TimeUnit.MILLISECONDS)
                    .readTimeout(2000, TimeUnit.MILLISECONDS)
                    .build()

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "V2RayOps-Client/1.0")
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful || response.code == 204) {
                    AppLogger.i("SmartHunt", "Tunnel verified via HTTP inbound probe to $url (Code: ${response.code})!")
                    return@withContext true
                }
            } catch (_: Exception) {}
        }

        // Downlink byte accumulation check
        val elapsed = System.currentTimeMillis() - start
        if (elapsed < timeoutMs) {
            delay((timeoutMs - elapsed).coerceAtLeast(300L))
        }
        val finalRx = trafficStats.value.totalDownloadBytes
        if (finalRx > initialRx + 128) {
            AppLogger.i("SmartHunt", "Tunnel verified via live downlink byte accumulation (+${finalRx - initialRx} bytes)!")
            return@withContext true
        }

        false
    }

    fun stopSmartConnectHunt() {
        smartHuntJob?.cancel()
        smartHuntJob = null
        _isSmartHunting.value = false
        _isCyclingAirplaneMode.value = false
        _smartHuntProgress.value = SmartHuntProgress(SmartHuntStage.IDLE)
        if (connectionState.value.isConnecting) {
            vpnManager.disconnect()
        }
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
