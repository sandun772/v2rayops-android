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
        const val EXTRA_MTU = "extra_mtu"

        private const val NOTIFICATION_ID = 7788
        private const val CHANNEL_ID = "v2rayops_vpn_channel"

        @Volatile
        var isRunning: Boolean = false
            private set
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
                val mtu = intent.getIntExtra(EXTRA_MTU, 1360)
                startVpnTunnel(serverName, serverEndpoint, configJson, mtu)
            }
            ACTION_STOP -> {
                stopVpnTunnel()
            }
        }

        return START_NOT_STICKY
    }

    private val connectionLock = Any()

    private fun startVpnTunnel(serverName: String, serverEndpoint: String, configJson: String, mtu: Int = 1360) {
        synchronized(connectionLock) {
            // Clean up previous interface and engine safely
            try {
                vpnInterface?.close()
            } catch (t: Throwable) {
                AppLogger.w("V2RayOpsVpnService", "Error closing previous vpnInterface: ${t.message}")
            }
            vpnInterface = null

            try {
                xrayEngine.stop()
            } catch (t: Throwable) {
                AppLogger.w("V2RayOpsVpnService", "Error stopping previous xray engine: ${t.message}")
            }

            // Small release delay so Android OS cleans up the previous TUN file descriptor
            try {
                Thread.sleep(120)
            } catch (_: InterruptedException) {}

            val initialNotification = buildNotification("Connecting to $serverName...")
            startForeground(NOTIFICATION_ID, initialNotification)
            isServiceRunning = true

            AppLogger.i("V2RayOpsVpnService", "Establishing TUN interface for node: $serverName ($serverEndpoint) [MTU: $mtu]")

            try {
                val builder = Builder()
                    .setSession("V2RayOPS - $serverName")
                    .setMtu(mtu)
                    .addAddress("172.19.0.1", 30)
                    .addRoute("0.0.0.0", 0)
                    .addDnsServer("1.1.1.1")
                    .addDnsServer("8.8.8.8")

                // Add IPv6 dummy ULA address & catch-all route so Android OS routes IPv6 to TUN,
                // preventing IPv6 leaks and blackholes that cause 10-second carrier timeouts on dual-stack SIMs/Wi-Fi
                try {
                    builder.addAddress("fdfe:dcba:9876::1", 126)
                    builder.addRoute("::", 0)
                } catch (e: Exception) {
                    AppLogger.w("V2RayOpsVpnService", "Could not add IPv6 route to TUN: ${e.message}")
                }

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
                val tunFd = pfd.fd

                AppLogger.i("V2RayOpsVpnService", "TUN interface established with fd: $tunFd")

                val result = xrayEngine.start(configJson, tunFd)
                if (result.isSuccess) {
                    isRunning = true
                    AppLogger.i("V2RayOpsVpnService", "Xray core engine successfully started on TUN fd $tunFd")
                    val activeNotification = buildNotification("Protected • Connected to $serverName")
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.notify(NOTIFICATION_ID, activeNotification)
                } else {
                    isRunning = false
                    val err = result.exceptionOrNull()?.message ?: "Xray startup failed"
                    AppLogger.e("V2RayOpsVpnService", "Failed to start Xray core: $err")
                    stopVpnTunnel()
                }
            } catch (t: Throwable) {
                isRunning = false
                AppLogger.e("V2RayOpsVpnService", "Exception establishing VPN tunnel", t)
                stopVpnTunnel()
            }
        }
    }

    private fun stopVpnTunnel() {
        synchronized(connectionLock) {
            isRunning = false
            AppLogger.i("V2RayOpsVpnService", "Stopping VPN Service and closing TUN interface")
            try {
                vpnInterface?.close()
            } catch (t: Throwable) {
                AppLogger.w("V2RayOpsVpnService", "Error closing vpnInterface: ${t.message}")
            }
            vpnInterface = null

            try {
                xrayEngine.stop()
            } catch (t: Throwable) {
                AppLogger.w("V2RayOpsVpnService", "Error stopping xray engine: ${t.message}")
            }

            isServiceRunning = false

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.cancel(NOTIFICATION_ID)
            } catch (t: Throwable) {
                AppLogger.w("V2RayOpsVpnService", "Error removing notification: ${t.message}")
            }

            stopSelf()
        }
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

        val largeIconBitmap = try {
            android.graphics.BitmapFactory.decodeResource(resources, R.drawable.v2rayops_logo)
        } catch (_: Exception) {
            null
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("V2RayOPS VPN")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_notification_v2)
            .setColor(0xFF6366F1.toInt())
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .addAction(0, "Disconnect", stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (largeIconBitmap != null) {
            builder.setLargeIcon(largeIconBitmap)
        }

        return builder.build()
    }
}
