package com.gorilla.gallery.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorilla.gallery.AppContainer
import com.gorilla.gallery.data.model.MediaItem
import com.gorilla.gallery.data.repo.PersonCategory
import com.gorilla.gallery.data.repo.TripCard
import com.gorilla.gallery.ui.viewModelFactory
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class SearchViewModel(container: AppContainer) : ViewModel() {
    private val mediaRepo = container.mediaRepository
    private val imageLabelRepo = container.imageLabelRepository
    private val objectIndexRepo = container.objectIndexRepository
    private val textIndexRepo = container.textIndexRepository
    private val peopleRepo = container.peopleRepository
    private val tripsRepo = container.tripsRepository
    private val prefs = container.context.getSharedPreferences("people_prefs", android.content.Context.MODE_PRIVATE)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    private val fullDate = DateTimeFormatter.ofPattern("MMM d yyyy", Locale.getDefault())

    private val _peopleCategories = MutableStateFlow<List<PersonCategory>>(emptyList())
    val peopleCategories: StateFlow<List<PersonCategory>> = _peopleCategories.asStateFlow()

    private val _trips = MutableStateFlow<List<TripCard>>(emptyList())
    val trips: StateFlow<List<TripCard>> = _trips.asStateFlow()

    init {
        viewModelScope.launch { refreshPeople() }
        viewModelScope.launch { refreshTrips() }
        viewModelScope.launch {
            peopleRepo.categoriesChanged.collect { refreshPeople() }
        }
    }

    private fun hiddenClusterIds(): Set<Int> {
        val raw = prefs.getString("hidden_clusters", "") ?: ""
        return if (raw.isEmpty()) emptySet() else raw.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    fun hidePerson(clusterId: Int) {
        val current = hiddenClusterIds() + clusterId
        prefs.edit().putString("hidden_clusters", current.joinToString(",")).apply()
        viewModelScope.launch { refreshPeople() }
    }

    fun unhidePerson(clusterId: Int) {
        val current = hiddenClusterIds() - clusterId
        prefs.edit().putString("hidden_clusters", current.joinToString(",")).apply()
        viewModelScope.launch { refreshPeople() }
    }

    suspend fun refreshPeople() {
        val allMedia = mediaRepo.items.first()
        val screenshotPaths = allMedia.filter { it.isScreenshot }.map { it.uri.toString() }.toSet()
        val hidden = hiddenClusterIds()
        val raw = peopleRepo.getCategories()
        _peopleCategories.value = raw.map { cat ->
            cat.copy(imagePaths = cat.imagePaths.filter { it !in screenshotPaths }.toSet())
        }.filter { it.imagePaths.isNotEmpty() && it.clusterId !in hidden }
    }

    fun renamePerson(clusterId: Int, newName: String) {
        viewModelScope.launch {
            peopleRepo.renamePerson(clusterId, newName)
            refreshPeople()
        }
    }

    suspend fun refreshTrips() {
        val items = mediaRepo.items.first()
        _trips.value = tripsRepo.buildTrips(items)
    }

    @OptIn(FlowPreview::class)
    val results: StateFlow<List<MediaItem>> =
        combine(
            mediaRepo.items,
            _query.debounce(150),
            _peopleCategories,
            _trips
        ) { items, q, people, trips ->
            val term = q.trim()
            if (term.isBlank()) emptyList()
            else {
                val t = term.lowercase()
                if (t == "video" || t == "videos") return@combine items.filter { it.isVideo }
                if (t == "photo" || t == "photos") return@combine items.filter { !it.isVideo }
                if (t == "screenshot" || t == "screenshots") return@combine items.filter { it.isScreenshot }
                if (t == "favorite" || t == "favorites") return@combine items.filter { it.isFavorite }
                if (t == "edited") return@combine items.filter { it.isEdited }

                val labelMatches = imageLabelRepo.searchImagePaths(term)
                val objectMatches = objectIndexRepo.searchImagePaths(term)
                val textMatches = textIndexRepo.searchImagePaths(term)
                val peopleMatches = people.filter { it.label.contains(term, ignoreCase = true) }.flatMap { it.imagePaths }.toSet()
                val tripMatches = trips.filter { 
                    it.locationName.contains(term, ignoreCase = true) || 
                    it.dateRange.contains(term, ignoreCase = true) 
                }.flatMap { it.imagePaths }.toSet()
                items.filter {
                    matches(it, term) ||
                        it.uri.toString() in labelMatches ||
                        it.uri.toString() in objectMatches ||
                        it.uri.toString() in textMatches ||
                        it.uri.toString() in peopleMatches ||
                        it.uri.toString() in tripMatches
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun matches(item: MediaItem, term: String): Boolean {
        if (item.displayName.contains(term, ignoreCase = true)) return true
        if (item.bucketName.contains(term, ignoreCase = true)) return true
        
        val t = term.lowercase()
        if ((t == "video" || t == "videos") && item.isVideo) return true
        if ((t == "photo" || t == "photos") && !item.isVideo) return true
        if ((t == "screenshot" || t == "screenshots") && item.isScreenshot) return true
        if ((t == "favorite" || t == "favorites") && item.isFavorite) return true
        if (t == "edited" && item.isEdited) return true

        val date = Instant.ofEpochMilli(item.dateTakenMs).atZone(ZoneId.systemDefault())
        if (date.format(monthYear).contains(term, ignoreCase = true)) return true
        if (date.format(fullDate).contains(term, ignoreCase = true)) return true
        return false
    }

    fun setQuery(q: String) { _query.value = q }

    companion object {
        val Factory = viewModelFactory { container -> SearchViewModel(container) }
    }
}
