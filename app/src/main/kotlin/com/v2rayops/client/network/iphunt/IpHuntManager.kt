package com.v2rayops.client.network.iphunt

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
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
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

data class IpHuntState(
    val isHunting: Boolean = false,
    val currentIp: String? = null,
    val selectedTargetPrefix: String = "100.",
    val isMatched: Boolean = false,
    val attempts: Int = 0,
    val isRootAvailable: Boolean = false,
    val isAutoCycleRoot: Boolean = false,
    val isCyclingNow: Boolean = false,
    val statusMessage: String = "Ready to hunt"
)

@Singleton
class IpHuntManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: AppDispatchers
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)

    private val _state = MutableStateFlow(
        IpHuntState(
            currentIp = getMobileDataIp(),
            isRootAvailable = checkRootAvailable()
        )
    )
    val state: StateFlow<IpHuntState> = _state.asStateFlow()

    private var huntingJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var lastObservedIp: String? = null

    init {
        // Initial check for current active IP
        val initialIp = getMobileDataIp()
        lastObservedIp = initialIp
        _state.value = _state.value.copy(
            currentIp = initialIp,
            isMatched = isIpMatching(initialIp, _state.value.selectedTargetPrefix)
        )
    }

    /**
     * Checks whether device has Superuser (su) installed for automated airplane mode toggling.
     */
    private fun checkRootAvailable(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            process.waitFor() == 0
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Extracts the active cellular IPv4 address from device network interfaces.
     * Prioritizes cellular radio interfaces (rmnet, ccmni, pdp, etc.) and CGNAT/Private ranges.
     */
    fun getMobileDataIp(): String? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces() ?: return null)
            val candidates = mutableListOf<Pair<String, String>>() // ifaceName -> IPv4

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

            // 1. Check known cellular interface names
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

    private fun isIpMatching(ip: String?, targetPrefix: String): Boolean {
        if (ip.isNullOrBlank() || targetPrefix.isBlank()) return false
        val cleanPrefixes = targetPrefix.split(",", " ", ";")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return cleanPrefixes.any { prefix ->
            ip.startsWith(prefix, ignoreCase = true)
        }
    }

    fun setTargetPrefix(prefix: String) {
        val clean = prefix.trim()
        val current = _state.value.currentIp
        val matched = isIpMatching(current, clean)
        _state.value = _state.value.copy(
            selectedTargetPrefix = clean,
            isMatched = matched
        )
    }

    fun setAutoCycleRoot(enabled: Boolean) {
        _state.value = _state.value.copy(isAutoCycleRoot = enabled)
    }

    /**
     * Starts the IP hunting monitoring loop.
     * Listens for network changes and polls mobile data IP until matching target prefix is acquired.
     */
    fun startHunt(
        targetPrefix: String = _state.value.selectedTargetPrefix,
        onMatched: (String) -> Unit = {}
    ) {
        stopHunt()

        val cleanPrefix = targetPrefix.trim().ifBlank { "100." }
        val current = getMobileDataIp()
        lastObservedIp = current
        val matched = isIpMatching(current, cleanPrefix)

        _state.value = _state.value.copy(
            isHunting = !matched,
            currentIp = current,
            selectedTargetPrefix = cleanPrefix,
            isMatched = matched,
            attempts = 0,
            statusMessage = if (matched) "Target IP already acquired: $current" else "Hunting for $cleanPrefix*"
        )

        if (matched && current != null) {
            AppLogger.i("IpHuntManager", "Current IP $current already matches target prefix $cleanPrefix")
            onMatched(current)
            return
        }

        registerNetworkWatcher(cleanPrefix, onMatched)

        huntingJob = scope.launch {
            while (isActive && _state.value.isHunting) {
                val ip = getMobileDataIp()
                if (ip != lastObservedIp) {
                    val wasOffline = lastObservedIp == null
                    lastObservedIp = ip
                    val newAttempts = if (ip != null && !wasOffline) _state.value.attempts + 1 else _state.value.attempts

                    if (isIpMatching(ip, cleanPrefix)) {
                        AppLogger.i("IpHuntManager", "MATCH FOUND: $ip on attempt #$newAttempts")
                        _state.value = _state.value.copy(
                            isHunting = false,
                            isMatched = true,
                            currentIp = ip,
                            attempts = newAttempts,
                            statusMessage = "Target acquired: $ip"
                        )
                        withContext(dispatchers.main) {
                            onMatched(ip!!)
                        }
                        break
                    } else {
                        _state.value = _state.value.copy(
                            currentIp = ip,
                            attempts = newAttempts,
                            statusMessage = if (ip == null) "Offline / Airplane Mode" else "IP: $ip (Mismatched)"
                        )
                    }
                }

                // If root auto-cycling is enabled, trigger automatic airplane mode cycle
                if (_state.value.isAutoCycleRoot && _state.value.isRootAvailable && !_state.value.isCyclingNow && !_state.value.isMatched) {
                    performAutoCycleRoot()
                }

                delay(600)
            }
        }
    }

    private suspend fun performAutoCycleRoot() {
        _state.value = _state.value.copy(isCyclingNow = true, statusMessage = "Auto-cycling airplane mode (Root)...")
        withContext(dispatchers.io) {
            try {
                // Method 1: cmd connectivity airplane-mode
                val p1 = Runtime.getRuntime().exec(arrayOf("su", "-c", "cmd connectivity airplane-mode enable"))
                if (p1.waitFor() == 0) {
                    delay(1600)
                    val p2 = Runtime.getRuntime().exec(arrayOf("su", "-c", "cmd connectivity airplane-mode disable"))
                    p2.waitFor()
                } else {
                    // Method 2: svc data toggle
                    val p3 = Runtime.getRuntime().exec(arrayOf("su", "-c", "svc data disable && sleep 1.5 && svc data enable"))
                    p3.waitFor()
                }
            } catch (e: Exception) {
                AppLogger.w("IpHuntManager", "Root auto-cycle failed: ${e.message}")
            }
        }
        delay(2500) // Wait for modem to negotiate with cell tower
        _state.value = _state.value.copy(isCyclingNow = false)
    }

    private fun registerNetworkWatcher(targetPrefix: String, onMatched: (String) -> Unit) {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    handleNetworkCheck(targetPrefix, onMatched)
                }

                override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                    handleNetworkCheck(targetPrefix, onMatched)
                }

                override fun onLost(network: Network) {
                    _state.value = _state.value.copy(
                        currentIp = null,
                        statusMessage = "Network disconnected / Airplane active"
                    )
                }
            }
            cm.registerNetworkCallback(request, networkCallback!!)
        } catch (e: Exception) {
            AppLogger.w("IpHuntManager", "Failed to register NetworkCallback: ${e.message}")
        }
    }

    private fun handleNetworkCheck(targetPrefix: String, onMatched: (String) -> Unit) {
        val ip = getMobileDataIp() ?: return
        if (ip != lastObservedIp) {
            lastObservedIp = ip
            val newAttempts = _state.value.attempts + 1
            if (isIpMatching(ip, targetPrefix)) {
                _state.value = _state.value.copy(
                    isHunting = false,
                    isMatched = true,
                    currentIp = ip,
                    attempts = newAttempts,
                    statusMessage = "Target acquired: $ip"
                )
                scope.launch(dispatchers.main) {
                    onMatched(ip)
                }
            } else {
                _state.value = _state.value.copy(
                    currentIp = ip,
                    attempts = newAttempts,
                    statusMessage = "IP: $ip (Mismatched)"
                )
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
            isCyclingNow = false
        )
    }

    /**
     * Launches the system Airplane Mode settings screen for effortless one-tap manual cycling.
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
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                ctx.startActivity(intent)
            }
        }
    }

    /**
     * Executes single airplane mode toggle cycle if root is available, otherwise opens settings.
     */
    fun cycleAirplaneModeManual(ctx: Context = context) {
        if (_state.value.isRootAvailable) {
            scope.launch {
                performAutoCycleRoot()
            }
        } else {
            openAirplaneSettings(ctx)
        }
    }
}
