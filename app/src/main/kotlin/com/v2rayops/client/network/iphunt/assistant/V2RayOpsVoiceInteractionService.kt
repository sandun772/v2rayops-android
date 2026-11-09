package com.v2rayops.client.network.iphunt.assistant

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.service.voice.VoiceInteractionService
import com.v2rayops.client.core.logger.AppLogger

class V2RayOpsVoiceInteractionService : VoiceInteractionService() {

    companion object {
        const val EXTRA_AIRPLANE_MODE = "airplane_mode_enabled"

        @Volatile
        var instance: V2RayOpsVoiceInteractionService? = null
            private set

        fun isServiceActive(context: Context): Boolean {
            return try {
                val cn = ComponentName(context, V2RayOpsVoiceInteractionService::class.java)
                VoiceInteractionService.isActiveService(context, cn)
            } catch (_: Exception) {
                false
            }
        }
    }

    override fun onReady() {
        super.onReady()
        instance = this
        AppLogger.i("VoiceAssist", "V2RayOpsVoiceInteractionService ready and active.")
    }

    override fun onShutdown() {
        super.onShutdown()
        if (instance === this) {
            instance = null
        }
        AppLogger.i("VoiceAssist", "V2RayOpsVoiceInteractionService shut down.")
    }

    fun requestAirplaneMode(enable: Boolean): Boolean {
        val args = Bundle().apply {
            putBoolean(EXTRA_AIRPLANE_MODE, enable)
        }
        return try {
            showSession(args, 0)
            AppLogger.d("VoiceAssist", "showSession dispatched with airplane_mode_enabled=$enable")
            true
        } catch (e: Exception) {
            AppLogger.e("VoiceAssist", "Failed to dispatch showSession: ${e.message}")
            false
        }
    }
}
