package com.telegramdrive.app.worker

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.telegramdrive.app.data.repository.FileRepository
import com.telegramdrive.app.engine.TelegramManager
import com.telegramdrive.app.utils.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class FileUploadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val fileRepository: FileRepository,
    private val telegramManager: TelegramManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_CHAT_ID = "chat_id"
        const val KEY_FILE_PATHS = "file_paths"
        const val KEY_FILE_NAMES = "file_names"
        const val KEY_MIME_TYPES = "mime_types"
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = NotificationHelper.buildFileUploadProgressNotification(
            context = context,
            current = 0,
            total = 1,
            fileName = "Preparing upload..."
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NotificationHelper.USER_UPLOAD_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(
                NotificationHelper.USER_UPLOAD_NOTIFICATION_ID,
                notification
            )
        }
    }

    override suspend fun doWork(): Result {
        val chatId = inputData.getLong(KEY_CHAT_ID, 0L)
        val filePaths = inputData.getStringArray(KEY_FILE_PATHS) ?: emptyArray()
        val fileNames = inputData.getStringArray(KEY_FILE_NAMES) ?: emptyArray()
        val mimeTypes = inputData.getStringArray(KEY_MIME_TYPES) ?: emptyArray()

        if (chatId == 0L || filePaths.isEmpty()) {
            Log.w("FileUploadWorker", "No files or invalid chatId provided for upload.")
            return Result.success()
        }

        val totalFiles = filePaths.size

        // 1. Promote worker to Foreground Service to survive app swipe-away from Recents
        try {
            setForeground(getForegroundInfo())
        } catch (e: Exception) {
            Log.w("FileUploadWorker", "Unable to set initial foreground: ${e.message}")
        }

        // 2. Acquire CPU wake lock to ensure background transfers don't stall when screen is locked
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TDrive::FileUploadWakeLock")?.apply {
            setReferenceCounted(false)
            acquire(60 * 60 * 1000L) // 60 minutes safety timeout
        }

        try {
            // Ensure TDLib client is alive and authorized
            telegramManager.ensureReady()

            var lastNotifTime = 0L

            for (i in filePaths.indices) {
                val path = filePaths[i]
                val name = fileNames.getOrNull(i) ?: "File_${i + 1}"
                val mime = mimeTypes.getOrNull(i) ?: "*/*"
                val fileNum = i + 1

                // Initial notification for current file
                updateNotification(fileNum, totalFiles, name, 0L, 0L)

                fileRepository.uploadFileAndWait(
                    chatId = chatId,
                    localPath = path,
                    mimeType = mime,
                    fileName = name
                ) { uploaded, total ->
                    val now = System.currentTimeMillis()
                    if (now - lastNotifTime >= 500L) { // Throttle updates to avoid flooding system notification service
                        lastNotifTime = now
                        updateNotification(fileNum, totalFiles, name, uploaded, total)
                    }
                }
            }

            // Show final completion notification
            NotificationHelper.showFileUploadCompletionNotification(context, totalFiles)
            return Result.success()
        } catch (e: Exception) {
            Log.e("FileUploadWorker", "Error during file upload", e)
            return Result.failure()
        } finally {
            try {
                wakeLock?.release()
            } catch (_: Exception) {}
        }
    }

    private fun updateNotification(
        current: Int,
        total: Int,
        fileName: String,
        currentBytes: Long,
        totalBytes: Long
    ) {
        try {
            val notif = NotificationHelper.buildFileUploadProgressNotification(
                context = context,
                current = current,
                total = total,
                fileName = fileName,
                currentFileBytes = currentBytes,
                totalFileBytes = totalBytes
            )
            androidx.core.app.NotificationManagerCompat.from(context)
                .notify(NotificationHelper.USER_UPLOAD_NOTIFICATION_ID, notif)
        } catch (e: Exception) {
            Log.w("FileUploadWorker", "Failed to update progress notification: ${e.message}")
        }
    }
}
