package com.telegramdrive.app.ui.screen.folders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telegramdrive.app.data.local.entity.FileMetadataEntity
import com.telegramdrive.app.data.local.entity.toTelegramFile
import com.telegramdrive.app.data.repository.FileRepository
import com.telegramdrive.app.domain.model.TelegramFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

import android.content.Context
import android.net.Uri
import com.telegramdrive.app.data.repository.FolderRepository
import com.telegramdrive.app.domain.model.DateFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

    /** The folder's sticker, shown in the header (and flown there from the tapped card). */
    val folderEmoji: StateFlow<String?> = folderRepository.getAllFolders()
        .map { list -> list.firstOrNull { it.id == chatId }?.emoji }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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
                fileRepository.syncFolder(chatId, force = true)
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

    private val _selectedDateFilter = MutableStateFlow<DateFilter?>(null)
    val selectedDateFilter: StateFlow<DateFilter?> = _selectedDateFilter.asStateFlow()

    fun setDateFilter(filter: DateFilter?) {
        _selectedDateFilter.value = filter
        // Selected items can fall out of view when filtering; never act on files you cannot see
        clearSelection()
    }

    /** Files of this folder, converted once and off the main thread; shared by every derived flow. */
    private val allFiles: StateFlow<List<TelegramFile>?> = fileRepository.getFilesInFolder(chatId)
        .map { list -> list.map { it.toTelegramFile() } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val availableMonths: StateFlow<List<DateFilter.Month>> = allFiles
        .map { list ->
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val cal = Calendar.getInstance()
            val seen = HashSet<Int>()
            val months = ArrayList<DateFilter.Month>()
            for (file in list.orEmpty()) {
                if (file.effectiveDate <= 0) continue
                cal.timeInMillis = file.effectiveDate
                val y = cal.get(Calendar.YEAR)
                val m = cal.get(Calendar.MONTH)
                if (!seen.add(y * 12 + m)) continue
                val rawName = if (y == currentYear) {
                    SimpleDateFormat("MMMM", Locale.getDefault()).format(Date(file.effectiveDate))
                } else {
                    SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(file.effectiveDate))
                }
                val name = rawName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                months.add(DateFilter.Month(year = y, month = m, displayName = name))
            }
            months.sortedWith(compareByDescending<DateFilter.Month> { it.year }.thenByDescending { it.month })
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isFilesLoaded = MutableStateFlow(false)

    val files: StateFlow<List<TelegramFile>> = combine(
        allFiles,
        _sortOption,
        _sortDirection,
        _selectedDateFilter
    ) { source, sort, direction, dateFilter ->
        var telegramFiles = source ?: return@combine null

        // ── Apply Date / Month Filter ──
        if (dateFilter != null) {
            val cal = Calendar.getInstance()
            telegramFiles = when (dateFilter) {
                is DateFilter.Month -> telegramFiles.filter { file ->
                    cal.timeInMillis = file.effectiveDate
                    cal.get(Calendar.YEAR) == dateFilter.year && cal.get(Calendar.MONTH) == dateFilter.month
                }
                is DateFilter.SpecificDate -> telegramFiles.filter { file ->
                    file.effectiveDate in dateFilter.startOfDayMillis..dateFilter.endOfDayMillis
                }
            }
        }

        val descending = direction == SortDirection.DESCENDING
        when (sort) {
            SortOption.DATE -> if (descending) {
                telegramFiles.sortedByDescending { it.effectiveDate }
            } else {
                telegramFiles.sortedBy { it.effectiveDate }
            }
            // Name / Type read naturally A→Z by default; the arrow flips them
            SortOption.NAME -> if (descending) {
                telegramFiles.sortedBy { it.fileName.lowercase() }
            } else {
                telegramFiles.sortedByDescending { it.fileName.lowercase() }
            }
            SortOption.SIZE -> if (descending) {
                telegramFiles.sortedByDescending { it.fileSize }
            } else {
                telegramFiles.sortedBy { it.fileSize }
            }
            SortOption.TYPE -> if (descending) {
                telegramFiles.sortedBy { it.fileType.name }
            } else {
                telegramFiles.sortedByDescending { it.fileType.name }
            }
        }
    }
    .filterNotNull()
    .flowOn(Dispatchers.Default)
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

    fun createSubFolder(name: String, emoji: String? = null) {
        viewModelScope.launch {
            runCatching { folderRepository.createFolder(name, emoji) }.onFailure {
                android.widget.Toast.makeText(context, "Couldn't create the folder. Try again.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}
