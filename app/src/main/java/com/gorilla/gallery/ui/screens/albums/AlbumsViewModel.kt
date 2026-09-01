package com.gorilla.gallery.ui.screens.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorilla.gallery.AppContainer
import com.gorilla.gallery.data.model.Album
import com.gorilla.gallery.data.model.MediaItem
import com.gorilla.gallery.ui.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class AlbumsViewModel(private val container: AppContainer) : ViewModel() {
    val albums: StateFlow<List<Album>> =
        container.albumRepository.albums
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Same Loading contract as the Timeline: stay in Loading until the library has items or the
    // scan has confirmed the device is genuinely empty (libraryEmpty == true), so albums never
    // flash an empty grid while freshly-scanned rows are still propagating from Room.
    val isLoading: StateFlow<Boolean> =
        combine(
            container.mediaRepository.items,
            container.mediaRepository.libraryEmpty,
        ) { items, libraryEmpty -> items.isEmpty() && libraryEmpty != true }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val photoCount: StateFlow<Int> =
        container.mediaRepository.items
            .map { items -> items.count { !it.isVideo } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val videoCount: StateFlow<Int> =
        container.mediaRepository.items
            .map { items -> items.count { it.isVideo } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val screenshotCount: StateFlow<Int> =
        container.mediaRepository.items
            .map { items -> items.count { it.isScreenshot } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val selfieCount: StateFlow<Int> =
        combine(
            container.mediaRepository.items,
            container.faceIndexRepository.selfiePaths,
        ) { items, selfiePaths ->
            items.count { !it.isVideo && it.uri.toString() in selfiePaths }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val favoriteCount: StateFlow<Int> =
        container.mediaRepository.items
            .map { items -> items.count { it.isFavorite } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val editedCount: StateFlow<Int> =
        container.mediaRepository.items
            .map { items -> items.count { it.isEdited } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val trashItems: StateFlow<List<MediaItem>> =
        container.trashRepository.items
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun moveAlbum(album: Album, visibleAlbums: List<Album>, direction: Int): Boolean =
        container.albumRepository.moveAlbum(album.bucketId, visibleAlbums, direction)

    fun deleteAlbumEntry(album: Album) {
        container.albumRepository.deleteAlbumEntry(album)
    }

    companion object {
        val Factory = viewModelFactory { container -> AlbumsViewModel(container) }
    }
}
