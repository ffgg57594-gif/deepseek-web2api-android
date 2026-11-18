package com.deepseekweb2api.app.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.deepseekweb2api.app.MainActivity
import com.deepseekweb2api.app.R

/**
 * Foreground service that keeps the embedded Ktor API server alive while the
 * app is backgrounded, subject to Android battery restrictions.
 */
class LocalApiService : Service() {

    private var server: KtorServer? = null

    companion object {
        const val ACTION_START_SERVER = "com.deepseekweb2api.app.action.START_SERVER"
        const val ACTION_STOP_SERVER = "com.deepseekweb2api.app.action.STOP_SERVER"
        const val PORT = 8080
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "deepseek_api_channel"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when {
            ACTION_START_SERVER == action -> startServerLocked()
            ACTION_STOP_SERVER == action -> stopServerLocked()
        }
        return START_NOT_STICKY
    }

    private fun startServerLocked() {
        val notification = buildNotification("خادم DeepSeek API يعمل", "متاح على 127.0.0.1:$PORT")
        startForeground(NOTIFICATION_ID, notification)
        server = KtorServer(PORT)
        server?.start()
    }

    private fun stopServerLocked() {
        server?.stop()
        server = null
        stopForeground(true)
        stopSelf()
    }

    private fun buildNotification(title: String, text: String): Notification {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "DeepSeek API", NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
        val activityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = PendingIntent.getActivity(
            this, 0, activityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pending)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent): IBinder? = null
}
