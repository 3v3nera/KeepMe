package com.telegramdrive.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val chatId: Long,
    val name: String,
    val isSavedMessages: Boolean,
    val fileCount: Int,
    val lastSyncedAt: Long,
    val emoji: String? = null
)
