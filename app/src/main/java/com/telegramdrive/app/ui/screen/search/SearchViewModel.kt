package com.telegramdrive.app.ui.screen.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telegramdrive.app.data.local.entity.FileMetadataEntity
import com.telegramdrive.app.data.local.entity.toTelegramFile
import com.telegramdrive.app.data.repository.FileRepository
import com.telegramdrive.app.domain.model.TelegramFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val fileRepository: FileRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                onQueryChange(_searchQuery.value)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    val searchResults: StateFlow<List<TelegramFile>> = _searchQuery
        .debounce(300L) // 300ms debounce as per requirements
        .onEach { _isSearching.value = true }
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                fileRepository.searchFiles(query).map { list -> list.map { it.toTelegramFile() } }
            }
        }
        .onEach { _isSearching.value = false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(query: String) {
        _searchQuery.value = query
    }
}
