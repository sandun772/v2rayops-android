package com.v2rayops.client.ui.optimizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.v2rayops.client.core.logger.AppLogger
import com.v2rayops.client.network.BeastOptimizationReport
import com.v2rayops.client.network.BeastOptimizerEngine
import com.v2rayops.client.network.BeastProgressState
import com.v2rayops.client.network.BeastTestProfile
import com.v2rayops.client.network.CongestionStressProgress
import com.v2rayops.client.network.CongestionStressResult
import com.v2rayops.client.network.Download100MbProgress
import com.v2rayops.client.network.Download100MbResult
import com.v2rayops.client.network.DownloadBenchmarkProgress
import com.v2rayops.client.network.DownloadBenchmarkResult
import com.v2rayops.client.network.InAppBenchmarkReport
import com.v2rayops.client.network.NetworkOptimizationReport
import com.v2rayops.client.network.NetworkOptimizerEngine
import com.v2rayops.client.network.NetworkStressSuiteEngine
import com.v2rayops.client.network.SpeedTestProgress
import com.v2rayops.client.network.SpeedTestResult
import com.v2rayops.client.network.StabilityProgress
import com.v2rayops.client.network.StreamingBenchmarkResult
import com.v2rayops.client.network.ThroughputBenchmarkEngine
import com.v2rayops.client.network.TwoMinuteStabilityResult
import com.v2rayops.client.domain.model.ConnectionState
import com.v2rayops.client.domain.model.VpnConfig
import com.v2rayops.client.domain.repository.SubscriptionRepository
import com.v2rayops.client.network.FineTuneState
import com.v2rayops.client.network.OneClickFineTuneManager
import com.v2rayops.client.vpn.VpnManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NetworkOptimizerViewModel @Inject constructor(
    private val optimizerEngine: NetworkOptimizerEngine,
    private val throughputEngine: ThroughputBenchmarkEngine,
    private val beastOptimizerEngine: BeastOptimizerEngine,
    private val stressSuiteEngine: NetworkStressSuiteEngine,
    private val oneClickFineTuneManager: OneClickFineTuneManager,
    private val subscriptionRepository: SubscriptionRepository,
    private val vpnManager: VpnManager
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = vpnManager.connectionState
    val fineTuneState: StateFlow<FineTuneState> = oneClickFineTuneManager.state

    val selectedConfig: StateFlow<VpnConfig?> = subscriptionRepository.observeSelectedConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val beastProgressState: StateFlow<BeastProgressState> = beastOptimizerEngine.progressState

    fun startOneClickFineTune() {
        val target = selectedConfig.value
        oneClickFineTuneManager.startFineTune(target)
    }

    fun stopOneClickFineTune() {
        oneClickFineTuneManager.stopFineTune()
    }

    fun dismissFineTuneReport() {
        oneClickFineTuneManager.dismissReport()
    }

    private val _report = MutableStateFlow<NetworkOptimizationReport?>(null)
    val report: StateFlow<NetworkOptimizationReport?> = _report.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _isApplying = MutableStateFlow(false)
    val isApplying: StateFlow<Boolean> = _isApplying.asStateFlow()

    private val _applySuccess = MutableStateFlow(false)
    val applySuccess: StateFlow<Boolean> = _applySuccess.asStateFlow()

    // --- Legacy / Quick Micro-Payload Download & Streaming Benchmark States ---
    private val _downloadProgress = MutableStateFlow(DownloadBenchmarkProgress())
    val downloadProgress: StateFlow<DownloadBenchmarkProgress> = _downloadProgress.asStateFlow()

    private val _downloadResult = MutableStateFlow<DownloadBenchmarkResult?>(null)
    val downloadResult: StateFlow<DownloadBenchmarkResult?> = _downloadResult.asStateFlow()

    private val _streamingResult = MutableStateFlow<StreamingBenchmarkResult?>(null)
    val streamingResult: StateFlow<StreamingBenchmarkResult?> = _streamingResult.asStateFlow()

    private val _isTestingDownload = MutableStateFlow(false)
    val isTestingDownload: StateFlow<Boolean> = _isTestingDownload.asStateFlow()

    private val _isTestingStreaming = MutableStateFlow(false)
    val isTestingStreaming: StateFlow<Boolean> = _isTestingStreaming.asStateFlow()

    private val _benchmarkReport = MutableStateFlow<InAppBenchmarkReport?>(null)
    val benchmarkReport: StateFlow<InAppBenchmarkReport?> = _benchmarkReport.asStateFlow()

    private val _isApplyingBenchmark = MutableStateFlow(false)
    val isApplyingBenchmark: StateFlow<Boolean> = _isApplyingBenchmark.asStateFlow()

    private val _benchmarkApplySuccess = MutableStateFlow(false)
    val benchmarkApplySuccess: StateFlow<Boolean> = _benchmarkApplySuccess.asStateFlow()

    // --- 1. Two-Minute Deep Stability Audit States ---
    private var stabilityJob: Job? = null
    private val _stabilityProgress = MutableStateFlow(StabilityProgress())
    val stabilityProgress: StateFlow<StabilityProgress> = _stabilityProgress.asStateFlow()

    private val _stabilityResult = MutableStateFlow<TwoMinuteStabilityResult?>(null)
    val stabilityResult: StateFlow<TwoMinuteStabilityResult?> = _stabilityResult.asStateFlow()

    // --- 2. 100MB Download & Dev Report States ---
    private var download100MbJob: Job? = null
    private val _download100MbProgress = MutableStateFlow(Download100MbProgress())
    val download100MbProgress: StateFlow<Download100MbProgress> = _download100MbProgress.asStateFlow()

    private val _download100MbResult = MutableStateFlow<Download100MbResult?>(null)
    val download100MbResult: StateFlow<Download100MbResult?> = _download100MbResult.asStateFlow()

    // --- 3. Bidirectional Congestion / Stress Test States ---
    private var congestionJob: Job? = null
    private val _congestionProgress = MutableStateFlow(CongestionStressProgress())
    val congestionProgress: StateFlow<CongestionStressProgress> = _congestionProgress.asStateFlow()

    private val _congestionResult = MutableStateFlow<CongestionStressResult?>(null)
    val congestionResult: StateFlow<CongestionStressResult?> = _congestionResult.asStateFlow()

    // --- 4. Speedtest (Ping, Jitter, Download, Upload) States ---
    private var speedTestJob: Job? = null
    private val _speedTestProgress = MutableStateFlow(SpeedTestProgress())
    val speedTestProgress: StateFlow<SpeedTestProgress> = _speedTestProgress.asStateFlow()

    private val _speedTestResult = MutableStateFlow<SpeedTestResult?>(null)
    val speedTestResult: StateFlow<SpeedTestResult?> = _speedTestResult.asStateFlow()

    init {
        runAnalysis()
    }

    fun runAnalysis() {
        viewModelScope.launch {
            _isAnalyzing.value = true
            _applySuccess.value = false
            try {
                val result = optimizerEngine.runFullAnalysis()
                _report.value = result
            } catch (e: Exception) {
                AppLogger.e("NetworkOptimizerVM", "Optimization benchmark failed", e)
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun applyOptimization() {
        val currentReport = _report.value ?: return
        viewModelScope.launch {
            _isApplying.value = true
            try {
                optimizerEngine.applyOptimizedSettings(currentReport)
                _applySuccess.value = true
                _report.value = currentReport.copy(isAlreadyOptimized = true)
            } catch (e: Exception) {
                AppLogger.e("NetworkOptimizerVM", "Failed to apply optimization", e)
            } finally {
                _isApplying.value = false
            }
        }
    }

    fun runDownloadTest() {
        if (_isTestingDownload.value) return
        viewModelScope.launch {
            _isTestingDownload.value = true
            _benchmarkApplySuccess.value = false
            try {
                val dlRes = throughputEngine.runDownloadBenchmark { progress ->
                    _downloadProgress.value = progress
                }
                _downloadResult.value = dlRes
                recalculateBenchmarkReport()
            } catch (e: Exception) {
                AppLogger.e("NetworkOptimizerVM", "Download test failed: ${e.message}")
            } finally {
                _isTestingDownload.value = false
            }
        }
    }

    fun runStreamingTest() {
        if (_isTestingStreaming.value) return
        viewModelScope.launch {
            _isTestingStreaming.value = true
            _benchmarkApplySuccess.value = false
            try {
                val streamRes = throughputEngine.runStreamingBenchmark()
                _streamingResult.value = streamRes
                recalculateBenchmarkReport()
            } catch (e: Exception) {
                AppLogger.e("NetworkOptimizerVM", "Streaming test failed: ${e.message}")
            } finally {
                _isTestingStreaming.value = false
            }
        }
    }

    fun runFullThroughputTest() {
        viewModelScope.launch {
            runDownloadTest()
            runStreamingTest()
        }
    }

    private fun recalculateBenchmarkReport() {
        _benchmarkReport.value = throughputEngine.computeOptimizationRecommendations(
            download = _downloadResult.value,
            streaming = _streamingResult.value
        )
    }

    fun applyBenchmarkOptimization() {
        val rep = _benchmarkReport.value ?: return
        viewModelScope.launch {
            _isApplyingBenchmark.value = true
            try {
                throughputEngine.applyThroughputOptimizations(rep)
                _benchmarkApplySuccess.value = true
            } catch (e: Exception) {
                AppLogger.e("NetworkOptimizerVM", "Failed to apply throughput optimizations: ${e.message}")
            } finally {
                _isApplyingBenchmark.value = false
            }
        }
    }

    // =========================================================================
    // NEW: 2-MINUTE DEEP STABILITY AUDIT TRIGGERS
    // =========================================================================

    fun startTwoMinuteStabilityTest(totalSeconds: Int = 120) {
        if (_stabilityProgress.value.isRunning) return
        stabilityJob = viewModelScope.launch {
            try {
                _stabilityResult.value = null
                val result = stressSuiteEngine.runTwoMinuteStabilityTest(totalSeconds) { progress ->
                    _stabilityProgress.value = progress
                }
                _stabilityResult.value = result
            } catch (ce: CancellationException) {
                _stabilityProgress.value = _stabilityProgress.value.copy(
                    isRunning = false,
                    statusSummary = "Stability audit cancelled by user"
                )
            } catch (e: Exception) {
                AppLogger.e("NetworkOptimizerVM", "2-minute stability test error: ${e.message}")
                _stabilityProgress.value = _stabilityProgress.value.copy(
                    isRunning = false,
                    statusSummary = "Audit Error: ${e.message}"
                )
            }
        }
    }

    fun stopTwoMinuteStabilityTest() {
        stabilityJob?.cancel()
        stabilityJob = null
        _stabilityProgress.value = _stabilityProgress.value.copy(
            isRunning = false,
            statusSummary = "Audit stopped"
        )
    }

    // =========================================================================
    // NEW: 100MB DOWNLOAD & AUTO DEV REPORT TRIGGERS
    // =========================================================================

    fun start100MbDownloadTest() {
        if (_download100MbProgress.value.isRunning) return
        download100MbJob = viewModelScope.launch {
            try {
                _download100MbResult.value = null
                val result = stressSuiteEngine.run100MbDownloadTest { progress ->
                    _download100MbProgress.value = progress
                }
                _download100MbResult.value = result
            } catch (ce: CancellationException) {
                _download100MbProgress.value = _download100MbProgress.value.copy(
                    isRunning = false,
                    statusMessage = "100MB download cancelled"
                )
            } catch (e: Exception) {
                AppLogger.e("NetworkOptimizerVM", "100MB download test error: ${e.message}")
                _download100MbProgress.value = _download100MbProgress.value.copy(
                    isRunning = false,
                    statusMessage = "Test Error: ${e.message}"
                )
            }
        }
    }

    fun stop100MbDownloadTest() {
        download100MbJob?.cancel()
        download100MbJob = null
        _download100MbProgress.value = _download100MbProgress.value.copy(
            isRunning = false,
            statusMessage = "Download stopped"
        )
    }

    // =========================================================================
    // NEW: BIDIRECTIONAL CONGESTION / STRESS TEST TRIGGERS
    // =========================================================================

    fun startCongestionStressTest(durationSeconds: Int = 30) {
        if (_congestionProgress.value.isRunning) return
        congestionJob = viewModelScope.launch {
            try {
                _congestionResult.value = null
                val result = stressSuiteEngine.runCongestionStressTest(durationSeconds) { progress ->
                    _congestionProgress.value = progress
                }
                _congestionResult.value = result
            } catch (ce: CancellationException) {
                _congestionProgress.value = _congestionProgress.value.copy(
                    isRunning = false,
                    statusMessage = "Stress test cancelled"
                )
            } catch (e: Exception) {
                AppLogger.e("NetworkOptimizerVM", "Congestion stress test error: ${e.message}")
                _congestionProgress.value = _congestionProgress.value.copy(
                    isRunning = false,
                    statusMessage = "Error: ${e.message}"
                )
            }
        }
    }

    fun stopCongestionStressTest() {
        congestionJob?.cancel()
        congestionJob = null
        _congestionProgress.value = _congestionProgress.value.copy(
            isRunning = false,
            statusMessage = "Stress test stopped"
        )
    }

    // =========================================================================
    // NEW: FULL SPEEDTEST TRIGGERS
    // =========================================================================

    fun startFullSpeedTest() {
        if (_speedTestProgress.value.isRunning) return
        speedTestJob = viewModelScope.launch {
            try {
                _speedTestResult.value = null
                val result = stressSuiteEngine.runFullSpeedTest { progress ->
                    _speedTestProgress.value = progress
                }
                _speedTestResult.value = result
            } catch (ce: CancellationException) {
                _speedTestProgress.value = _speedTestProgress.value.copy(
                    isRunning = false,
                    statusText = "Speedtest cancelled"
                )
            } catch (e: Exception) {
                AppLogger.e("NetworkOptimizerVM", "Speedtest error: ${e.message}")
                _speedTestProgress.value = _speedTestProgress.value.copy(
                    isRunning = false,
                    statusText = "Error: ${e.message}"
                )
            }
        }
    }

    fun stopFullSpeedTest() {
        speedTestJob?.cancel()
        speedTestJob = null
        _speedTestProgress.value = _speedTestProgress.value.copy(
            isRunning = false,
            statusText = "Speedtest stopped"
        )
    }

    // --- Beast Optimization Suite Triggers ---
    fun startBeastOptimization(profile: BeastTestProfile) {
        beastOptimizerEngine.startBeastOptimization(profile)
    }

    fun stopBeastOptimization() {
        beastOptimizerEngine.stopOptimization()
    }

    fun applyCurrentBestBeast() {
        viewModelScope.launch {
            beastOptimizerEngine.applyCurrentBest()
        }
    }
}
