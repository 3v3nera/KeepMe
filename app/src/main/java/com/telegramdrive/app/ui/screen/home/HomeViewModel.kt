package com.telegramdrive.app.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telegramdrive.app.data.local.entity.FileMetadataEntity
import com.telegramdrive.app.data.local.entity.toTelegramFile
import com.telegramdrive.app.data.repository.FileRepository
import com.telegramdrive.app.domain.model.FileType
import com.telegramdrive.app.domain.model.TelegramFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.telegramdrive.app.data.repository.FolderRepository
import com.telegramdrive.app.engine.TelegramManager
import org.drinkless.tdlib.TdApi

import com.telegramdrive.app.data.datastore.SettingsDataStore
import com.telegramdrive.app.domain.update.AppUpdateInfo
import com.telegramdrive.app.domain.update.AppUpdateManager
import com.telegramdrive.app.domain.update.UpdateDownloadState
import com.telegramdrive.app.worker.CameraBackupWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import java.io.File

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val fileRepository: FileRepository,
    private val folderRepository: FolderRepository,
    private val telegramManager: TelegramManager,
    private val settingsDataStore: SettingsDataStore,
    private val appUpdateManager: AppUpdateManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    init {
        viewModelScope.launch {
            folderRepository.syncFolders()
            fileRepository.syncAllFolders()
        }
        viewModelScope.launch {
            // Wait until user is authenticated & online
            telegramManager.currentUser.collect { user ->
                if (user != null) {
                    // Auto-subscribe to @siddTdrive and check for updates
                    appUpdateManager.autoSubscribeToChannel()
                    appUpdateManager.checkForUpdates(force = false, maxRetries = 3)
                }
            }
        }
    }

    val currentUser: StateFlow<TdApi.User?> = telegramManager.currentUser
    val userProfilePhotoPath: StateFlow<String?> = telegramManager.userProfilePhotoPath

    val availableUpdate: StateFlow<AppUpdateInfo?> = appUpdateManager.availableUpdate
    val updateDownloadState: StateFlow<UpdateDownloadState> = appUpdateManager.downloadState

    fun startUpdateDownload(info: AppUpdateInfo) {
        appUpdateManager.startDownload(info)
    }

    fun cancelUpdateDownload(fileId: Int) {
        appUpdateManager.cancelDownload(fileId)
    }

    fun dismissUpdate(versionCode: Int) {
        appUpdateManager.dismissUpdate(versionCode)
    }

    fun installApk(apkFile: File) {
        appUpdateManager.installApk(context, apkFile)
    }

    fun resetDownloadState() {
        appUpdateManager.resetDownloadState()
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                folderRepository.syncFolders()
                fileRepository.syncAllFolders()
                appUpdateManager.checkForUpdates(force = true)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    val recentFiles: StateFlow<List<TelegramFile>> = fileRepository.getRecentFiles(20)
        .map { list -> list.map { it.toTelegramFile() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteFiles: StateFlow<List<TelegramFile>> = fileRepository.getFavoriteFiles()
        .map { list -> list.map { it.toTelegramFile() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val storageStats: StateFlow<List<com.telegramdrive.app.domain.model.StorageStat>> = fileRepository.getStorageStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun requestThumbnail(fileId: Int?) {
        if (fileId != null) {
            fileRepository.requestThumbnailDownload(fileId)
        }
    }

    val isBackupEnabled: StateFlow<Boolean> = settingsDataStore.isCameraBackupEnabledFlow

    val lastBackupTime: StateFlow<Long> = settingsDataStore.lastBackupCompletedTimeFlow

    fun toggleBackup(enabled: Boolean) {
        settingsDataStore.setCameraBackupEnabled(enabled)
        if (enabled || settingsDataStore.isCustomFolderBackupEnabled()) {
            CameraBackupWorker.schedule(
                context = context,
                wifiOnly = settingsDataStore.isBackupWifiOnly(),
                chargingOnly = settingsDataStore.isBackupChargingOnly(),
                targetHour = settingsDataStore.getBackupHour(),
                targetMinute = settingsDataStore.getBackupMinute()
            )
            if (enabled) {
                CameraBackupWorker.runOnce(
                    context = context,
                    wifiOnly = settingsDataStore.isBackupWifiOnly(),
                    chargingOnly = settingsDataStore.isBackupChargingOnly()
                )
            }
        } else {
            CameraBackupWorker.cancel(context)
        }
    }
}
