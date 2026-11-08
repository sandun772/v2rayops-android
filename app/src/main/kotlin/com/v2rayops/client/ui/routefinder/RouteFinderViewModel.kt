package com.v2rayops.client.ui.routefinder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.v2rayops.client.core.logger.AppLogger
import com.v2rayops.client.core.result.AppResult
import com.v2rayops.client.network.RouteScannerEngine
import com.v2rayops.client.network.RouteStrategy
import com.v2rayops.client.network.ScanProfile
import com.v2rayops.client.network.SniCategory
import com.v2rayops.client.network.SniRouteCandidate
import com.v2rayops.client.network.SniStatus
import com.v2rayops.client.vpn.VpnManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RouteFinderViewModel @Inject constructor(
    private val routeScannerEngine: RouteScannerEngine,
    private val vpnManager: VpnManager
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
}
