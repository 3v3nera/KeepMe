package com.telegramdrive.app.ui.screen.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telegramdrive.app.data.local.entity.FileMetadataEntity
import com.telegramdrive.app.data.local.entity.toTelegramFile
import com.telegramdrive.app.data.repository.FileRepository
import com.telegramdrive.app.domain.model.TelegramFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val fileRepository: FileRepository
) : ViewModel() {

    val trashFiles: StateFlow<List<TelegramFile>> = fileRepository.getTrashFiles()
        .map { list -> list.distinctBy { "${it.chatId}_${it.messageId}" }.map { it.toTelegramFile() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

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

    fun restoreFile(file: TelegramFile) {
        viewModelScope.launch {
            fileRepository.restoreFromTrash(file.chatId, file.messageId)
        }
    }

    fun deletePermanently(file: TelegramFile) {
        viewModelScope.launch {
            fileRepository.deletePermanently(file)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            fileRepository.emptyTrash()
        }
    }

    fun requestThumbnail(fileId: Int?) {
        if (fileId != null) {
            fileRepository.requestThumbnailDownload(fileId)
        }
    }
}
