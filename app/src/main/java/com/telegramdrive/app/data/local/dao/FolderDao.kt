package com.telegramdrive.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.telegramdrive.app.data.local.entity.FolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("""
        SELECT f.chatId, f.name, f.isSavedMessages, 
               (SELECT COUNT(*) FROM files WHERE chatId = f.chatId AND isInTrash = 0) as fileCount, 
               f.lastSyncedAt,
               f.emoji
        FROM folders f 
        WHERE f.isSavedMessages = 1 OR f.name LIKE '%[TG]%' OR f.name LIKE '%[TD]%'
        ORDER BY f.isSavedMessages DESC, f.name ASC
    """)
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE chatId = :chatId LIMIT 1")
    suspend fun getFolderById(chatId: Long): FolderEntity?

    @Upsert
    suspend fun upsertFolder(folder: FolderEntity)

    @Delete
    suspend fun deleteFolder(folder: FolderEntity)

    @Query("SELECT chatId FROM folders WHERE isSavedMessages = 0")
    suspend fun getCustomFolderIds(): List<Long>

    @Query("DELETE FROM folders WHERE chatId = :chatId")
    suspend fun deleteFolderById(chatId: Long)
}
