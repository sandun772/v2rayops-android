package com.v2rayops.client.ui.optimizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.v2rayops.client.core.logger.AppLogger
import com.v2rayops.client.network.BeastOptimizationReport
import com.v2rayops.client.network.BeastOptimizerEngine
import com.v2rayops.client.network.BeastProgressState
import com.v2rayops.client.network.BeastTestProfile
import com.v2rayops.client.network.DownloadBenchmarkProgress
import com.v2rayops.client.network.DownloadBenchmarkResult
import com.v2rayops.client.network.InAppBenchmarkReport
import com.v2rayops.client.network.NetworkOptimizationReport
import com.v2rayops.client.network.NetworkOptimizerEngine
import com.v2rayops.client.network.StreamingBenchmarkResult
import com.v2rayops.client.network.ThroughputBenchmarkEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NetworkOptimizerViewModel @Inject constructor(
    private val optimizerEngine: NetworkOptimizerEngine,
    private val throughputEngine: ThroughputBenchmarkEngine,
    private val beastOptimizerEngine: BeastOptimizerEngine
) : ViewModel() {

    val beastProgressState: StateFlow<BeastProgressState> = beastOptimizerEngine.progressState

    private val _report = MutableStateFlow<NetworkOptimizationReport?>(null)
    val report: StateFlow<NetworkOptimizationReport?> = _report.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _isApplying = MutableStateFlow(false)
    val isApplying: StateFlow<Boolean> = _isApplying.asStateFlow()

    private val _applySuccess = MutableStateFlow(false)
    val applySuccess: StateFlow<Boolean> = _applySuccess.asStateFlow()

    // --- In-App Download & Streaming Benchmark States ---
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
