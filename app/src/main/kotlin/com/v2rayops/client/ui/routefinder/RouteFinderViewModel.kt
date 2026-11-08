package com.v2rayops.client.ui.routefinder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.v2rayops.client.core.logger.AppLogger
import com.v2rayops.client.core.result.AppResult
import com.v2rayops.client.network.CongestionStressProgress
import com.v2rayops.client.network.CongestionStressResult
import com.v2rayops.client.network.Download100MbProgress
import com.v2rayops.client.network.Download100MbResult
import com.v2rayops.client.network.NetworkStressSuiteEngine
import com.v2rayops.client.network.RouteScannerEngine
import com.v2rayops.client.network.RouteStrategy
import com.v2rayops.client.network.ScanProfile
import com.v2rayops.client.network.SniCategory
import com.v2rayops.client.network.SniRouteCandidate
import com.v2rayops.client.network.SniStatus
import com.v2rayops.client.network.SpeedTestProgress
import com.v2rayops.client.network.SpeedTestResult
import com.v2rayops.client.network.StabilityProgress
import com.v2rayops.client.network.TwoMinuteStabilityResult
import com.v2rayops.client.vpn.VpnManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CandidateBenchmarkType(val title: String) {
    SPEEDTEST("FULL SPEEDTEST"),
    STABILITY_2MIN("2-MIN STABILITY AUDIT"),
    DOWNLOAD_100MB("100MB DOWNLOAD & DEV REPORT"),
    CONGESTION_STRESS("CONGESTION (DL+UL+PING) STRESS")
}

@HiltViewModel
class RouteFinderViewModel @Inject constructor(
    private val routeScannerEngine: RouteScannerEngine,
    private val vpnManager: VpnManager,
    private val stressSuiteEngine: NetworkStressSuiteEngine
) : ViewModel() {

    private val _candidates = MutableStateFlow<List<SniRouteCandidate>>(RouteScannerEngine.DEFAULT_CANDIDATES)
    val candidates: StateFlow<List<SniRouteCandidate>> = _candidates.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scannedCount = MutableStateFlow(0)
    val scannedCount: StateFlow<Int> = _scannedCount.asStateFlow()

    private val _totalCount = MutableStateFlow(RouteScannerEngine.DEFAULT_CANDIDATES.size)
    val totalCount: StateFlow<Int> = _totalCount.asStateFlow()

    private val _activeCategory = MutableStateFlow<SniCategory?>(null)
    val activeCategory: StateFlow<SniCategory?> = _activeCategory.asStateFlow()

    private val _activeStrategy = MutableStateFlow<RouteStrategy?>(null)
    val activeStrategy: StateFlow<RouteStrategy?> = _activeStrategy.asStateFlow()

    private val _activeScanProfile = MutableStateFlow<ScanProfile>(ScanProfile.QUICK_AUDIT)
    val activeScanProfile: StateFlow<ScanProfile> = _activeScanProfile.asStateFlow()

    private val _appliedRouteKey = MutableStateFlow<String?>(null)
    val appliedRouteKey: StateFlow<String?> = _appliedRouteKey.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _wordQuery = MutableStateFlow("")
    val wordQuery: StateFlow<String> = _wordQuery.asStateFlow()

    val vpnConnectionState = vpnManager.connectionState

    val physicalNetworkType: String
        get() = routeScannerEngine.getPhysicalNetworkType()

    // --- Targeted Candidate Benchmark States ---
    private var benchmarkJob: Job? = null

    private val _selectedCandidate = MutableStateFlow<SniRouteCandidate?>(null)
    val selectedCandidate: StateFlow<SniRouteCandidate?> = _selectedCandidate.asStateFlow()

    private val _activeBenchmarkType = MutableStateFlow<CandidateBenchmarkType>(CandidateBenchmarkType.SPEEDTEST)
    val activeBenchmarkType: StateFlow<CandidateBenchmarkType> = _activeBenchmarkType.asStateFlow()

    private val _isBenchmarkSheetOpen = MutableStateFlow(false)
    val isBenchmarkSheetOpen: StateFlow<Boolean> = _isBenchmarkSheetOpen.asStateFlow()

    // Individual progress & results
    private val _stabilityProgress = MutableStateFlow(StabilityProgress())
    val stabilityProgress: StateFlow<StabilityProgress> = _stabilityProgress.asStateFlow()
    private val _stabilityResult = MutableStateFlow<TwoMinuteStabilityResult?>(null)
    val stabilityResult: StateFlow<TwoMinuteStabilityResult?> = _stabilityResult.asStateFlow()

    private val _download100MbProgress = MutableStateFlow(Download100MbProgress())
    val download100MbProgress: StateFlow<Download100MbProgress> = _download100MbProgress.asStateFlow()
    private val _download100MbResult = MutableStateFlow<Download100MbResult?>(null)
    val download100MbResult: StateFlow<Download100MbResult?> = _download100MbResult.asStateFlow()

    private val _congestionProgress = MutableStateFlow(CongestionStressProgress())
    val congestionProgress: StateFlow<CongestionStressProgress> = _congestionProgress.asStateFlow()
    private val _congestionResult = MutableStateFlow<CongestionStressResult?>(null)
    val congestionResult: StateFlow<CongestionStressResult?> = _congestionResult.asStateFlow()

    private val _speedTestProgress = MutableStateFlow(SpeedTestProgress())
    val speedTestProgress: StateFlow<SpeedTestProgress> = _speedTestProgress.asStateFlow()
    private val _speedTestResult = MutableStateFlow<SpeedTestResult?>(null)
    val speedTestResult: StateFlow<SpeedTestResult?> = _speedTestResult.asStateFlow()

    // --- 3X-UI Inbound Guide & Multi-Port State ---
    private val _selectedInboundCandidate = MutableStateFlow<SniRouteCandidate?>(null)
    val selectedInboundCandidate: StateFlow<SniRouteCandidate?> = _selectedInboundCandidate.asStateFlow()

    private val _selectedInboundPort = MutableStateFlow(443)
    val selectedInboundPort: StateFlow<Int> = _selectedInboundPort.asStateFlow()

    private val _inboundSpec = MutableStateFlow<com.v2rayops.client.network.ThreeXUiInboundSpec?>(null)
    val inboundSpec: StateFlow<com.v2rayops.client.network.ThreeXUiInboundSpec?> = _inboundSpec.asStateFlow()

    private val _isInboundGuideOpen = MutableStateFlow(false)
    val isInboundGuideOpen: StateFlow<Boolean> = _isInboundGuideOpen.asStateFlow()

    private val _isAuditingPorts = MutableStateFlow(false)
    val isAuditingPorts: StateFlow<Boolean> = _isAuditingPorts.asStateFlow()

    init {
        startScan()
    }

    fun setWordQuery(query: String) {
        _wordQuery.value = query
    }

    fun pauseVpn() {
        viewModelScope.launch {
            vpnManager.disconnect()
            _statusMessage.value = "VPN paused for pure carrier scanning"
        }
    }

    fun scanWords(rawWords: String) {
        val splitWords = rawWords.split(",", " ", ";", "\n").filter { it.isNotBlank() }
        if (splitWords.isEmpty()) return

        val newCandidates = RouteScannerEngine.generateCandidatesFromWords(splitWords)
        val existingKeys = _candidates.value.map { "${it.strategy.name}:${it.domain}" }.toSet()
        val uniqueNew = newCandidates.filter { "${it.strategy.name}:${it.domain}" !in existingKeys }

        val combined = uniqueNew + _candidates.value
        _candidates.value = combined
        _totalCount.value = combined.size
        _wordQuery.value = ""

        startScan()
    }

    fun scanPresetPack(packType: String) {
        val words = when (packType.lowercase()) {
            "edu" -> listOf("zoom", "teams", "learn", "dialog", "mobitel", "slt", "office")
            "tiktok" -> listOf("tiktok", "tiktokcdn", "p16-va", "v16m", "byteoversea")
            "social" -> listOf("whatsapp", "facebook", "instagram", "meta")
            "cloudflare" -> listOf("cloudflare", "one.one.one.one", "cdn")
            else -> listOf("zoom", "dialog", "tiktok", "teams")
        }
        val generated = RouteScannerEngine.generateCandidatesFromWords(words)
        val existingKeys = _candidates.value.map { "${it.strategy.name}:${it.domain}" }.toSet()
        val uniqueNew = generated.filter { "${it.strategy.name}:${it.domain}" !in existingKeys }

        val combined = uniqueNew + _candidates.value
        _candidates.value = combined
        _totalCount.value = combined.size
        startScan()
    }

    fun startScan() {
        if (_isScanning.value) return
        viewModelScope.launch {
            _isScanning.value = true
            _scannedCount.value = 0
            _statusMessage.value = null
            val currentList = _candidates.value.map {
                it.copy(
                    status = SniStatus.TCP_FAILED,
                    latencyMs = 0L,
                    avgLatencyMs = 0L,
                    stabilityScore = 0,
                    packetLossPercent = 0
                )
            }
            _candidates.value = currentList
            _totalCount.value = currentList.size

            try {
                val results = routeScannerEngine.scanAll(currentList, _activeScanProfile.value) { scanned, total, latest ->
                    _scannedCount.value = scanned
                    _totalCount.value = total
                    val updated = _candidates.value.map {
                        if (it.domain == latest.domain && it.strategy == latest.strategy) latest else it
                    }
                    _candidates.value = updated
                }
                _candidates.value = results
                val best = results.firstOrNull { it.status == SniStatus.WS_READY || it.status == SniStatus.TLS_OK }
                if (best != null) {
                    _statusMessage.value = "Best Verified: [${best.strategy.shortTag}] ${best.domain} (${best.stabilityScore}% Stable • ${best.avgLatencyMs}ms • ${best.packetLossPercent}% Loss)"
                }
            } catch (ce: CancellationException) {
                // Expected when user navigates away or starts new scan
            } catch (e: Exception) {
                AppLogger.e("RouteFinderVM", "Error running route scan: ${e.message}")
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun applyCandidate(candidate: SniRouteCandidate) {
        viewModelScope.launch {
            when (val res = routeScannerEngine.applySniRoute(candidate)) {
                is AppResult.Success -> {
                    _appliedRouteKey.value = "${candidate.strategy.name}:${candidate.domain}"
                    _statusMessage.value = "Applied [${candidate.strategy.shortTag}] '${candidate.domain}' to active configuration"
                }
                is AppResult.Failure -> {
                    _statusMessage.value = "Failed: ${res.error.message}"
                }
                is AppResult.Loading -> Unit
            }
        }
    }

    // --- Candidate Benchmark Management ---
    fun openBenchmark(candidate: SniRouteCandidate, type: CandidateBenchmarkType = CandidateBenchmarkType.SPEEDTEST) {
        _selectedCandidate.value = candidate
        _activeBenchmarkType.value = type
        _isBenchmarkSheetOpen.value = true
        // Reset results
        _stabilityResult.value = null
        _download100MbResult.value = null
        _congestionResult.value = null
        _speedTestResult.value = null
    }

    fun closeBenchmark() {
        stopActiveBenchmark()
        _isBenchmarkSheetOpen.value = false
    }

    fun selectBenchmarkType(type: CandidateBenchmarkType) {
        if (_activeBenchmarkType.value != type) {
            stopActiveBenchmark()
            _activeBenchmarkType.value = type
        }
    }

    fun applyAndRunBenchmark(candidate: SniRouteCandidate, type: CandidateBenchmarkType) {
        viewModelScope.launch {
            applyCandidate(candidate)
            openBenchmark(candidate, type)
            runActiveBenchmark()
        }
    }

    fun runActiveBenchmark() {
        val candidate = _selectedCandidate.value ?: return
        stopActiveBenchmark()
        AppLogger.i("RouteFinderVM", "Starting ${_activeBenchmarkType.value.title} on candidate '${candidate.domain}'")

        benchmarkJob = viewModelScope.launch {
            try {
                when (_activeBenchmarkType.value) {
                    CandidateBenchmarkType.SPEEDTEST -> {
                        _speedTestResult.value = null
                        val res = stressSuiteEngine.runFullSpeedTest { _speedTestProgress.value = it }
                        _speedTestResult.value = res
                    }
                    CandidateBenchmarkType.STABILITY_2MIN -> {
                        _stabilityResult.value = null
                        val res = stressSuiteEngine.runTwoMinuteStabilityTest(120) { _stabilityProgress.value = it }
                        _stabilityResult.value = res
                    }
                    CandidateBenchmarkType.DOWNLOAD_100MB -> {
                        _download100MbResult.value = null
                        val res = stressSuiteEngine.run100MbDownloadTest { _download100MbProgress.value = it }
                        _download100MbResult.value = res
                    }
                    CandidateBenchmarkType.CONGESTION_STRESS -> {
                        _congestionResult.value = null
                        val res = stressSuiteEngine.runCongestionStressTest(30) { _congestionProgress.value = it }
                        _congestionResult.value = res
                    }
                }
            } catch (ce: CancellationException) {
                // Ignore cancel
            } catch (e: Exception) {
                AppLogger.e("RouteFinderVM", "Candidate benchmark failed: ${e.message}")
            }
        }
    }

    fun stopActiveBenchmark() {
        benchmarkJob?.cancel()
        benchmarkJob = null
        _speedTestProgress.value = _speedTestProgress.value.copy(isRunning = false)
        _stabilityProgress.value = _stabilityProgress.value.copy(isRunning = false)
        _download100MbProgress.value = _download100MbProgress.value.copy(isRunning = false)
        _congestionProgress.value = _congestionProgress.value.copy(isRunning = false)
    }

    fun setScanProfile(profile: ScanProfile) {
        if (_activeScanProfile.value == profile) return
        _activeScanProfile.value = profile
        startScan()
    }

    fun addCustomHost(rawDomain: String, strategy: RouteStrategy) {
        val trimmed = rawDomain.trim().lowercase()
        if (trimmed.isBlank() || _candidates.value.any { it.domain.equals(trimmed, ignoreCase = true) && it.strategy == strategy }) return

        val newCandidate = if (strategy == RouteStrategy.ADDR_SNI) {
            SniRouteCandidate(
                domain = trimmed,
                strategy = RouteStrategy.ADDR_SNI,
                category = SniCategory.CUSTOM,
                targetHost = trimmed,
                targetPort = 443,
                sni = RouteScannerEngine.DEFAULT_VPS_DOMAIN,
                wsHost = RouteScannerEngine.DEFAULT_VPS_DOMAIN,
                wsPath = "/v2rayops"
            )
        } else {
            SniRouteCandidate(
                domain = trimmed,
                strategy = RouteStrategy.STANDARD_SNI,
                category = SniCategory.CUSTOM,
                targetHost = RouteScannerEngine.DEFAULT_VPS_IP,
                targetPort = 443,
                sni = trimmed,
                wsHost = RouteScannerEngine.DEFAULT_VPS_DOMAIN,
                wsPath = "/v2rayops"
            )
        }

        val updated = listOf(newCandidate) + _candidates.value
        _candidates.value = updated
        _totalCount.value = updated.size

        viewModelScope.launch {
            try {
                val scanned = routeScannerEngine.scanCandidate(newCandidate, _activeScanProfile.value)
                _candidates.value = _candidates.value.map {
                    if (it.domain == scanned.domain && it.strategy == scanned.strategy) scanned else it
                }.sortedWith(
                    compareBy<SniRouteCandidate> {
                        when {
                            it.status == SniStatus.WS_READY && it.stabilityScore >= 80 -> 0
                            it.status == SniStatus.WS_READY -> 1
                            it.status == SniStatus.TLS_OK && it.stabilityScore >= 80 -> 2
                            it.status == SniStatus.TLS_OK -> 3
                            it.status == SniStatus.BLOCKED_TIMEOUT -> 4
                            else -> 5
                        }
                    }.thenByDescending { it.stabilityScore }
                     .thenBy { it.packetLossPercent }
                     .thenBy { it.jitterMs }
                     .thenBy { it.avgLatencyMs.takeIf { lat -> lat > 0 } ?: Long.MAX_VALUE }
                )
            } catch (ce: CancellationException) {
                // Ignore cancellation
            } catch (e: Exception) {
                AppLogger.e("RouteFinderVM", "Failed custom candidate probe: ${e.message}")
            }
        }
    }

    fun setCategory(category: SniCategory?) {
        _activeCategory.value = category
    }

    fun setStrategy(strategy: RouteStrategy?) {
        _activeStrategy.value = strategy
    }

    fun openInboundGuide(candidate: SniRouteCandidate, port: Int = candidate.targetPort) {
        _selectedInboundCandidate.value = candidate
        _selectedInboundPort.value = port
        _isInboundGuideOpen.value = true
        viewModelScope.launch {
            _inboundSpec.value = routeScannerEngine.generate3xUiInbound(candidate, port)
        }
    }

    fun selectInboundPort(port: Int) {
        val candidate = _selectedInboundCandidate.value ?: return
        _selectedInboundPort.value = port
        viewModelScope.launch {
            _inboundSpec.value = routeScannerEngine.generate3xUiInbound(candidate, port)
        }
    }

    fun closeInboundGuide() {
        _isInboundGuideOpen.value = false
        _inboundSpec.value = null
    }

    fun selectCandidatePort(candidate: SniRouteCandidate, port: Int) {
        val updated = _candidates.value.map {
            if (it.domain == candidate.domain && it.strategy == candidate.strategy) {
                it.copy(targetPort = port)
            } else it
        }
        _candidates.value = updated
        _statusMessage.value = "Selected port $port for [${candidate.strategy.shortTag}] ${candidate.domain}"
    }

    fun runFullPortAudit(candidate: SniRouteCandidate) {
        viewModelScope.launch {
            _isAuditingPorts.value = true
            try {
                val auditResults = routeScannerEngine.auditAllPorts(candidate)
                val openPorts = auditResults.filter { it.isOpen }.map { it.port }
                val updated = _candidates.value.map {
                    if (it.domain == candidate.domain && it.strategy == candidate.strategy) {
                        it.copy(
                            verifiedPorts = if (openPorts.isNotEmpty()) openPorts else listOf(candidate.targetPort),
                            portAuditResults = auditResults
                        )
                    } else it
                }
                _candidates.value = updated
                val updatedCandidate = updated.firstOrNull { it.domain == candidate.domain && it.strategy == candidate.strategy }
                if (updatedCandidate != null) {
                    _selectedInboundCandidate.value = updatedCandidate
                    _inboundSpec.value = routeScannerEngine.generate3xUiInbound(updatedCandidate, _selectedInboundPort.value)
                }
                _statusMessage.value = "Audited 13 ports for ${candidate.domain}: ${openPorts.size} open ports discovered"
            } catch (e: Exception) {
                AppLogger.e("RouteFinderVM", "Port audit failed: ${e.message}")
            } finally {
                _isAuditingPorts.value = false
            }
        }
    }

    fun applyPortRoute(candidate: SniRouteCandidate, port: Int) {
        val tunedCandidate = candidate.copy(targetPort = port)
        applyCandidate(tunedCandidate)
    }
}
