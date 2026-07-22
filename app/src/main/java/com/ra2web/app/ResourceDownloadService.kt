package com.ra2web.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * 资源下载前台服务
 * 适配 Android 14+ 的前台服务类型要求 (dataSync)
 */
class ResourceDownloadService : Service() {

    companion object {
        const val CHANNEL_ID = "ra2web_download_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.ra2web.action.START_DOWNLOAD"
        const val ACTION_STOP = "com.ra2web.action.STOP_DOWNLOAD"

        fun startService(context: Context) {
            val intent = Intent(context, ResourceDownloadService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, ResourceDownloadService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    private var resourceManager: ResourceManager? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        resourceManager = ResourceManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, createNotification("准备下载游戏资源...", 0))
                startDownload()
            }
            ACTION_STOP -> {
                stopDownload()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startDownload() {
        resourceManager?.downloadResources(
            onProgress = { progress, status ->
                updateNotification(status, progress)
            },
            onComplete = {
                updateNotification("资源下载完成", 100)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            },
            onError = { error ->
                updateNotification("下载失败: $error", 0)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        )
    }

    private fun stopDownload() {
        // 可以在这里添加取消下载的逻辑
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "资源下载",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "游戏资源下载进度通知"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String, progress: Int): Notification {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("网页红井 RA2WEB")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .setProgress(100, progress, progress == 0)
            .setOngoing(progress in 1..99)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(text: String, progress: Int) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification(text, progress))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
    }
}
