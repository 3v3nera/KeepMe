package com.telegramdrive.app.ui.screen.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telegramdrive.app.data.repository.FolderRepository
import com.telegramdrive.app.data.repository.FileRepository
import com.telegramdrive.app.domain.model.TelegramFolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FoldersViewModel @Inject constructor(
    private val folderRepository: FolderRepository,
    private val fileRepository: FileRepository,
    private val uploadManager: com.telegramdrive.app.data.manager.UploadManager
) : ViewModel() {

    val folders: StateFlow<List<TelegramFolder>> = folderRepository.getAllFolders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _events = kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 4)
    /** One-shot user-facing messages (e.g. a failed folder action). */
    val events: kotlinx.coroutines.flow.SharedFlow<String> = _events

    private val _isRefreshing = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    init {
        syncFolders()
    }

    fun syncFolders(force: Boolean = false) {
        viewModelScope.launch {
            folderRepository.syncFolders()
            fileRepository.syncAllFolders(force)
        }
    }

    /** Pull-to-refresh: bypasses the sync cool-down and shows progress. */
    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                folderRepository.syncFolders()
                fileRepository.syncAllFolders(force = true)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun createFolder(name: String, emoji: String? = null) {
        viewModelScope.launch {
            runCatching { folderRepository.createFolder(name, emoji) }
                .onFailure { _events.tryEmit("Couldn't create the folder. Check your connection and try again.") }
        }
    }

    /** Returns the new chat id, or null (after emitting a message) when creation failed. */
    suspend fun createFolderAndGetId(name: String, emoji: String? = null): Long? {
        return runCatching { folderRepository.createFolder(name, emoji) }
            .onFailure { _events.tryEmit("Couldn't create the folder. Check your connection and try again.") }
            .getOrNull()
    }

    fun renameFolder(chatId: Long, newName: String, emoji: String? = null) {
        viewModelScope.launch {
            runCatching { folderRepository.renameFolder(chatId, newName, emoji) }
                .onFailure { _events.tryEmit("Couldn't rename the folder. Try again.") }
        }
    }

    fun deleteFolder(chatId: Long) {
        viewModelScope.launch {
            runCatching { folderRepository.deleteFolder(chatId) }
                .onFailure { _events.tryEmit("Couldn't delete the folder. Try again.") }
        }
    }

    fun uploadFiles(uris: List<android.net.Uri>, context: android.content.Context, targetChatId: Long? = null) {
        viewModelScope.launch {
            val savedMessages = folders.value.firstOrNull { it.isSavedMessages }
            val chatId = targetChatId ?: savedMessages?.id ?: folderRepository.getSavedMessagesChatId()
            if (chatId != 0L) {
                uploadManager.enqueueUploads(chatId, uris)
            }
        }
    }
}
