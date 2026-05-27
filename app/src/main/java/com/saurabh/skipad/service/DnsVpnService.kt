package com.saurabh.skipad.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class DnsVpnService : VpnService() {

    companion object {
        const val ACTION_START = "ACTION_START_VPN"
        const val ACTION_STOP = "ACTION_STOP_VPN"
        const val EXTRA_TARGET_APP = "TARGET_APP"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "vpn_dns_block"
        private const val BUFFER_SIZE = 32 * 1024

        private val _isRunning = MutableStateFlow(false)
        val isRunning = _isRunning.asStateFlow()
    }

    @Volatile
    private var running = false
    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnThread: Thread? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_STOP -> {
                stopVpn()
                stopSelf()
                START_NOT_STICKY
            }

            ACTION_START -> {
                val targetPackage = intent.getStringExtra(EXTRA_TARGET_APP)
                    ?: return START_NOT_STICKY
                startForegroundCompat()
                stopVpn()
                startVpnForApp(targetPackage)
                START_STICKY
            }

            else -> START_NOT_STICKY
        }
    }

    override fun onDestroy() {
        stopVpn()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun startForegroundCompat() {
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Ad Blocker VPN",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Active while ad blocking is running"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val stopPendingIntent = PendingIntent.getService(
            this, 0,
            Intent(this, DnsVpnService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focus Mode Active")
            .setContentText("Running for selected app")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(android.R.drawable.ic_delete, "Stop", stopPendingIntent)
            .build()
    }

    private fun startVpnForApp(targetPackage: String) {
        val pfd = try {
            Builder()
                .setSession("DNS Ad Block")
                .addAddress("10.0.0.2", 32)
                .addRoute("0.0.0.0", 0)
                .addAllowedApplication(targetPackage)
                .establish()
        } catch (e: Exception) {
            stopSelf(); return
        } ?: run { stopSelf(); return }

        vpnInterface = pfd
        _isRunning.value = true   // ← UI ko batao: ON
        startPacketLoop(pfd)
    }

    private fun stopVpn() {
        running = false
        vpnThread?.interrupt()
        vpnThread = null
        try {
            vpnInterface?.close()
        } catch (_: IOException) {
        }
        vpnInterface = null
        _isRunning.value = false  // ← UI ko batao: OFF
    }

    private fun startPacketLoop(pfd: ParcelFileDescriptor) {
        running = true
        vpnThread = Thread({
            val input = FileInputStream(pfd.fileDescriptor)
            val output = FileOutputStream(pfd.fileDescriptor)
            val buffer = ByteArray(BUFFER_SIZE)
            try {
                while (running) {
                    val length = input.read(buffer)
                    if (length <= 0) continue
                    if (isUdpPort53(buffer, length)) continue
                    output.write(buffer, 0, length)
                }
            } catch (_: IOException) {
            }
        }, "vpn-packet-thread").also { it.isDaemon = true }
        vpnThread?.start()
    }

    private fun isUdpPort53(packet: ByteArray, length: Int): Boolean {
        if (length < 28) return false
        if (packet[9].toInt() and 0xFF != 17) return false
        val destPort = ((packet[22].toInt() and 0xFF) shl 8) or (packet[23].toInt() and 0xFF)
        return destPort == 53
    }
}