package com.waheed.universaldownloader.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waheed.universaldownloader.data.local.DownloadDao
import com.waheed.universaldownloader.data.local.DownloadEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ViewMode { GRID, LIST }
enum class FilterMode { ALL, AUDIO_ONLY, VIDEO_ONLY }
enum class SortMode(val queryValue: String, val label: String) {
    NEWEST("newest", "Newest first"),
    OLDEST("oldest", "Oldest first"),
    NAME_ASC("name_asc", "Name (A-Z)"),
    NAME_DESC("name_desc", "Name (Z-A)"),
    SIZE_ASC("size_asc", "Size (smallest)"),
    SIZE_DESC("size_desc", "Size (largest)")
}

data class LibraryUiState(
    val downloads: List<DownloadEntity> = emptyList(),
    val viewMode: ViewMode = ViewMode.GRID,
    val filterMode: FilterMode = FilterMode.ALL,
    val sortMode: SortMode = SortMode.NEWEST,
    val searchQuery: String = "",
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val totalCount: Int = 0,
    val totalStorageBytes: Long = 0L,
    val audioCount: Int = 0,
    val videoCount: Int = 0,
    val isLoading: Boolean = true
)

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val downloadDao: DownloadDao
) : ViewModel() {

    private val _viewMode = MutableStateFlow(ViewMode.GRID)
    private val _filterMode = MutableStateFlow(FilterMode.ALL)
    private val _sortMode = MutableStateFlow(SortMode.NEWEST)
    private val _searchQuery = MutableStateFlow("")
    private val _isSelectionMode = MutableStateFlow(false)
    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())

    private val debouncedSearch = _searchQuery
        .debounce(300)
        .distinctUntilChanged()

    private val filteredDownloads = combine(
        debouncedSearch, _filterMode, _sortMode
    ) { query, filter, sort ->
        Triple(query, filter, sort)
    }.flatMapLatest { (query, filter, sort) ->
        downloadDao.searchAndFilter(
            searchQuery = query,
            audioOnly = filter == FilterMode.AUDIO_ONLY,
            videoOnly = filter == FilterMode.VIDEO_ONLY,
            sortBy = sort.queryValue
        )
    }

    val uiState: StateFlow<LibraryUiState> = combine(
        filteredDownloads,
        _viewMode,
        _filterMode,
        _sortMode,
        _searchQuery
    ) { downloads, viewMode, filterMode, sortMode, searchQuery ->
        LibraryUiState(
            downloads = downloads,
            viewMode = viewMode,
            filterMode = filterMode,
            sortMode = sortMode,
            searchQuery = searchQuery,
            isLoading = false
        )
    }.combine(_isSelectionMode) { state, selectionMode ->
        state.copy(isSelectionMode = selectionMode)
    }.combine(_selectedIds) { state, selectedIds ->
        state.copy(selectedIds = selectedIds)
    }.combine(downloadDao.getTotalCount()) { state, total ->
        state.copy(totalCount = total)
    }.combine(downloadDao.getTotalStorageBytes()) { state, bytes ->
        state.copy(totalStorageBytes = bytes)
    }.combine(downloadDao.getAudioCount()) { state, audio ->
        state.copy(audioCount = audio)
    }.combine(downloadDao.getVideoCount()) { state, video ->
        state.copy(videoCount = video)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState()
    )

    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID
    }

    fun setFilterMode(mode: FilterMode) {
        _filterMode.value = mode
    }

    fun setSortMode(mode: SortMode) {
        _sortMode.value = mode
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun enterSelectionMode(initialId: Long) {
        _isSelectionMode.value = true
        _selectedIds.value = setOf(initialId)
    }

    fun toggleSelection(id: Long) {
        val current = _selectedIds.value
        _selectedIds.value = if (id in current) current - id else current + id
        if (_selectedIds.value.isEmpty()) {
            _isSelectionMode.value = false
        }
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedIds.value = emptySet()
    }

    fun selectAll() {
        _selectedIds.value = uiState.value.downloads.map { it.id }.toSet()
    }

    fun deleteSelected() {
        viewModelScope.launch {
            downloadDao.deleteByIds(_selectedIds.value.toList())
            exitSelectionMode()
        }
    }

    fun deleteSingle(download: DownloadEntity) {
        viewModelScope.launch {
            downloadDao.delete(download)
        }
    }
}
