package com.telegramdrive.app.ui.screen.preview

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telegramdrive.app.data.repository.FileRepository
import com.telegramdrive.app.domain.model.TelegramFile
import com.telegramdrive.app.engine.TelegramManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject

/**
 * One instance per file page (keyed by messageId via hiltViewModel(key = ...)).
 * Each instance tracks only its own file's download state, ensuring no cross-page bleed.
 */
@HiltViewModel
class MediaPreviewViewModel @Inject constructor(
    private val fileRepository: FileRepository,
    private val telegramManager: TelegramManager
) : ViewModel() {

    private val _localPath = MutableStateFlow<String?>(null)
    val localPath: StateFlow<String?> = _localPath.asStateFlow()

    private val _thumbnailPath = MutableStateFlow<String?>(null)
    val thumbnailPath: StateFlow<String?> = _thumbnailPath.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Emits true while a save-to-gallery operation is running. */
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    /** Emits a one-shot message (null = idle). */
    private val _saveResult = MutableStateFlow<String?>(null)
    val saveResult: StateFlow<String?> = _saveResult.asStateFlow()

    // Tracks the active download collection jobs
    private var downloadJob: Job? = null
    private var thumbnailJob: Job? = null
    private var currentFileId: Int? = null

    /**
     * Call once when the preview page becomes active. Resolves thumbnail and
     * full-resolution file, validating that files actually exist on disk.
     */
    fun prepareFile(file: TelegramFile) {
        val fileId = file.tdlibFileId
        val thumbId = file.thumbnailFileId

        // 1. Resolve thumbnail path immediately if it exists on disk
        val initialThumb = file.thumbnailPath?.takeIf { File(it).exists() }
            ?: thumbId?.let { telegramManager.downloadedFiles.value[it] }?.takeIf { File(it).exists() }
        if (initialThumb != null) {
            _thumbnailPath.value = initialThumb
        }

        // 2. Request thumbnail download if not yet on disk
        if (_thumbnailPath.value == null && thumbId != null) {
            fileRepository.requestThumbnailDownload(thumbId)
            thumbnailJob?.cancel()
            thumbnailJob = viewModelScope.launch {
                telegramManager.fileUpdates.collect { tdFile ->
                    if (tdFile.id == thumbId && tdFile.local.isDownloadingCompleted && tdFile.local.path.isNotBlank()) {
                        if (File(tdFile.local.path).exists()) {
                            _thumbnailPath.value = tdFile.local.path
                        }
                    }
                }
            }
        }

        // 3. Check if full file already exists on disk
        val existingLocal = file.localFilePath?.takeIf { File(it).exists() }
            ?: fileId?.let { telegramManager.downloadedFiles.value[it] }?.takeIf { File(it).exists() }

        if (existingLocal != null) {
            _localPath.value = existingLocal
            _isLoading.value = false
            return
        }

        // If localFilePath was in DB but file was deleted on disk, clean up DB
        if (!file.localFilePath.isNullOrBlank() && !File(file.localFilePath).exists()) {
            viewModelScope.launch {
                fileRepository.clearInvalidLocalFilePath(file.messageId)
            }
        }

        if (fileId == null) return   // Can't download without integer file ID
        if (fileId == currentFileId && _isLoading.value) return  // Already preparing this file

        // Cancel any previous in-flight download collector
        downloadJob?.cancel()
        currentFileId = fileId
        _localPath.value = null
        _downloadProgress.value = 0f
        _isLoading.value = true

        downloadJob = viewModelScope.launch {
            // First check TDLib file cache
            try {
                val tdFile = telegramManager.sendSuspend(TdApi.GetFile(fileId))
                if (tdFile.local.isDownloadingCompleted && tdFile.local.path.isNotBlank()) {
                    if (File(tdFile.local.path).exists()) {
                        _localPath.value = tdFile.local.path
                        _isLoading.value = false
                        return@launch
                    } else {
                        // TDLib recorded it as downloaded, but local file was removed. Invalidate cache.
                        try {
                            telegramManager.sendSuspend(TdApi.DeleteFile(fileId))
                        } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {}

            fileRepository.downloadFullFile(fileId)

            telegramManager.fileUpdates.collect { tdFile ->
                if (tdFile.id == fileId) {
                    val downloaded = tdFile.local.downloadedSize.toFloat()
                    val total = tdFile.size.toFloat()
                    if (total > 0f) {
                        _downloadProgress.value = (downloaded / total).coerceIn(0f, 1f)
                    }
                    if (tdFile.local.isDownloadingCompleted && tdFile.local.path.isNotBlank()) {
                        if (File(tdFile.local.path).exists()) {
                            _localPath.value = tdFile.local.path
                            _isLoading.value = false
                        }
                    }
                }
            }
        }
    }

    /**
     * Copies the already-downloaded file to the device's Pictures/Downloads gallery.
     * Shows a result toast via [saveResult].
     */
    fun saveToGallery(context: Context, file: TelegramFile) {
        val sourcePath = _localPath.value ?: return
        if (_isSaving.value) return

        _isSaving.value = true
        viewModelScope.launch {
            try {
                val sourceFile = File(sourcePath)
                if (!sourceFile.exists()) {
                    _saveResult.value = "File not found locally"
                    return@launch
                }

                val mimeType = file.mimeType.ifBlank {
                    MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(sourceFile.extension) ?: "application/octet-stream"
                }
                val isImage = mimeType.startsWith("image/")
                val isVideo = mimeType.startsWith("video/")

                val targetFileName = file.fileName.ifBlank { sourceFile.name }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Scoped storage — use MediaStore
                    val collection = when {
                        isImage -> MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                        isVideo -> MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                        else    -> MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    }
                    val relativeDir = when {
                        isImage -> Environment.DIRECTORY_PICTURES + "/TelegramDrive"
                        isVideo -> Environment.DIRECTORY_MOVIES   + "/TelegramDrive"
                        else    -> Environment.DIRECTORY_DOWNLOADS + "/TelegramDrive"
                    }
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, targetFileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, relativeDir)
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                    val uri = context.contentResolver.insert(collection, values)
                        ?: throw Exception("MediaStore insert failed")
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        FileInputStream(sourceFile).use { it.copyTo(out) }
                    }
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                } else {
                    // Legacy: copy to external storage directly
                    val destDir = when {
                        isImage -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                        isVideo -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                        else    -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    }.also { File(it, "TelegramDrive").mkdirs() }
                    val dest = File(destDir, "TelegramDrive/$targetFileName")
                    sourceFile.copyTo(dest, overwrite = true)
                    // Notify MediaScanner
                    android.media.MediaScannerConnection.scanFile(
                        context, arrayOf(dest.absolutePath), arrayOf(mimeType), null
                    )
                }
                _saveResult.value = "Saved to your phone's gallery! 🎉"
            } catch (e: Exception) {
                _saveResult.value = "Couldn't save file: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    /** Call from the UI after showing the result snackbar/toast. */
    fun clearSaveResult() {
        _saveResult.value = null
    }

    override fun onCleared() {
        super.onCleared()
        downloadJob?.cancel()
        thumbnailJob?.cancel()
    }
}
