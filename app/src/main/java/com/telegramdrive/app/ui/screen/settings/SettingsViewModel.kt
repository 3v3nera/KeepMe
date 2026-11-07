package com.telegramdrive.app.ui.screen.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.telegramdrive.app.BuildConfig
import com.telegramdrive.app.data.datastore.SettingsDataStore
import com.telegramdrive.app.domain.update.AppUpdateInfo
import com.telegramdrive.app.domain.update.AppUpdateManager
import com.telegramdrive.app.domain.update.UpdateDownloadState
import com.telegramdrive.app.worker.CameraBackupWorker
import com.telegramdrive.app.engine.TelegramManager
import com.telegramdrive.app.data.repository.FileRepository
import com.telegramdrive.app.data.repository.FolderRepository
import org.drinkless.tdlib.TdApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore,
    private val workManager: WorkManager,
    private val appUpdateManager: AppUpdateManager,
    private val telegramManager: TelegramManager,
    private val fileRepository: FileRepository,
    private val folderRepository: FolderRepository
) : ViewModel() {

    val currentUser: StateFlow<TdApi.User?> = telegramManager.currentUser
    val userProfilePhotoPath: StateFlow<String?> = telegramManager.userProfilePhotoPath

    val storageStats: StateFlow<List<com.telegramdrive.app.domain.model.StorageStat>> = fileRepository.getStorageStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalFilesCount: StateFlow<Int> = fileRepository.getRecentFiles(5000)
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val foldersCount: StateFlow<Int> = folderRepository.getAllFolders()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun logout() {
        telegramManager.logout()
    }

    fun reloadUserProfile() {
        telegramManager.reloadCurrentUser()
    }

    val isBackupEnabled: StateFlow<Boolean> = settingsDataStore.isCameraBackupEnabledFlow

    private val _isWifiOnly = MutableStateFlow(settingsDataStore.isBackupWifiOnly())
    val isWifiOnly: StateFlow<Boolean> = _isWifiOnly.asStateFlow()

    private val _isChargingOnly = MutableStateFlow(settingsDataStore.isBackupChargingOnly())
    val isChargingOnly: StateFlow<Boolean> = _isChargingOnly.asStateFlow()

    private val _isBackupByYear = MutableStateFlow(settingsDataStore.isBackupByYear())
    val isBackupByYear: StateFlow<Boolean> = _isBackupByYear.asStateFlow()

    fun toggleBackupByYear(enabled: Boolean) {
        settingsDataStore.setBackupByYear(enabled)
        _isBackupByYear.value = enabled
    }

    private val _backupScope = MutableStateFlow(settingsDataStore.getBackupScope())
    val backupScope: StateFlow<String> = _backupScope.asStateFlow()

    private val _backupHour = MutableStateFlow(settingsDataStore.getBackupHour())
    val backupHour: StateFlow<Int> = _backupHour.asStateFlow()

    private val _backupMinute = MutableStateFlow(settingsDataStore.getBackupMinute())
    val backupMinute: StateFlow<Int> = _backupMinute.asStateFlow()

    private val _isCustomFolderBackupEnabled = MutableStateFlow(settingsDataStore.isCustomFolderBackupEnabled())
    val isCustomFolderBackupEnabled: StateFlow<Boolean> = _isCustomFolderBackupEnabled.asStateFlow()

    private val _customFolderUri = MutableStateFlow(settingsDataStore.getCustomFolderUri())
    val customFolderUri: StateFlow<String?> = _customFolderUri.asStateFlow()

    private val _customFolderName = MutableStateFlow(settingsDataStore.getCustomFolderName())
    val customFolderName: StateFlow<String?> = _customFolderName.asStateFlow()

    val lastBackupTime: StateFlow<Long> = settingsDataStore.lastBackupCompletedTimeFlow

    private val _isBackingUpNow = MutableStateFlow(false)
    val isBackingUpNow: StateFlow<Boolean> = _isBackingUpNow.asStateFlow()

    init {
        // Observe immediate backup status
        viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow(CameraBackupWorker.ONE_TIME_WORK_NAME).collect { workInfos ->
                val running = workInfos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
                _isBackingUpNow.value = running
            }
        }
    }

    fun toggleBackup(enabled: Boolean) {
        settingsDataStore.setCameraBackupEnabled(enabled)
        updateWorkManager()
        if (enabled) {
            triggerBackupNow()
        }
    }

    fun toggleWifiOnly(enabled: Boolean) {
        settingsDataStore.setBackupWifiOnly(enabled)
        _isWifiOnly.value = enabled
        updateWorkManager()
    }

    fun toggleMobileData(allow: Boolean) {
        toggleWifiOnly(!allow)
    }

    fun setBackupScope(scope: String) {
        settingsDataStore.setBackupScope(scope)
        _backupScope.value = scope
        if (scope == "FROM_TODAY") {
            val midnight = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
            settingsDataStore.setLastBackupTimestamp(midnight)
            settingsDataStore.setLastCustomFolderBackupTimestamp(midnight)
        } else {
            settingsDataStore.setLastBackupTimestamp(0L)
            settingsDataStore.setLastCustomFolderBackupTimestamp(0L)
        }
    }

    fun toggleChargingOnly(enabled: Boolean) {
        settingsDataStore.setBackupChargingOnly(enabled)
        _isChargingOnly.value = enabled
        updateWorkManager()
    }

    fun setBackupTime(hour: Int, minute: Int) {
        settingsDataStore.setBackupTime(hour, minute)
        _backupHour.value = hour
        _backupMinute.value = minute
        updateWorkManager()
    }

    fun toggleCustomFolderBackup(enabled: Boolean) {
        settingsDataStore.setCustomFolderBackupEnabled(enabled)
        _isCustomFolderBackupEnabled.value = enabled
        updateWorkManager()
    }

    fun setCustomFolder(uri: String, name: String) {
        settingsDataStore.setCustomFolderUri(uri)
        settingsDataStore.setCustomFolderName(name)
        _customFolderUri.value = uri
        _customFolderName.value = name
        updateWorkManager()
    }

    fun triggerBackupNow() {
        _isBackingUpNow.value = true
        CameraBackupWorker.runOnce(
            context = context,
            wifiOnly = _isWifiOnly.value,
            chargingOnly = _isChargingOnly.value
        )
    }

    fun refreshLastBackupTime() {
        // Automatically reflected by settingsDataStore.lastBackupCompletedTimeFlow
    }

    private fun updateWorkManager() {
        if (isBackupEnabled.value || _isCustomFolderBackupEnabled.value) {
            CameraBackupWorker.schedule(
                context = context,
                wifiOnly = _isWifiOnly.value,
                chargingOnly = _isChargingOnly.value,
                targetHour = _backupHour.value,
                targetMinute = _backupMinute.value
            )
        } else {
            CameraBackupWorker.cancel(context)
        }
    }

    // OTA Update Features
    private val _updateChannelUsername = MutableStateFlow(settingsDataStore.getUpdateChannelUsername())
    val updateChannelUsername: StateFlow<String> = _updateChannelUsername.asStateFlow()

    val isCheckingUpdate = appUpdateManager.isChecking
    val availableUpdate = appUpdateManager.availableUpdate
    val downloadState = appUpdateManager.downloadState

    private val _manualCheckMessage = MutableStateFlow<String?>(null)
    val manualCheckMessage: StateFlow<String?> = _manualCheckMessage.asStateFlow()

    fun checkForUpdates() {
        viewModelScope.launch {
            _manualCheckMessage.value = null
            val update = appUpdateManager.checkForUpdates(force = true)
            if (update == null) {
                _manualCheckMessage.value = "You're on the latest version (v${BuildConfig.VERSION_NAME})"
            }
        }
    }

    fun startUpdateDownload(info: AppUpdateInfo) {
        appUpdateManager.startDownload(info)
    }

    fun cancelUpdateDownload(fileId: Int) {
        appUpdateManager.cancelDownload(fileId)
    }

    fun installApk(apkFile: File) {
        appUpdateManager.installApk(context, apkFile)
    }

    fun dismissUpdate(versionCode: Int) {
        appUpdateManager.dismissUpdate(versionCode)
    }

    fun resetDownloadState() {
        appUpdateManager.resetDownloadState()
    }

    fun clearManualCheckMessage() {
        _manualCheckMessage.value = null
    }

    fun setUpdateChannel(channel: String) {
        settingsDataStore.setUpdateChannelUsername(channel)
        _updateChannelUsername.value = settingsDataStore.getUpdateChannelUsername()
        viewModelScope.launch {
            appUpdateManager.autoSubscribeToChannel()
        }
    }
}
