package com.v2rayops.client.network.iphunt

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.provider.Settings
import com.v2rayops.client.core.dispatchers.AppDispatchers
import com.v2rayops.client.core.logger.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class IpHuntState(
    val isHunting: Boolean = false,
    val currentIp: String? = null,
    val selectedTargetPrefix: String = "100.",
    val isMatched: Boolean = false,
    val attempts: Int = 0,
    val isRootGranted: Boolean = false,
    val isAccessibilityActive: Boolean = false,
    val isSecureSettingsGranted: Boolean = false,
    val isCyclingNow: Boolean = false,
    val statusMessage: String = "Ready to hunt"
) {
    val isFullyAutomated: Boolean
        get() = isRootGranted || isAccessibilityActive || isSecureSettingsGranted
}

@Singleton
class IpHuntManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: AppDispatchers
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)

    private val _state = MutableStateFlow(
        IpHuntState(
            currentIp = getMobileDataIp()
        )
    )
    val state: StateFlow<IpHuntState> = _state.asStateFlow()

    private var huntingJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var lastObservedIp: String? = null

    init {
        val initialIp = getMobileDataIp()
        lastObservedIp = initialIp
        _state.value = _state.value.copy(
            currentIp = initialIp,
            isMatched = isIpMatching(initialIp, _state.value.selectedTargetPrefix)
        )
        refreshAutomationCapabilities()
    }

    /**
     * Inspects device for all available automation mechanisms:
     * 1. Superuser / Root (su)
     * 2. Accessibility Service (IpHuntAccessibilityService)
     * 3. WRITE_SECURE_SETTINGS permission (via ADB / Shizuku)
     */
    fun refreshAutomationCapabilities() {
        scope.launch {
            val rootOk = checkRootPermission()
            val accessibilityOk = IpHuntAccessibilityService.isRunning() ||
                    IpHuntAccessibilityService.isAccessibilityEnabled(context)
            val secureSettingsOk = checkSecureSettingsPermission()

            _state.value = _state.value.copy(
                isRootGranted = rootOk,
                isAccessibilityActive = accessibilityOk,
                isSecureSettingsGranted = secureSettingsOk
            )
        }
    }

    suspend fun checkRootPermission(): Boolean = withContext(dispatchers.io) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val reader = process.inputStream.bufferedReader()
            val output = reader.readLine().orEmpty()
            val code = process.waitFor()
            code == 0 && output.contains("uid=0")
        } catch (_: Exception) {
            false
        }
    }

    private fun checkSecureSettingsPermission(): Boolean {
        return try {
            context.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Extracts active cellular IPv4 address from network interfaces.
     * Prioritizes cellular radio interfaces (rmnet, ccmni, pdp, wwan, cell, etc.).
     */
    fun getMobileDataIp(): String? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces() ?: return null)
            val candidates = mutableListOf<Pair<String, String>>()

            for (iface in interfaces) {
                if (!iface.isUp || iface.isLoopback) continue
                val name = iface.name.lowercase()
                for (addr in Collections.list(iface.inetAddresses)) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress ?: continue
                        candidates.add(name to host)
                    }
                }
            }

            // 1. Cellular interface priority
            val cellular = candidates.find { (name, _) ->
                name.startsWith("rmnet") || name.startsWith("ccmni") ||
                name.startsWith("pdp") || name.startsWith("v4-") ||
                name.startsWith("clat") || name.startsWith("wwan") ||
                name.contains("cell") || name.startsWith("usb")
            }
            if (cellular != null) {
                return cellular.second
            }

            // 2. Carrier CGNAT (100.x) or Telco Private (10.x)
            val carrierSubnet = candidates.find { (_, ip) ->
                ip.startsWith("100.") || ip.startsWith("10.")
            }
            if (carrierSubnet != null) {
                return carrierSubnet.second
            }

            // 3. Fallback: exclude wlan (Wi-Fi) if possible
            val nonWifi = candidates.find { (name, _) -> !name.startsWith("wlan") }
            return nonWifi?.second ?: candidates.firstOrNull()?.second
        } catch (e: Exception) {
            AppLogger.w("IpHuntManager", "Failed to query network interfaces: ${e.message}")
            return null
        }
    }

    fun isIpMatching(ip: String?, targetPrefix: String): Boolean {
        if (ip.isNullOrBlank() || targetPrefix.isBlank()) return false
        val cleanPrefixes = targetPrefix.split(",", " ", ";")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return cleanPrefixes.any { prefix ->
            ip.startsWith(prefix, ignoreCase = true)
        }
    }

    fun setTargetPrefix(prefix: String) {
        val clean = prefix.trim().ifBlank { "100." }
        val current = _state.value.currentIp
        val matched = isIpMatching(current, clean)
        _state.value = _state.value.copy(
            selectedTargetPrefix = clean,
            isMatched = matched
        )
    }

    fun isAirplaneModeOn(): Boolean = IpHuntAccessibilityService.isAirplaneModeOn(context)

    suspend fun waitForConnectionStabilization(timeoutMs: Long = 14000L): Boolean = withContext(dispatchers.io) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val start = System.currentTimeMillis()

        AppLogger.i("IpHuntManager", "Stabilization Phase 1: Checking Airplane Mode...")
        // Phase 1: Wait for Airplane Mode to be completely OFF
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (!isAirplaneModeOn()) break
            delay(250)
        }

        if (isAirplaneModeOn()) {
            AppLogger.w("IpHuntManager", "Airplane Mode is still ON. Cannot stabilize connection.")
            return@withContext false
        }

        AppLogger.i("IpHuntManager", "Stabilization Phase 2: Airplane Mode is OFF. Waiting for mobile data & IP assignment...")

        // Phase 2: Wait for cellular or network connectivity and an assigned IP address
        var networkReady = false
        while (System.currentTimeMillis() - start < timeoutMs) {
            // Safety: if airplane mode turned back on, abort
            if (isAirplaneModeOn()) return@withContext false

            val ip = getMobileDataIp()
            val activeNet = cm?.activeNetwork
            val caps = if (activeNet != null) cm.getNetworkCapabilities(activeNet) else null

            val hasDataCapability = caps != null && (
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            )

            if (!ip.isNullOrBlank() && hasDataCapability) {
                networkReady = true
                AppLogger.i("IpHuntManager", "Cellular data link established! Assigned IP: $ip")
                break
            }
            delay(350)
        }

        if (!networkReady) {
            AppLogger.w("IpHuntManager", "Cellular data did not stabilize within ${timeoutMs}ms")
            return@withContext false
        }

        // Phase 3: Crucial 2000ms settling delay for cellular baseband radio & kernel routing tables
        AppLogger.i("IpHuntManager", "Stabilization Phase 3: Settling radio connection for 2000ms...")
        delay(2000)
        true
    }

    suspend fun ensureAirplaneModeOn(): Boolean = withContext(dispatchers.io) {
        if (isAirplaneModeOn()) return@withContext true

        // Tier 1: Root
        if (_state.value.isRootGranted) {
            val rootOk = setAirplaneModeViaRoot(true)
            if (rootOk && isAirplaneModeOn()) return@withContext true
        }

        // Tier 2: Real Hardware Switch Toggle via Accessibility Service
        var service = IpHuntAccessibilityService.instance
        if (service == null) {
            for (i in 0 until 25) {
                delay(100)
                service = IpHuntAccessibilityService.instance
                if (service != null) break
            }
        }

        if (service != null) {
            return@withContext service.setAirplaneMode(true)
        }

        false
    }

    suspend fun ensureAirplaneModeOff(): Boolean = withContext(dispatchers.io) {
        if (!isAirplaneModeOn()) return@withContext true

        // Tier 1: Root
        if (_state.value.isRootGranted) {
            val rootOk = setAirplaneModeViaRoot(false)
            if (rootOk && !isAirplaneModeOn()) return@withContext true
        }

        // Tier 2: Real Hardware Switch Toggle via Accessibility Service
        var service = IpHuntAccessibilityService.instance
        if (service == null) {
            for (i in 0 until 25) {
                delay(100)
                service = IpHuntAccessibilityService.instance
                if (service != null) break
            }
        }

        if (service != null) {
            return@withContext service.setAirplaneMode(false)
        }

        false
    }

    private suspend fun setAirplaneModeViaRoot(enable: Boolean): Boolean = withContext(dispatchers.io) {
        try {
            val cmd = if (enable) "cmd connectivity airplane-mode enable" else "cmd connectivity airplane-mode disable"
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            p.waitFor() == 0
        } catch (_: Exception) {
            false
        }
    }

    suspend fun executeSingleAutoCycle(): Boolean {
        refreshAutomationCapabilities()
        return executeAutoAirplaneToggle()
    }

    /**
     * Executes automatic airplane mode toggle cycle using the highest available privilege tier:
     * - Tier 1: Root command (su) -> background silent
     * - Tier 2: WRITE_SECURE_SETTINGS -> system settings write & broadcast
     * - Tier 3: Accessibility Service -> automated UI toggle without root
     */
    private suspend fun executeAutoAirplaneToggle(): Boolean {
        // Tier 1: Root
        if (_state.value.isRootGranted) {
            val rootSuccess = executeRootToggle()
            if (rootSuccess) return true
        }

        // Tier 2: WRITE_SECURE_SETTINGS
        if (_state.value.isSecureSettingsGranted) {
            val secureSuccess = executeSecureSettingsToggle()
            if (secureSuccess) return true
        }

        // Tier 3: Accessibility Service (For unrooted devices)
        var accessibilityService = IpHuntAccessibilityService.instance
        if (accessibilityService == null && IpHuntAccessibilityService.isAccessibilityEnabled(context)) {
            for (i in 0 until 10) {
                delay(100)
                accessibilityService = IpHuntAccessibilityService.instance
                if (accessibilityService != null) break
            }
        }

        if (accessibilityService != null) {
            AppLogger.d("IpHuntManager", "Triggering Accessibility Service automated toggle...")
            return suspendCancellableCoroutine { cont ->
                accessibilityService.performAutoToggleCycle { success ->
                    if (cont.isActive) {
                        cont.resume(success)
                    }
                }
            }
        }

        return false
    }

    private suspend fun executeRootToggle(): Boolean = withContext(dispatchers.io) {
        // Method 1: cmd connectivity airplane-mode
        try {
            val p1 = Runtime.getRuntime().exec(arrayOf("su", "-c", "cmd connectivity airplane-mode enable"))
            if (p1.waitFor() == 0) {
                delay(1500)
                val p2 = Runtime.getRuntime().exec(arrayOf("su", "-c", "cmd connectivity airplane-mode disable"))
                if (p2.waitFor() == 0) return@withContext true
            }
        } catch (_: Exception) {}

        // Method 2: svc data toggle
        try {
            val p3 = Runtime.getRuntime().exec(arrayOf("su", "-c", "svc data disable"))
            if (p3.waitFor() == 0) {
                delay(1500)
                val p4 = Runtime.getRuntime().exec(arrayOf("su", "-c", "svc data enable"))
                if (p4.waitFor() == 0) return@withContext true
            }
        } catch (_: Exception) {}

        false
    }

    private suspend fun executeSecureSettingsToggle(): Boolean = withContext(dispatchers.io) {
        try {
            Settings.Global.putInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 1)
            try {
                val onIntent = Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED).apply {
                    putExtra("state", true)
                }
                context.sendBroadcast(onIntent)
            } catch (_: Exception) {}

            delay(1500)

            Settings.Global.putInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0)
            try {
                val offIntent = Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED).apply {
                    putExtra("state", false)
                }
                context.sendBroadcast(offIntent)
            } catch (_: Exception) {}
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Starts the IP Hunting loop.
     * If automated (Root, Accessibility, or Secure Settings), continuously cycles until target IP matches.
     * If unprivileged, monitors IP actively and provides one-tap permission setup.
     */
    fun startAutoHunt(
        targetPrefix: String = _state.value.selectedTargetPrefix,
        onMatched: (String) -> Unit = {}
    ) {
        stopHunt()

        val cleanPrefix = targetPrefix.trim().ifBlank { "100." }
        val initialIp = getMobileDataIp()
        lastObservedIp = initialIp

        if (isIpMatching(initialIp, cleanPrefix)) {
            AppLogger.i("IpHuntManager", "Current IP $initialIp already matches target $cleanPrefix")
            _state.value = _state.value.copy(
                isHunting = false,
                isMatched = true,
                currentIp = initialIp,
                statusMessage = "Target IP already acquired: $initialIp"
            )
            onMatched(initialIp!!)
            return
        }

        refreshAutomationCapabilities()

        _state.value = _state.value.copy(
            isHunting = true,
            selectedTargetPrefix = cleanPrefix,
            isMatched = false,
            attempts = 0,
            statusMessage = "Starting IP hunt for $cleanPrefix*..."
        )

        registerNetworkWatcher(cleanPrefix, onMatched)

        huntingJob = scope.launch {
            var cycleCount = 0

            while (isActive && _state.value.isHunting) {
                // Check if automation mechanism is available
                val isAutomated = _state.value.isFullyAutomated

                if (!isAutomated) {
                    _state.value = _state.value.copy(
                        isCyclingNow = false,
                        statusMessage = "Permission required to automate airplane mode cycling on this device."
                    )
                    // Wait for user to grant permission or toggle manually
                    delay(2000)
                    refreshAutomationCapabilities()
                    continue
                }

                cycleCount++
                _state.value = _state.value.copy(
                    attempts = cycleCount,
                    isCyclingNow = true,
                    statusMessage = "Cycle #$cycleCount: Auto-toggling Airplane Mode ON..."
                )

                // Execute automated toggle cycle
                val success = executeAutoAirplaneToggle()

                if (!success) {
                    AppLogger.w("IpHuntManager", "Automated toggle failed or timed out.")
                    _state.value = _state.value.copy(
                        isCyclingNow = false,
                        statusMessage = "Auto-toggle interrupted. Re-checking permissions..."
                    )
                    delay(1500)
                    refreshAutomationCapabilities()
                    continue
                }

                _state.value = _state.value.copy(
                    statusMessage = "Cycle #$cycleCount: Airplane OFF. Awaiting carrier IP from tower..."
                )

                // Wait and poll for new IP assignment (up to 8 seconds)
                var acquiredIp: String? = null
                val pollStart = System.currentTimeMillis()
                while (isActive && System.currentTimeMillis() - pollStart < 8000L) {
                    delay(450)
                    val ip = getMobileDataIp()
                    if (ip != null) {
                        acquiredIp = ip
                        _state.value = _state.value.copy(currentIp = ip)
                        if (isIpMatching(ip, cleanPrefix)) {
                            break
                        }
                    }
                }

                // Verify result of cycle
                if (acquiredIp != null && isIpMatching(acquiredIp, cleanPrefix)) {
                    AppLogger.i("IpHuntManager", "TARGET IP ACQUIRED: $acquiredIp on cycle #$cycleCount!")
                    _state.value = _state.value.copy(
                        isHunting = false,
                        isMatched = true,
                        isCyclingNow = false,
                        currentIp = acquiredIp,
                        attempts = cycleCount,
                        statusMessage = "TARGET ACQUIRED: $acquiredIp"
                    )
                    withContext(dispatchers.main) {
                        onMatched(acquiredIp)
                    }
                    break
                } else {
                    val mismatchMsg = if (acquiredIp != null) "Assigned $acquiredIp (Mismatch) -> Auto-recycling..." else "No signal -> Retrying..."
                    _state.value = _state.value.copy(
                        isCyclingNow = false,
                        statusMessage = mismatchMsg
                    )
                    delay(1200) // Brief pause before starting next cycle
                }
            }
        }
    }

    private fun registerNetworkWatcher(targetPrefix: String, onMatched: (String) -> Unit) {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    checkAndMatch(targetPrefix, onMatched)
                }

                override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                    checkAndMatch(targetPrefix, onMatched)
                }

                override fun onLost(network: Network) {
                    _state.value = _state.value.copy(
                        currentIp = null,
                        statusMessage = "Cellular radio cycling..."
                    )
                }
            }
            cm.registerNetworkCallback(request, networkCallback!!)
        } catch (e: Exception) {
            AppLogger.w("IpHuntManager", "Failed to register NetworkCallback: ${e.message}")
        }
    }

    private fun checkAndMatch(targetPrefix: String, onMatched: (String) -> Unit) {
        val ip = getMobileDataIp() ?: return
        if (ip != lastObservedIp) {
            lastObservedIp = ip
            if (isIpMatching(ip, targetPrefix)) {
                _state.value = _state.value.copy(
                    isHunting = false,
                    isMatched = true,
                    isCyclingNow = false,
                    currentIp = ip,
                    statusMessage = "TARGET ACQUIRED: $ip"
                )
                huntingJob?.cancel()
                scope.launch(dispatchers.main) {
                    onMatched(ip)
                }
            }
        }
    }

    fun stopHunt() {
        huntingJob?.cancel()
        huntingJob = null

        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            networkCallback?.let { cm?.unregisterNetworkCallback(it) }
        } catch (_: Exception) {}
        networkCallback = null

        _state.value = _state.value.copy(
            isHunting = false,
            isCyclingNow = false,
            statusMessage = "Hunt stopped"
        )
    }

    /**
     * Opens the direct App Info & Permissions page for V2RayOps in Android system settings.
     */
    fun openAppPermissionSettings(ctx: Context = context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", ctx.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            ctx.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                ctx.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    /**
     * Opens Android Accessibility Settings so user can enable V2RayOps Auto-Pilot with 1 tap.
     */
    fun openAccessibilitySettings(ctx: Context = context) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            ctx.startActivity(intent)
        } catch (_: Exception) {
            openAppPermissionSettings(ctx)
        }
    }

    /**
     * Opens system Airplane Mode settings for manual toggle.
     */
    fun openAirplaneSettings(ctx: Context = context) {
        try {
            val intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            ctx.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent("android.settings.WIRELESS_SETTINGS").apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                ctx.startActivity(intent)
            } catch (_: Exception) {
                openAppPermissionSettings(ctx)
            }
        }
    }
}
