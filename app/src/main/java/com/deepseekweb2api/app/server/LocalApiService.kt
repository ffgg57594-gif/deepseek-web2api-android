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
import com.deepseekweb2api.app.client.DeepSeekClient
import com.deepseekweb2api.app.data.AccountRepository
import com.deepseekweb2api.app.data.CredentialStore

/**
 * Foreground service that keeps the embedded Ktor API server alive while the
 * app is backgrounded. Subject to Android battery restrictions.
 *
 * The service builds a real [DeepSeekClient] from the first available
 * credential-bearing account in [AccountRepository] so that
 * /v1/chat/completions can actually forward to DeepSeek Web.
 */
class LocalApiService : Service() {

    private var server: KtorServer? = null
    @Volatile private var isRunningFlag = false

    /** Returns true if the local server is currently running. */
    fun isServerRunning(): Boolean = isRunningFlag

    companion object {
        const val ACTION_START_SERVER = "com.deepseekweb2api.app.action.START_SERVER"
        const val ACTION_STOP_SERVER = "com.deepseekweb2api.app.action.STOP_SERVER"
        const val PORT = 8080
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "deepseek_api_channel"
        private const val STOP_REQUEST_CODE = 100

        @Volatile
        var accounts: AccountRepository? = null

        /** Tracks if a server is already running to prevent duplicates. */
        @Volatile
        var isServerActive: Boolean = false
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SERVER -> startServerLocked()
            ACTION_STOP_SERVER -> stopServerLocked()
            else -> {
                if (!isRunningFlag) {
                    startServerLocked()
                }
            }
        }
        return START_STICKY
    }

    private fun startServerLocked() {
        if (isRunningFlag || isServerActive) {
            return // Prevent duplicate servers
        }

        val notification = buildNotification(
            title = "DeepSeek Web2API",
            text = "الخادم يعمل على 127.0.0.1:$PORT",
            status = "Running"
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        val client = resolveClient()
        server = KtorServer(PORT, client)
        server?.start()
        isRunningFlag = true
        isServerActive = true
    }

    private fun stopServerLocked() {
        server?.stop()
        server = null
        isRunningFlag = false
        isServerActive = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun resolveClient(): DeepSeekClient? {
        val repo = accounts ?: return null
        val account = repo.firstAccount() ?: return null
        val token = account.token
        if (token.isBlank()) return null
        val cookies = CredentialStore.getCookies(account.id) ?: account.cookies
        return DeepSeekClient.create(token, cookies)
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "DeepSeek API Server",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "DeepSeek Web2API local server status"
                setShowBadge(false)
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, text: String, status: String): Notification {
        val stopIntent = Intent(this, LocalApiService::class.java).apply {
            action = ACTION_STOP_SERVER
        }
        val stopPendingIntent = PendingIntent.getService(
            this, STOP_REQUEST_CODE, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val activityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val activityPending = PendingIntent.getActivity(
            this, 0, activityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("$text — $status")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(activityPending)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إيقاف", stopPendingIntent)
            .build()
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onDestroy() {
        server?.stop()
        server = null
        isRunningFlag = false
        isServerActive = false
        super.onDestroy()
    }
}
