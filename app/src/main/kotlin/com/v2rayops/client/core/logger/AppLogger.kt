package com.v2rayops.client.core.logger

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedDeque

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

data class LogEntry(
    val id: Long = nextId(),
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwableStackTrace: String? = null
) {
    companion object {
        private var idCounter = 1L
        @Synchronized
        private fun nextId(): Long = idCounter++
    }

    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
            return sdf.format(Date(timestamp))
        }

    fun toFormattedLine(): String {
        val lvl = level.name.padEnd(5)
        val trace = if (throwableStackTrace != null) "\n$throwableStackTrace" else ""
        return "$formattedTime [$lvl] [$tag] $message$trace"
    }
}

object AppLogger {
    private const val DEFAULT_TAG = "V2RayOps"
    private const val MAX_LOGS = 100

    fun initLogFile(dir: java.io.File) {
        // Zero-disk-footprint policy: remove any legacy log files to conserve user device storage
        try {
            val legacyFile = java.io.File(dir, "app_debug.log")
            if (legacyFile.exists()) {
                legacyFile.delete()
            }
        } catch (_: Exception) {}
    }

    private val _logsFlow = MutableStateFlow<List<LogEntry>>(emptyList())
    val logsFlow: StateFlow<List<LogEntry>> = _logsFlow.asStateFlow()

    private val logBuffer = ConcurrentLinkedDeque<LogEntry>()

    init {
        i("AppLogger", "V2RayOPS In-Memory Logging Subsystem Initialized")
    }

    fun d(tag: String = DEFAULT_TAG, message: String) {
        Log.d(tag, message)
        append(LogLevel.DEBUG, tag, message, null)
    }

    fun i(tag: String = DEFAULT_TAG, message: String) {
        Log.i(tag, message)
        append(LogLevel.INFO, tag, message, null)
    }

    fun w(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        Log.w(tag, message, throwable)
        append(LogLevel.WARN, tag, message, throwable)
    }

    fun e(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        append(LogLevel.ERROR, tag, message, throwable)
    }

    private fun append(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        val entry = LogEntry(
            level = level,
            tag = tag,
            message = message,
            throwableStackTrace = throwable?.stackTraceToString()
        )
        logBuffer.addLast(entry)
        while (logBuffer.size > MAX_LOGS) {
            logBuffer.pollFirst()
        }
        _logsFlow.value = logBuffer.toList()
    }

    fun clear() {
        logBuffer.clear()
        _logsFlow.value = emptyList()
        i("AppLogger", "Logs cleared by user")
    }

    fun getFormattedLogs(): String {
        return logBuffer.joinToString("\n") { it.toFormattedLine() }
    }

    /**
     * Returns an optimized, structured list of the most recent warning/error events
     * and state transitions for failure diagnostics (zero irrelevant debug noise).
     */
    fun getDiagnosticBreadcrumbs(limit: Int = 25): List<String> {
        return logBuffer.filter { it.level == LogLevel.WARN || it.level == LogLevel.ERROR }
            .takeLast(limit)
            .map { it.toFormattedLine() }
    }

    fun getFullDiagnosticDump(): String {
        val sb = StringBuilder()
        sb.appendLine("=== V2RayOPS Application Diagnostics Breadcrumbs ===")
        sb.appendLine("Dump Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())}")
        sb.appendLine("----------------------------------------")
        getDiagnosticBreadcrumbs(50).forEach { sb.appendLine(it) }
        sb.appendLine("----------------------------------------")
        return sb.toString()
    }
}
