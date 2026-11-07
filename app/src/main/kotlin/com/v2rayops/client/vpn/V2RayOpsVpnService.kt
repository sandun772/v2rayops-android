package com.v2rayops.client.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.v2rayops.client.MainActivity
import com.v2rayops.client.R
import com.v2rayops.client.core.logger.AppLogger
import com.v2rayops.client.xray.XrayEngine
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class V2RayOpsVpnService : VpnService() {

    companion object {
        const val ACTION_START = "com.v2rayops.client.vpn.START"
        const val ACTION_STOP = "com.v2rayops.client.vpn.STOP"
        const val EXTRA_SERVER_NAME = "extra_server_name"
        const val EXTRA_SERVER_ENDPOINT = "extra_server_endpoint"
        const val EXTRA_CONFIG_JSON = "extra_config_json"

        private const val NOTIFICATION_ID = 7788
        private const val CHANNEL_ID = "v2rayops_vpn_channel"
    }

    @Inject
    lateinit var xrayEngine: XrayEngine

    private var isServiceRunning = false
    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_START -> {
                val serverName = intent.getStringExtra(EXTRA_SERVER_NAME) ?: "V2RayOps Server"
                val serverEndpoint = intent.getStringExtra(EXTRA_SERVER_ENDPOINT) ?: ""
                val configJson = intent.getStringExtra(EXTRA_CONFIG_JSON) ?: ""
                startVpnTunnel(serverName, serverEndpoint, configJson)
            }
            ACTION_STOP -> {
                stopVpnTunnel()
            }
        }

        return START_NOT_STICKY
    }

    private fun startVpnTunnel(serverName: String, serverEndpoint: String, configJson: String) {
        if (isServiceRunning) return

        val initialNotification = buildNotification("Connecting to $serverName...")
        startForeground(NOTIFICATION_ID, initialNotification)
        isServiceRunning = true

        AppLogger.i("V2RayOpsVpnService", "Establishing TUN interface for node: $serverName ($serverEndpoint)")

        try {
            val builder = Builder()
                .setSession("V2RayOPS - $serverName")
                .setMtu(1500)
                .addAddress("172.19.0.1", 30)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setMetered(false)
            }

            // Exclude V2RayOps app itself so Xray Core's outbound connections to the VPS do not loop into TUN
            try {
                builder.addDisallowedApplication(packageName)
            } catch (e: Exception) {
                AppLogger.w("V2RayOpsVpnService", "Could not disallow package from VPN: ${e.message}")
            }

            val pfd = builder.establish()
            if (pfd == null) {
                AppLogger.e("V2RayOpsVpnService", "Failed to establish VPN interface: builder.establish() returned null")
                stopVpnTunnel()
                return
            }

            vpnInterface = pfd
            val tunFd = pfd.detachFd()

            AppLogger.i("V2RayOpsVpnService", "TUN interface established with detached fd: $tunFd")

            val result = xrayEngine.start(configJson, tunFd)
            if (result.isSuccess) {
                AppLogger.i("V2RayOpsVpnService", "Xray core engine successfully started on TUN fd $tunFd")
                val activeNotification = buildNotification("Protected • Connected to $serverName")
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIFICATION_ID, activeNotification)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Xray startup failed"
                AppLogger.e("V2RayOpsVpnService", "Failed to start Xray core: $err")
                stopVpnTunnel()
            }
        } catch (t: Throwable) {
            AppLogger.e("V2RayOpsVpnService", "Exception establishing VPN tunnel", t)
            stopVpnTunnel()
        }
    }

    private fun stopVpnTunnel() {
        AppLogger.i("V2RayOpsVpnService", "Stopping VPN Service and closing TUN interface")
        try {
            xrayEngine.stop()
        } catch (t: Throwable) {
            AppLogger.w("V2RayOpsVpnService", "Error stopping xray engine: ${t.message}")
        }

        try {
            vpnInterface?.close()
        } catch (t: Throwable) {
            AppLogger.w("V2RayOpsVpnService", "Error closing vpnInterface: ${t.message}")
        }
        vpnInterface = null
        isServiceRunning = false

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopVpnTunnel()
        super.onDestroy()
    }

    override fun onRevoke() {
        AppLogger.w("V2RayOpsVpnService", "VPN permission revoked by system or user")
        stopVpnTunnel()
        super.onRevoke()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.vpn_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.vpn_channel_description)
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, V2RayOpsVpnService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("V2RayOPS VPN")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .addAction(0, "Disconnect", stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
