package com.telegramdrive.app.domain.model

data class TelegramFolder(
    val id: Long,           // Telegram chat ID
    val name: String,       // Channel title or "Saved Messages"
    val isSavedMessages: Boolean,
    val fileCount: Int,     // Cached count
    val thumbnailUrl: String?, // Latest file thumbnail
    val createdAt: Long,
    val emoji: String? = null
) {
    val displayName: String
        get() = if (isSavedMessages) name else name.replace("[TG]", "").replace("[TD]", "").trim()
}
