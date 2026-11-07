package com.v2rayops.client.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
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
        private const val NOTIFICATION_ID = 7788
        private const val CHANNEL_ID = "v2rayops_vpn_channel"
    }

    @Inject
    lateinit var xrayEngine: XrayEngine

    private var isServiceRunning = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_START -> {
                val serverName = intent.getStringExtra(EXTRA_SERVER_NAME) ?: "V2RayOps Server"
                startVpnTunnel(serverName)
            }
            ACTION_STOP -> {
                stopVpnTunnel()
            }
        }

        return START_NOT_STICKY
    }

    private fun startVpnTunnel(serverName: String) {
        if (isServiceRunning) return

        val notification = buildNotification("Connecting to $serverName...")
        startForeground(NOTIFICATION_ID, notification)
        isServiceRunning = true

        AppLogger.i("V2RayOpsVpnService", "VPN Service started for node: $serverName")
    }

    private fun stopVpnTunnel() {
        AppLogger.i("V2RayOpsVpnService", "Stopping VPN Service")
        xrayEngine.stop()
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
