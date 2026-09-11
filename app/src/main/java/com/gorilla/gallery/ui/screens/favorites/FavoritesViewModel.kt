package com.gorilla.gallery.ui.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorilla.gallery.AppContainer
import com.gorilla.gallery.data.model.MediaItem
import com.gorilla.gallery.ui.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class FavoritesViewModel(container: AppContainer) : ViewModel() {
    val favorites: StateFlow<List<MediaItem>> =
        container.favoritesRepository.favorites
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    companion object {
        val Factory = viewModelFactory { container -> FavoritesViewModel(container) }
    }
}
