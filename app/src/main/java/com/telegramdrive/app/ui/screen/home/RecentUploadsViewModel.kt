package com.telegramdrive.app.ui.screen.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telegramdrive.app.data.local.entity.toTelegramFile
import com.telegramdrive.app.data.repository.FileRepository
import com.telegramdrive.app.data.repository.FolderRepository
import com.telegramdrive.app.data.telegram.FileManager
import com.telegramdrive.app.domain.model.FileType
import com.telegramdrive.app.domain.model.TelegramFile
import com.telegramdrive.app.ui.screen.folders.SortDirection
import com.telegramdrive.app.ui.screen.folders.SortOption
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecentUploadsViewModel @Inject constructor(
    private val fileRepository: FileRepository,
    private val folderRepository: FolderRepository,
    private val fileManager: FileManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _sortOption = MutableStateFlow(SortOption.DATE)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _sortDirection = MutableStateFlow(SortDirection.DESCENDING)
    val sortDirection: StateFlow<SortDirection> = _sortDirection.asStateFlow()

    private val _selectedFilter = MutableStateFlow<FileType?>(null) // null = ALL
    val selectedFilter: StateFlow<FileType?> = _selectedFilter.asStateFlow()

    private val _selectedFileIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedFileIds: StateFlow<Set<Long>> = _selectedFileIds.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val rawRecentFiles = fileRepository.getRecentFiles(500)
        .map { list -> list.map { it.toTelegramFile() } }

    val files: StateFlow<List<TelegramFile>> = combine(
        rawRecentFiles,
        _selectedFilter,
        _sortOption,
        _sortDirection
    ) { allFiles, filter, sort, dir ->
        val filtered = if (filter == null) {
            allFiles
        } else {
            allFiles.filter { it.fileType == filter }
        }

        val sorted = when (sort) {
            SortOption.DATE -> filtered.sortedBy { it.effectiveDate }
            SortOption.NAME -> filtered.sortedBy { it.fileName.lowercase() }
            SortOption.SIZE -> filtered.sortedBy { it.fileSize }
            SortOption.TYPE -> filtered.sortedBy { it.fileType.name }
        }

        if (dir == SortDirection.DESCENDING) sorted.reversed() else sorted
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(type: FileType?) {
        _selectedFilter.value = type
    }

    fun setSortOption(option: SortOption) {
        if (_sortOption.value == option) {
            toggleSortDirection()
        } else {
            _sortOption.value = option
        }
    }

    fun toggleSortDirection() {
        _sortDirection.value = if (_sortDirection.value == SortDirection.ASCENDING) {
            SortDirection.DESCENDING
        } else {
            SortDirection.ASCENDING
        }
    }

    fun toggleSelection(messageId: Long) {
        _selectedFileIds.update { current ->
            if (current.contains(messageId)) current - messageId else current + messageId
        }
    }

    fun selectFileById(messageId: Long) {
        _selectedFileIds.update { it + messageId }
    }

    fun clearSelection() {
        _selectedFileIds.value = emptySet()
    }

    fun deleteSelected() {
        val toDelete = _selectedFileIds.value
        if (toDelete.isEmpty()) return
        viewModelScope.launch {
            val all = files.value.filter { it.messageId in toDelete }
            all.forEach { file ->
                fileRepository.deleteFile(file.chatId, file.messageId)
            }
            clearSelection()
        }
    }

    fun downloadSelected() {
        val selected = files.value.filter { it.messageId in _selectedFileIds.value }
        selected.forEach { file ->
            val id = file.tdlibFileId
            if (id != null) {
                fileRepository.downloadFullFile(id)
            }
        }
        clearSelection()
    }

    fun requestThumbnail(fileId: Int?) {
        if (fileId != null) {
            fileRepository.requestThumbnailDownload(fileId)
        }
    }

    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                fileRepository.syncAllFolders()
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
