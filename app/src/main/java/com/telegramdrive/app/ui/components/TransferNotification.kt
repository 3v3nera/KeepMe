package com.telegramdrive.app.ui.components

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.telegramdrive.app.domain.model.TransferProgress
import com.telegramdrive.app.domain.model.TransferState

class TransferNotification(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId = "transfer_channel"

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "File Transfers",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for file uploads and downloads"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showOrUpdateNotification(progress: TransferProgress) {
        val title = if (progress.isUpload) "Uploading ${progress.fileName}" else "Downloading ${progress.fileName}"
        
        val contentText = when (progress.state) {
            TransferState.QUEUED -> "Queued..."
            TransferState.IN_PROGRESS -> "${(progress.percentage * 100).toInt()}% • ${formatSize(progress.speedBytesPerSec)}/s"
            TransferState.PAUSED -> "Paused"
            TransferState.COMPLETED -> "Completed"
            TransferState.FAILED -> "Failed"
        }

        val max = 100
        val current = (progress.percentage * 100).toInt()
        val indeterminate = progress.state == TransferState.QUEUED

        val builder = NotificationCompat.Builder(context, channelId)
            // Note: Replace with actual drawable later if needed. For now use android internal icon.
            .setSmallIcon(android.R.drawable.stat_sys_upload) 
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(progress.state == TransferState.IN_PROGRESS || progress.state == TransferState.PAUSED)
            
        if (progress.state == TransferState.IN_PROGRESS || progress.state == TransferState.QUEUED) {
            builder.setProgress(max, current, indeterminate)
        } else if (progress.state == TransferState.COMPLETED) {
            builder.setProgress(0, 0, false)
            builder.setSmallIcon(android.R.drawable.stat_sys_download_done)
        }

        notificationManager.notify(progress.fileId, builder.build())
    }

    fun cancelNotification(fileId: Int) {
        notificationManager.cancel(fileId)
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format("%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format("%.1f GB", gb)
    }
}
