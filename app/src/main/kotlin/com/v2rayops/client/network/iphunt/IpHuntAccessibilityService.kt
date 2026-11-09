package com.v2rayops.client.network.iphunt

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.v2rayops.client.MainActivity
import com.v2rayops.client.core.logger.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IpHuntAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var activeCycleJob: Job? = null

    companion object {
        @Volatile
        var instance: IpHuntAccessibilityService? = null
            private set

        fun isRunning(): Boolean = instance != null

        fun isAccessibilityEnabled(context: Context): Boolean {
            return try {
                val enabledServices = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                ) ?: return false
                enabledServices.contains(context.packageName)
            } catch (_: Exception) {
                false
            }
        }

        fun isAirplaneModeOn(context: Context): Boolean {
            return try {
                Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
            } catch (_: Exception) {
                false
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        AppLogger.i("IpHuntAccessibility", "V2RayOps Auto-Pilot Accessibility Service connected.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No-op: Active polling loop in performAutoToggleCycle handles window state precisely
    }

    override fun onInterrupt() {
        activeCycleJob?.cancel()
        activeCycleJob = null
        AppLogger.w("IpHuntAccessibility", "Accessibility service interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        activeCycleJob?.cancel()
        serviceScope.cancel()
        if (instance === this) {
            instance = null
        }
        AppLogger.i("IpHuntAccessibility", "Accessibility service destroyed.")
    }

    /**
     * Executes one complete automated Airplane Mode toggle cycle:
     * 1. Launches Airplane Mode Settings directly (NO notification bar swiping)
     * 2. Finds the hardware toggle switch
     * 3. Clicks to turn Airplane Mode ON and verifies with Settings.Global
     * 4. Waits 1.6s for carrier tower disconnection
     * 5. Clicks to turn Airplane Mode OFF and verifies with Settings.Global
     * 6. Smoothly brings V2RayOps back to the front
     */
    fun performAutoToggleCycle(onDone: (Boolean) -> Unit) {
        activeCycleJob?.cancel()
        activeCycleJob = serviceScope.launch {
            var cycleSuccess = false
            try {
                cycleSuccess = executeDirectSettingsToggle()
            } catch (e: Exception) {
                AppLogger.e("IpHuntAccessibility", "Error during auto toggle cycle: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    onDone(cycleSuccess)
                }
            }
        }
    }

    private suspend fun executeDirectSettingsToggle(): Boolean {
        // Step 1: Launch Airplane Mode Settings directly
        val intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)

        // Step 2: Poll for switch node on Settings screen (up to 3 seconds)
        var toggleNode: AccessibilityNodeInfo? = null
        val searchStart = System.currentTimeMillis()
        while (System.currentTimeMillis() - searchStart < 3000L) {
            delay(150)
            val root = rootInActiveWindow ?: continue
            toggleNode = findAirplaneToggleNode(root)
            if (toggleNode != null) break
        }

        if (toggleNode == null) {
            AppLogger.w("IpHuntAccessibility", "Could not locate Airplane toggle switch on settings screen")
            bringV2RayOpsToFront()
            return false
        }

        // Step 3: Turn Airplane Mode ON if not already ON
        if (!isAirplaneModeOn(this)) {
            AppLogger.d("IpHuntAccessibility", "Clicking toggle switch to turn Airplane Mode ON...")
            performClick(toggleNode)

            // Confirm hardware status changed to ON (poll up to 1.2s)
            val onStart = System.currentTimeMillis()
            while (!isAirplaneModeOn(this) && System.currentTimeMillis() - onStart < 1200L) {
                delay(150)
                val freshRoot = rootInActiveWindow
                if (freshRoot != null) {
                    val freshNode = findAirplaneToggleNode(freshRoot)
                    performClick(freshNode)
                }
            }
        }

        AppLogger.d("IpHuntAccessibility", "Airplane Mode is ON. Waiting 1.6s for cell tower detachment...")
        // Step 4: Hold in Airplane Mode for 1.6s
        delay(1600)

        // Step 5: Turn Airplane Mode OFF
        AppLogger.d("IpHuntAccessibility", "Clicking toggle switch to turn Airplane Mode OFF...")
        val offRoot = rootInActiveWindow
        val offNode = if (offRoot != null) findAirplaneToggleNode(offRoot) else toggleNode
        performClick(offNode)

        // Confirm hardware status changed to OFF (poll up to 1.2s)
        val offStart = System.currentTimeMillis()
        while (isAirplaneModeOn(this) && System.currentTimeMillis() - offStart < 1200L) {
            delay(150)
            val freshRoot = rootInActiveWindow
            if (freshRoot != null) {
                val freshNode = findAirplaneToggleNode(freshRoot)
                performClick(freshNode)
            }
        }

        delay(300)
        // Step 6: Bring V2RayOps back
        bringV2RayOpsToFront()

        val isNowOff = !isAirplaneModeOn(this)
        AppLogger.i("IpHuntAccessibility", "Toggle cycle finished. Radio re-enabled: $isNowOff")
        return isNowOff
    }

    private fun bringV2RayOpsToFront() {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
        } catch (_: Exception) {
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    private fun performClick(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false

        // Attempt 1: Direct action click
        if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            return true
        }

        // Attempt 2: Click parent container
        var parent = node.parent
        while (parent != null) {
            if (parent.isClickable && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }
            parent = parent.parent
        }

        // Attempt 3: Force ACTION_CLICK on node even if not marked clickable
        if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            return true
        }

        return false
    }

    private fun findAirplaneToggleNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Strategy 1: Standard Android Settings switch IDs
        val knownIds = listOf(
            "android:id/switch_widget",
            "android:id/switch1",
            "com.android.settings:id/switch_widget",
            "com.android.settings:id/switch_button",
            "com.android.settings:id/airplane_mode",
            "com.android.settings:id/main_switch",
            "com.android.settings:id/main_switch_button",
            "com.android.settings:id/switch_bar"
        )
        for (id in knownIds) {
            try {
                val nodes = root.findAccessibilityNodeInfosByViewId(id)
                for (n in nodes) {
                    if (n.isClickable || n.isCheckable) return n
                    n.parent?.let { if (it.isClickable) return it }
                }
            } catch (_: Exception) {}
        }

        // Strategy 2: Text keyword search
        val keywords = listOf("airplane", "flight", "aeroplane")
        for (kw in keywords) {
            try {
                val textNodes = root.findAccessibilityNodeInfosByText(kw)
                for (tn in textNodes) {
                    if (tn.isCheckable) return tn

                    // Check parents and children of row
                    var p: AccessibilityNodeInfo? = tn
                    for (level in 0..3) {
                        p = p?.parent ?: break
                        if (p.isClickable) return p
                        for (i in 0 until p.childCount) {
                            val c = p.getChild(i) ?: continue
                            val className = c.className?.toString() ?: ""
                            if (c.isCheckable || className.contains("Switch") || className.contains("CompoundButton")) {
                                return c
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // Strategy 3: Find first checkable switch on the Airplane Settings screen
        fun findFirstCheckable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null) return null
            val className = node.className?.toString() ?: ""
            if (node.isCheckable || className.contains("Switch") || className.contains("CompoundButton")) {
                return node
            }
            for (i in 0 until node.childCount) {
                val found = findFirstCheckable(node.getChild(i))
                if (found != null) return found
            }
            return null
        }

        return findFirstCheckable(root)
    }
}
