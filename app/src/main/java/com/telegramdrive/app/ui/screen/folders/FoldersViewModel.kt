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

    init {
        syncFolders()
    }

    fun syncFolders() {
        viewModelScope.launch {
            folderRepository.syncFolders()
            fileRepository.syncAllFolders()
        }
    }

    fun createFolder(name: String, emoji: String? = null) {
        viewModelScope.launch {
            folderRepository.createFolder(name, emoji)
        }
    }

    suspend fun createFolderAndGetId(name: String, emoji: String? = null): Long {
        return folderRepository.createFolder(name, emoji)
    }

    fun renameFolder(chatId: Long, newName: String, emoji: String? = null) {
        viewModelScope.launch {
            folderRepository.renameFolder(chatId, newName, emoji)
        }
    }

    fun deleteFolder(chatId: Long) {
        viewModelScope.launch {
            folderRepository.deleteFolder(chatId)
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
