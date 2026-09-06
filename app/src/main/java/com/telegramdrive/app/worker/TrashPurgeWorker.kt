package com.telegramdrive.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.telegramdrive.app.data.local.dao.FileMetadataDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class TrashPurgeWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val fileMetadataDao: FileMetadataDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Calculate cutoff: 30 days ago
            val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000
            val cutoff = System.currentTimeMillis() - thirtyDaysInMillis
            
            // Note: In a full implementation, you should also delete these files
            // from Telegram using TDLib, but here we just purge the Room DB.
            fileMetadataDao.purgeOldTrash(cutoff)
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
