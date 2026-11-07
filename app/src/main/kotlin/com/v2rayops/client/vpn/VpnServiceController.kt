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
    fun startService(name: String, endpoint: String, configJson: String, mtu: Int = 1380)
    fun startService(server: ServerNode, configJson: String, mtu: Int = 1380)
    fun stopService()
}

@Singleton
class VpnServiceControllerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : VpnServiceController {

    override fun prepareVpn(context: Context): Intent? {
        return VpnService.prepare(context)
    }

    override fun startService(name: String, endpoint: String, configJson: String, mtu: Int) {
        val intent = Intent(context, V2RayOpsVpnService::class.java).apply {
            action = V2RayOpsVpnService.ACTION_START
            putExtra(V2RayOpsVpnService.EXTRA_SERVER_NAME, name)
            putExtra(V2RayOpsVpnService.EXTRA_SERVER_ENDPOINT, endpoint)
            putExtra(V2RayOpsVpnService.EXTRA_CONFIG_JSON, configJson)
            putExtra(V2RayOpsVpnService.EXTRA_MTU, mtu)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    override fun startService(server: ServerNode, configJson: String, mtu: Int) {
        startService(server.displayName, server.displayEndpoint, configJson, mtu)
    }

    override fun stopService() {
        val intent = Intent(context, V2RayOpsVpnService::class.java).apply {
            action = V2RayOpsVpnService.ACTION_STOP
        }
        context.startService(intent)
    }
}
