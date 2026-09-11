package com.gorilla.gallery.data.repo

import android.content.Context
import com.gorilla.gallery.data.model.Album
import com.gorilla.gallery.data.model.MediaItem
import com.gorilla.gallery.data.model.classifyAlbum
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** Auto-albums derived live from the cached library, plus per-album item views. */
class AlbumRepository(
    private val context: Context,
    private val mediaRepo: MediaStoreRepository,
) {
    private val prefs = context.getSharedPreferences("custom_albums", Context.MODE_PRIVATE)
    private val customAlbumPaths = MutableStateFlow(loadCustomAlbumPaths())
    private val albumOrder = MutableStateFlow(loadAlbumOrder())
    private val hiddenAlbums = MutableStateFlow(loadHiddenAlbums())
    private val renamedAlbums = MutableStateFlow(loadRenamedAlbums())

    val albums: Flow<List<Album>> =
        combine(mediaRepo.items, customAlbumPaths, albumOrder, hiddenAlbums, renamedAlbums) { items, customPaths, order, hidden, renamed ->
            val derived = mediaRepo.deriveAlbums(items)
            val existingPaths = derived.map { it.relativePath.normalizedAlbumPath() }.toHashSet()
            val custom = customPaths
                .filter { it.normalizedAlbumPath() !in existingPaths }
                .map { path ->
                    val name = path.trimEnd('/').substringAfterLast('/').ifBlank { "Album" }
                    Album(
                        bucketId = customBucketId(path),
                        name = name,
                        itemCount = 0,
                        coverUri = null,
                        coverIsVideo = false,
                        previewItems = emptyList(),
                        relativePath = path,
                        kind = classifyAlbum(path, name),
                    )
                }
            val base = (derived + custom)
                .filterNot { it.albumKey() in hidden }
                .map { if (renamed.containsKey(it.bucketId.toString())) it.copy(name = renamed[it.bucketId.toString()]!!) else it }
                .sortedWith(compareBy({ it.kind.ordinal }, { -it.itemCount }, { it.name.lowercase() }))
            applyAlbumOrder(base, order)
        }

    fun itemsInAlbum(bucketId: Long): Flow<List<MediaItem>> =
        combine(mediaRepo.items, customAlbumPaths) { items, customPaths ->
            val customPath = customPaths.firstOrNull { customBucketId(it) == bucketId }
            if (customPath != null) {
                items.filter { it.relativePath.normalizedAlbumPath() == customPath.normalizedAlbumPath() }
            } else {
                items.filter { it.bucketId == bucketId }
            }
        }

    fun albumName(bucketId: Long): Flow<String> =
        combine(mediaRepo.items, customAlbumPaths, renamedAlbums) { items, customPaths, renamed ->
            renamed[bucketId.toString()] ?: run {
                val customPath = customPaths.firstOrNull { customBucketId(it) == bucketId }
                customPath?.trimEnd('/')?.substringAfterLast('/')?.ifBlank { "Album" }
                    ?: items.firstOrNull { it.bucketId == bucketId }?.bucketName
                    ?: "Album"
            }
        }

    fun createAlbum(name: String): Boolean {
        val cleanName = name.trim().replace(Regex("""[\\/:*?"<>|]+"""), " ").replace(Regex("\\s+"), " ")
        if (cleanName.isBlank()) return false
        val path = "Pictures/$cleanName/"
        val next = (customAlbumPaths.value + path).distinctBy { it.normalizedAlbumPath() }
        prefs.edit().putStringSet(KEY_PATHS, next.toSet()).apply()
        customAlbumPaths.value = next
        return true
    }

    fun renameAlbum(bucketId: Long, newName: String) {
        val current = prefs.getStringSet(KEY_RENAMES, emptySet()).orEmpty().toMutableSet()
        current.removeAll { it.startsWith("$bucketId=") }
        current.add("$bucketId=$newName")
        prefs.edit().putStringSet(KEY_RENAMES, current).apply()
        val nextMap = renamedAlbums.value.toMutableMap()
        nextMap[bucketId.toString()] = newName
        renamedAlbums.value = nextMap
    }

    fun moveAlbum(bucketId: Long, visibleAlbums: List<Album>, direction: Int): Boolean {
        val current = visibleAlbums.map { it.albumKey() }.toMutableList()
        val index = visibleAlbums.indexOfFirst { it.bucketId == bucketId }
        if (index == -1) return false
        val target = (index + direction).coerceIn(0, current.lastIndex)
        if (target == index) return false
        current.add(target, current.removeAt(index))
        saveAlbumOrder(current)
        return true
    }

    fun deleteAlbumEntry(album: Album) {
        val normalizedPath = album.relativePath.normalizedAlbumPath()
        val customNext = customAlbumPaths.value.filterNot { it.normalizedAlbumPath() == normalizedPath }
        if (customNext.size != customAlbumPaths.value.size) {
            prefs.edit().putStringSet(KEY_PATHS, customNext.toSet()).apply()
            customAlbumPaths.value = customNext
            if (album.itemCount > 0) {
                val hiddenNext = hiddenAlbums.value + album.albumKey()
                prefs.edit().putStringSet(KEY_HIDDEN, hiddenNext).apply()
                hiddenAlbums.value = hiddenNext
            }
        } else {
            val hiddenNext = hiddenAlbums.value + album.albumKey()
            prefs.edit().putStringSet(KEY_HIDDEN, hiddenNext).apply()
            hiddenAlbums.value = hiddenNext
        }
        val orderedNext = albumOrder.value.filterNot { it == album.albumKey() }
        saveAlbumOrder(orderedNext)
    }

    private fun loadCustomAlbumPaths(): List<String> =
        prefs.getStringSet(KEY_PATHS, emptySet()).orEmpty().toList().sorted()

    private fun loadAlbumOrder(): List<String> =
        prefs.getString(KEY_ORDER, "").orEmpty()
            .split("|")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    private fun loadHiddenAlbums(): Set<String> =
        prefs.getStringSet(KEY_HIDDEN, emptySet()).orEmpty()

    private fun loadRenamedAlbums(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        prefs.getStringSet(KEY_RENAMES, emptySet())?.forEach { entry ->
            val parts = entry.split("=", limit = 2)
            if (parts.size == 2) map[parts[0]] = parts[1]
        }
        return map
    }

    private fun saveAlbumOrder(order: List<String>) {
        val distinct = order.distinct()
        prefs.edit().putString(KEY_ORDER, distinct.joinToString("|")).apply()
        albumOrder.value = distinct
    }

    private fun applyAlbumOrder(albums: List<Album>, order: List<String>): List<Album> {
        if (order.isEmpty()) return albums
        val byKey = albums.associateBy { it.albumKey() }
        val ordered = order.mapNotNull { byKey[it] }
        val orderedKeys = ordered.map { it.albumKey() }.toHashSet()
        return ordered + albums.filterNot { it.albumKey() in orderedKeys }
    }

    private fun customBucketId(path: String): Long =
        -kotlin.math.abs(path.normalizedAlbumPath().hashCode().toLong()).coerceAtLeast(10_000L)

    private fun String.normalizedAlbumPath(): String = trim().trimEnd('/').lowercase()

    private fun Album.albumKey(): String =
        relativePath.normalizedAlbumPath().ifBlank { "bucket:$bucketId" }

    private companion object {
        const val KEY_PATHS = "paths"
        const val KEY_ORDER = "order"
        const val KEY_HIDDEN = "hidden"
        const val KEY_RENAMES = "renames"
    }
}
