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

    private val _type = MutableStateFlow(SearchType.ALL)
    val type: StateFlow<SearchType> = _type.asStateFlow()
    fun setType(type: SearchType) { _type.value = type }

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    /** Remember a query the user actually acted on (search key or opened a result). */
    fun commitSearch(query: String) {
        val q = query.trim()
        if (q.length < 2) return
        _recentSearches.value = (listOf(q) + _recentSearches.value.filterNot { it.equals(q, ignoreCase = true) }).take(6)
    }

    fun clearRecentSearches() { _recentSearches.value = emptyList() }

    private val rawResults: StateFlow<List<TelegramFile>> = _searchQuery
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

    val searchResults: StateFlow<List<TelegramFile>> = combine(rawResults, _type) { list, type ->
        when (type) {
            SearchType.ALL -> list
            SearchType.PHOTOS -> list.filter { it.fileType == com.telegramdrive.app.domain.model.FileType.IMAGE }
            SearchType.VIDEOS -> list.filter { it.fileType == com.telegramdrive.app.domain.model.FileType.VIDEO }
            SearchType.AUDIO -> list.filter { it.fileType == com.telegramdrive.app.domain.model.FileType.AUDIO }
            SearchType.DOCS -> list.filter {
                it.fileType == com.telegramdrive.app.domain.model.FileType.DOCUMENT ||
                    it.fileType == com.telegramdrive.app.domain.model.FileType.OTHER
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(query: String) {
        _searchQuery.value = query
        // Show the loading state immediately, not only after the debounce, so the UI never
        // flashes a false "no results" message while the user is still typing
        _isSearching.value = query.isNotBlank()
    }
}

/** Result filter chips shown under the search field. */
enum class SearchType(val label: String) {
    ALL("All"), PHOTOS("Photos"), VIDEOS("Videos"), DOCS("Docs"), AUDIO("Audio")
}