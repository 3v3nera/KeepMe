package com.telegramdrive.app.config

/**
 * Global Telegram API Configuration.
 *
 * If [DEFAULT_API_ID] and [DEFAULT_API_HASH] are provided,
 * the app skips the manual API credentials setup screen completely
 * and takes all users straight to the Phone Number login screen.
 */
object TelegramConfig {
    const val DEFAULT_API_ID: Int = 36472189
    const val DEFAULT_API_HASH: String = "94934d2f758b83ea92a6551e712c5a75"

    val isConfigured: Boolean
        get() = DEFAULT_API_ID > 0 && DEFAULT_API_HASH.isNotBlank()
}
