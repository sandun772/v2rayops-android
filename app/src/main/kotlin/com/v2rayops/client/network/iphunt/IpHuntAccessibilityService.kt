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
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        AppLogger.i("IpHuntAccessibility", "V2RayOps Auto-Pilot Accessibility Service connected.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Events are observed when necessary during active cycling
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
     * 1. Opens Quick Settings or Airplane Settings
     * 2. Toggles Airplane Mode ON
     * 3. Waits 1.6s for carrier cell tower detachment
     * 4. Toggles Airplane Mode OFF
     * 5. Smoothly returns to V2RayOps
     */
    fun performAutoToggleCycle(onDone: (Boolean) -> Unit) {
        activeCycleJob?.cancel()
        activeCycleJob = serviceScope.launch {
            var cycleSuccess = false
            try {
                // Phase 1: Try Quick Settings Toggle first (least intrusive)
                cycleSuccess = tryQuickSettingsCycle()

                // Phase 2: If Quick Settings tile not found, fallback to Settings Activity
                if (!cycleSuccess) {
                    AppLogger.d("IpHuntAccessibility", "Quick Settings tile not found, falling back to Settings screen")
                    cycleSuccess = trySettingsScreenCycle()
                }
            } catch (e: Exception) {
                AppLogger.e("IpHuntAccessibility", "Auto toggle cycle encountered error: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    onDone(cycleSuccess)
                }
            }
        }
    }

    private suspend fun tryQuickSettingsCycle(): Boolean {
        // Expand Quick Settings
        val opened = performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
        if (!opened) return false

        delay(400) // Brief pause for shade expansion animation

        val root = rootInActiveWindow ?: return false
        val toggleNode = findAirplaneToggleNode(root)

        if (toggleNode != null) {
            AppLogger.d("IpHuntAccessibility", "Found Airplane tile in Quick Settings. Toggling ON...")
            toggleNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)

            // Wait while in airplane mode for carrier tower drop
            delay(1600)

            // Find tile again to toggle OFF
            val refreshRoot = rootInActiveWindow
            val offNode = if (refreshRoot != null) findAirplaneToggleNode(refreshRoot) else toggleNode
            offNode?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            AppLogger.d("IpHuntAccessibility", "Toggled Airplane tile OFF.")

            delay(400)
            // Dismiss quick settings
            performGlobalAction(GLOBAL_ACTION_BACK)
            return true
        }

        // Collapse Quick Settings if not found
        performGlobalAction(GLOBAL_ACTION_BACK)
        return false
    }

    private suspend fun trySettingsScreenCycle(): Boolean {
        val intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)

        // Poll for Airplane Mode toggle node on Settings screen (up to 2.5 seconds)
        var toggleNode: AccessibilityNodeInfo? = null
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < 2500) {
            delay(200)
            val root = rootInActiveWindow ?: continue
            toggleNode = findAirplaneToggleNode(root)
            if (toggleNode != null) break
        }

        if (toggleNode == null) {
            AppLogger.w("IpHuntAccessibility", "Could not locate Airplane toggle node in Settings screen")
            bringV2RayOpsToFront()
            return false
        }

        AppLogger.d("IpHuntAccessibility", "Found Airplane toggle in Settings screen. Toggling ON...")
        toggleNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)

        delay(1600)

        // Refresh and toggle OFF
        val refreshRoot = rootInActiveWindow
        val offNode = if (refreshRoot != null) findAirplaneToggleNode(refreshRoot) else toggleNode
        offNode?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        AppLogger.d("IpHuntAccessibility", "Toggled Airplane toggle OFF.")

        delay(400)
        bringV2RayOpsToFront()
        return true
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

    private fun findAirplaneToggleNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val keywords = listOf("airplane", "flight", "aeroplane")

        // Depth-first search for matching node
        fun search(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null) return null

            val text = (node.text?.toString() ?: "").lowercase()
            val desc = (node.contentDescription?.toString() ?: "").lowercase()
            val viewId = (node.viewIdResourceName ?: "").lowercase()

            val matchesKeyword = keywords.any { kw ->
                text.contains(kw) || desc.contains(kw) || viewId.contains(kw)
            }

            if (matchesKeyword) {
                // If this node is clickable or a switch, return it
                if (node.isClickable) return node

                // If this node is inside a switch row, find switch or clickable parent/sibling
                var parent = node.parent
                while (parent != null) {
                    if (parent.isClickable) return parent
                    for (i in 0 until parent.childCount) {
                        val child = parent.getChild(i)
                        if (child != null && (child.className?.contains("Switch") == true || child.className?.contains("CompoundButton") == true)) {
                            return child
                        }
                    }
                    parent = parent.parent
                }
                return node
            }

            for (i in 0 until node.childCount) {
                val found = search(node.getChild(i))
                if (found != null) return found
            }

            return null
        }

        return search(root)
    }
}
