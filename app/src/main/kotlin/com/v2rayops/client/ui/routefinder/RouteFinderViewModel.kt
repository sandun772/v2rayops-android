package com.v2rayops.client.ui.routefinder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.v2rayops.client.core.logger.AppLogger
import com.v2rayops.client.core.result.AppResult
import com.v2rayops.client.network.RouteScannerEngine
import com.v2rayops.client.network.SniCategory
import com.v2rayops.client.network.SniRouteCandidate
import com.v2rayops.client.network.SniStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RouteFinderViewModel @Inject constructor(
    private val routeScannerEngine: RouteScannerEngine
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

    private val _appliedSni = MutableStateFlow<String?>(null)
    val appliedSni: StateFlow<String?> = _appliedSni.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        startScan()
    }

    fun startScan() {
        if (_isScanning.value) return
        viewModelScope.launch {
            _isScanning.value = true
            _scannedCount.value = 0
            _statusMessage.value = null
            val currentList = _candidates.value.map { it.copy(status = SniStatus.TCP_FAILED, latencyMs = 0L) }
            _candidates.value = currentList
            _totalCount.value = currentList.size

            try {
                val results = routeScannerEngine.scanAll(currentList) { scanned, total, latest ->
                    _scannedCount.value = scanned
                    _totalCount.value = total
                    val updated = _candidates.value.map { if (it.sni == latest.sni) latest else it }
                    _candidates.value = updated
                }
                _candidates.value = results
                val best = results.firstOrNull { it.status == SniStatus.WS_READY || it.status == SniStatus.TLS_OK }
                if (best != null) {
                    _statusMessage.value = "Fastest Route Found: ${best.sni} (${best.latencyMs}ms)"
                }
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
                    _appliedSni.value = candidate.sni
                    _statusMessage.value = "Applied SNI '${candidate.sni}' to active configuration"
                }
                is AppResult.Failure -> {
                    _statusMessage.value = "Failed: ${res.error.message}"
                }
                is AppResult.Loading -> Unit
            }
        }
    }

    fun addCustomSni(rawSni: String) {
        val trimmed = rawSni.trim().lowercase()
        if (trimmed.isBlank() || _candidates.value.any { it.sni.equals(trimmed, ignoreCase = true) }) return

        val newCandidate = SniRouteCandidate(
            sni = trimmed,
            category = SniCategory.CUSTOM
        )
        val updated = listOf(newCandidate) + _candidates.value
        _candidates.value = updated
        _totalCount.value = updated.size

        viewModelScope.launch {
            val scanned = routeScannerEngine.scanCandidate(newCandidate)
            _candidates.value = _candidates.value.map { if (it.sni == scanned.sni) scanned else it }
                .sortedWith(
                    compareBy<SniRouteCandidate> {
                        when (it.status) {
                            SniStatus.WS_READY -> 0
                            SniStatus.TLS_OK -> 1
                            SniStatus.BLOCKED_TIMEOUT -> 2
                            SniStatus.TCP_FAILED -> 3
                        }
                    }.thenBy { it.latencyMs }
                )
        }
    }

    fun setCategory(category: SniCategory?) {
        _activeCategory.value = category
    }
}
