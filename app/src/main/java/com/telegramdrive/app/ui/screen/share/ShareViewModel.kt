package com.telegramdrive.app.ui.screen.share

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telegramdrive.app.data.manager.UploadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShareViewModel @Inject constructor(
    private val uploadManager: UploadManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    fun uploadFiles(chatId: Long, uris: List<Uri>, onComplete: () -> Unit) {
        viewModelScope.launch {
            uploadManager.enqueueUploads(chatId, uris)
            onComplete()
        }
    }
}
