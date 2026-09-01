package com.gorilla.gallery.data.repo

import android.content.Context
import android.content.IntentSender
import android.net.Uri
import com.gorilla.gallery.data.db.AppDatabase
import com.gorilla.gallery.data.db.SecureItemEntity
import com.gorilla.gallery.data.model.MediaItem
import com.gorilla.gallery.data.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class SecurePrep(
    val sender: IntentSender,
    val mediaIds: List<Long>,
    val secureIds: List<String>,
    val originalUris: List<Uri>,
)

/**
 * Secure Folder. Secured bytes live in app-internal [filesDir]/secure — a directory
 * MediaStore never indexes, so the photos genuinely leave the public gallery. Move-in
 * copies bytes inside then deletes the public original (consent); move-out re-inserts
 * into MediaStore. The unlocked flag auto-resets after >2 min in the background.
 */
class SecureFolderRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val mediaRepo: MediaStoreRepository,
) {
    private val dao = db.secureDao()
    private val dir = File(context.filesDir, "secure")

    val items: Flow<List<MediaItem>> = dao.observeAll().map { rows -> rows.map { it.toItem() } }
    val count: Flow<Int> = dao.observeCount()

    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    fun unlock() { _unlocked.value = true }
    fun lock() { _unlocked.value = false }

    /** Reuses MainActivity's background-gap measurement to relock after >2 minutes away. */
    fun onAppResumed(gapMs: Long) {
        if (gapMs > 2 * 60 * 1000) _unlocked.value = false
    }

    fun internalFile(item: MediaItem): File = File(item.uri.path ?: "")

    /** Copy bytes into the vault + write rows, return the write consent intent. */
    suspend fun prepareMoveIn(items: List<MediaItem>): SecurePrep = withContext(Dispatchers.IO) {
        dir.mkdirs()
        val secureIds = ArrayList<String>()
        for (item in items) {
            val id = UUID.randomUUID().toString()
            val ext = MediaIo.extOf(item.mimeType, if (item.isVideo) "mp4" else "jpg")
            val file = File(dir, "$id.$ext")
            MediaIo.copyUriToFile(context.contentResolver, item.uri, file)
            dao.insert(
                SecureItemEntity(
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
                    securedAtMs = System.currentTimeMillis(),
                ),
            )
            secureIds += id
        }
        SecurePrep(
            sender = mediaRepo.createWriteRequest(items.map { it.uri }),
            mediaIds = items.map { it.id },
            secureIds = secureIds,
            originalUris = items.map { it.uri },
        )
    }

    suspend fun onMoveInConfirmed(mediaIds: List<Long>, originalUris: List<Uri>) = withContext(Dispatchers.IO) {
        originalUris.forEach { uri ->
            context.contentResolver.delete(uri, null, null)
        }
        mediaRepo.dropCached(mediaIds)
    }

    suspend fun rollback(secureIds: List<String>) = withContext(Dispatchers.IO) {
        secureIds.forEach { id ->
            dao.getById(id)?.let { File(it.internalPath).delete() }
            dao.deleteById(id)
        }
    }

    /** Move out of the vault: app-owned MediaStore insert (no consent), then delete internal. */
    suspend fun moveOut(secureId: String) = withContext(Dispatchers.IO) {
        val row = dao.getById(secureId) ?: return@withContext
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
        dao.deleteById(secureId)
    }

    /** Permanently delete an item from the vault. */
    suspend fun deleteForever(secureId: String) = withContext(Dispatchers.IO) {
        dao.getById(secureId)?.let { File(it.internalPath).delete() }
        dao.deleteById(secureId)
    }
}
