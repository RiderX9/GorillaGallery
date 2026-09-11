package com.gorilla.gallery.data.repo

import android.content.Context
import android.content.ContentUris
import android.provider.MediaStore
import com.gorilla.gallery.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

data class PersonCategory(
    val label: String,
    val imagePaths: Set<String>,
    val isPet: Boolean = false,
    val clusterId: Int = -1,
)

class PeopleRepository(database: AppDatabase, private val context: Context) {
    private val faceDao = database.faceIndexDao()
    private val labelDao = database.imageLabelDao()
    private val embeddingDao = database.faceEmbeddingDao()
    private val prefs = context.getSharedPreferences("person_manual_photos", Context.MODE_PRIVATE)

    private val _categoriesChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val categoriesChanged = _categoriesChanged.asSharedFlow()

    companion object {
        fun normalizeMediaUri(uri: String): String {
            return try {
                val parsed = android.net.Uri.parse(uri)
                val id = ContentUris.parseId(parsed)
                if (id > 0) {
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id).toString()
                } else {
                    uri
                }
            } catch (_: Exception) {
                uri
            }
        }

        fun normalizePaths(paths: Set<String>): Set<String> = paths.map { normalizeMediaUri(it) }.toSet()
    }

    suspend fun peopleImagePaths(): Set<String> = faceDao.peopleImagePaths()

    suspend fun petImagePaths(): Set<String> = labelDao.petImagePaths()

    fun addManualPhotos(clusterId: Int, paths: Set<String>) {
        val existing = getManualPhotos(clusterId)
        val normalized = normalizePaths(paths)
        prefs.edit().putString("cluster_$clusterId", (existing + normalized).joinToString("\n")).apply()
        _categoriesChanged.tryEmit(Unit)
    }

    fun getManualPhotos(clusterId: Int): Set<String> {
        val raw = prefs.getString("cluster_$clusterId", "") ?: ""
        if (raw.isEmpty()) return emptySet()
        val stored = raw.split("\n").filter { it.isNotEmpty() }.toSet()
        return normalizePaths(stored)
    }

    fun removeManualPhoto(clusterId: Int, path: String) {
        val normalized = normalizeMediaUri(path)
        val existing = getManualPhotos(clusterId)
        prefs.edit().putString("cluster_$clusterId", (existing - normalized).joinToString("\n")).apply()
        _categoriesChanged.tryEmit(Unit)
    }

    suspend fun renamePerson(clusterId: Int, newName: String) {
        embeddingDao.updateLabelForCluster(clusterId, newName)
        _categoriesChanged.tryEmit(Unit)
    }

    fun addPhotosToPerson(clusterId: Int, paths: List<String>) {
        addManualPhotos(clusterId, paths.toSet())
    }

    suspend fun getCategories(): List<PersonCategory> = withContext(Dispatchers.IO) {
        buildList {
            val clusterIds = embeddingDao.distinctClusterIds()

            if (clusterIds.isNotEmpty()) {
                val labels = embeddingDao.labelForClusterBatch(clusterIds)

                for (clusterId in clusterIds) {
                    val paths = embeddingDao.imagePathsForCluster(clusterId)
                    val manual = getManualPhotos(clusterId)
                    val allPaths = paths + manual
                    if (allPaths.isEmpty()) continue
                    val label = labels[clusterId] ?: "Person ${clusterId + 1}"
                    add(
                        PersonCategory(
                            label = label,
                            imagePaths = allPaths,
                            clusterId = clusterId,
                        ),
                    )
                }
            } else {
                val people = peopleImagePaths()
                if (people.isNotEmpty()) {
                    add(PersonCategory(label = "People", imagePaths = people))
                }
            }

            val pets = petImagePaths()
            if (pets.isNotEmpty()) {
                add(PersonCategory(label = "Pets", imagePaths = pets, isPet = true))
            }
        }
    }
}
