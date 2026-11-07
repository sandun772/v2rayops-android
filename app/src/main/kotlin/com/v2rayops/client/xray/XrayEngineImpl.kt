package com.v2rayops.client.xray

import android.content.Context
import com.v2rayops.client.core.logger.AppLogger
import com.v2rayops.client.domain.model.TrafficStats
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class XrayEngineImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : XrayEngine {

    companion object {
        private const val TAG = "XrayEngineImpl"
    }

    private val _state = MutableStateFlow<XrayState>(XrayState.Stopped)
    override val state: StateFlow<XrayState> = _state.asStateFlow()

    private var controller: CoreController? = null
    private var isInitialized = false

    private val totalUploadBytes = AtomicLong(0L)
    private val totalDownloadBytes = AtomicLong(0L)

    override val isBinaryAvailable: Boolean
        get() = try {
            Libv2ray.checkVersionX().isNotBlank()
        } catch (t: Throwable) {
            AppLogger.w(TAG, "Native Xray library check failed: ${t.message}")
            false
        }

    override val version: String
        get() = try {
            Libv2ray.checkVersionX()
        } catch (t: Throwable) {
            "Xray-core embedded"
        }

    override fun initialize(context: Context): Boolean {
        if (isInitialized) return true

        return try {
            val filesDir = context.filesDir
            if (!filesDir.exists()) {
                filesDir.mkdirs()
            }

            // Extract essential GeoIP and GeoSite routing databases from AAR assets
            copyAssetIfNeeded(context, "geoip.dat", File(filesDir, "geoip.dat"))
            copyAssetIfNeeded(context, "geosite.dat", File(filesDir, "geosite.dat"))

            Libv2ray.initCoreEnv(filesDir.absolutePath, "")
            isInitialized = true
            AppLogger.i(TAG, "Initialized Xray core environment at: ${filesDir.absolutePath}, version: $version")
            true
        } catch (t: Throwable) {
            AppLogger.e(TAG, "Failed to initialize Xray core environment", t)
            false
        }
    }

    private fun copyAssetIfNeeded(context: Context, assetName: String, destFile: File) {
        if (destFile.exists() && destFile.length() > 0) return
        try {
            context.assets.open(assetName).use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            AppLogger.d(TAG, "Extracted asset $assetName to ${destFile.absolutePath} (${destFile.length()} bytes)")
        } catch (e: Exception) {
            AppLogger.w(TAG, "Asset $assetName not copied: ${e.message}")
        }
    }

    override fun start(configJson: String, tunFd: Int): Result<Unit> {
        return try {
            if (!isInitialized) {
                initialize(context)
            }

            AppLogger.i(TAG, "Starting Xray loop (tunFd: $tunFd)...")
            _state.value = XrayState.Starting

            // Clean up any stale controller before starting a new one
            stop()

            val callbackHandler = object : CoreCallbackHandler {
                override fun startup(): Long {
                    AppLogger.i(TAG, "CoreCallbackHandler: startup() signaled from Go core")
                    return 0L
                }

                override fun shutdown(): Long {
                    AppLogger.i(TAG, "CoreCallbackHandler: shutdown() signaled from Go core")
                    return 0L
                }

                override fun onEmitStatus(code: Long, message: String?): Long {
                    AppLogger.d(TAG, "CoreCallbackHandler: status emit [$code]: $message")
                    return 0L
                }
            }

            val ctrl = Libv2ray.newCoreController(callbackHandler)
            ctrl.startLoop(configJson, tunFd)
            controller = ctrl

            // Reset session statistics counters
            totalUploadBytes.set(0L)
            totalDownloadBytes.set(0L)

            _state.value = XrayState.Running(System.currentTimeMillis())
            AppLogger.i(TAG, "Xray core loop successfully started and running")
            Result.success(Unit)
        } catch (t: Throwable) {
            AppLogger.e(TAG, "Error starting Xray core loop", t)
            _state.value = XrayState.Error(t.message ?: "Failed to start Xray core")
            Result.failure(t)
        }
    }

    override fun stop(): Result<Unit> {
        return try {
            val ctrl = controller
            if (ctrl != null) {
                AppLogger.i(TAG, "Stopping Xray core loop...")
                try {
                    if (ctrl.isRunning) {
                        ctrl.stopLoop()
                    }
                } catch (t: Throwable) {
                    AppLogger.w(TAG, "Exception during ctrl.stopLoop(): ${t.message}")
                }
                controller = null
            }
            _state.value = XrayState.Stopped
            Result.success(Unit)
        } catch (t: Throwable) {
            AppLogger.e(TAG, "Error stopping Xray core", t)
            Result.failure(t)
        }
    }

    override fun queryTrafficStats(): TrafficStats {
        val ctrl = controller
        if (ctrl == null || !ctrl.isRunning) {
            return TrafficStats(
                uploadBytesPerSec = 0L,
                downloadBytesPerSec = 0L,
                totalUploadBytes = totalUploadBytes.get(),
                totalDownloadBytes = totalDownloadBytes.get()
            )
        }

        return try {
            val rawStats = ctrl.queryAllOutboundTrafficStats() ?: ""
            var currentUplink = 0L
            var currentDownlink = 0L

            if (rawStats.isNotBlank()) {
                val records = rawStats.split(";")
                for (record in records) {
                    val parts = record.split(",")
                    if (parts.size >= 3) {
                        val direction = parts[1].trim()
                        val value = parts[2].trim().toLongOrNull() ?: 0L

                        if (direction.equals("uplink", ignoreCase = true)) {
                            currentUplink += value
                        } else if (direction.equals("downlink", ignoreCase = true)) {
                            currentDownlink += value
                        }
                    }
                }
            }

            val totalUp = totalUploadBytes.addAndGet(currentUplink)
            val totalDown = totalDownloadBytes.addAndGet(currentDownlink)

            TrafficStats(
                uploadBytesPerSec = currentUplink,
                downloadBytesPerSec = currentDownlink,
                totalUploadBytes = totalUp,
                totalDownloadBytes = totalDown
            )
        } catch (t: Throwable) {
            AppLogger.w(TAG, "Failed to query outbound traffic stats: ${t.message}")
            TrafficStats(
                uploadBytesPerSec = 0L,
                downloadBytesPerSec = 0L,
                totalUploadBytes = totalUploadBytes.get(),
                totalDownloadBytes = totalDownloadBytes.get()
            )
        }
    }
}
