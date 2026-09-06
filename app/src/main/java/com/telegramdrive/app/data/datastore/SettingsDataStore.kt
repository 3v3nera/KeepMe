package com.telegramdrive.app.data.datastore

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsDataStore(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_settings",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _isCameraBackupEnabledFlow = MutableStateFlow(sharedPreferences.getBoolean("camera_backup_enabled", false))
    val isCameraBackupEnabledFlow: StateFlow<Boolean> = _isCameraBackupEnabledFlow.asStateFlow()

    private val _lastBackupCompletedTimeFlow = MutableStateFlow(sharedPreferences.getLong("last_backup_completed_time", 0L))
    val lastBackupCompletedTimeFlow: StateFlow<Long> = _lastBackupCompletedTimeFlow.asStateFlow()

    fun saveApiCredentials(apiId: Int, apiHash: String) {
        sharedPreferences.edit()
            .putInt("api_id", apiId)
            .putString("api_hash", apiHash)
            .apply()
    }

    fun getApiId(): Int {
        if (com.telegramdrive.app.config.TelegramConfig.isConfigured) {
            return com.telegramdrive.app.config.TelegramConfig.DEFAULT_API_ID
        }
        return sharedPreferences.getInt("api_id", 0)
    }

    fun getApiHash(): String? {
        if (com.telegramdrive.app.config.TelegramConfig.isConfigured) {
            return com.telegramdrive.app.config.TelegramConfig.DEFAULT_API_HASH
        }
        return sharedPreferences.getString("api_hash", null)
    }
    
    fun hasApiCredentials(): Boolean {
        return getApiId() != 0 && !getApiHash().isNullOrBlank()
    }

    fun clearApiCredentials() {
        sharedPreferences.edit()
            .remove("api_id")
            .remove("api_hash")
            .apply()
    }

    // Camera Backup Settings
    fun isCameraBackupEnabled(): Boolean {
        return sharedPreferences.getBoolean("camera_backup_enabled", false)
    }

    fun setCameraBackupEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("camera_backup_enabled", enabled).apply()
        _isCameraBackupEnabledFlow.value = enabled
    }

    fun isBackupWifiOnly(): Boolean {
        return sharedPreferences.getBoolean("backup_wifi_only", true)
    }

    fun setBackupWifiOnly(wifiOnly: Boolean) {
        sharedPreferences.edit().putBoolean("backup_wifi_only", wifiOnly).apply()
    }

    fun isBackupChargingOnly(): Boolean {
        return sharedPreferences.getBoolean("backup_charging_only", false)
    }

    fun setBackupChargingOnly(chargingOnly: Boolean) {
        sharedPreferences.edit().putBoolean("backup_charging_only", chargingOnly).apply()
    }

    fun getLastBackupTimestamp(): Long {
        return sharedPreferences.getLong("last_backup_timestamp", 0L)
    }

    fun setLastBackupTimestamp(timestamp: Long) {
        sharedPreferences.edit().putLong("last_backup_timestamp", timestamp).apply()
    }

    // Backup Scope ("FROM_TODAY" vs "ALL_TIME")
    fun getBackupScope(): String {
        return sharedPreferences.getString("backup_scope", "FROM_TODAY") ?: "FROM_TODAY"
    }

    fun setBackupScope(scope: String) {
        sharedPreferences.edit().putString("backup_scope", scope).apply()
    }

    // Custom Backup Time (Hour 0-23, Minute 0-59)
    fun getBackupHour(): Int {
        return sharedPreferences.getInt("backup_hour", 2) // Default 2:00 AM
    }

    fun getBackupMinute(): Int {
        return sharedPreferences.getInt("backup_minute", 0)
    }

    fun setBackupTime(hour: Int, minute: Int) {
        sharedPreferences.edit()
            .putInt("backup_hour", hour)
            .putInt("backup_minute", minute)
            .apply()
    }

    // Custom Folder Auto-Backup Settings (SAF)
    fun isCustomFolderBackupEnabled(): Boolean {
        return sharedPreferences.getBoolean("custom_folder_backup_enabled", false)
    }

    fun setCustomFolderBackupEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("custom_folder_backup_enabled", enabled).apply()
    }

    fun getCustomFolderUri(): String? {
        return sharedPreferences.getString("custom_folder_uri", null)
    }

    fun setCustomFolderUri(uri: String?) {
        sharedPreferences.edit().putString("custom_folder_uri", uri).apply()
    }

    fun getCustomFolderName(): String? {
        return sharedPreferences.getString("custom_folder_name", null)
    }

    fun setCustomFolderName(name: String?) {
        sharedPreferences.edit().putString("custom_folder_name", name).apply()
    }

    fun getLastCustomFolderBackupTimestamp(): Long {
        return sharedPreferences.getLong("last_custom_folder_backup_timestamp", 0L)
    }

    fun setLastCustomFolderBackupTimestamp(timestamp: Long) {
        sharedPreferences.edit().putLong("last_custom_folder_backup_timestamp", timestamp).apply()
    }

    fun getLastBackupCompletedTime(): Long {
        return sharedPreferences.getLong("last_backup_completed_time", 0L)
    }

    fun setLastBackupCompletedTime(time: Long) {
        sharedPreferences.edit().putLong("last_backup_completed_time", time).apply()
        _lastBackupCompletedTimeFlow.value = time
    }

    // OTA Update Settings
    fun getUpdateChannelUsername(): String {
        return sharedPreferences.getString("update_channel_username", "siddTdrive") ?: "siddTdrive"
    }

    fun setUpdateChannelUsername(username: String) {
        val clean = username.removePrefix("@").removePrefix("https://t.me/").removePrefix("t.me/").trim()
        sharedPreferences.edit().putString("update_channel_username", clean).apply()
    }

    fun getDismissedUpdateVersionCode(): Int {
        return sharedPreferences.getInt("dismissed_update_version_code", 0)
    }

    fun setDismissedUpdateVersionCode(versionCode: Int) {
        sharedPreferences.edit().putInt("dismissed_update_version_code", versionCode).apply()
    }

    fun isChannelSubscribed(): Boolean {
        return sharedPreferences.getBoolean("channel_auto_subscribed", false)
    }

    fun setChannelSubscribed(subscribed: Boolean) {
        sharedPreferences.edit().putBoolean("channel_auto_subscribed", subscribed).apply()
    }
}
