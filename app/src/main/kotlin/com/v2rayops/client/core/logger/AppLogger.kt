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
    private const val MAX_LOGS = 1000

    private val _logsFlow = MutableStateFlow<List<LogEntry>>(emptyList())
    val logsFlow: StateFlow<List<LogEntry>> = _logsFlow.asStateFlow()

    private val logBuffer = ConcurrentLinkedDeque<LogEntry>()

    init {
        i("AppLogger", "V2RayOPS Developer Logging Subsystem Initialized")
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

    fun getFullDiagnosticDump(): String {
        val sb = StringBuilder()
        sb.appendLine("=== V2RayOPS Application Dev Logs ===")
        sb.appendLine("Dump Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())}")
        sb.appendLine("Total In-App Log Entries: ${logBuffer.size}")
        sb.appendLine("----------------------------------------")
        sb.appendLine(getFormattedLogs())
        sb.appendLine("----------------------------------------")

        try {
            val process = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-v", "time", "-t", "120"))
            val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
            sb.appendLine("=== Process Logcat (Recent 120 Lines) ===")
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.appendLine(line)
            }
            process.waitFor()
        } catch (t: Throwable) {
            sb.appendLine("Could not read logcat: ${t.message}")
        }
        sb.appendLine("=== End of Diagnostics Dump ===")
        return sb.toString()
    }
}
