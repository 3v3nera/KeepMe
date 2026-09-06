package com.telegramdrive.app.ui.screen.folders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telegramdrive.app.data.local.entity.FileMetadataEntity
import com.telegramdrive.app.data.local.entity.toTelegramFile
import com.telegramdrive.app.data.repository.FileRepository
import com.telegramdrive.app.domain.model.TelegramFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

import android.content.Context
import android.net.Uri
import com.telegramdrive.app.data.repository.FolderRepository
import dagger.hilt.android.qualifiers.ApplicationContext

enum class SortOption {
    DATE, NAME, SIZE, TYPE
}

enum class SortDirection {
    DESCENDING, ASCENDING
}

@HiltViewModel
class FolderContentViewModel @Inject constructor(
    private val fileRepository: FileRepository,
    private val folderRepository: FolderRepository,
    private val uploadManager: com.telegramdrive.app.data.manager.UploadManager,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val chatId: Long = savedStateHandle.get<String>("chatId")?.toLongOrNull() ?: 0L

    private val _isRefreshing = MutableStateFlow(true)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var initialLoadDone = false

    init {
        viewModelScope.launch {
            try {
                fileRepository.syncFolder(chatId)
            } finally {
                _isRefreshing.value = false
                initialLoadDone = true
            }
        }
    }

    fun refresh() {
        if (_isRefreshing.value && initialLoadDone) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                fileRepository.syncFolder(chatId)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private val _sortOption = MutableStateFlow(SortOption.DATE)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _sortDirection = MutableStateFlow(SortDirection.DESCENDING)
    val sortDirection: StateFlow<SortDirection> = _sortDirection.asStateFlow()

    fun toggleSortDirection() {
        _sortDirection.value = if (_sortDirection.value == SortDirection.DESCENDING) {
            SortDirection.ASCENDING
        } else {
            SortDirection.DESCENDING
        }
    }

    private val _isFilesLoaded = MutableStateFlow(false)

    val files: StateFlow<List<TelegramFile>> = combine(
        fileRepository.getFilesInFolder(chatId),
        _sortOption,
        _sortDirection
    ) { files, sort, direction ->
        val telegramFiles = files.map { it.toTelegramFile() }
        when (sort) {
            SortOption.DATE -> if (direction == SortDirection.DESCENDING) {
                telegramFiles.sortedByDescending { it.effectiveDate }
            } else {
                telegramFiles.sortedBy { it.effectiveDate }
            }
            SortOption.NAME -> telegramFiles.sortedBy { it.fileName.lowercase() }
            SortOption.SIZE -> if (direction == SortDirection.DESCENDING) {
                telegramFiles.sortedByDescending { it.fileSize }
            } else {
                telegramFiles.sortedBy { it.fileSize }
            }
            SortOption.TYPE -> telegramFiles.sortedBy { it.fileType.name }
        }
    }
    .onEach { _isFilesLoaded.value = true }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isLoading: StateFlow<Boolean> = combine(_isRefreshing, _isFilesLoaded) { refreshing, filesLoaded ->
        refreshing || !filesLoaded
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _selectedFileIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedFileIds: StateFlow<Set<Long>> = _selectedFileIds.asStateFlow()

    fun toggleSelection(messageId: Long) {
        val current = _selectedFileIds.value
        _selectedFileIds.value = if (current.contains(messageId)) {
            current - messageId
        } else {
            current + messageId
        }
    }

    fun clearSelection() {
        _selectedFileIds.value = emptySet()
    }

    fun selectFileById(messageId: Long) {
        _selectedFileIds.value = _selectedFileIds.value + messageId
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun toggleFavorite(file: TelegramFile) {
        viewModelScope.launch {
            fileRepository.setFavorite(file.chatId, file.messageId, !file.isFavorite)
        }
    }

    fun moveToTrash(file: TelegramFile) {
        viewModelScope.launch {
            fileRepository.deleteFile(file.chatId, file.messageId)
        }
    }

    fun deleteSelected() {
        viewModelScope.launch {
            _selectedFileIds.value.forEach { id ->
                fileRepository.deleteFile(chatId, id)
            }
            clearSelection()
        }
    }

    fun downloadSelected() {
        viewModelScope.launch {
            val selectedFiles = files.value.filter { it.messageId in _selectedFileIds.value }
            selectedFiles.forEach { file ->
                file.tdlibFileId?.let { tdId ->
                    fileRepository.downloadFullFile(tdId)
                }
            }
            val msg = if (selectedFiles.size == 1) "Downloading 1 file..." else "Downloading ${selectedFiles.size} files..."
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            clearSelection()
        }
    }

    fun requestThumbnail(fileId: Int?) {
        if (fileId != null) {
            fileRepository.requestThumbnailDownload(fileId)
        }
    }

    fun uploadFiles(uris: List<Uri>) {
        viewModelScope.launch {
            uploadManager.enqueueUploads(chatId, uris)
        }
    }

    fun createSubFolder(name: String) {
        viewModelScope.launch {
            folderRepository.createFolder(name)
        }
    }
}
