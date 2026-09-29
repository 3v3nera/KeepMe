package com.telegramdrive.app.domain.model

import android.os.Parcelable
import com.telegramdrive.app.utils.FileDateUtils
import kotlinx.parcelize.Parcelize

@Parcelize
data class TelegramFile(
    val messageId: Long,       // Telegram message ID
    val chatId: Long,          // Which folder (channel) it's in
    val fileName: String,
    val fileSize: Long,        // Bytes
    val mimeType: String,
    val fileType: FileType,    // Image, Video, Document, Audio, Other
    val thumbnailPath: String?, // Local path to cached thumbnail
    val thumbnailFileId: Int?,  // TDLib file ID for the thumbnail
    val remoteFileId: String,  // TDLib remote file ID (string)
    val tdlibFileId: Int?,     // TDLib integer file ID for DownloadFile() calls
    val localFilePath: String?, // Local path if downloaded
    val uploadedAt: Long,      // Timestamp
    val width: Int?,           // For images/videos
    val height: Int?,          // For images/videos
    val duration: Int?,        // For videos/audio (seconds)
    val isFavorite: Boolean,
    val isInTrash: Boolean,
    val trashedAt: Long? = null,
    val dateTaken: Long? = null
) : Parcelable {
    val effectiveDate: Long
        get() = dateTaken ?: FileDateUtils.extractDateTaken(fileName, localFilePath, thumbnailPath) ?: uploadedAt
}

@Parcelize
enum class FileType : Parcelable { IMAGE, VIDEO, DOCUMENT, AUDIO, OTHER }

/** Unique across chats (message ids alone repeat between folders). */
val TelegramFile.selectionKey: Long
    get() = (chatId * -7046029254386353131L) xor messageId

