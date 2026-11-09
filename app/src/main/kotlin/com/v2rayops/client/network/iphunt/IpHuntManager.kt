package com.v2rayops.client.network.iphunt

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
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
    val isRootGranted: Boolean = false,
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
        // Check root on startup
        scope.launch {
            val rootOk = checkRootPermission()
            _state.value = _state.value.copy(isRootGranted = rootOk)
        }
    }

    /**
     * Checks whether Root (su) permission is available and granted.
     * Executes 'su -c id' which prompts Magisk/KernelSU if permission is requested for the first time.
     */
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

    /**
     * Executes automatic airplane mode toggle cycle via root commands:
     * 1. Turns Airplane Mode ON (or disables data radio)
     * 2. Waits 1.5s for tower detachment
     * 3. Turns Airplane Mode OFF (or re-enables data radio)
     */
    private suspend fun executeAutoAirplaneToggle(): Boolean = withContext(dispatchers.io) {
        // Method 1: cmd connectivity airplane-mode (Modern standard Android 7-15)
        try {
            val p1 = Runtime.getRuntime().exec(arrayOf("su", "-c", "cmd connectivity airplane-mode enable"))
            if (p1.waitFor() == 0) {
                delay(1500)
                val p2 = Runtime.getRuntime().exec(arrayOf("su", "-c", "cmd connectivity airplane-mode disable"))
                if (p2.waitFor() == 0) return@withContext true
            }
        } catch (_: Exception) {}

        // Method 2: svc data toggle (Immediate cellular radio disconnect/re-negotiate)
        try {
            val p3 = Runtime.getRuntime().exec(arrayOf("su", "-c", "svc data disable"))
            if (p3.waitFor() == 0) {
                delay(1500)
                val p4 = Runtime.getRuntime().exec(arrayOf("su", "-c", "svc data enable"))
                if (p4.waitFor() == 0) return@withContext true
            }
        } catch (_: Exception) {}

        // Method 3: settings global toggle with broadcast
        try {
            val p5 = Runtime.getRuntime().exec(arrayOf(
                "su", "-c",
                "settings put global airplane_mode_on 1 && am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true"
            ))
            if (p5.waitFor() == 0) {
                delay(1500)
                val p6 = Runtime.getRuntime().exec(arrayOf(
                    "su", "-c",
                    "settings put global airplane_mode_on 0 && am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false"
                ))
                if (p6.waitFor() == 0) return@withContext true
            }
        } catch (_: Exception) {}

        false
    }

    /**
     * Starts the FULLY AUTOMATIC IP Hunting loop.
     * Continuously cycles airplane mode in background until carrier assigns target IP prefix.
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

        _state.value = _state.value.copy(
            isHunting = true,
            selectedTargetPrefix = cleanPrefix,
            isMatched = false,
            attempts = 0,
            statusMessage = "Initializing automated IP hunt for $cleanPrefix*..."
        )

        registerNetworkWatcher(cleanPrefix, onMatched)

        huntingJob = scope.launch {
            // First verify or prompt root access
            val hasRoot = checkRootPermission()
            _state.value = _state.value.copy(isRootGranted = hasRoot)

            var cycleCount = 0

            while (isActive && _state.value.isHunting) {
                cycleCount++
                _state.value = _state.value.copy(
                    attempts = cycleCount,
                    isCyclingNow = true,
                    statusMessage = "Cycle #$cycleCount: Auto-toggling Airplane Mode ON..."
                )

                // Execute the automated airplane mode cycle
                val success = executeAutoAirplaneToggle()

                if (!success) {
                    AppLogger.w("IpHuntManager", "Auto-toggle command failed (Root missing or not granted).")
                    _state.value = _state.value.copy(
                        isCyclingNow = false,
                        isHunting = false,
                        statusMessage = "Root permission required for automatic airplane mode cycling."
                    )
                    break
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
}
