package com.v2rayops.client.vpn

import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.core.content.ContextCompat
import com.v2rayops.client.domain.model.ServerNode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface VpnServiceController {
    fun prepareVpn(context: Context): Intent?
    fun startService(server: ServerNode, configJson: String)
    fun stopService()
}

@Singleton
class VpnServiceControllerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : VpnServiceController {

    override fun prepareVpn(context: Context): Intent? {
        return VpnService.prepare(context)
    }

    override fun startService(server: ServerNode, configJson: String) {
        val intent = Intent(context, V2RayOpsVpnService::class.java).apply {
            action = V2RayOpsVpnService.ACTION_START
            putExtra(V2RayOpsVpnService.EXTRA_SERVER_NAME, server.displayName)
            putExtra(V2RayOpsVpnService.EXTRA_SERVER_ENDPOINT, server.displayEndpoint)
            putExtra(V2RayOpsVpnService.EXTRA_CONFIG_JSON, configJson)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    override fun stopService() {
        val intent = Intent(context, V2RayOpsVpnService::class.java).apply {
            action = V2RayOpsVpnService.ACTION_STOP
        }
        context.startService(intent)
    }
}
