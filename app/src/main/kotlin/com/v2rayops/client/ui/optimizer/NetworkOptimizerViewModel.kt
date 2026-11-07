package com.v2rayops.client.ui.optimizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.v2rayops.client.core.logger.AppLogger
import com.v2rayops.client.network.NetworkOptimizationReport
import com.v2rayops.client.network.NetworkOptimizerEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NetworkOptimizerViewModel @Inject constructor(
    private val optimizerEngine: NetworkOptimizerEngine
) : ViewModel() {

    private val _report = MutableStateFlow<NetworkOptimizationReport?>(null)
    val report: StateFlow<NetworkOptimizationReport?> = _report.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _isApplying = MutableStateFlow(false)
    val isApplying: StateFlow<Boolean> = _isApplying.asStateFlow()

    private val _applySuccess = MutableStateFlow(false)
    val applySuccess: StateFlow<Boolean> = _applySuccess.asStateFlow()

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
}
