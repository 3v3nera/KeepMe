package com.telegramdrive.app.data.repository

import android.content.Context
import android.net.Uri
import com.telegramdrive.app.data.local.dao.FileMetadataDao
import dagger.hilt.android.qualifiers.ApplicationContext
import com.telegramdrive.app.data.local.entity.FileMetadataEntity
import com.telegramdrive.app.domain.model.FileType
import com.telegramdrive.app.domain.model.TelegramFile
import com.telegramdrive.app.domain.model.TransferProgress
import com.telegramdrive.app.domain.model.TransferState
import com.telegramdrive.app.engine.TelegramManager
import kotlinx.coroutines.flow.Flow
import org.drinkless.tdlib.TdApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

import java.io.File
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import com.telegramdrive.app.data.local.dao.FolderDao
import kotlinx.coroutines.flow.firstOrNull

@Singleton
class FileRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileMetadataDao: FileMetadataDao,
    private val folderDao: FolderDao,
    private val telegramManager: TelegramManager
) {

    init {
        CoroutineScope(Dispatchers.IO).launch {
            // One-time cleanup for the prior cross-chat trash corruption
            val prefs = context.getSharedPreferences("tdrive_migration", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("trash_scoped_migration_done", false)) {
                fileMetadataDao.resetAllTrash()
                prefs.edit().putBoolean("trash_scoped_migration_done", true).apply()
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            telegramManager.fileUpdates.collect { file ->
                if (file.local.isDownloadingCompleted && file.local.path.isNotBlank() && File(file.local.path).exists()) {
                    // Update thumbnail path (for thumbnail file IDs)
                    fileMetadataDao.updateThumbnailPath(file.id, file.local.path)
                    // Update local file path (for full-file TDLib file IDs)
                    fileMetadataDao.updateLocalFilePath(file.id, file.local.path)
                }
            }
        }
    }

    suspend fun syncAllFolders() = withContext(Dispatchers.IO) {
        try {
            val folders = folderDao.getAllFolders().firstOrNull() ?: emptyList()
            folders.map { folder ->
                async { syncFolder(folder.chatId) }
            }.awaitAll()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getFilesInFolder(chatId: Long): Flow<List<FileMetadataEntity>> {
        // Automatically fetch from network in background when accessed
        CoroutineScope(Dispatchers.IO).launch {
            try {
                syncFolder(chatId)
            } catch (_: Exception) {}
        }
        return fileMetadataDao.getFilesInFolder(chatId)
    }

    fun getRecentFiles(limit: Int = 20): Flow<List<FileMetadataEntity>> = fileMetadataDao.getRecentFiles(limit)
    
    fun getFavoriteFiles(): Flow<List<FileMetadataEntity>> = fileMetadataDao.getFavoriteFiles()
    
    fun getTrashFiles(): Flow<List<FileMetadataEntity>> = fileMetadataDao.getTrashFiles()
    
    fun searchFiles(query: String): Flow<List<FileMetadataEntity>> = fileMetadataDao.searchFiles(query)
    
    fun getStorageStats(): Flow<List<com.telegramdrive.app.domain.model.StorageStat>> = fileMetadataDao.getStorageStats()

    fun getFileByMessageId(messageId: Long): Flow<FileMetadataEntity?> = fileMetadataDao.getFileByMessageId(messageId)

    suspend fun clearInvalidLocalFilePath(messageId: Long) {
        fileMetadataDao.clearLocalFilePath(messageId)
    }
    
    suspend fun setFavorite(chatId: Long, messageId: Long, isFavorite: Boolean) {
        fileMetadataDao.setFavorite(chatId, messageId, isFavorite)
    }
    
    suspend fun restoreFromTrash(chatId: Long, messageId: Long) {
        fileMetadataDao.restoreFromTrash(chatId, messageId)
    }
    
    suspend fun purgeOldTrash(cutoff: Long) {
        fileMetadataDao.purgeOldTrash(cutoff)
    }

    suspend fun syncFolder(chatId: Long) = withContext(Dispatchers.IO) {
        try {
            try {
                telegramManager.sendSuspend(TdApi.GetChat(chatId))
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // We only need SearchMessagesFilterDocument since we upload everything as documents.
            // Also keep Photo/Video/Audio filters for backward compatibility with old uploads.
            val filters = listOf<TdApi.SearchMessagesFilter>(
                TdApi.SearchMessagesFilterDocument(),
                TdApi.SearchMessagesFilterPhoto(),
                TdApi.SearchMessagesFilterVideo(),
                TdApi.SearchMessagesFilterAudio()
            )

            val allEntities = mutableListOf<FileMetadataEntity>()

            // Fetch ALL pages for each filter type sequentially
            for (filter in filters) {
                var fromMessageId = 0L

                while (true) {
                    val request = TdApi.SearchChatMessages().apply {
                        this.chatId = chatId
                        this.query = ""
                        this.fromMessageId = fromMessageId
                        this.offset = 0
                        this.limit = 100
                        this.filter = filter
                    }

                    val response = try {
                        telegramManager.sendSuspend(request) as TdApi.FoundChatMessages
                    } catch (e: Exception) {
                        e.printStackTrace()
                        break
                    }

                    if (response.messages.isEmpty()) break

                    response.messages.mapNotNull { mapMessageToFileEntity(it) }
                        .also { allEntities.addAll(it) }

                    fromMessageId = response.nextFromMessageId
                    if (fromMessageId == 0L) break
                }
            }

            if (allEntities.isNotEmpty()) {
                val existingFiles = fileMetadataDao.getExistingFilesInChat(chatId).associateBy { it.messageId }
                val mergedEntities = allEntities
                    .distinctBy { it.messageId }
                    .map { newEntity ->
                        val existing = existingFiles[newEntity.messageId]
                        if (existing != null) {
                            newEntity.copy(
                                isInTrash = existing.isInTrash,
                                trashedAt = existing.trashedAt,
                                isFavorite = existing.isFavorite,
                                localFilePath = existing.localFilePath ?: newEntity.localFilePath,
                                thumbnailPath = existing.thumbnailPath ?: newEntity.thumbnailPath
                            )
                        } else {
                            newEntity
                        }
                    }
                fileMetadataDao.upsertFiles(mergedEntities)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun uploadFileAndWait(
        chatId: Long,
        localPath: String,
        mimeType: String,
        fileName: String,
        timeoutMs: Long = 30 * 60 * 1000L,
        onProgress: ((uploadedBytes: Long, totalBytes: Long) -> Unit)? = null
    ): TdApi.Message {
        val inputMessageContent = if (mimeType.startsWith("video/")) {
            var duration = 0
            var width = 0
            var height = 0
            try {
                android.media.MediaMetadataRetriever().apply {
                    setDataSource(localPath)
                    duration = (extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L).toInt() / 1000
                    width = extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                    height = extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                    release()
                }
            } catch (_: Exception) {}

            TdApi.InputMessageVideo(
                TdApi.InputFileLocal(localPath),
                null, // thumbnail
                null, // cover
                0, // startTimestamp
                null, // addedStickerFileIds
                duration,
                width,
                height,
                true, // supportsStreaming
                null, // caption
                false, // showCaptionAboveMedia
                null, // selfDestructType
                false // hasSpoiler
            )
        } else {
            TdApi.InputMessageDocument(
                TdApi.InputFileLocal(localPath),
                null,  // thumbnail
                true,  // disableContentTypeDetection
                null   // caption
            )
        }

        // Send via TDLib
        val request = TdApi.SendMessage(chatId, null, null, null, null, inputMessageContent)
        val tempMessage = telegramManager.sendSuspend(request) as TdApi.Message
        val tempMessageId = tempMessage.id
        val deferred = telegramManager.registerPendingMessage(tempMessageId)

        // Identify TDLib file id to listen for progress updates
        var fileIdToTrack: Int? = null
        var expectedFileSize: Long = 0L
        when (val c = tempMessage.content) {
            is TdApi.MessageDocument -> {
                fileIdToTrack = c.document.document.id
                expectedFileSize = c.document.document.size
            }
            is TdApi.MessageVideo -> {
                fileIdToTrack = c.video.video.id
                expectedFileSize = c.video.video.size
            }
            is TdApi.MessagePhoto -> {
                val bestSize = c.photo.sizes.lastOrNull()
                fileIdToTrack = bestSize?.photo?.id
                expectedFileSize = bestSize?.photo?.size?.toLong() ?: 0L
            }
        }

        if (expectedFileSize <= 0L) {
            try {
                expectedFileSize = File(localPath).length()
            } catch (_: Exception) {}
        }

        val progressJob = if (fileIdToTrack != null && onProgress != null) {
            CoroutineScope(Dispatchers.IO).launch {
                telegramManager.fileUpdates.collect { file ->
                    if (file.id == fileIdToTrack) {
                        val uploaded = file.remote.uploadedSize
                        val total = if (file.size > 0) file.size else (if (file.expectedSize > 0) file.expectedSize else expectedFileSize)
                        onProgress(uploaded, total)
                    }
                }
            }
        } else null

        return try {
            val serverMessage = withTimeout(timeoutMs) {
                deferred.await()
            }

            // Immediately persist completed message into Room DB so UI updates instantly
            mapMessageToFileEntity(serverMessage)?.let { entity ->
                fileMetadataDao.upsertFiles(listOf(entity))
            }

            serverMessage
        } finally {
            progressJob?.cancel()
            telegramManager.unregisterPendingMessage(tempMessageId)
            try {
                val f = File(localPath)
                if (f.exists() && f.absolutePath.contains("upload_cache")) {
                    f.delete()
                }
            } catch (_: Exception) {}
        }
    }

    suspend fun uploadFileAndWait(
        chatId: Long,
        uri: Uri,
        mimeType: String,
        fileName: String,
        timeoutMs: Long = 30 * 60 * 1000L,
        onProgress: ((uploadedBytes: Long, totalBytes: Long) -> Unit)? = null
    ): TdApi.Message {
        val localPath = com.telegramdrive.app.utils.FileUriUtils.resolveUriToLocalPath(context, uri, fileName)
        return uploadFileAndWait(chatId, localPath, mimeType, fileName, timeoutMs, onProgress)
    }

    suspend fun uploadFile(chatId: Long, uri: Uri, mimeType: String, fileName: String) {
        uploadFileAndWait(chatId, uri, mimeType, fileName)
    }

    suspend fun downloadFile(remoteFileId: Int) {
        val request = TdApi.DownloadFile(remoteFileId, 5, 0, 0, false)
        telegramManager.sendSuspend(request)
        // Progress tracked via UpdateFile
    }

    fun requestThumbnailDownload(fileId: Int) {
        telegramManager.downloadFile(fileId)
    }

    /**
     * Trigger a full-resolution file download. Uses priority 32 (highest) so it
     * downloads ahead of thumbnails. The result is emitted via [TelegramManager.fileUpdates]
     * and persisted to Room via the init {} collector above.
     */
    fun downloadFullFile(tdlibFileId: Int) {
        // Priority 32 = highest, synchronous = false (don't block)
        telegramManager.downloadFileHighPriority(tdlibFileId)
    }

    suspend fun deleteFile(chatId: Long, messageId: Long) {
        // Soft delete in Room for trash functionality
        fileMetadataDao.moveToTrash(chatId, messageId, System.currentTimeMillis())
    }

    suspend fun deletePermanently(file: TelegramFile) {
        try {
            telegramManager.sendSuspend(TdApi.DeleteMessages(file.chatId, longArrayOf(file.messageId), true))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        fileMetadataDao.deleteFilePermanently(file.chatId, file.messageId)
    }

    suspend fun deleteFilesInChat(chatId: Long) = withContext(Dispatchers.IO) {
        fileMetadataDao.deleteFilesInChat(chatId)
    }

    suspend fun emptyTrash() {
        val trashed = fileMetadataDao.getTrashFiles().firstOrNull() ?: emptyList()
        trashed.forEach { file ->
            try {
                telegramManager.sendSuspend(TdApi.DeleteMessages(file.chatId, longArrayOf(file.messageId), true))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        fileMetadataDao.emptyTrash()
    }

    // Convert from TDLib Message to Room Entity (helper method for syncFolder / listeners)
    fun mapMessageToFileEntity(message: TdApi.Message): FileMetadataEntity? {
        val content = message.content
        var fileId = ""
        var tdlibFileId: Int? = null
        var fileName = ""
        var fileSize = 0L
        var mimeType = ""
        var fileType = FileType.OTHER
        var width: Int? = null
        var height: Int? = null
        var duration: Int? = null
        var localPath: String? = null
        var thumbnail: String? = null
        var thumbnailFileId: Int? = null

        when (content) {
            is TdApi.MessageDocument -> {
                val doc = content.document.document
                fileId = doc.remote.id
                tdlibFileId = doc.id  // Integer TDLib file ID
                fileName = content.document.fileName
                fileSize = doc.size.toLong()
                mimeType = content.document.mimeType
                localPath = doc.local.path.takeIf { doc.local.isDownloadingCompleted && it.isNotBlank() && File(it).exists() }

                // Detect actual file type from mimeType so images/videos get previews
                fileType = when {
                    mimeType.startsWith("image/") -> FileType.IMAGE
                    mimeType.startsWith("video/") -> FileType.VIDEO
                    mimeType.startsWith("audio/") -> FileType.AUDIO
                    else -> FileType.DOCUMENT
                }

                // Extract thumbnail if TDLib provides one (it often does for image/video documents)
                val thumbFile = content.document.thumbnail?.file
                if (thumbFile != null) {
                    thumbnailFileId = thumbFile.id
                    if (thumbFile.local.isDownloadingCompleted && thumbFile.local.path.isNotBlank() && File(thumbFile.local.path).exists()) {
                        thumbnail = thumbFile.local.path
                    }
                }

                // Extract width/height from document thumbnail for aspect ratio
                val docThumb = content.document.thumbnail
                if (docThumb != null) {
                    width = docThumb.width
                    height = docThumb.height
                }
            }
            is TdApi.MessagePhoto -> {
                val biggestPhoto = content.photo.sizes.maxByOrNull { it.width }
                if (biggestPhoto != null) {
                    fileId = biggestPhoto.photo.remote.id
                    tdlibFileId = biggestPhoto.photo.id  // Integer TDLib file ID
                    fileName = "Photo_${message.id}.jpg"
                    fileSize = biggestPhoto.photo.size.toLong()
                    mimeType = "image/jpeg"
                    fileType = FileType.IMAGE
                    width = biggestPhoto.width
                    height = biggestPhoto.height
                    localPath = biggestPhoto.photo.local.path.takeIf { biggestPhoto.photo.local.isDownloadingCompleted && it.isNotBlank() && File(it).exists() }
                }
                val smallestPhoto = content.photo.sizes.minByOrNull { it.width }
                if (smallestPhoto != null) {
                    thumbnailFileId = smallestPhoto.photo.id
                    if (smallestPhoto.photo.local.isDownloadingCompleted && smallestPhoto.photo.local.path.isNotBlank() && File(smallestPhoto.photo.local.path).exists()) {
                        thumbnail = smallestPhoto.photo.local.path
                    }
                }
            }
            is TdApi.MessageVideo -> {
                val vid = content.video.video
                fileId = vid.remote.id
                tdlibFileId = vid.id  // Integer TDLib file ID
                fileName = content.video.fileName
                fileSize = vid.size.toLong()
                mimeType = content.video.mimeType
                fileType = FileType.VIDEO
                width = content.video.width
                height = content.video.height
                duration = content.video.duration
                localPath = vid.local.path.takeIf { vid.local.isDownloadingCompleted && it.isNotBlank() && File(it).exists() }

                val videoThumb = content.video.thumbnail
                if (videoThumb != null) {
                    thumbnailFileId = videoThumb.file.id
                    if (videoThumb.file.local.isDownloadingCompleted && videoThumb.file.local.path.isNotBlank() && File(videoThumb.file.local.path).exists()) {
                        thumbnail = videoThumb.file.local.path
                    }
                }
            }
            is TdApi.MessageAudio -> {
                val aud = content.audio.audio
                fileId = aud.remote.id
                tdlibFileId = aud.id  // Integer TDLib file ID
                fileName = content.audio.fileName
                fileSize = aud.size.toLong()
                mimeType = content.audio.mimeType
                fileType = FileType.AUDIO
                duration = content.audio.duration
                localPath = aud.local.path.takeIf { aud.local.isDownloadingCompleted }
            }
            else -> return null // Not a supported file message
        }

        val forwardDate = (message.forwardInfo as? TdApi.MessageForwardInfo)?.date?.let { it.toLong() * 1000L }
        val dateTaken = forwardDate ?: com.telegramdrive.app.utils.FileDateUtils.extractDateTaken(fileName, localPath, thumbnail)

        return FileMetadataEntity(
             messageId = message.id,
             chatId = message.chatId,
             fileName = fileName,
             fileSize = fileSize,
             mimeType = mimeType,
             fileType = fileType,
             thumbnailPath = thumbnail,
             thumbnailFileId = thumbnailFileId,
             remoteFileId = fileId,
             tdlibFileId = tdlibFileId,
             localFilePath = localPath,
             uploadedAt = message.date.toLong() * 1000L,
             width = width,
             height = height,
             duration = duration,
             isFavorite = false,
             isInTrash = false,
             dateTaken = dateTaken
        )
    }
}
