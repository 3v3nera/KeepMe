package com.telegramdrive.app.ui.screen.preview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telegramdrive.app.data.local.entity.toTelegramFile
import com.telegramdrive.app.data.repository.FileRepository
import com.telegramdrive.app.domain.model.TelegramFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MediaViewerViewModel @Inject constructor(
    private val fileRepository: FileRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Nav args — passed as route parameters or updated via setTarget
    private val navFolderId: Long = savedStateHandle.get<String>("folderId")?.toLongOrNull() ?: 0L
    private val navInitialFileId: Long = savedStateHandle.get<String>("fileId")?.toLongOrNull() ?: 0L

    private val targetFolderId = MutableStateFlow(navFolderId)
    private val targetInitialFileId = MutableStateFlow(navInitialFileId)

    fun setTarget(folderId: Long, fileId: Long) {
        targetFolderId.value = folderId
        targetInitialFileId.value = fileId
    }

    // Query Room for all files in this folder, sorted by date descending (matching grid sort).
    // If folder files are empty or don't include target file, fallback to single file query by messageId.
    val files: StateFlow<List<TelegramFile>> = combine(
        targetFolderId.flatMapLatest { fId ->
            if (fId != 0L) {
                fileRepository.getFilesInFolder(fId)
                    .map { list -> list.map { it.toTelegramFile() }.sortedByDescending { it.uploadedAt } }
            } else {
                flowOf(emptyList())
            }
        },
        targetInitialFileId.flatMapLatest { mId ->
            if (mId != 0L) {
                fileRepository.getFileByMessageId(mId).map { it?.toTelegramFile() }
            } else {
                flowOf(null)
            }
        }
    ) { folderFiles, singleFile ->
        when {
            folderFiles.isNotEmpty() -> folderFiles
            singleFile != null -> listOf(singleFile)
            else -> emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Computed: index of the file the user tapped
    val initialIndex: Int
        get() = files.value.indexOfFirst { it.messageId == targetInitialFileId.value }.coerceAtLeast(0)
}
