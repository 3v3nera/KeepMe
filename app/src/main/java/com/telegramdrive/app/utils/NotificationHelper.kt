package com.telegramdrive.app.utils

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {

    const val CHANNEL_PROGRESS_ID = "backup_progress_v2"
    const val CHANNEL_COMPLETED_ID = "backup_completed_v2"
    const val CHANNEL_USER_UPLOADS_ID = "user_uploads_v2"

    const val PROGRESS_NOTIFICATION_ID = 2001
    const val COMPLETION_NOTIFICATION_ID = 2002
    const val USER_UPLOAD_NOTIFICATION_ID = 3001
    const val TEST_NOTIFICATION_ID = 9999

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            // 1. Progress Channel (Shows active upload progress bar)
            val progressChannel = NotificationChannel(
                CHANNEL_PROGRESS_ID,
                "Backup Upload Progress",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows live upload progress while files are uploading"
                setShowBadge(false)
            }

            // 2. Completion Channel (Shows daily completion alert)
            val completedChannel = NotificationChannel(
                CHANNEL_COMPLETED_ID,
                "Daily Backup Completion",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts you every day when your cloud backup has finished"
                setShowBadge(true)
                enableVibration(true)
            }

            // 3. User Uploads Channel (Shows live progress when user initiates uploads)
            val uploadChannel = NotificationChannel(
                CHANNEL_USER_UPLOADS_ID,
                "File Uploads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live upload progress when you upload files"
                setShowBadge(false)
            }

            manager.createNotificationChannel(progressChannel)
            manager.createNotificationChannel(completedChannel)
            manager.createNotificationChannel(uploadChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun buildProgressNotification(
        context: Context,
        current: Int,
        total: Int,
        fileName: String
    ): Notification {
        createNotificationChannels(context)
        val percent = if (total > 0) ((current.toFloat() / total.toFloat()) * 100).toInt() else 0
        val title = if (total > 0) "Backing up to KeepMe ($percent%)" else "KeepMe Backup"
        val content = if (total > 0) "Uploading $current of $total • $fileName" else "Syncing files..."

        return NotificationCompat.Builder(context, CHANNEL_PROGRESS_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle(title)
            .setContentText(content)
            .setProgress(total, current, total == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    fun showCompletionNotification(context: Context, totalFilesUploaded: Int) {
        if (!hasNotificationPermission(context)) return
        createNotificationChannels(context)

        val title = "Backup Complete"
        val content = if (totalFilesUploaded > 0) {
            "All done! $totalFilesUploaded file${if (totalFilesUploaded == 1) "" else "s"} backed up safely to KeepMe."
        } else {
            "You're all caught up! Everything is safely backed up."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_COMPLETED_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setOngoing(false)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(COMPLETION_NOTIFICATION_ID, notification)
        } catch (_: Exception) {}
    }

    fun sendTestNotification(context: Context) {
        if (!hasNotificationPermission(context)) return
        createNotificationChannels(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_COMPLETED_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentTitle("KeepMe Notifications Working! 🎉")
            .setContentText("You will receive live progress during uploads and daily completion alerts.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setOngoing(false)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(TEST_NOTIFICATION_ID, notification)
        } catch (_: Exception) {}
    }

    fun buildFileUploadProgressNotification(
        context: Context,
        current: Int,
        total: Int,
        fileName: String,
        currentFileBytes: Long = 0L,
        totalFileBytes: Long = 0L
    ): Notification {
        createNotificationChannels(context)

        val percentStr = if (totalFileBytes > 0L) {
            val pct = ((currentFileBytes.toDouble() / totalFileBytes.toDouble()) * 100).toInt().coerceIn(0, 100)
            "$pct%"
        } else ""

        val title = if (total > 1) {
            "Uploading to KeepMe ($current of $total)"
        } else {
            "Uploading to KeepMe"
        }

        val content = buildString {
            append(fileName)
            if (percentStr.isNotBlank()) {
                append(" • ")
                append(percentStr)
            }
        }

        val currentPct = if (totalFileBytes > 0L) {
            ((currentFileBytes.toDouble() / totalFileBytes.toDouble()) * 100).toInt().coerceIn(0, 100)
        } else 0

        return NotificationCompat.Builder(context, CHANNEL_USER_UPLOADS_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle(title)
            .setContentText(content)
            .setProgress(100, currentPct, totalFileBytes <= 0L)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    fun showFileUploadCompletionNotification(context: Context, totalFiles: Int) {
        if (!hasNotificationPermission(context)) return
        createNotificationChannels(context)

        val title = "Upload Complete"
        val content = if (totalFiles == 1) {
            "File successfully uploaded to KeepMe."
        } else {
            "All $totalFiles files successfully uploaded to KeepMe."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_COMPLETED_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setOngoing(false)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(USER_UPLOAD_NOTIFICATION_ID + 1, notification)
        } catch (_: Exception) {}
    }
}
