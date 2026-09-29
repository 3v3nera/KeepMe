package com.telegramdrive.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.telegramdrive.app.domain.model.FileType
import com.telegramdrive.app.domain.model.TelegramFile

@Entity(tableName = "files", primaryKeys = ["messageId", "chatId"])
data class FileMetadataEntity(
    val messageId: Long,
    val chatId: Long,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val fileType: FileType,
    val thumbnailPath: String?,
    val thumbnailFileId: Int?,
    val remoteFileId: String,
    val localFilePath: String?,
    val uploadedAt: Long,
    val width: Int?,
    val height: Int?,
    val duration: Int?,
    val isFavorite: Boolean,
    val isInTrash: Boolean,
    val trashedAt: Long? = null,
    val tdlibFileId: Int? = null,  // Integer TDLib file ID for DownloadFile() calls
    val dateTaken: Long? = null
)

fun FileMetadataEntity.toTelegramFile(): TelegramFile {
    return TelegramFile(
        messageId = messageId,
        chatId = chatId,
        fileName = fileName,
        fileSize = fileSize,
        mimeType = mimeType,
        fileType = fileType,
        thumbnailPath = thumbnailPath,
        thumbnailFileId = thumbnailFileId,
        remoteFileId = remoteFileId,
        tdlibFileId = tdlibFileId,
        localFilePath = localFilePath,
        uploadedAt = uploadedAt,
        width = width,
        height = height,
        duration = duration,
        isFavorite = isFavorite,
        isInTrash = isInTrash,
        trashedAt = trashedAt,
        dateTaken = dateTaken
    )
}
