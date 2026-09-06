package com.telegramdrive.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.telegramdrive.app.data.local.entity.FileMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FileMetadataDao {
    @Query("SELECT * FROM files WHERE chatId = :chatId AND isInTrash = 0 ORDER BY COALESCE(dateTaken, uploadedAt) DESC")
    fun getFilesInFolder(chatId: Long): Flow<List<FileMetadataEntity>>

    @Query("SELECT * FROM files WHERE isInTrash = 0 ORDER BY uploadedAt DESC LIMIT :limit")
    fun getRecentFiles(limit: Int = 50): Flow<List<FileMetadataEntity>>

    @Query("SELECT * FROM files WHERE isFavorite = 1 AND isInTrash = 0")
    fun getFavoriteFiles(): Flow<List<FileMetadataEntity>>

    @Query("SELECT * FROM files WHERE isInTrash = 1")
    fun getTrashFiles(): Flow<List<FileMetadataEntity>>

    @Query("SELECT * FROM files WHERE fileName LIKE '%' || :query || '%' AND isInTrash = 0")
    fun searchFiles(query: String): Flow<List<FileMetadataEntity>>

    @Query("SELECT fileType, SUM(fileSize) as totalSize, COUNT(*) as fileCount FROM files WHERE isInTrash = 0 GROUP BY fileType")
    fun getStorageStats(): Flow<List<com.telegramdrive.app.domain.model.StorageStat>>

    @Upsert
    suspend fun upsertFile(file: FileMetadataEntity)

    @Query("UPDATE files SET thumbnailPath = :path WHERE thumbnailFileId = :fileId")
    suspend fun updateThumbnailPath(fileId: Int, path: String)

    @Query("UPDATE files SET localFilePath = :path WHERE tdlibFileId = :fileId")
    suspend fun updateLocalFilePath(fileId: Int, path: String)

    @Upsert
    suspend fun upsertFiles(files: List<FileMetadataEntity>)

    @Query("UPDATE files SET isFavorite = :isFav WHERE chatId = :chatId AND messageId = :messageId")
    suspend fun setFavorite(chatId: Long, messageId: Long, isFav: Boolean)

    @Query("UPDATE files SET isInTrash = 1, trashedAt = :timestamp WHERE chatId = :chatId AND messageId = :messageId")
    suspend fun moveToTrash(chatId: Long, messageId: Long, timestamp: Long)


    @Query("UPDATE files SET isInTrash = 0, trashedAt = null WHERE chatId = :chatId AND messageId = :messageId")
    suspend fun restoreFromTrash(chatId: Long, messageId: Long)

    @Query("DELETE FROM files WHERE isInTrash = 1 AND trashedAt < :cutoff")
    suspend fun purgeOldTrash(cutoff: Long)

    @Query("SELECT * FROM files WHERE chatId = :chatId")
    suspend fun getExistingFilesInChat(chatId: Long): List<FileMetadataEntity>

    @Query("DELETE FROM files WHERE chatId = :chatId AND messageId = :messageId")
    suspend fun deleteFilePermanently(chatId: Long, messageId: Long)

    @Query("DELETE FROM files WHERE chatId = :chatId")
    suspend fun deleteFilesInChat(chatId: Long)

    @Query("SELECT * FROM files WHERE messageId = :messageId LIMIT 1")
    fun getFileByMessageId(messageId: Long): Flow<FileMetadataEntity?>

    @Query("UPDATE files SET localFilePath = NULL WHERE messageId = :messageId")
    suspend fun clearLocalFilePath(messageId: Long)

    @Query("DELETE FROM files WHERE isInTrash = 1")
    suspend fun emptyTrash()

    @Query("UPDATE files SET isInTrash = 0, trashedAt = null")
    suspend fun resetAllTrash()
}

