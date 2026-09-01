package com.gorilla.gallery.data.repo

import com.gorilla.gallery.data.db.AppDatabase
import com.gorilla.gallery.data.db.FavoriteEntity
import com.gorilla.gallery.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Room-backed hearts. The "Favorites" album is just a live view over the media flow
 * filtered by the favourites table, so a heart toggled in the viewer reflects everywhere.
 */
class FavoritesRepository(
    db: AppDatabase,
    private val mediaRepo: MediaStoreRepository,
) {
    private val dao = db.favoriteDao()

    val favorites: Flow<List<MediaItem>> =
        mediaRepo.items.map { items -> items.filter { it.isFavorite } }

    suspend fun toggle(item: MediaItem): Boolean = withContext(Dispatchers.IO) {
        val now = !dao.isFavorite(item.id)
        if (now) dao.add(FavoriteEntity(item.id)) else dao.remove(item.id)
        now
    }

    suspend fun setFavorite(item: MediaItem, favorite: Boolean) = withContext(Dispatchers.IO) {
        if (favorite) dao.add(FavoriteEntity(item.id)) else dao.remove(item.id)
    }
}
