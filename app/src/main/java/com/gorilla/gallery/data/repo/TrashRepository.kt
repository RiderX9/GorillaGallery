package com.gorilla.gallery.data.repo

import android.content.Context
import android.content.IntentSender
import com.gorilla.gallery.data.db.AppDatabase
import com.gorilla.gallery.data.db.TrashEntity
import com.gorilla.gallery.data.model.MediaItem
import com.gorilla.gallery.data.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Result of staging a trash operation: launch [sender], then confirm or roll back. */
data class TrashPrep(
    val sender: IntentSender,
    val mediaIds: List<Long>,
    val trashIds: List<String>,
)

/**
 * App-owned trash. Bytes are copied to internal storage and the public MediaStore copy is
 * deleted (consent intent). Restorable for 30 days; expired entries are swept on app start.
 */
class TrashRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val mediaRepo: MediaStoreRepository,
) {
    private val dao = db.trashDao()
    private val dir = File(context.filesDir, "trash")
    private val retentionMs = 30L * 24 * 60 * 60 * 1000

    val items: Flow<List<MediaItem>> = dao.observeAll().map { rows -> rows.map { it.toItem() } }

    /** Copy bytes + write rows, then return the delete consent intent for the originals. */
    suspend fun prepareTrash(items: List<MediaItem>): TrashPrep = withContext(Dispatchers.IO) {
        dir.mkdirs()
        val trashIds = ArrayList<String>()
        for (item in items) {
            val id = UUID.randomUUID().toString()
            val ext = MediaIo.extOf(item.mimeType, if (item.isVideo) "mp4" else "jpg")
            val file = File(dir, "$id.$ext")
            MediaIo.copyUriToFile(context.contentResolver, item.uri, file)
            dao.insert(
                TrashEntity(
                    id = id,
                    internalPath = file.absolutePath,
                    displayName = item.displayName,
                    mimeType = item.mimeType,
                    type = item.type.name,
                    originalRelativePath = item.relativePath,
                    dateTakenMs = item.dateTakenMs,
                    sizeBytes = item.sizeBytes,
                    width = item.width,
                    height = item.height,
                    durationMs = item.durationMs,
                    deletedAtMs = System.currentTimeMillis(),
                ),
            )
            trashIds += id
        }
        TrashPrep(
            sender = mediaRepo.createDeleteRequest(items.map { it.uri }),
            mediaIds = items.map { it.id },
            trashIds = trashIds,
        )
    }

    /** User approved deletion: drop the now-gone originals from the media cache. */
    suspend fun onTrashConfirmed(mediaIds: List<Long>) = mediaRepo.dropCached(mediaIds)

    /** User cancelled the delete: undo the staged internal copies + rows. */
    suspend fun rollback(trashIds: List<String>) = withContext(Dispatchers.IO) {
        trashIds.forEach { id ->
            dao.getById(id)?.let { File(it.internalPath).delete() }
            dao.deleteById(id)
        }
    }

    /** Re-insert the file into MediaStore (app-owned, no consent), then remove from trash. */
    suspend fun restore(trashId: String) = withContext(Dispatchers.IO) {
        val row = dao.getById(trashId) ?: return@withContext
        val file = File(row.internalPath)
        if (file.exists()) {
            try {
                MediaIo.insertFromFile(
                    context = context,
                    source = file,
                    displayName = row.displayName,
                    mimeType = row.mimeType,
                    type = runCatching { MediaType.valueOf(row.type) }.getOrDefault(MediaType.IMAGE),
                    relativePath = row.originalRelativePath,
                    dateTakenMs = row.dateTakenMs,
                )
                file.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        dao.deleteById(trashId)
    }

    /** Permanently remove a single trash item. */
    suspend fun deleteForever(trashId: String) = withContext(Dispatchers.IO) {
        dao.getById(trashId)?.let { File(it.internalPath).delete() }
        dao.deleteById(trashId)
    }

    suspend fun emptyExpired() = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - retentionMs
        dao.expired(cutoff).forEach { File(it.internalPath).delete() }
        dao.deleteExpired(cutoff)
    }

    /** Empty Trash button — internal copies are the only remaining bytes, so just delete them. */
    suspend fun emptyAll() = withContext(Dispatchers.IO) {
        if (dir.exists()) dir.listFiles()?.forEach { it.delete() }
        dao.expired(Long.MAX_VALUE).forEach { /* path already removed above */ }
        dao.deleteExpired(Long.MAX_VALUE)
    }
}
