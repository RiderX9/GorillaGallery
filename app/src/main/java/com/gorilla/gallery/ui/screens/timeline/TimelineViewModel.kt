package com.gorilla.gallery.ui.screens.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorilla.gallery.data.settings.DateGranularity
import com.gorilla.gallery.ui.components.MediaSection
import com.gorilla.gallery.ui.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The three mutually-exclusive states the Timeline can be in — never observed inconsistently. */
sealed interface TimelineUiState {
    data object Loading : TimelineUiState
    data class Content(val sections: List<MediaSection>) : TimelineUiState
    data object Empty : TimelineUiState
}

class TimelineViewModel(
    private val container: com.gorilla.gallery.AppContainer,
) : ViewModel() {
    private val mediaRepo = container.mediaRepository
    private val settingsRepo = container.settingsRepository

    // A SINGLE source of truth for the screen. Deriving "loading" and "sections" as two
    // independent flows let Compose observe an inconsistent pair for a frame (loading already
    // false, sections not yet recomputed) which flashed the empty state. Collapsing the whole
    // decision into one flow makes Loading / Content / Empty mutually exclusive and atomic.
    private val _sortAscending = kotlinx.coroutines.flow.MutableStateFlow(false)
    val sortAscending: StateFlow<Boolean> = _sortAscending
    
    private val _sortMode = kotlinx.coroutines.flow.MutableStateFlow("Date Taken")
    val sortMode: StateFlow<String> = _sortMode

    val uiState: StateFlow<TimelineUiState> =
        combine(
            mediaRepo.items,
            settingsRepo.settings.map { it.dateGranularity },
            mediaRepo.libraryEmpty,
            _sortAscending,
            _sortMode
        ) { args ->
            val items = args[0] as List<com.gorilla.gallery.data.model.MediaItem>
            val granularity = args[1] as DateGranularity
            val libraryEmpty = args[2] as Boolean?
            val ascending = args[3] as Boolean
            val mode = args[4] as String
            
            when {
                items.isNotEmpty() -> {
                    val sorted = when (mode) {
                        "Date Taken" -> items.sortedBy { it.dateTakenMs }
                        "Date Modified" -> items.sortedBy { it.dateModifiedSec }
                        "Name (A-Z)" -> items.sortedBy { it.displayName }
                        "File Size" -> items.sortedBy { it.sizeBytes }
                        else -> items.sortedBy { it.dateTakenMs }
                    }
                    val ordered = if (ascending) sorted else sorted.reversed()
                    
                    val sections = if (mode == "Date Taken" || mode == "Date Modified") {
                        com.gorilla.gallery.ui.screens.timeline.groupByDate(ordered, granularity, ascending)
                    } else {
                        listOf(MediaSection(mode, mode, ordered))
                    }
                    TimelineUiState.Content(sections)
                }
                libraryEmpty == true -> TimelineUiState.Empty
                else -> TimelineUiState.Loading
            }
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TimelineUiState.Loading)

    fun setGranularity(g: DateGranularity) = viewModelScope.launch { settingsRepo.setDateGranularity(g) }
    fun setColumns(c: Int) = viewModelScope.launch { settingsRepo.setGridColumns(c) }
    
    fun setSortAscending(ascending: Boolean) {
        _sortAscending.value = ascending
    }
    
    fun setSortMode(mode: String) {
        _sortMode.value = mode
    }

    companion object {
        val Factory = viewModelFactory { container -> TimelineViewModel(container) }
    }
}
