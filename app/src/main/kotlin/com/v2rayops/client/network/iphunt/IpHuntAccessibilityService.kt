package com.v2rayops.client.network.iphunt

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
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

    private var maskView: View? = null
    private val windowManager by lazy { getSystemService(Context.WINDOW_SERVICE) as WindowManager }

    private suspend fun showMaskOverlay() = withContext(Dispatchers.Main) {
        if (maskView != null) return@withContext
        try {
            val layout = FrameLayout(this@IpHuntAccessibilityService).apply {
                setBackgroundColor(0xFF050811.toInt()) // Exact cosmic obsidian matching V2RayOps HUD

                val centerContent = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                }

                val title = TextView(context).apply {
                    text = "• CYCLING AIRPLANE MODE •"
                    setTextColor(0xFFFB7185.toInt())
                    textSize = 14f
                    gravity = Gravity.CENTER
                    typeface = android.graphics.Typeface.MONOSPACE
                }

                val subtitle = TextView(context).apply {
                    text = "Rotating cellular IP session..."
                    setTextColor(0xFF94A3B8.toInt())
                    textSize = 12f
                    gravity = Gravity.CENTER
                    setPadding(0, 16, 0, 0)
                    typeface = android.graphics.Typeface.MONOSPACE
                }

                centerContent.addView(title)
                centerContent.addView(subtitle)

                val centerParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
                addView(centerContent, centerParams)
            }

            val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            )

            windowManager.addView(layout, params)
            maskView = layout
        } catch (e: Exception) {
            AppLogger.w("IpHuntAccessibility", "Could not show mask overlay: ${e.message}")
        }
    }

    private suspend fun hideMaskOverlay() = withContext(Dispatchers.Main) {
        try {
            maskView?.let {
                windowManager.removeView(it)
                maskView = null
            }
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        activeCycleJob?.cancel()
        serviceScope.cancel()
        try {
            maskView?.let { windowManager.removeView(it) }
        } catch (_: Exception) {}
        maskView = null
        if (instance === this) {
            instance = null
        }
        AppLogger.i("IpHuntAccessibility", "Accessibility service destroyed.")
    }

    /**
     * Changes Airplane Mode directly to targetState (true = ON, false = OFF)
     * without exposing the Settings activity to the user (seamless overlay).
     */
    suspend fun setAirplaneMode(targetState: Boolean): Boolean {
        if (isAirplaneModeOn(this) == targetState) return true

        AppLogger.i("IpHuntAccessibility", "setAirplaneMode -> $targetState requested...")

        showMaskOverlay()

        try {
            // Attempt 1: Direct Settings Screen drill-down
            var reached = executeSettingsToggleTo(targetState)

            // Attempt 2: Quick Settings tile fallback
            if (!reached || isAirplaneModeOn(this) != targetState) {
                AppLogger.w("IpHuntAccessibility", "Settings toggle did not achieve $targetState. Trying Quick Settings...")
                executeQuickSettingsToggleTo(targetState)
            }
        } finally {
            bringV2RayOpsToFront()
            delay(120)
            hideMaskOverlay()
        }

        val finalResult = isAirplaneModeOn(this) == targetState
        AppLogger.i("IpHuntAccessibility", "setAirplaneMode -> $targetState result: $finalResult")
        return finalResult
    }

    private fun getAirplaneSettingsIntents(): List<Intent> {
        val list = mutableListOf<Intent>()

        // 1. Vivo / FuntouchOS / OriginOS dedicated network settings activity
        if (Build.MANUFACTURER.contains("vivo", ignoreCase = true) || Build.BRAND.contains("vivo", ignoreCase = true)) {
            list.add(
                Intent().apply {
                    setClassName("com.android.settings", "com.android.settings.Settings\$ExtraNetworkConnectionActivity")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
                }
            )
        }

        // 2. Standard Android Airplane Mode settings
        list.add(
            Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
            }
        )

        // 3. Wireless settings
        list.add(
            Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
            }
        )

        // 4. Main Settings fallback
        list.add(
            Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
            }
        )

        return list
    }

    private suspend fun executeSettingsToggleTo(targetState: Boolean): Boolean {
        if (isAirplaneModeOn(this) == targetState) return true

        val intents = getAirplaneSettingsIntents()

        for (intent in intents) {
            if (isAirplaneModeOn(this) == targetState) return true

            try {
                val bundle = ActivityOptions.makeCustomAnimation(this, 0, 0).toBundle()
                startActivity(intent, bundle)
            } catch (_: Exception) {
                continue
            }

            delay(100)

            var switchNode: AccessibilityNodeInfo? = null
            var switchBounds = Rect()

            val searchStart = System.currentTimeMillis()
            while (System.currentTimeMillis() - searchStart < 2500L) {
                if (isAirplaneModeOn(this) == targetState) return true

                val root = getActiveOrSettingsRoot()
                if (root != null) {
                    val found = findAirplaneSwitchAndBounds(root)
                    if (found != null) {
                        switchNode = found.first
                        switchBounds = found.second
                        break
                    }
                    val clicked = clickNetworkCategoryIfPresent(root)
                    if (clicked) {
                        delay(250)
                        continue
                    }
                }
                delay(80)
            }

            if (switchNode != null && !switchBounds.isEmpty) {
                if (isAirplaneModeOn(this) != targetState) {
                    clickSwitch(switchNode, switchBounds)

                    val waitStart = System.currentTimeMillis()
                    while (isAirplaneModeOn(this) != targetState && System.currentTimeMillis() - waitStart < 800L) {
                        delay(40)
                    }

                    if (isAirplaneModeOn(this) != targetState && !switchBounds.isEmpty) {
                        switchNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        val retryStart = System.currentTimeMillis()
                        while (isAirplaneModeOn(this) != targetState && System.currentTimeMillis() - retryStart < 1000L) {
                            delay(40)
                        }
                    }
                }

                if (isAirplaneModeOn(this) == targetState) {
                    return true
                }
            }
        }

        return isAirplaneModeOn(this) == targetState
    }

    private fun getActiveOrSettingsRoot(): AccessibilityNodeInfo? {
        val root = rootInActiveWindow
        if (root != null) return root

        try {
            val wins = windows
            for (w in wins) {
                val r = w.root ?: continue
                val pkg = r.packageName?.toString() ?: ""
                if (pkg.contains("settings")) {
                    return r
                }
            }
        } catch (_: Exception) {}
        return null
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
        val keywords = listOf("Airplane mode", "Flight mode", "Airplane", "Flight")
        for (kw in keywords) {
            val textNodes = root.findAccessibilityNodeInfosByText(kw)
            for (tn in textNodes) {
                val text = tn.text?.toString() ?: ""
                if (text.length > 35) continue

                val rowRect = Rect()
                tn.getBoundsInScreen(rowRect)
                if (rowRect.isEmpty) continue

                // 1. Search in parent hierarchy (up to 4 levels)
                var parent: AccessibilityNodeInfo? = tn
                for (depth in 0..4) {
                    parent = parent?.parent ?: break
                    val count = parent.childCount
                    for (i in 0 until count) {
                        val child = parent.getChild(i) ?: continue
                        val cName = child.className?.toString() ?: ""
                        val id = child.viewIdResourceName ?: ""
                        if (child.isCheckable || cName.contains("Switch") || cName.contains("Button") || id.contains("checkbox") || id.contains("switch")) {
                            val rect = Rect()
                            child.getBoundsInScreen(rect)
                            if (!rect.isEmpty && kotlin.math.abs(rect.centerY() - rowRect.centerY()) <= 80) {
                                return Pair(child, rect)
                            }
                        }
                    }
                }

                // 2. If switch was in separate layout container on the right side of the row (e.g. Vivo Funtouch OS)
                // Search all switches on screen that share the same vertical Y coordinate range
                val allSwitches = findAllSwitches(root)
                for (s in allSwitches) {
                    val sRect = Rect()
                    s.getBoundsInScreen(sRect)
                    if (kotlin.math.abs(sRect.centerY() - rowRect.centerY()) <= 80) {
                        return Pair(s, sRect)
                    }
                }
            }
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
        // 1. Physical touch gesture directly to the switch center (most reliable across all Android versions/OEMs)
        if (!bounds.isEmpty && bounds.centerX() > 0 && bounds.centerY() > 0) {
            val tapped = dispatchTouchTap(bounds.centerX().toFloat(), bounds.centerY().toFloat())
            if (tapped) return true
        }

        // 2. Try standard accessibility action
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
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION
            }
            val bundle = ActivityOptions.makeCustomAnimation(this, 0, 0).toBundle()
            startActivity(intent, bundle)
        } catch (_: Exception) {
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }
}
