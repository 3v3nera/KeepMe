package com.telegramdrive.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "backup_prefs")

@Singleton
class BackupPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore
    
    companion object {
        private val BACKUP_ENABLED = booleanPreferencesKey("backup_enabled")
        private val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        private val LAST_BACKUP_TIME = longPreferencesKey("last_backup_time")
    }

    val isBackupEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[BACKUP_ENABLED] ?: false
    }

    val isWifiOnly: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[WIFI_ONLY] ?: true
    }

    val lastBackupTime: Flow<Long> = dataStore.data.map { prefs ->
        prefs[LAST_BACKUP_TIME] ?: 0L
    }

    suspend fun setBackupEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[BACKUP_ENABLED] = enabled }
    }

    suspend fun setWifiOnly(wifiOnly: Boolean) {
        dataStore.edit { prefs -> prefs[WIFI_ONLY] = wifiOnly }
    }

    suspend fun setLastBackupTime(timestamp: Long) {
        dataStore.edit { prefs -> prefs[LAST_BACKUP_TIME] = timestamp }
    }
}
