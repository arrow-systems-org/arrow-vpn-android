package org.arrowx.vpn

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat

class ArrowVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DISCONNECT) {
            Log.d(TAG, "Orden de apagado recibida. Destruyendo túnel...")
            closeTunnel()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(
            NOTIFICATION_ID,
            buildNotification(getString(R.string.service_notification_connecting))
        )
        Log.d(TAG, "Iniciando servicio VPN...")
        closeTunnel()

        try {
            val builder = Builder()
            builder.setSession(getString(R.string.app_name))
                .setMtu(1500)
                .addAddress("10.0.0.2", 32)
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")
                .addRoute("0.0.0.0", 0)

            vpnInterface = builder.establish()
            if (vpnInterface == null) {
                Log.e(TAG, "No se pudo establecer el túnel VPN.")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            Log.d(TAG, "Túnel creado con éxito. FileDescriptor listo.")
            notifyStatus(getString(R.string.service_notification_active))

        } catch (e: Exception) {
            Log.e(TAG, "Error al crear el túnel: ${e.message}")
            closeTunnel()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        closeTunnel()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun closeTunnel() {
        runCatching { vpnInterface?.close() }
        vpnInterface = null
    }

    private fun buildNotification(contentText: String) =
        NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

    private fun notifyStatus(contentText: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(contentText))
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.service_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.service_channel_description)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "ArrowVPN"
        private const val ACTION_DISCONNECT = "DISCONNECT"
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_CHANNEL_ID = "arrowvpn_connection"
    }
}