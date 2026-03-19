package com.xoomat.tgwsproxy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.app.Service
import android.os.Handler
import android.os.IBinder
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat

class ProxyService : Service() {
    @Volatile
    private var startInProgress = false

    private val monitorHandler = Handler(Looper.getMainLooper())
    private val monitorRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            if (!ProxyController.isRunning()) {
                val err = ProxyController.lastError()
                Log.e("TgWsProxy", "Proxy thread stopped unexpectedly: $err")
                notifyStatus(false, err)
                stopSelf()
                return
            }
            monitorHandler.postDelayed(this, 1000)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            ACTION_START -> startProxy()
            else -> startProxy()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopProxy()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startProxy() {
        if (isRunning || startInProgress) return
        startInProgress = true
        createChannelIfNeeded()
        try {
            startForeground(NOTIFICATION_ID, buildNotification())
        } catch (e: Exception) {
            Log.e("TgWsProxy", "startForeground failed", e)
            startInProgress = false
            notifyStatus(false, e.message ?: "startForeground failed")
            stopSelf()
            return
        }

        Thread {
            val result = ProxyController.start(applicationContext)
            monitorHandler.post {
                if (!startInProgress) return@post
                startInProgress = false
                if (result.isFailure) {
                    val err = result.exceptionOrNull()?.message ?: "start failed"
                    Log.e("TgWsProxy", "Proxy start failed", result.exceptionOrNull())
                    notifyStatus(false, err)
                    stopSelf()
                    return@post
                }
                isRunning = true
                notifyStatus(true, "")
                monitorHandler.postDelayed(monitorRunnable, 1000)
            }
        }.start()
    }

    private fun stopProxy() {
        isRunning = false
        startInProgress = false
        monitorHandler.removeCallbacks(monitorRunnable)
        ProxyController.stop()
        notifyStatus(false, "")
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            100,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this,
            101,
            Intent(this, ProxyService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.status_running))
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(0, getString(R.string.stop_button), stopIntent)
            .build()
    }

    private fun notifyStatus(running: Boolean, error: String) {
        val intent = Intent(ACTION_STATUS).apply {
            `package` = packageName
            putExtra(EXTRA_RUNNING, running)
            putExtra(EXTRA_ERROR, error)
        }
        sendBroadcast(intent)
    }

    companion object {
        const val ACTION_START = "com.xoomat.tgwsproxy.action.START"
        const val ACTION_STOP = "com.xoomat.tgwsproxy.action.STOP"
        const val ACTION_STATUS = "com.xoomat.tgwsproxy.action.STATUS"
        const val EXTRA_RUNNING = "running"
        const val EXTRA_ERROR = "error"
        private const val CHANNEL_ID = "tg_ws_proxy_service"
        private const val NOTIFICATION_ID = 777

        @Volatile
        var isRunning: Boolean = false
    }
}
