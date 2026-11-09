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
    fun performAutoToggleCycle(onDone: (Boolean) -> Unit) {
        activeCycleJob?.cancel()
        activeCycleJob = serviceScope.launch {
            var cycleSuccess = false
            try {
                // Tier 1: Try direct Settings activity drill-down
                cycleSuccess = executeDirectSettingsToggle()

                // Tier 2: If Settings toggle failed, fallback immediately to Quick Settings tile
                if (!cycleSuccess) {
                    AppLogger.w("IpHuntAccessibility", "Settings toggle did not turn on Airplane Mode, attempting Quick Settings tile fallback...")
                    cycleSuccess = tryQuickSettingsToggle()
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

    private suspend fun executeDirectSettingsToggle(): Boolean {
        // Step 1: Launch Settings (Action Airplane Mode Settings)
        val intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)

        delay(400)

        // Step 2: Locate the Airplane Mode Switch node, drilling down into intermediate categories if on Homepage
        var switchNode: AccessibilityNodeInfo? = null
        var switchBounds = Rect()

        val searchStart = System.currentTimeMillis()
        while (System.currentTimeMillis() - searchStart < 4500L) {
            val root = rootInActiveWindow
            if (root != null) {
                val found = findAirplaneSwitchAndBounds(root)
                if (found != null) {
                    switchNode = found.first
                    switchBounds = found.second
                    AppLogger.i("IpHuntAccessibility", "Located Airplane Mode switch at $switchBounds")
                    break
                }

                // If no switch was found on this window, we may be on Settings Homepage (Vivo / Samsung / MIUI).
                // Drill down by clicking intermediate network category
                val clickedCategory = clickNetworkCategoryIfPresent(root)
                if (clickedCategory) {
                    delay(500) // Allow subpage to load
                    continue
                }
            }
            delay(150)
        }

        if (switchNode == null || switchBounds.isEmpty) {
            AppLogger.w("IpHuntAccessibility", "Could not locate Airplane Mode switch on settings screen")
            return false
        }

        AppLogger.i("IpHuntAccessibility", "Target switch found at bounds $switchBounds. Current airplane status: ${isAirplaneModeOn(this)}")

        // Step 3: Toggle Airplane Mode ON (only if currently OFF)
        if (!isAirplaneModeOn(this)) {
            AppLogger.d("IpHuntAccessibility", "Toggling Airplane Mode ON...")
            clickSwitch(switchNode, switchBounds)

            // Wait up to 2.5s for hardware state to register ON without spamming clicks
            val onStart = System.currentTimeMillis()
            while (!isAirplaneModeOn(this) && System.currentTimeMillis() - onStart < 2500L) {
                delay(200)
            }

            // Fallback gesture tap if action click was ignored
            if (!isAirplaneModeOn(this) && !switchBounds.isEmpty) {
                AppLogger.d("IpHuntAccessibility", "Retrying toggle ON via gesture tap fallback...")
                dispatchTouchTap(switchBounds.centerX().toFloat(), switchBounds.centerY().toFloat())
                val retryStart = System.currentTimeMillis()
                while (!isAirplaneModeOn(this) && System.currentTimeMillis() - retryStart < 1500L) {
                    delay(200)
                }
            }
        }

        val turnedOn = isAirplaneModeOn(this)
        AppLogger.i("IpHuntAccessibility", "Airplane Mode ON confirmed: $turnedOn")
        if (!turnedOn) {
            AppLogger.w("IpHuntAccessibility", "Failed to turn Airplane Mode ON via Settings.")
            return false
        }

        // Step 4: Hold in Airplane Mode for 2.0s so mobile network fully detaches from tower
        delay(2000)

        // Step 5: Toggle Airplane Mode OFF
        AppLogger.d("IpHuntAccessibility", "Toggling Airplane Mode OFF...")
        val freshRoot = rootInActiveWindow
        val freshSwitch = if (freshRoot != null) findAirplaneSwitchAndBounds(freshRoot) else null
        val offNode = freshSwitch?.first ?: switchNode
        val offBounds = freshSwitch?.second ?: switchBounds

        clickSwitch(offNode, offBounds)

        // Wait up to 2.5s for hardware state to register OFF without spamming clicks
        val offStart = System.currentTimeMillis()
        while (isAirplaneModeOn(this) && System.currentTimeMillis() - offStart < 2500L) {
            delay(200)
        }

        // Fallback gesture tap if action click was ignored
        if (isAirplaneModeOn(this) && !offBounds.isEmpty) {
            AppLogger.d("IpHuntAccessibility", "Retrying toggle OFF via gesture tap fallback...")
            dispatchTouchTap(offBounds.centerX().toFloat(), offBounds.centerY().toFloat())
            val retryOffStart = System.currentTimeMillis()
            while (isAirplaneModeOn(this) && System.currentTimeMillis() - retryOffStart < 2000L) {
                delay(200)
            }
        }

        // Emergency guarantee: If Airplane mode is STILL ON, try clickable parent
        if (isAirplaneModeOn(this)) {
            AppLogger.w("IpHuntAccessibility", "Emergency: Airplane mode still ON! Attempting parent click...")
            offNode.parent?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            val emergStart = System.currentTimeMillis()
            while (isAirplaneModeOn(this) && System.currentTimeMillis() - emergStart < 1500L) {
                delay(200)
            }
        }

        val turnedOff = !isAirplaneModeOn(this)
        AppLogger.i("IpHuntAccessibility", "Airplane Mode OFF confirmed: $turnedOff")

        delay(400)
        // Step 6: Return smoothly to V2RayOps
        bringV2RayOpsToFront()

        return turnedOff
    }

    private suspend fun tryQuickSettingsToggle(): Boolean {
        AppLogger.i("IpHuntAccessibility", "Attempting Quick Settings tile toggle fallback...")
        val opened = performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
        if (!opened) return false

        delay(700) // Wait for notification shade expansion animation

        val root = rootInActiveWindow ?: return false
        val tile = findQuickSettingsAirplaneTile(root)
        if (tile == null) {
            AppLogger.w("IpHuntAccessibility", "Quick Settings tile not found")
            performGlobalAction(GLOBAL_ACTION_BACK)
            bringV2RayOpsToFront()
            return false
        }

        val tileBounds = Rect()
        tile.getBoundsInScreen(tileBounds)

        // 1. Toggle ON
        if (!isAirplaneModeOn(this)) {
            AppLogger.d("IpHuntAccessibility", "Clicking Quick Settings Airplane tile ON...")
            clickSwitch(tile, tileBounds)
            val onStart = System.currentTimeMillis()
            while (!isAirplaneModeOn(this) && System.currentTimeMillis() - onStart < 2500L) {
                delay(200)
            }
        }

        val isNowOn = isAirplaneModeOn(this)
        AppLogger.i("IpHuntAccessibility", "Quick Settings Airplane ON: $isNowOn")
        if (!isNowOn) {
            performGlobalAction(GLOBAL_ACTION_BACK)
            bringV2RayOpsToFront()
            return false
        }

        // 2. Hold in Airplane Mode
        delay(2000)

        // 3. Toggle OFF
        val freshRoot = rootInActiveWindow
        val freshTile = if (freshRoot != null) findQuickSettingsAirplaneTile(freshRoot) else tile
        val offTile = freshTile ?: tile
        val offBounds = Rect().apply { offTile.getBoundsInScreen(this) }

        AppLogger.d("IpHuntAccessibility", "Clicking Quick Settings Airplane tile OFF...")
        clickSwitch(offTile, offBounds)
        val offStart = System.currentTimeMillis()
        while (isAirplaneModeOn(this) && System.currentTimeMillis() - offStart < 2500L) {
            delay(200)
        }

        val isNowOff = !isAirplaneModeOn(this)
        AppLogger.i("IpHuntAccessibility", "Quick Settings Airplane OFF: $isNowOff")

        delay(300)
        performGlobalAction(GLOBAL_ACTION_BACK)
        bringV2RayOpsToFront()
        return isNowOff
    }

    private fun findQuickSettingsAirplaneTile(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val keywords = listOf("airplane", "flight", "aeroplane")
        for (kw in keywords) {
            val textNodes = root.findAccessibilityNodeInfosByText(kw)
            for (n in textNodes) {
                var current: AccessibilityNodeInfo? = n
                while (current != null) {
                    val p = current
                    if (p?.isClickable == true) return p
                    current = p?.parent
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
                    val p = current
                    if (p?.isClickable == true) return p
                    current = p?.parent
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
