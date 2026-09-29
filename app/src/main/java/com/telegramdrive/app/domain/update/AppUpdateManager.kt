package com.telegramdrive.app.domain.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.telegramdrive.app.BuildConfig
import com.telegramdrive.app.data.datastore.SettingsDataStore
import com.telegramdrive.app.engine.TelegramManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import java.io.File
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val changelog: String,
    val fileSize: Long,
    val fileId: Int,
    val messageId: Long,
    val fileName: String,
    val date: Long
)

sealed interface UpdateDownloadState {
    object Idle : UpdateDownloadState
    data class Downloading(
        val downloadedBytes: Long,
        val totalBytes: Long,
        val progress: Float
    ) : UpdateDownloadState
    data class ReadyToInstall(
        val apkFile: File,
        val info: AppUpdateInfo
    ) : UpdateDownloadState
    data class Error(val message: String) : UpdateDownloadState
}

@Singleton
class AppUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val telegramManager: TelegramManager,
    private val settingsDataStore: SettingsDataStore
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var downloadJob: Job? = null

    private val _availableUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    val availableUpdate: StateFlow<AppUpdateInfo?> = _availableUpdate.asStateFlow()

    private val _downloadState = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)
    val downloadState: StateFlow<UpdateDownloadState> = _downloadState.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    companion object {
        private const val TAG = "AppUpdateManager"
    }

    /**
     * Option 2: Automatically joins the user to the update broadcast channel (@siddTdrive).
     * Runs in the background and is idempotent.
     */
    fun autoSubscribeToChannel() {
        scope.launch {
            try {
                val username = settingsDataStore.getUpdateChannelUsername()
                val chat = telegramManager.sendSuspend(TdApi.SearchPublicChat(username))
                telegramManager.sendSuspend(TdApi.JoinChat(chat.id))
                settingsDataStore.setChannelSubscribed(true)
                Log.d(TAG, "Successfully subscribed user to update channel: @$username (chatId=${chat.id})")
            } catch (e: Exception) {
                // If user is already a member, or network issues, fail gracefully
                Log.w(TAG, "Auto-subscribe notice: ${e.message}")
            }
        }
    }

    /**
     * Checks the Telegram update channel for the latest release APK.
     * Compares remote versionCode against BuildConfig.VERSION_CODE.
     */
    /**
     * Checks the Telegram update channel for the latest release APK.
     * Compares remote versionCode against BuildConfig.VERSION_CODE.
     */
    suspend fun checkForUpdates(force: Boolean = false, maxRetries: Int = 3): AppUpdateInfo? {
        _isChecking.value = true
        try {
            var attempts = 0
            var lastError: Exception? = null

            while (attempts < maxRetries) {
                attempts++
                try {
                    val username = settingsDataStore.getUpdateChannelUsername()
                    Log.d(TAG, "Searching for public chat: @$username (attempt $attempts/$maxRetries)")
                    val chat = telegramManager.sendSuspend(TdApi.SearchPublicChat(username))

                    val candidateMessages = mutableListOf<TdApi.Message>()

                    // 1. Fetch latest 30 messages from the channel
                    try {
                        val history = telegramManager.sendSuspend(
                            TdApi.GetChatHistory(chat.id, 0, 0, 30, false)
                        )
                        candidateMessages.addAll(history.messages)
                    } catch (e: Exception) {
                        Log.w(TAG, "GetChatHistory error (attempt $attempts): ${e.message}")
                    }

                    // 2. Also search for document filter if no APK found yet
                    if (candidateMessages.none { isApkMessage(it) }) {
                        try {
                            val searchReq = TdApi.SearchChatMessages().apply {
                                this.chatId = chat.id
                                this.query = ""
                                this.fromMessageId = 0
                                this.offset = 0
                                this.limit = 20
                                this.filter = TdApi.SearchMessagesFilterDocument()
                            }
                            val foundDocs = telegramManager.sendSuspend(searchReq) as TdApi.FoundChatMessages
                            candidateMessages.addAll(foundDocs.messages)
                        } catch (e: Exception) {
                            Log.w(TAG, "SearchChatMessages error (attempt $attempts): ${e.message}")
                        }
                    }

                    var foundUpdate: AppUpdateInfo? = null

                    for (msg in candidateMessages) {
                        val content = msg.content
                        if (content is TdApi.MessageDocument) {
                            val doc = content.document
                            val fileName = doc.fileName ?: ""
                            val mimeType = doc.mimeType ?: ""

                            val isApk = fileName.endsWith(".apk", ignoreCase = true) ||
                                    mimeType.equals("application/vnd.android.package-archive", ignoreCase = true)

                            if (isApk) {
                                val captionText = content.caption?.text ?: ""
                                val messageTime = msg.date.toLong() * 1000L
                                val updateInfo = parseUpdateInfo(
                                    fileName = fileName,
                                    caption = captionText,
                                    fileId = doc.document.id,
                                    fileSize = if (doc.document.size > 0) doc.document.size else doc.document.expectedSize,
                                    messageId = msg.id,
                                    date = messageTime
                                )

                                if (updateInfo != null) {
                                    val isNewer = isNewerVersion(updateInfo.versionCode, updateInfo.versionName, messageTime)
                                    val isDismissed = !force && updateInfo.versionCode <= settingsDataStore.getDismissedUpdateVersionCode()

                                    Log.d(TAG, "Found APK candidate: ${updateInfo.fileName} v${updateInfo.versionName} (b${updateInfo.versionCode}), isNewer=$isNewer, isDismissed=$isDismissed")

                                    if (isNewer && !isDismissed) {
                                        foundUpdate = updateInfo
                                        break // First match is the newest
                                    }
                                }
                            }
                        }
                    }

                    _availableUpdate.value = foundUpdate
                    return foundUpdate
                } catch (e: Exception) {
                    lastError = e
                    Log.w(TAG, "Attempt $attempts/$maxRetries to check for updates failed: ${e.message}")
                    if (attempts < maxRetries) {
                        kotlinx.coroutines.delay(1500L * attempts)
                    }
                }
            }

            Log.e(TAG, "All $maxRetries attempts to check updates failed", lastError)
            return null
        } finally {
            _isChecking.value = false
        }
    }

    private fun isApkMessage(msg: TdApi.Message): Boolean {
        if (msg.content !is TdApi.MessageDocument) return false
        val doc = (msg.content as TdApi.MessageDocument).document
        val fileName = doc.fileName ?: ""
        val mime = doc.mimeType ?: ""
        return fileName.endsWith(".apk", ignoreCase = true) || mime.equals("application/vnd.android.package-archive", ignoreCase = true)
    }

    /**
     * Extracts version details from the Telegram post's caption or filename.
     * Flexibly handles KeepMe, TelegramDrive, or any APK uploaded to the update channel.
     */
    private fun parseUpdateInfo(
        fileName: String,
        caption: String,
        fileId: Int,
        fileSize: Long,
        messageId: Long,
        date: Long
    ): AppUpdateInfo? {
        var parsedVersionCode = 0
        var parsedVersionName = ""
        var changelog = ""

        // 1. Check caption for versionCode / build / b:
        val codePattern = Pattern.compile("(?i)(?:versionCode|build(?:Number)?|code|build|b)[:=\\s]*(\\d+)")
        val codeMatcher = codePattern.matcher(caption)
        if (codeMatcher.find()) {
            parsedVersionCode = codeMatcher.group(1)?.toIntOrNull() ?: 0
        }

        // 2. Check caption for versionName: matches "v1.0.2", "version: 1.0.2", "version 1.0.2", "1.0.2", etc.
        val namePattern = Pattern.compile("(?i)(?:versionName|version|ver)?[:=\\s]*v?([0-9]+(?:\\.[0-9]+)+(?:-[a-zA-Z0-9._-]+)?)")
        val nameMatcher = namePattern.matcher(caption)
        if (nameMatcher.find()) {
            parsedVersionName = nameMatcher.group(1) ?: ""
        }

        // 3. Fallback extraction from filename: e.g. KeepMe-v1.0.2-b3-debug.apk, TelegramDrive-v1.0.2-b3-debug.apk
        if (parsedVersionCode == 0) {
            val fileCodePattern = Pattern.compile("(?i)(?:-b|_b|build|code|v[0-9.]+-)(\\d+)")
            val fileCodeMatcher = fileCodePattern.matcher(fileName)
            if (fileCodeMatcher.find()) {
                parsedVersionCode = fileCodeMatcher.group(1)?.toIntOrNull() ?: 0
            }
        }

        if (parsedVersionName.isBlank()) {
            val fileNamePattern = Pattern.compile("(?i)(?:v|ver|version-?)?([0-9]+(?:\\.[0-9]+)+(?:-[a-zA-Z0-9._-]+)?)")
            val fileNameMatcher = fileNamePattern.matcher(fileName)
            if (fileNameMatcher.find()) {
                parsedVersionName = fileNameMatcher.group(1) ?: ""
            }
        }

        // 4. If versionCode is still 0, auto-derive from versionName (e.g., 1.0.3 -> 10003)
        if (parsedVersionCode == 0 && parsedVersionName.isNotBlank()) {
            val parts = parsedVersionName.split('.', '-').mapNotNull { it.toIntOrNull() }
            if (parts.isNotEmpty()) {
                parsedVersionCode = parts.fold(0) { acc, num -> acc * 100 + num }
            }
        }

        // 5. Extract changelog from caption:
        val changelogPattern = Pattern.compile("(?i)(?:changelog|what's new|notes|changes)[:\\s\\n]+([\\s\\S]+)", Pattern.MULTILINE)
        val changelogMatcher = changelogPattern.matcher(caption)
        changelog = if (changelogMatcher.find()) {
            changelogMatcher.group(1)?.trim() ?: ""
        } else if (caption.isNotBlank()) {
            caption.trim()
        } else {
            "New performance optimizations, bug fixes, and feature enhancements."
        }

        // 6. Robust fallback for any APK uploaded to @siddTdrive without explicit version numbers
        if (parsedVersionCode == 0 && parsedVersionName.isBlank()) {
            val dateStr = java.text.SimpleDateFormat("dd MMM", java.util.Locale.US).format(java.util.Date(date))
            parsedVersionName = "Latest ($dateStr)"
            parsedVersionCode = (date / 10000L).toInt().coerceAtLeast(BuildConfig.VERSION_CODE + 1)
        } else if (parsedVersionName.isBlank()) {
            parsedVersionName = "1.0.$parsedVersionCode"
        }

        return AppUpdateInfo(
            versionCode = parsedVersionCode,
            versionName = parsedVersionName,
            changelog = changelog,
            fileSize = fileSize,
            fileId = fileId,
            messageId = messageId,
            fileName = fileName,
            date = date
        )
    }

    private fun isNewerVersion(remoteCode: Int, remoteName: String, msgDate: Long): Boolean {
        if (remoteCode > BuildConfig.VERSION_CODE) {
            return true
        }
        if (compareSemanticVersion(remoteName, BuildConfig.VERSION_NAME) > 0) {
            return true
        }
        // If versions are similar or unversioned, check if APK was published after app was built/installed
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            val appInstallTime = maxOf(packageInfo.lastUpdateTime, packageInfo.firstInstallTime)
            if (msgDate > appInstallTime && remoteCode >= BuildConfig.VERSION_CODE) {
                Log.d(TAG, "Remote APK date ($msgDate) is newer than app install time ($appInstallTime).")
                return true
            }
        } catch (_: Exception) {}

        return false
    }

    private fun compareSemanticVersion(v1: String, v2: String): Int {
        val parts1 = v1.split(".", "-").mapNotNull { it.toIntOrNull() }
        val parts2 = v2.split(".", "-").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1.compareTo(p2)
        }
        return 0
    }

    private var wakeLock: android.os.PowerManager.WakeLock? = null

    /**
     * Initiates a live download of the update APK file via TDLib.
     */
    fun startDownload(info: AppUpdateInfo) {
        downloadJob?.cancel()
        _downloadState.value = UpdateDownloadState.Downloading(0L, info.fileSize, 0f)

        // Acquire WakeLock so download doesn't suspend when screen sleeps
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            wakeLock = powerManager?.newWakeLock(
                android.os.PowerManager.PARTIAL_WAKE_LOCK,
                "KeepMe:AppUpdateDownloadWakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire(30 * 60 * 1000L) // 30 min max safety timeout
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire update WakeLock: ${e.message}")
        }

        downloadJob = scope.launch {
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val cleanVersion = info.versionName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val destApk = File(updatesDir, "KeepMe-v${cleanVersion}.apk")

            try {
                // 1. Check if complete APK is already cached locally
                if (destApk.exists() && destApk.length() > 0 && (info.fileSize <= 0 || destApk.length() >= info.fileSize - 1024)) {
                    Log.d(TAG, "Cached update APK already exists: ${destApk.absolutePath}")
                    _downloadState.value = UpdateDownloadState.ReadyToInstall(destApk, info)
                    installApk(context, destApk)
                    return@launch
                }

                // 2. Check TDLib's current file state directly
                try {
                    val initialFile = telegramManager.sendSuspend(TdApi.GetFile(info.fileId)) as? TdApi.File
                    if (initialFile != null) {
                        if (initialFile.local.isDownloadingCompleted && initialFile.local.path.isNotBlank()) {
                            val source = File(initialFile.local.path)
                            if (source.exists()) {
                                source.copyTo(destApk, overwrite = true)
                                _downloadState.value = UpdateDownloadState.ReadyToInstall(destApk, info)
                                installApk(context, destApk)
                                return@launch
                            }
                        } else if (initialFile.local.downloadedSize > 0) {
                            val total = if (initialFile.expectedSize > 0) initialFile.expectedSize else info.fileSize
                            val progress = if (total > 0) (initialFile.local.downloadedSize.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
                            _downloadState.value = UpdateDownloadState.Downloading(initialFile.local.downloadedSize, total, progress)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "GetFile pre-check notice: ${e.message}")
                }

                // 3. Request download with priority 32 (highest)
                telegramManager.downloadFileHighPriority(info.fileId)

                // 4. Polling fallback job (queries GetFile every 1000ms in case SharedFlow events drop)
                val pollingJob = launch {
                    while (true) {
                        kotlinx.coroutines.delay(1000L)
                        try {
                            val currentFile = telegramManager.sendSuspend(TdApi.GetFile(info.fileId)) as? TdApi.File
                            if (currentFile != null) {
                                val downloaded = currentFile.local.downloadedSize
                                val total = if (currentFile.expectedSize > 0) currentFile.expectedSize else info.fileSize
                                val progress = if (total > 0) (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
                                _downloadState.value = UpdateDownloadState.Downloading(downloaded, total, progress)

                                if (currentFile.local.isDownloadingCompleted && currentFile.local.path.isNotBlank()) {
                                    val source = File(currentFile.local.path)
                                    if (source.exists()) {
                                        source.copyTo(destApk, overwrite = true)
                                        _downloadState.value = UpdateDownloadState.ReadyToInstall(destApk, info)
                                        installApk(context, destApk)
                                        downloadJob?.cancel()
                                        break
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }

                // 5. SharedFlow event listener
                telegramManager.fileUpdates.collect { file ->
                    if (file.id == info.fileId) {
                        val downloaded = file.local.downloadedSize
                        val total = if (file.expectedSize > 0) file.expectedSize else info.fileSize
                        val progress = if (total > 0) (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f

                        _downloadState.value = UpdateDownloadState.Downloading(downloaded, total, progress)

                        if (file.local.isDownloadingCompleted && file.local.path.isNotBlank()) {
                            val source = File(file.local.path)
                            if (source.exists()) {
                                pollingJob.cancel()
                                source.copyTo(destApk, overwrite = true)
                                _downloadState.value = UpdateDownloadState.ReadyToInstall(destApk, info)
                                installApk(context, destApk)
                                this@launch.cancel()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed downloading update: ${e.message}", e)
                _downloadState.value = UpdateDownloadState.Error("Download failed: ${e.localizedMessage}")
            } finally {
                try {
                    wakeLock?.release()
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Cancels an ongoing APK download.
     */
    fun cancelDownload(fileId: Int) {
        downloadJob?.cancel()
        downloadJob = null
        try {
            wakeLock?.release()
        } catch (_: Exception) {}
        _downloadState.value = UpdateDownloadState.Idle
        scope.launch {
            try {
                telegramManager.sendSuspend(TdApi.CancelDownloadFile(fileId, false))
            } catch (e: Exception) {
                Log.w(TAG, "Cancel download: ${e.message}")
            }
        }
    }

    fun dismissUpdate(versionCode: Int) {
        settingsDataStore.setDismissedUpdateVersionCode(versionCode)
        _availableUpdate.value = null
    }

    fun resetDownloadState() {
        _downloadState.value = UpdateDownloadState.Idle
    }

    /**
     * Prompts the Android OS package installer to install the downloaded APK.
     */
    fun installApk(context: Context, apkFile: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(settingsIntent)
                    return
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer: ${e.message}", e)
            _downloadState.value = UpdateDownloadState.Error("Installer error: ${e.localizedMessage}")
        }
    }
}
