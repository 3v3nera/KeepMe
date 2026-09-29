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
import kotlinx.coroutines.flow.flowOn
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

    /** [userInitiated] = pull-to-refresh; automatic refreshes respect the sync cool-down. */
    fun refresh(userInitiated: Boolean = true) {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                folderRepository.syncFolders()
                fileRepository.syncAllFolders(force = userInitiated)
                if (userInitiated) appUpdateManager.checkForUpdates(force = true)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /** One cover card per month, newest first, used by Home's photo month cards. */
    val monthCovers: StateFlow<List<MonthCover>> = fileRepository.getRecentFiles(1000)
        .map { list ->
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            list.map { it.toTelegramFile() }
                .filter { it.effectiveDate > 0 }
                .groupBy { file ->
                    val cal = java.util.Calendar.getInstance().apply { timeInMillis = file.effectiveDate }
                    cal.get(java.util.Calendar.YEAR) * 12 + cal.get(java.util.Calendar.MONTH)
                }
                .toSortedMap(compareByDescending { it })
                .map { (key, files) ->
                    val year = key / 12
                    val month = key % 12
                    val pattern = if (year == currentYear) "MMMM" else "MMMM yyyy"
                    val name = java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault())
                        .format(java.util.Date(files.first().effectiveDate))
                        .replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase(java.util.Locale.getDefault()) else c.toString() }
                    val cover = files.firstOrNull { it.fileType == FileType.IMAGE && it.thumbnailPath != null }
                        ?: files.firstOrNull { it.thumbnailPath != null }
                    MonthCover(
                        filter = com.telegramdrive.app.domain.model.DateFilter.Month(year, month, name),
                        count = files.size,
                        coverPath = cover?.thumbnailPath,
                        coverThumbnailFileId = files.firstOrNull { it.thumbnailPath == null }?.thumbnailFileId
                    )
                }
        }
        .flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Runs the camera backup right now (Home quick action). */
    fun backupNow() {
        CameraBackupWorker.runOnce(
            context = context,
            wifiOnly = settingsDataStore.isBackupWifiOnly(),
            chargingOnly = settingsDataStore.isBackupChargingOnly()
        )
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

data class MonthCover(
    val filter: com.telegramdrive.app.domain.model.DateFilter.Month,
    val count: Int,
    val coverPath: String?,
    /** TDLib thumbnail id to download when no cover has been fetched yet */
    val coverThumbnailFileId: Int?
)

/** Hand-off so tapping a Home month card opens Recent Uploads already filtered to that month. */
object HomeMonthLink {
    @Volatile var pending: com.telegramdrive.app.domain.model.DateFilter.Month? = null
}