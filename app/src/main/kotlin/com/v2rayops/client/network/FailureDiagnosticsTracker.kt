package com.v2rayops.client.network

import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import com.v2rayops.client.BuildConfig
import com.v2rayops.client.core.dispatchers.AppDispatchers
import com.v2rayops.client.core.logger.AppLogger
import com.v2rayops.client.data.api.V2RayOpsClientApi
import com.v2rayops.client.data.api.dto.StructuredIncidentReportDto
import com.v2rayops.client.data.storage.SecureCredentialStore
import com.v2rayops.client.domain.model.VpnConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FailureDiagnosticsTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: V2RayOpsClientApi,
    private val secureStore: SecureCredentialStore,
    private val networkMonitor: NetworkMonitor,
    private val dispatchers: AppDispatchers
) {
    companion object {
        private const val TAG = "FailureDiagnostics"
        const val FAILURE_THRESHOLD = 3
        private const val COOLDOWN_MS = 60 * 60 * 1000L // 1 hour cooldown per incident cluster
    }

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)
    private val consecutiveFailures = AtomicInteger(0)
    private var lastReportTimeMs = 0L

    fun getConsecutiveFailures(): Int = consecutiveFailures.get()

    /**
     * Called whenever a VPN connection succeeds.
     * Resets failure counter to 0 — ensures ZERO diagnostics are uploaded during healthy operation.
     */
    fun recordSuccess() {
        val previous = consecutiveFailures.getAndSet(0)
        if (previous > 0) {
            AppLogger.i(TAG, "Connection recovered successfully. Reset consecutive failure counter from $previous to 0.")
        }
    }

    /**
     * Called whenever a connection attempt fails.
     * Only triggers report compilation and dispatch when threshold (3+) is reached.
     */
    fun recordFailure(
        reasonCode: String,
        activeConfig: VpnConfig? = null,
        error: Throwable? = null
    ) {
        val count = consecutiveFailures.incrementAndGet()
        AppLogger.w(TAG, "Connection failure recorded ($count/$FAILURE_THRESHOLD): $reasonCode", error)

        val now = System.currentTimeMillis()
        if (count >= FAILURE_THRESHOLD && (now - lastReportTimeMs > COOLDOWN_MS)) {
            lastReportTimeMs = now
            dispatchIncidentReport(reasonCode, count, activeConfig, error)
        }
    }

    private fun dispatchIncidentReport(
        reasonCode: String,
        failCount: Int,
        config: VpnConfig?,
        error: Throwable?
    ) {
        scope.launch {
            try {
                val customerCode = secureStore.getCustomerCode() ?: "anonymous"
                val netStatus = networkMonitor.getCurrentStatus()

                val telephonyManager = try {
                    context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                } catch (_: Exception) { null }
                val carrierName = telephonyManager?.networkOperatorName?.ifBlank { "Unknown" } ?: "Unknown"

                val breadcrumbs = AppLogger.getDiagnosticBreadcrumbs(25)
                val effectiveReason = if (error != null) "$reasonCode: ${error.message ?: error.javaClass.simpleName}" else reasonCode

                val report = StructuredIncidentReportDto(
                    customerCode = customerCode,
                    failureReasonCode = effectiveReason,
                    consecutiveFailures = failCount,
                    carrierName = carrierName,
                    networkType = netStatus.type.name,
                    endpointHost = config?.host ?: "Unknown",
                    endpointPort = config?.port ?: 443,
                    sni = config?.effectiveSni ?: "",
                    alpn = "http/1.1",
                    deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                    androidVersion = "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
                    appVersion = BuildConfig.VERSION_NAME,
                    timestamp = System.currentTimeMillis(),
                    breadcrumbs = breadcrumbs
                )

                AppLogger.i(TAG, "Dispatching optimized incident report to server ($reasonCode, $failCount attempts)...")
                val response = api.uploadIncidentReport(report)
                if (response.isSuccessful && response.body()?.ok == true) {
                    AppLogger.i(TAG, "Failure incident report logged on server (ID: ${response.body()?.reportId})")
                } else {
                    AppLogger.w(TAG, "Incident report ingestion returned HTTP ${response.code()}")
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "Failed to upload incident report: ${e.message}")
            }
        }
    }
}
