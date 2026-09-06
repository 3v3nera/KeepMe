package com.telegramdrive.app.ui.utils

import kotlin.math.abs

object FolderEmojiUtils {
    val STICKER_EMOJIS = listOf(
        "🚀", "🔥", "⚡", "💎", "🎨", "👾", "🦊", "🔮",
        "🪐", "🎯", "🎧", "📸", "💡", "🌴", "🍕", "✨",
        "🕹️", "🌈", "🦄", "🥑", "🎸", "🌊", "🛸", "🍩",
        "🌸", "🏀", "🧊", "🧸", "🦁", "🏎️", "☕", "🍿",
        "🏆", "🧩", "📀", "🪄", "🍀", "🕶️", "👑", "🌮"
    )

    /**
     * Returns a stable deterministic random emoji for any folder ID if none was chosen
     */
    fun getDeterministicEmoji(folderId: Long): String {
        val index = abs(folderId.hashCode()) % STICKER_EMOJIS.size
        return STICKER_EMOJIS[index]
    }

    /**
     * Returns a random emoji from the sticker collection
     */
    fun getRandomEmoji(): String {
        return STICKER_EMOJIS.random()
    }
}
