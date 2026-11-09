package com.v2rayops.client.network.iphunt.assistant

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.service.voice.VoiceInteractionSession
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.v2rayops.client.core.logger.AppLogger

class V2RayOpsVoiceSession(context: Context) : VoiceInteractionSession(context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        @Volatile
        var activeSession: V2RayOpsVoiceSession? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        activeSession = this
        try {
            window?.window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setDimAmount(0f)
                addFlags(
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                )
            }
        } catch (_: Exception) {}
    }

    override fun onCreateContentView(): View {
        return View(context).apply {
            layoutParams = ViewGroup.LayoutParams(1, 1)
            setBackgroundColor(Color.TRANSPARENT)
        }
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        if (args != null && args.containsKey(V2RayOpsVoiceInteractionService.EXTRA_AIRPLANE_MODE)) {
            val enable = args.getBoolean(V2RayOpsVoiceInteractionService.EXTRA_AIRPLANE_MODE)
            toggleAirplaneMode(enable)
        }
    }

    fun toggleAirplaneMode(enable: Boolean): Boolean {
        return try {
            val intent = Intent("android.settings.VOICE_CONTROL_AIRPLANE_MODE").apply {
                setPackage("com.android.settings")
                putExtra("airplane_mode_enabled", enable)
            }
            startVoiceActivity(intent)
            AppLogger.i("VoiceSession", "startVoiceActivity for airplane_mode_enabled=$enable dispatched without NEW_TASK flag.")
            true
        } catch (e: Exception) {
            AppLogger.e("VoiceSession", "startVoiceActivity failed: ${e.message}")
            false
        } finally {
            mainHandler.postDelayed({
                try {
                    hide()
                } catch (_: Exception) {}
            }, 600)
        }
    }

    override fun onTaskStarted(intent: Intent?, taskId: Int) {
        super.onTaskStarted(intent, taskId)
        try {
            hide()
        } catch (_: Exception) {}
    }

    override fun onTaskFinished(intent: Intent?, taskId: Int) {
        super.onTaskFinished(intent, taskId)
        try {
            hide()
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        if (activeSession === this) {
            activeSession = null
        }
    }
}
