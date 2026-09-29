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
import com.telegramdrive.app.domain.model.selectionKey
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

    private val _selectedDateFilter = MutableStateFlow<com.telegramdrive.app.domain.model.DateFilter?>(
        HomeMonthLink.pending.also { HomeMonthLink.pending = null }
    )
    val selectedDateFilter: StateFlow<com.telegramdrive.app.domain.model.DateFilter?> = _selectedDateFilter.asStateFlow()

    fun setDateFilter(filter: com.telegramdrive.app.domain.model.DateFilter?) {
        _selectedDateFilter.value = filter
        _selectedFileIds.value = emptySet()
    }

    private val _selectedFileIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedFileIds: StateFlow<Set<Long>> = _selectedFileIds.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val rawRecentFiles = fileRepository.getRecentFiles(500)
        .map { list -> list.map { it.toTelegramFile() } }
        .flowOn(kotlinx.coroutines.Dispatchers.Default)
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), replay = 1)

    val availableMonths: StateFlow<List<com.telegramdrive.app.domain.model.DateFilter.Month>> = rawRecentFiles
        .map { list ->
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            list.filter { it.effectiveDate > 0 }
                .map { file ->
                    val cal = java.util.Calendar.getInstance().apply { timeInMillis = file.effectiveDate }
                    val y = cal.get(java.util.Calendar.YEAR)
                    val m = cal.get(java.util.Calendar.MONTH)
                    val rawName = if (y == currentYear) {
                        java.text.SimpleDateFormat("MMMM", java.util.Locale.getDefault()).format(java.util.Date(file.effectiveDate))
                    } else {
                        java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(file.effectiveDate))
                    }
                    val name = rawName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                    com.telegramdrive.app.domain.model.DateFilter.Month(year = y, month = m, displayName = name)
                }
                .distinctBy { "${it.year}_${it.month}" }
                .sortedWith(compareByDescending<com.telegramdrive.app.domain.model.DateFilter.Month> { it.year }.thenByDescending { it.month })
        }
        .flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val files: StateFlow<List<TelegramFile>> = combine(
        rawRecentFiles,
        _selectedFilter,
        _selectedDateFilter,
        _sortOption,
        _sortDirection
    ) { allFiles, filter, dateFilter, sort, dir ->
        var filtered = if (filter == null) {
            allFiles
        } else {
            allFiles.filter { it.fileType == filter }
        }

        if (dateFilter != null) {
            filtered = when (dateFilter) {
                is com.telegramdrive.app.domain.model.DateFilter.Month -> {
                    filtered.filter { file ->
                        val cal = java.util.Calendar.getInstance().apply { timeInMillis = file.effectiveDate }
                        cal.get(java.util.Calendar.YEAR) == dateFilter.year && cal.get(java.util.Calendar.MONTH) == dateFilter.month
                    }
                }
                is com.telegramdrive.app.domain.model.DateFilter.SpecificDate -> {
                    filtered.filter { file ->
                        file.effectiveDate in dateFilter.startOfDayMillis..dateFilter.endOfDayMillis
                    }
                }
            }
        }

        val sorted = when (sort) {
            SortOption.DATE -> filtered.sortedBy { it.effectiveDate }
            SortOption.NAME -> filtered.sortedBy { it.fileName.lowercase() }
            SortOption.SIZE -> filtered.sortedBy { it.fileSize }
            SortOption.TYPE -> filtered.sortedBy { it.fileType.name }
        }

        if (dir == SortDirection.DESCENDING) sorted.reversed() else sorted
    }.flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(type: FileType?) {
        _selectedFilter.value = type
        _selectedFileIds.value = emptySet()
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
            val all = files.value.filter { it.selectionKey in toDelete }
            all.forEach { file ->
                fileRepository.deleteFile(file.chatId, file.messageId)
            }
            clearSelection()
        }
    }

    fun downloadSelected() {
        val selected = files.value.filter { it.selectionKey in _selectedFileIds.value }
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
                fileRepository.syncAllFolders(force = true)
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
