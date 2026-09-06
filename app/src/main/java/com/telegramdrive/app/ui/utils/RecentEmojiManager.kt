package com.telegramdrive.app.ui.utils

import android.content.Context
import androidx.compose.runtime.mutableStateListOf

object RecentEmojiManager {
    private const val PREFS_NAME = "tdrive_recent_emojis"
    private const val KEY_RECENT = "recent_emojis_list"
    private const val MAX_RECENT = 10

    private val DEFAULT_RECENT = listOf("🚀", "🔥", "⚡", "💎", "🎨")

    private val _recentEmojis = mutableStateListOf<String>()
    private var isInitialized = false

    fun getRecentEmojis(context: Context): List<String> {
        if (!isInitialized) {
            init(context)
        }
        return _recentEmojis
    }

    private fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_RECENT, null)
        _recentEmojis.clear()
        if (!saved.isNullOrBlank()) {
            val items = saved.split(",").map { it.trim() }.filter { it.isNotBlank() }
            _recentEmojis.addAll(items)
        } else {
            _recentEmojis.addAll(DEFAULT_RECENT)
        }
        isInitialized = true
    }

    fun addRecentEmoji(context: Context, emoji: String) {
        val trimmed = emoji.trim()
        if (trimmed.isBlank()) return
        if (!isInitialized) {
            init(context)
        }
        _recentEmojis.remove(trimmed)
        _recentEmojis.add(0, trimmed)
        while (_recentEmojis.size > MAX_RECENT) {
            _recentEmojis.removeAt(_recentEmojis.size - 1)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_RECENT, _recentEmojis.joinToString(",")).apply()
    }
}
