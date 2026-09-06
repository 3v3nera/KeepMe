package com.telegramdrive.app.worker

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.content.pm.ServiceInfo
import android.os.PowerManager
import android.util.Log
import androidx.work.ForegroundInfo
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.telegramdrive.app.data.datastore.SettingsDataStore
import com.telegramdrive.app.data.repository.FileRepository
import com.telegramdrive.app.data.repository.FolderRepository
import com.telegramdrive.app.utils.FileUriUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar
import java.util.concurrent.TimeUnit

@HiltWorker
class CameraBackupWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val fileRepository: FileRepository,
    private val folderRepository: FolderRepository,
    private val telegramManager: com.telegramdrive.app.engine.TelegramManager,
    private val settingsDataStore: SettingsDataStore
) : CoroutineWorker(context, workerParams) {

    private data class MediaItem(
        val uri: Uri,
        val mimeType: String,
        val name: String,
        val dateAdded: Long,
        val dateTaken: Long = 0L
    )

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = com.telegramdrive.app.utils.NotificationHelper.buildProgressNotification(
            context, 0, 0, "Checking files..."
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                com.telegramdrive.app.utils.NotificationHelper.PROGRESS_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(
                com.telegramdrive.app.utils.NotificationHelper.PROGRESS_NOTIFICATION_ID,
                notification
            )
        }
    }

    override suspend fun doWork(): Result {
        val isCameraBackup = settingsDataStore.isCameraBackupEnabled()
        val isCustomFolderBackup = settingsDataStore.isCustomFolderBackupEnabled()

        if (!isCameraBackup && !isCustomFolderBackup) {
            return Result.success()
        }

        // 1. Promote to Foreground Service to prevent OS process killing when swiped from Recents
        try {
            setForeground(getForegroundInfo())
        } catch (e: Exception) {
            Log.w("CameraBackupWorker", "Unable to set foreground: ${e.message}")
        }

        // 2. Acquire a partial wake lock to keep the device CPU running during background transfer
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TDrive::BackupWakeLock")?.apply {
            setReferenceCounted(false)
            acquire(45 * 60 * 1000L) // 45 minutes safety timeout
        }

        try {
            telegramManager.ensureReady()
            val scope = settingsDataStore.getBackupScope()

            // Make sure local folder cache is synced
            folderRepository.syncFolders()
            val folders = folderRepository.getAllFolders().firstOrNull() ?: emptyList()

            val mediaItems = mutableListOf<MediaItem>()
            var backupChatId: Long = 0L

            // -------------------------------------------------------------
            // 1. Camera Backup (Photos & Videos)
            // -------------------------------------------------------------
            if (isCameraBackup) {
                var backupFolder = folders.find { 
                    it.name.startsWith("Camera Backup", ignoreCase = true) || it.name == "Camera Backup" 
                }
                backupChatId = backupFolder?.id ?: folderRepository.createFolder("Camera Backup")

                val storedTimestamp = settingsDataStore.getLastBackupTimestamp()
                val effectiveTimestamp = if (storedTimestamp > 0L) {
                    storedTimestamp
                } else if (scope == "FROM_TODAY") {
                    Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                } else {
                    0L
                }

                // Helper to ensure only actual camera photos/videos are collected (filters out WhatsApp, Screenshots, Downloads, etc.)
                fun isCameraMedia(bucketName: String?, relativePath: String?, dataPath: String?): Boolean {
                    val bucket = bucketName?.lowercase()?.trim() ?: ""
                    val rel = relativePath?.lowercase()?.trim() ?: ""
                    val data = dataPath?.lowercase()?.trim() ?: ""

                    // Strictly exclude non-camera directories/apps
                    if (bucket.contains("screenshot") || rel.contains("screenshot") || data.contains("screenshot")) return false
                    if (bucket.contains("whatsapp") || rel.contains("whatsapp") || data.contains("whatsapp")) return false
                    if (bucket.contains("telegram") || rel.contains("telegram") || data.contains("telegram")) return false
                    if (bucket.contains("instagram") || rel.contains("instagram") || data.contains("instagram")) return false
                    if (bucket.contains("download") || rel.contains("download") || data.contains("download")) return false
                    if (bucket.contains("facebook") || rel.contains("facebook") || data.contains("facebook")) return false
                    if (bucket.contains("snapchat") || rel.contains("snapchat") || data.contains("snapchat")) return false
                    if (bucket.contains("twitter") || rel.contains("twitter") || data.contains("twitter") || bucket == "x") return false
                    if (bucket.contains("pin") || rel.contains("pin") || data.contains("pin")) return false

                    // Explicit camera matches
                    if (bucket == "camera" || bucket == "100andro" || bucket == "100media") return true
                    if (rel.startsWith("dcim/camera") || rel.startsWith("dcim/100andro") || rel.startsWith("dcim/100media")) return true
                    if (data.contains("/dcim/camera/") || data.contains("/dcim/100andro/") || data.contains("/dcim/100media/")) return true
                    if ((bucket == "dcim" || rel.startsWith("dcim/")) && !rel.contains("screenshot")) return true

                    return false
                }

                // Collect Images
                val imageProjection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    arrayOf(
                        MediaStore.Images.Media._ID,
                        MediaStore.Images.Media.DATE_ADDED,
                        MediaStore.Images.Media.DATE_TAKEN,
                        MediaStore.Images.Media.MIME_TYPE,
                        MediaStore.Images.Media.DISPLAY_NAME,
                        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        MediaStore.MediaColumns.DATA
                    )
                } else {
                    arrayOf(
                        MediaStore.Images.Media._ID,
                        MediaStore.Images.Media.DATE_ADDED,
                        MediaStore.Images.Media.DATE_TAKEN,
                        MediaStore.Images.Media.MIME_TYPE,
                        MediaStore.Images.Media.DISPLAY_NAME,
                        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                        MediaStore.MediaColumns.DATA
                    )
                }
                val imageSelection = "${MediaStore.Images.Media.DATE_ADDED} > ?"
                val imageSelectionArgs = arrayOf((effectiveTimestamp / 1000).toString())

                context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    imageProjection,
                    imageSelection,
                    imageSelectionArgs,
                    "${MediaStore.Images.Media.DATE_ADDED} ASC"
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                    val takenCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                    val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val bucketCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                    val relCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH) else -1
                    val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)

                    while (cursor.moveToNext()) {
                        val bucketName = if (bucketCol != -1) cursor.getString(bucketCol) else null
                        val relPath = if (relCol != -1) cursor.getString(relCol) else null
                        val dataPath = if (dataCol != -1) cursor.getString(dataCol) else null

                        if (!isCameraMedia(bucketName, relPath, dataPath)) {
                            continue
                        }

                        val id = cursor.getLong(idCol)
                        val dateAdded = cursor.getLong(dateCol) * 1000L
                        val dateTaken = if (takenCol != -1 && !cursor.isNull(takenCol)) cursor.getLong(takenCol) else dateAdded
                        val mimeType = cursor.getString(mimeCol) ?: "image/jpeg"
                        val rawName = cursor.getString(nameCol)
                        val name = if (!rawName.isNullOrBlank() && rawName.contains('.')) {
                            rawName
                        } else if (dateTaken > 0L) {
                            "IMG_${java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date(dateTaken))}.jpg"
                        } else {
                            "photo_$id.jpg"
                        }
                        val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                        mediaItems.add(MediaItem(contentUri, mimeType, name, dateAdded, dateTaken))
                    }
                }

                // Collect Videos
                val videoProjection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    arrayOf(
                        MediaStore.Video.Media._ID,
                        MediaStore.Video.Media.DATE_ADDED,
                        MediaStore.Video.Media.DATE_TAKEN,
                        MediaStore.Video.Media.MIME_TYPE,
                        MediaStore.Video.Media.DISPLAY_NAME,
                        MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        MediaStore.MediaColumns.DATA
                    )
                } else {
                    arrayOf(
                        MediaStore.Video.Media._ID,
                        MediaStore.Video.Media.DATE_ADDED,
                        MediaStore.Video.Media.DATE_TAKEN,
                        MediaStore.Video.Media.MIME_TYPE,
                        MediaStore.Video.Media.DISPLAY_NAME,
                        MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                        MediaStore.MediaColumns.DATA
                    )
                }
                val videoSelection = "${MediaStore.Video.Media.DATE_ADDED} > ?"
                val videoSelectionArgs = arrayOf((effectiveTimestamp / 1000).toString())

                context.contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    videoProjection,
                    videoSelection,
                    videoSelectionArgs,
                    "${MediaStore.Video.Media.DATE_ADDED} ASC"
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                    val takenCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_TAKEN)
                    val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                    val bucketCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                    val relCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH) else -1
                    val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)

                    while (cursor.moveToNext()) {
                        val bucketName = if (bucketCol != -1) cursor.getString(bucketCol) else null
                        val relPath = if (relCol != -1) cursor.getString(relCol) else null
                        val dataPath = if (dataCol != -1) cursor.getString(dataCol) else null

                        if (!isCameraMedia(bucketName, relPath, dataPath)) {
                            continue
                        }

                        val id = cursor.getLong(idCol)
                        val dateAdded = cursor.getLong(dateCol) * 1000L
                        val dateTaken = if (takenCol != -1 && !cursor.isNull(takenCol)) cursor.getLong(takenCol) else dateAdded
                        val mimeType = cursor.getString(mimeCol) ?: "video/mp4"
                        val rawName = cursor.getString(nameCol)
                        val name = if (!rawName.isNullOrBlank() && rawName.contains('.')) {
                            rawName
                        } else if (dateTaken > 0L) {
                            "VID_${java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date(dateTaken))}.mp4"
                        } else {
                            "video_$id.mp4"
                        }
                        val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                        mediaItems.add(MediaItem(contentUri, mimeType, name, dateAdded, dateTaken))
                    }
                }

                mediaItems.sortBy { it.dateAdded }
            }

            // -------------------------------------------------------------
            // 2. Custom Folder Backup (SAF Tree)
            // -------------------------------------------------------------
            val pendingCustomFiles = mutableListOf<DocumentFile>()
            var customChatId: Long = 0L

            if (isCustomFolderBackup) {
                val customFolderUriStr = settingsDataStore.getCustomFolderUri()
                val customFolderName = settingsDataStore.getCustomFolderName() ?: "Custom Backup"

                if (!customFolderUriStr.isNullOrBlank()) {
                    val treeUri = Uri.parse(customFolderUriStr)
                    val docFolder = DocumentFile.fromTreeUri(context, treeUri)

                    if (docFolder != null && docFolder.isDirectory) {
                        val targetTitle = customFolderName.replace("[TD]", "").trim()
                        val customFolderEntity = folders.find { 
                            it.name.contains(targetTitle, ignoreCase = true) 
                        }
                        customChatId = customFolderEntity?.id ?: folderRepository.createFolder(targetTitle)

                        val storedCustomTimestamp = settingsDataStore.getLastCustomFolderBackupTimestamp()
                        val effectiveCustomTimestamp = if (storedCustomTimestamp > 0L) {
                            storedCustomTimestamp
                        } else if (scope == "FROM_TODAY") {
                            Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                        } else {
                            0L
                        }

                        pendingCustomFiles.addAll(
                            docFolder.listFiles()
                                .filter { it.isFile && it.lastModified() > effectiveCustomTimestamp }
                                .sortedBy { it.lastModified() }
                        )
                    }
                }
            }

            val totalFiles = mediaItems.size + pendingCustomFiles.size
            if (totalFiles == 0) {
                // Show completion notification so user always knows daily check ran
                com.telegramdrive.app.utils.NotificationHelper.showCompletionNotification(context, 0)
                settingsDataStore.setLastBackupCompletedTime(System.currentTimeMillis())
                return Result.success()
            }

            var currentUploaded = 0
            val initialNotification = com.telegramdrive.app.utils.NotificationHelper.buildProgressNotification(
                context, currentUploaded, totalFiles, "Starting upload..."
            )
            try {
                val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ForegroundInfo(com.telegramdrive.app.utils.NotificationHelper.PROGRESS_NOTIFICATION_ID, initialNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                } else {
                    ForegroundInfo(com.telegramdrive.app.utils.NotificationHelper.PROGRESS_NOTIFICATION_ID, initialNotification)
                }
                setForeground(info)
            } catch (_: Exception) {
                androidx.core.app.NotificationManagerCompat.from(context).notify(com.telegramdrive.app.utils.NotificationHelper.PROGRESS_NOTIFICATION_ID, initialNotification)
            }

            var lastNotifTime = 0L

            // Progressively upload camera items
            for (item in mediaItems) {
                currentUploaded++
                val notif = com.telegramdrive.app.utils.NotificationHelper.buildProgressNotification(
                    context, currentUploaded, totalFiles, item.name
                )
                try {
                    androidx.core.app.NotificationManagerCompat.from(context).notify(com.telegramdrive.app.utils.NotificationHelper.PROGRESS_NOTIFICATION_ID, notif)
                } catch (_: Exception) {}

                fileRepository.uploadFileAndWait(
                    chatId = backupChatId,
                    uri = item.uri,
                    mimeType = item.mimeType,
                    fileName = item.name
                ) { uploaded, total ->
                    val now = System.currentTimeMillis()
                    if (now - lastNotifTime >= 500L) {
                        lastNotifTime = now
                        val pctStr = if (total > 0L) " (${((uploaded.toDouble() / total.toDouble()) * 100).toInt()}%)" else ""
                        val progressNotif = com.telegramdrive.app.utils.NotificationHelper.buildProgressNotification(
                            context, currentUploaded, totalFiles, "${item.name}$pctStr"
                        )
                        try {
                            androidx.core.app.NotificationManagerCompat.from(context).notify(com.telegramdrive.app.utils.NotificationHelper.PROGRESS_NOTIFICATION_ID, progressNotif)
                        } catch (_: Exception) {}
                    }
                }

                if (item.dateAdded > settingsDataStore.getLastBackupTimestamp()) {
                    settingsDataStore.setLastBackupTimestamp(item.dateAdded)
                }
            }

            // Progressively upload custom folder files
            for (docFile in pendingCustomFiles) {
                currentUploaded++
                val mime = docFile.type ?: "*/*"
                val name = docFile.name ?: "doc_${System.currentTimeMillis()}"
                val notif = com.telegramdrive.app.utils.NotificationHelper.buildProgressNotification(
                    context, currentUploaded, totalFiles, name
                )
                try {
                    androidx.core.app.NotificationManagerCompat.from(context).notify(com.telegramdrive.app.utils.NotificationHelper.PROGRESS_NOTIFICATION_ID, notif)
                } catch (_: Exception) {}

                fileRepository.uploadFileAndWait(
                    chatId = customChatId,
                    uri = docFile.uri,
                    mimeType = mime,
                    fileName = name
                ) { uploaded, total ->
                    val now = System.currentTimeMillis()
                    if (now - lastNotifTime >= 500L) {
                        lastNotifTime = now
                        val pctStr = if (total > 0L) " (${((uploaded.toDouble() / total.toDouble()) * 100).toInt()}%)" else ""
                        val progressNotif = com.telegramdrive.app.utils.NotificationHelper.buildProgressNotification(
                            context, currentUploaded, totalFiles, "$name$pctStr"
                        )
                        try {
                            androidx.core.app.NotificationManagerCompat.from(context).notify(com.telegramdrive.app.utils.NotificationHelper.PROGRESS_NOTIFICATION_ID, progressNotif)
                        } catch (_: Exception) {}
                    }
                }

                if (docFile.lastModified() > settingsDataStore.getLastCustomFolderBackupTimestamp()) {
                    settingsDataStore.setLastCustomFolderBackupTimestamp(docFile.lastModified())
                }
            }

            // Record completed backup timestamp, show completion notification & clean up temporary upload cache
            com.telegramdrive.app.utils.NotificationHelper.showCompletionNotification(context, totalFiles)
            settingsDataStore.setLastBackupCompletedTime(System.currentTimeMillis())
            FileUriUtils.cleanupOldUploadCache(context)

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        } finally {
            try {
                wakeLock?.release()
            } catch (_: Exception) {}
        }
    }

    companion object {
        const val WORK_NAME = "CameraBackupWork"
        const val ONE_TIME_WORK_NAME = "CameraBackupImmediate"
        const val NOTIFICATION_CHANNEL_ID = "backup_channel"
        const val NOTIFICATION_ID = 2001
        const val EXACT_ALARM_REQUEST_CODE = 9001

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Backup Progress",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shows live progress during KeepMe backup"
                    setShowBadge(false)
                }
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                manager?.createNotificationChannel(channel)
            }
        }

        /**
         * Schedules 24h periodic backup at the requested [targetHour] and [targetMinute].
         * Uses AlarmManager.setAlarmClock for exact-minute waking even from Doze / swiped Recents,
         * with WorkManager PeriodicWork as an OS safety net.
         */
        fun schedule(
            context: Context,
            wifiOnly: Boolean = true,
            chargingOnly: Boolean = false,
            targetHour: Int = 2,
            targetMinute: Int = 0
        ) {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, targetHour)
                set(Calendar.MINUTE, targetMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now)) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            // 1. Exact Alarm Clock scheduling (guaranteed to wake device at exact minute)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (alarmManager != null) {
                val intent = Intent(context, com.telegramdrive.app.receiver.CameraBackupAlarmReceiver::class.java).apply {
                    action = com.telegramdrive.app.receiver.CameraBackupAlarmReceiver.ACTION_TRIGGER_BACKUP
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    EXACT_ALARM_REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        target.timeInMillis,
                        pendingIntent
                    )
                    Log.d("CameraBackupWorker", "Scheduled silent exact alarm for: ${target.time}")
                } catch (e: Exception) {
                    try {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            target.timeInMillis,
                            pendingIntent
                        )
                    } catch (e2: Exception) {
                        alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            target.timeInMillis,
                            pendingIntent
                        )
                    }
                }
            }

            // 2. WorkManager Periodic Work as OS safety net
            val initialDelayMs = target.timeInMillis - now.timeInMillis
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                .setRequiresCharging(chargingOnly)
                .build()

            val backupRequest = PeriodicWorkRequestBuilder<CameraBackupWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                backupRequest
            )
        }

        /**
         * Triggers an immediate one-time backup on demand.
         */
        fun runOnce(
            context: Context,
            wifiOnly: Boolean = false,
            chargingOnly: Boolean = false
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                .setRequiresCharging(chargingOnly)
                .build()

            val oneTimeRequest = OneTimeWorkRequestBuilder<CameraBackupWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest
            )
        }

        fun cancel(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            val intent = Intent(context, com.telegramdrive.app.receiver.CameraBackupAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                EXACT_ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager?.cancel(pendingIntent)
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
