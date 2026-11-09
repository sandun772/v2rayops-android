package com.v2rayops.client.network.iphunt

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
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

    override fun onCreate() {
        super.onCreate()
        instance = this
        AppLogger.i("IpHuntAccessibility", "V2RayOps Auto-Pilot Accessibility Service created.")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        AppLogger.i("IpHuntAccessibility", "V2RayOps Auto-Pilot Accessibility Service connected.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Handled directly via polling loop in performAutoToggleCycle
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
     * 1. Opens Settings and navigates directly to Airplane Mode
     * 2. Toggles Airplane Mode ON (confirmed via Settings.Global)
     * 3. Waits 1.6s for carrier tower disconnection
     * 4. Toggles Airplane Mode OFF (confirmed via Settings.Global)
     * 5. Smoothly returns to V2RayOps
     */
    /**
     * Changes Airplane Mode directly to targetState (true = ON, false = OFF)
     * using direct Settings drill-down with automatic Quick Settings tile fallback.
     * Brings V2RayOps back to front when done.
     */
    suspend fun setAirplaneMode(targetState: Boolean): Boolean {
        if (isAirplaneModeOn(this) == targetState) return true

        AppLogger.i("IpHuntAccessibility", "setAirplaneMode -> $targetState requested...")

        // Attempt 1: Direct Settings Screen drill-down
        var reached = executeSettingsToggleTo(targetState)

        // Attempt 2: Quick Settings tile fallback
        if (!reached || isAirplaneModeOn(this) != targetState) {
            AppLogger.w("IpHuntAccessibility", "Settings toggle did not achieve $targetState. Trying Quick Settings...")
            executeQuickSettingsToggleTo(targetState)
        }

        // Return to V2RayOps smoothly
        delay(200)
        bringV2RayOpsToFront()

        val finalResult = isAirplaneModeOn(this) == targetState
        AppLogger.i("IpHuntAccessibility", "setAirplaneMode -> $targetState result: $finalResult")
        return finalResult
    }

    private suspend fun executeSettingsToggleTo(targetState: Boolean): Boolean {
        if (isAirplaneModeOn(this) == targetState) return true

        val intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)

        delay(400)

        var switchNode: AccessibilityNodeInfo? = null
        var switchBounds = Rect()

        val searchStart = System.currentTimeMillis()
        while (System.currentTimeMillis() - searchStart < 4000L) {
            val root = rootInActiveWindow
            if (root != null) {
                val found = findAirplaneSwitchAndBounds(root)
                if (found != null) {
                    switchNode = found.first
                    switchBounds = found.second
                    break
                }
                val clicked = clickNetworkCategoryIfPresent(root)
                if (clicked) {
                    delay(500)
                    continue
                }
            }
            delay(150)
        }

        if (switchNode == null || switchBounds.isEmpty) return false

        if (isAirplaneModeOn(this) != targetState) {
            clickSwitch(switchNode, switchBounds)

            val waitStart = System.currentTimeMillis()
            while (isAirplaneModeOn(this) != targetState && System.currentTimeMillis() - waitStart < 2500L) {
                delay(150)
            }

            if (isAirplaneModeOn(this) != targetState && !switchBounds.isEmpty) {
                dispatchTouchTap(switchBounds.centerX().toFloat(), switchBounds.centerY().toFloat())
                val retryStart = System.currentTimeMillis()
                while (isAirplaneModeOn(this) != targetState && System.currentTimeMillis() - retryStart < 1500L) {
                    delay(150)
                }
            }
        }

        return isAirplaneModeOn(this) == targetState
    }

    private suspend fun executeQuickSettingsToggleTo(targetState: Boolean): Boolean {
        if (isAirplaneModeOn(this) == targetState) return true

        val opened = performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
        if (!opened) return false

        delay(700)

        val root = rootInActiveWindow
        if (root == null) {
            performGlobalAction(GLOBAL_ACTION_BACK)
            return false
        }

        val tile = findQuickSettingsAirplaneTile(root)
        if (tile == null) {
            performGlobalAction(GLOBAL_ACTION_BACK)
            return false
        }

        val tileBounds = Rect().apply { tile.getBoundsInScreen(this) }
        clickSwitch(tile, tileBounds)

        val waitStart = System.currentTimeMillis()
        while (isAirplaneModeOn(this) != targetState && System.currentTimeMillis() - waitStart < 2500L) {
            delay(150)
        }

        delay(200)
        performGlobalAction(GLOBAL_ACTION_BACK)
        return isAirplaneModeOn(this) == targetState
    }

    fun performAutoToggleCycle(onDone: (Boolean) -> Unit) {
        activeCycleJob?.cancel()
        activeCycleJob = serviceScope.launch {
            var cycleSuccess = false
            try {
                val onOk = setAirplaneMode(true)
                if (onOk) {
                    delay(3000)
                    cycleSuccess = setAirplaneMode(false)
                }
            } catch (e: Exception) {
                AppLogger.e("IpHuntAccessibility", "Error during auto toggle cycle: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    onDone(cycleSuccess)
                }
            }
        }
    }

    private fun findQuickSettingsAirplaneTile(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val keywords = listOf("airplane", "flight", "aeroplane")
        for (kw in keywords) {
            val textNodes = root.findAccessibilityNodeInfosByText(kw)
            for (n in textNodes) {
                var current: AccessibilityNodeInfo? = n
                while (current != null) {
                    val p = current ?: break
                    if (p.isClickable) return p
                    current = p.parent
                }
                if (n.isClickable) return n
            }
        }

        fun searchDesc(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null) return null
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""
            if (keywords.any { desc.contains(it) }) {
                var current: AccessibilityNodeInfo? = node
                while (current != null) {
                    val p = current ?: break
                    if (p.isClickable) return p
                    current = p.parent
                }
                return node
            }
            for (i in 0 until node.childCount) {
                val f = searchDesc(node.getChild(i))
                if (f != null) return f
            }
            return null
        }

        return searchDesc(root)
    }

    private fun clickNetworkCategoryIfPresent(root: AccessibilityNodeInfo): Boolean {
        // Search for intermediate network category tiles (Vivo, Samsung, MIUI, AOSP)
        val categoryKeywords = listOf(
            "Other networks",
            "Connections",
            "Network & internet",
            "Wireless",
            "Connection & sharing",
            "Mobile network"
        )

        for (kw in categoryKeywords) {
            val nodes = root.findAccessibilityNodeInfosByText(kw)
            for (node in nodes) {
                // Find clickable ancestor
                var clickableParent: AccessibilityNodeInfo? = node
                while (clickableParent != null && !clickableParent.isClickable) {
                    clickableParent = clickableParent.parent
                }
                if (clickableParent != null && clickableParent.isClickable) {
                    AppLogger.d("IpHuntAccessibility", "Clicking intermediate category: $kw")
                    val ok = clickableParent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    if (ok) return true
                }

                // If performAction failed or node wasn't marked clickable, tap bounds center
                val rect = Rect()
                node.getBoundsInScreen(rect)
                if (!rect.isEmpty && rect.centerX() > 0 && rect.centerY() > 0) {
                    AppLogger.d("IpHuntAccessibility", "Tapping bounds center for: $kw at $rect")
                    val tapped = dispatchTouchTap(rect.centerX().toFloat(), rect.centerY().toFloat())
                    if (tapped) return true
                }
            }
        }
        return false
    }

    /**
     * Finds the Airplane Mode switch and its exact screen bounds:
     * - Looks for "Airplane mode" / "Flight mode" row, then finds the Switch widget on that row
     * - Also checks direct switch IDs (android:id/checkbox on Vivo, switch_widget on AOSP, etc.)
     */
    private fun findAirplaneSwitchAndBounds(root: AccessibilityNodeInfo): Pair<AccessibilityNodeInfo, Rect>? {
        // Strategy A: Find row with text "Airplane mode" / "Flight mode"
        val keywords = listOf("Airplane mode", "Flight mode", "Airplane", "Flight")
        for (kw in keywords) {
            val textNodes = root.findAccessibilityNodeInfosByText(kw)
            for (tn in textNodes) {
                val rowRect = Rect()
                tn.getBoundsInScreen(rowRect)

                // Search siblings/ancestors for switch
                var parent: AccessibilityNodeInfo? = tn
                for (depth in 0..4) {
                    parent = parent?.parent ?: break
                    val count = parent.childCount
                    for (i in 0 until count) {
                        val child = parent.getChild(i) ?: continue
                        val cName = child.className?.toString() ?: ""
                        if (child.isCheckable || cName.contains("Switch") || cName.contains("CompoundButton") || child.viewIdResourceName?.contains("checkbox") == true) {
                            val rect = Rect()
                            child.getBoundsInScreen(rect)
                            if (!rect.isEmpty) return Pair(child, rect)
                        }
                    }
                }

                // If switch was in separate layout container on the right side of the row (e.g. Vivo Funtouch OS)
                // Search all switches on screen that share the same vertical Y coordinate range
                val allSwitches = findAllSwitches(root)
                for (s in allSwitches) {
                    val sRect = Rect()
                    s.getBoundsInScreen(sRect)
                    if (sRect.top >= rowRect.top - 60 && sRect.bottom <= rowRect.bottom + 60) {
                        return Pair(s, sRect)
                    }
                }
            }
        }

        // Strategy B: Direct ID search (Vivo uses android:id/checkbox, AOSP uses switch_widget)
        val knownIds = listOf(
            "android:id/checkbox",
            "android:id/switch_widget",
            "com.android.settings:id/switch_widget",
            "com.android.settings:id/switch_button",
            "com.android.settings:id/main_switch",
            "com.android.settings:id/main_switch_button"
        )
        for (id in knownIds) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            if (nodes.isNotEmpty()) {
                val first = nodes.first()
                val rect = Rect()
                first.getBoundsInScreen(rect)
                if (!rect.isEmpty) return Pair(first, rect)
            }
        }

        // Strategy C: First checkable switch on screen
        val anySwitches = findAllSwitches(root)
        if (anySwitches.isNotEmpty()) {
            val first = anySwitches.first()
            val rect = Rect()
            first.getBoundsInScreen(rect)
            if (!rect.isEmpty) return Pair(first, rect)
        }

        return null
    }

    private fun findAllSwitches(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null) return
            val cName = node.className?.toString() ?: ""
            val id = node.viewIdResourceName ?: ""
            if (node.isCheckable || cName.contains("Switch") || id.contains("checkbox") || id.contains("switch")) {
                result.add(node)
            }
            for (i in 0 until node.childCount) {
                traverse(node.getChild(i))
            }
        }
        traverse(root)
        return result
    }

    private fun clickSwitch(node: AccessibilityNodeInfo, bounds: Rect): Boolean {
        // 1. Try standard Accessibility action
        if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            return true
        }
        var p = node.parent
        while (p != null) {
            if (p.isClickable && p.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }
            p = p.parent
        }

        // 2. Fallback: physical touch gesture if accessibility action wasn't handled
        if (!bounds.isEmpty && bounds.centerX() > 0 && bounds.centerY() > 0) {
            return dispatchTouchTap(bounds.centerX().toFloat(), bounds.centerY().toFloat())
        }
        return false
    }

    private fun dispatchTouchTap(x: Float, y: Float): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                val path = Path().apply { moveTo(x, y) }
                val stroke = GestureDescription.StrokeDescription(path, 0, 50)
                val gesture = GestureDescription.Builder().addStroke(stroke).build()
                return dispatchGesture(gesture, null, null)
            } catch (e: Exception) {
                AppLogger.w("IpHuntAccessibility", "dispatchGesture failed: ${e.message}")
            }
        }
        return false
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
}
