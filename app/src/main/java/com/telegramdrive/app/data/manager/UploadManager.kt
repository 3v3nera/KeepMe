package com.telegramdrive.app.data.manager

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.telegramdrive.app.utils.FileUriUtils
import com.telegramdrive.app.worker.FileUploadWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Copies selected Uris into internal app cache immediately on IO dispatcher
     * (while the calling Activity holds transient read permissions),
     * and enqueues resilient background upload work via WorkManager.
     */
    suspend fun enqueueUploads(chatId: Long, uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        if (uris.isEmpty() || chatId == 0L) return@withContext 0

        val localPaths = mutableListOf<String>()
        val fileNames = mutableListOf<String>()
        val mimeTypes = mutableListOf<String>()

        for (uri in uris) {
            try {
                val mimeType = context.contentResolver.getType(uri) ?: "*/*"
                var name = "Uploaded_File_${System.currentTimeMillis()}"
                try {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            name = cursor.getString(nameIndex) ?: name
                        }
                    }
                } catch (_: Exception) {}

                val localPath = FileUriUtils.resolveUriToLocalPath(context, uri, name)
                if (File(localPath).exists()) {
                    localPaths.add(localPath)
                    fileNames.add(name)
                    mimeTypes.add(mimeType)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (localPaths.isNotEmpty()) {
            val inputData = Data.Builder()
                .putLong(FileUploadWorker.KEY_CHAT_ID, chatId)
                .putStringArray(FileUploadWorker.KEY_FILE_PATHS, localPaths.toTypedArray())
                .putStringArray(FileUploadWorker.KEY_FILE_NAMES, fileNames.toTypedArray())
                .putStringArray(FileUploadWorker.KEY_MIME_TYPES, mimeTypes.toTypedArray())
                .build()

            val workRequest = OneTimeWorkRequestBuilder<FileUploadWorker>()
                .setInputData(inputData)
                .addTag("user_upload")
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }

        return@withContext localPaths.size
    }
}
