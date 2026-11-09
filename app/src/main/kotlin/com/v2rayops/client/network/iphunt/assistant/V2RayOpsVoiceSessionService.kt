package com.v2rayops.client.network.iphunt.assistant

import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import com.v2rayops.client.core.logger.AppLogger

class V2RayOpsVoiceSessionService : VoiceInteractionSessionService() {

    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        AppLogger.d("VoiceSessionService", "onNewSession created with args: $args")
        return V2RayOpsVoiceSession(this)
    }
}
