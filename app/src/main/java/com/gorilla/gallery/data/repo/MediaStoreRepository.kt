package com.gorilla.gallery.data.repo

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import com.gorilla.gallery.data.db.AppDatabase
import com.gorilla.gallery.data.db.MediaEntity
import com.gorilla.gallery.data.model.Album
import com.gorilla.gallery.data.model.MediaItem
import com.gorilla.gallery.data.model.MediaType
import com.gorilla.gallery.data.model.classifyAlbum
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owns the photo/video library: a MediaStore scan cached in Room, a [ContentObserver]
 * that triggers incremental rescans without an app restart, favourites merged in, and
 * the scoped-storage consent-intent builders for delete / trash / move / overwrite.
 *
 * Mirrors GorillaMusic's MusicRepository: full vs incremental scan, a guard that never
 * wipes the cache on an empty result, and a debounced observer flow.
 */
class MediaStoreRepository(
    private val context: Context,
    private val db: AppDatabase,
) {
    private val resolver: ContentResolver = context.contentResolver
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mediaDao = db.mediaDao()
    private val favoriteDao = db.favoriteDao()
    private val prefs = context.getSharedPreferences("metadata_overrides", Context.MODE_PRIVATE)

    private val _scanResults = MutableSharedFlow<Int>(extraBufferCapacity = 4)
    val scanResults: Flow<Int> = _scanResults

    private val _mediaStoreChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    // Tri-state "is the library empty?", as known from the MediaStore scan:
    //   null  = the first scan hasn't finished yet            → UI stays in Loading
    //   false = the scan found media (rows may still be        → UI stays in Loading until the
    //           propagating from Room into [items])              [items] flow catches up
    //   true  = the scan confirmed zero photos/videos on device → UI may show the empty state
    //
    // The earlier "scan complete" boolean flipped true the instant scan() returned, but Room
    // re-emits the freshly-written rows *asynchronously after* that — so for one frame [items]
    // was still the lagging empty list while "complete" was already true, which re-flashed
    // "No photos yet". This tri-state never reports `true` when the scan actually found media,
    // so the empty state can only appear on a genuinely empty device.
    private val _libraryEmpty = MutableStateFlow<Boolean?>(null)
    val libraryEmpty: StateFlow<Boolean?> = _libraryEmpty

    /** All cached items with favourite flags merged. Source for timeline/albums/search. */
    val items: Flow<List<MediaItem>> =
        combine(mediaDao.observeAll(), favoriteDao.observeIds()) { entities, favIds ->
            val favSet = favIds.toHashSet()
            entities.map { 
                val base = it.toItem(it.id in favSet)
                val overriddenName = prefs.getString("${it.id}_name", null)
                val overriddenDate = prefs.getLong("${it.id}_date", -1L).takeIf { d -> d != -1L }
                if (overriddenName != null || overriddenDate != null) {
                    base.copy(
                        displayName = overriddenName ?: base.displayName,
                        dateTakenMs = overriddenDate ?: base.dateTakenMs,
                        dateModifiedSec = if (overriddenDate != null) overriddenDate / 1000 else base.dateModifiedSec
                    )
                } else base
            }
        }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    // ---- Scanning -------------------------------------------------------------

    /** Full scan: query MediaStore, upsert, drop rows that disappeared. */
    suspend fun scan(): Int = withContext(Dispatchers.IO) {
        val fresh = queryAll()
        if (fresh.isEmpty() && mediaDao.count() > 0) {
            // Permission revoked mid-session or a transient empty cursor — keep the cache.
            // The library still has the cached media, so it is NOT empty.
            _libraryEmpty.value = false
            return@withContext 0
        }
        val freshIds = fresh.map { it.id }.toHashSet()
        val existing = mediaDao.getStamps().map { it.id }.toHashSet()
        mediaDao.upsertAll(fresh.map { MediaEntity.from(it) })
        val removed = existing.filter { it !in freshIds }
        if (removed.isNotEmpty()) mediaDao.deleteByIds(removed)
        val added = freshIds.count { it !in existing }
        // Report emptiness from what the scan actually saw — set AFTER the upsert so that when
        // media exists, `false` is published while the rows are already on their way into Room.
        // Only a truly empty device publishes `true`, which is the sole trigger for the empty UI.
        _libraryEmpty.value = fresh.isEmpty()
        added
    }

    /**
     * Incremental: upsert new/modified rows and prune deletions, WITHOUT re-reading the whole
     * library. New captures and in-place edits both bump DATE_MODIFIED, so the most-recent rows by
     * DATE_MODIFIED contain every change — reading a small window of them makes new media appear
     * near-instantly instead of after a full-library scan.
     */
    suspend fun incrementalScan(): Int = withContext(Dispatchers.IO) {
        val recent = queryRecent(RECENT_WINDOW)
        if (recent.isEmpty() && mediaDao.count() > 0) return@withContext 0

        val stamps = mediaDao.getStamps().associate { it.id to it.dateModifiedSec }
        val changed = recent.filter { stamps[it.id] != it.dateModifiedSec }

        // If EVERY row in the window is unknown, a bulk import may extend past the window — fall
        // back to a full scan so nothing is missed. The common single-capture case never hits this.
        if (changed.isNotEmpty() && changed.size == recent.size && recent.isNotEmpty()) {
            return@withContext fullReconcile()
        }

        if (changed.isNotEmpty()) mediaDao.upsertAll(changed.map { MediaEntity.from(it) })

        // Prune rows deleted from MediaStore by other apps, using a cheap id-only query.
        val liveIds = queryAllIds()
        if (liveIds.isNotEmpty()) {
            val removed = stamps.keys.filter { it !in liveIds }
            if (removed.isNotEmpty()) mediaDao.deleteByIds(removed)
        }
        changed.count { it.id !in stamps }
    }

    /** Full diff against the entire library — the incremental fallback for bulk imports. */
    private suspend fun fullReconcile(): Int {
        val fresh = queryAll()
        if (fresh.isEmpty() && mediaDao.count() > 0) return 0
        val stamps = mediaDao.getStamps().associate { it.id to it.dateModifiedSec }
        val changed = fresh.filter { stamps[it.id] != it.dateModifiedSec }
        if (changed.isNotEmpty()) mediaDao.upsertAll(changed.map { MediaEntity.from(it) })
        val freshIds = fresh.map { it.id }.toHashSet()
        val removed = stamps.keys.filter { it !in freshIds }
        if (removed.isNotEmpty()) mediaDao.deleteByIds(removed)
        return changed.count { it.id !in stamps }
    }

    private fun queryAll(): List<MediaItem> =
        queryItems(
            resolver.query(
                MEDIA_COLLECTION, MEDIA_PROJECTION, MEDIA_SELECTION, MEDIA_ARGS,
                "${FileColumns.DATE_TAKEN} DESC, ${FileColumns.DATE_ADDED} DESC",
            )
        )

    /**
     * The [limit] most-recently-modified rows. Used by the incremental scan so a single new capture
     * is read cheaply instead of re-querying the whole library. Uses QUERY_ARG_LIMIT (supported on
     * our minSdk 31) rather than a deprecated "LIMIT" suffix on the sort string.
     */
    private fun queryRecent(limit: Int): List<MediaItem> {
        val queryArgs = android.os.Bundle().apply {
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, MEDIA_ARGS)
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, MEDIA_SELECTION)
            putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, "${FileColumns.DATE_MODIFIED} DESC")
            putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
        }
        return queryItems(resolver.query(MEDIA_COLLECTION, MEDIA_PROJECTION, queryArgs, null))
    }

    /** Cheap id-only query of the whole library, for pruning rows deleted by other apps. */
    private fun queryAllIds(): HashSet<Long> {
        val out = HashSet<Long>()
        resolver.query(MEDIA_COLLECTION, arrayOf(FileColumns._ID), MEDIA_SELECTION, MEDIA_ARGS, null)?.use { c ->
            val idC = c.getColumnIndexOrThrow(FileColumns._ID)
            while (c.moveToNext()) out += c.getLong(idC)
        }
        return out
    }

    private fun queryItems(cursor: android.database.Cursor?): List<MediaItem> {
        val out = ArrayList<MediaItem>()
        cursor?.use { c ->
            val idC = c.getColumnIndexOrThrow(FileColumns._ID)
            val nameC = c.getColumnIndexOrThrow(FileColumns.DISPLAY_NAME)
            val mimeC = c.getColumnIndexOrThrow(FileColumns.MIME_TYPE)
            val sizeC = c.getColumnIndexOrThrow(FileColumns.SIZE)
            val addedC = c.getColumnIndexOrThrow(FileColumns.DATE_ADDED)
            val modC = c.getColumnIndexOrThrow(FileColumns.DATE_MODIFIED)
            val takenC = c.getColumnIndexOrThrow(FileColumns.DATE_TAKEN)
            val wC = c.getColumnIndexOrThrow(FileColumns.WIDTH)
            val hC = c.getColumnIndexOrThrow(FileColumns.HEIGHT)
            val bucketIdC = c.getColumnIndexOrThrow(FileColumns.BUCKET_ID)
            val bucketNameC = c.getColumnIndexOrThrow(FileColumns.BUCKET_DISPLAY_NAME)
            val relC = c.getColumnIndexOrThrow(FileColumns.RELATIVE_PATH)
            val orientC = c.getColumnIndexOrThrow(FileColumns.ORIENTATION)
            val durC = c.getColumnIndexOrThrow(FileColumns.DURATION)
            val typeC = c.getColumnIndexOrThrow(FileColumns.MEDIA_TYPE)

            while (c.moveToNext()) {
                val id = c.getLong(idC)
                val isVideo = c.getInt(typeC) == FileColumns.MEDIA_TYPE_VIDEO
                val type = if (isVideo) MediaType.VIDEO else MediaType.IMAGE
                val collectionUri = if (isVideo) {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
                val dateAdded = c.getLong(addedC)
                val dateTaken = c.getLong(takenC).takeIf { it > 0 } ?: (dateAdded * 1000)
                out += MediaItem(
                    id = id,
                    uri = ContentUris.withAppendedId(collectionUri, id),
                    type = type,
                    displayName = c.getString(nameC) ?: "Unknown",
                    mimeType = c.getString(mimeC) ?: if (isVideo) "video/*" else "image/*",
                    dateTakenMs = dateTaken,
                    dateAddedSec = dateAdded,
                    dateModifiedSec = c.getLong(modC),
                    sizeBytes = c.getLong(sizeC),
                    width = c.getInt(wC),
                    height = c.getInt(hC),
                    durationMs = if (isVideo) c.getLong(durC) else 0L,
                    bucketId = c.getLong(bucketIdC),
                    bucketName = c.getString(bucketNameC) ?: "Unknown",
                    relativePath = c.getString(relC) ?: "",
                    orientation = c.getInt(orientC),
                )
            }
        }
        return out
    }

    // ---- ContentObserver ------------------------------------------------------

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) { _mediaStoreChanges.tryEmit(Unit) }
    }
    private var observerRegistered = false

    @OptIn(FlowPreview::class)
    fun startObserving() {
        if (observerRegistered) return
        observerRegistered = true
        listOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        ).forEach { resolver.registerContentObserver(it, true, observer) }

        scope.launch {
            // Short debounce only to coalesce the burst of notifications a single capture/download
            // emits (temp file → final file → metadata). Kept small so new media appears near-instantly.
            _mediaStoreChanges.debounce(200).collect {
                val added = incrementalScan()
                if (added > 0) _scanResults.tryEmit(added)
            }
        }
    }

    /**
     * Rescan on foreground return. While the app is backgrounded (e.g. you're in the camera app),
     * Android may freeze our process and drop/delay the ContentObserver callback, so media captured
     * while away can otherwise take seconds to appear. Running an incremental scan the moment we
     * come back guarantees new photos/videos show up right away instead of waiting on the observer.
     */
    fun refreshNow() {
        scope.launch {
            val added = incrementalScan()
            if (added > 0) _scanResults.tryEmit(added)
        }
    }

    // ---- Albums ---------------------------------------------------------------

    fun deriveAlbums(items: List<MediaItem>): List<Album> =
        items.groupBy { it.bucketId }
            .map { (bucketId, group) ->
                val previews = group.sortedByDescending { it.dateTakenMs }.take(4)
                val newest = previews.first()
                Album(
                    bucketId = bucketId,
                    name = group.first().bucketName.ifBlank { "Unknown" },
                    itemCount = group.size,
                    coverUri = newest.uri,
                    coverIsVideo = newest.isVideo,
                    previewItems = previews,
                    relativePath = newest.relativePath,
                    kind = classifyAlbum(newest.relativePath, newest.bucketName),
                )
            }
            .sortedWith(compareBy({ it.kind.ordinal }, { -it.itemCount }))

    // ---- Scoped-storage consent intents --------------------------------------

    /** Move N items to the OS trash (recoverable) — caller must launch the IntentSender. */
    fun createTrashRequest(uris: List<Uri>): IntentSender =
        MediaStore.createTrashRequest(resolver, uris, true).intentSender

    /** Permanently delete N items — caller launches the IntentSender for consent. */
    fun createDeleteRequest(uris: List<Uri>): IntentSender =
        MediaStore.createDeleteRequest(resolver, uris).intentSender

    /** Request write access (for overwrite-save / move-out of secure). */
    fun createWriteRequest(uris: List<Uri>): IntentSender =
        MediaStore.createWriteRequest(resolver, uris).intentSender

    /** Drop cache rows after a confirmed delete so the grid updates instantly. */
    suspend fun dropCached(ids: List<Long>) = withContext(Dispatchers.IO) {
        mediaDao.deleteByIds(ids)
    }

    /** Mirror an accepted MediaStore rename/metadata change into Room so visible lists update immediately. */
    suspend fun updateCachedItemMetadata(id: Long, displayName: String, dateTakenMs: Long?) = withContext(Dispatchers.IO) {
        prefs.edit().apply {
            putString("${id}_name", displayName)
            if (dateTakenMs != null) putLong("${id}_date", dateTakenMs)
        }.apply()
        
        if (dateTakenMs != null) {
            mediaDao.updateMetadata(id, displayName, dateTakenMs, dateTakenMs / 1000)
        } else {
            mediaDao.updateDisplayName(id, displayName)
        }
    }

    // ---- Rename / Move (scoped-storage aware) ---------------------------------

    /** Build a new DISPLAY_NAME from a user-supplied base name, preserving the extension. */
    fun renamedDisplayName(item: MediaItem, newBaseName: String): String {
        val ext = item.displayName.substringAfterLast('.', "")
        val base = newBaseName.trim().ifBlank { item.displayName.substringBeforeLast('.') }
        return if (ext.isBlank()) base else "$base.$ext"
    }

    /** Edit EXIF metadata of a MediaStore item. Non-owned media needs a user consent grant. */
    fun tryEditExif(uri: Uri, newDateMs: Long?, newLat: Double?, newLng: Double?): WriteResult =
        try {
            android.util.Log.d("GorillaGallery", "tryEditExif: newDateMs=$newDateMs newLat=$newLat newLng=$newLng uri=$uri")
            var oldDateTaken: Long? = null
            var oldDateModified: Long? = null
            resolver.query(uri, arrayOf(MediaStore.MediaColumns.DATE_TAKEN, MediaStore.MediaColumns.DATE_MODIFIED), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val dtIdx = c.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
                    val dmIdx = c.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                    if (dtIdx >= 0 && !c.isNull(dtIdx)) oldDateTaken = c.getLong(dtIdx)
                    if (dmIdx >= 0 && !c.isNull(dmIdx)) oldDateModified = c.getLong(dmIdx)
                }
            }

            val ext = resolver.getType(uri)?.let { android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(it) } ?: "jpg"
            val tempFile = java.io.File.createTempFile("exif_edit_", ".$ext", context.cacheDir)
            
            // 1. Copy MediaStore file to temp file
            resolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            // 2. Edit EXIF on temp file
            val exif = androidx.exifinterface.media.ExifInterface(tempFile.absolutePath)
            
            if (newLat != null && newLng != null) {
                exif.setLatLong(newLat, newLng)
            }
            
            if (newDateMs != null) {
                val cal = java.util.Calendar.getInstance()
                cal.timeInMillis = newDateMs
                val format = java.text.SimpleDateFormat("yyyy:MM:dd HH:mm:ss", java.util.Locale.US)
                val dateStr = format.format(cal.time)
                exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME, dateStr)
                exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME_ORIGINAL, dateStr)
                exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME_DIGITIZED, dateStr)
            }
            
            try {
                exif.saveAttributes()
                
                // Verify it actually saved
                val verifyExif = androidx.exifinterface.media.ExifInterface(tempFile.absolutePath)
                val verifyLatLong = FloatArray(2)
                val success = verifyExif.getLatLong(verifyLatLong)
                android.util.Log.d("GorillaGallery", "EXIF saved. Verification hasLoc=$success")
                
                // 3. Write temp file back to MediaStore
                resolver.openOutputStream(uri, "wt")?.use { output ->
                    tempFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
            } catch (e: java.io.IOException) {
                android.util.Log.w("GorillaGallery", "EXIF save unsupported", e)
            } finally {
                tempFile.delete()
            }
            
            // Also update MediaStore database so it shows up correctly in queries
            val finalDateTaken = newDateMs ?: oldDateTaken
            val finalDateModified = if (newDateMs != null) newDateMs / 1000 else oldDateModified

            if (finalDateTaken != null || finalDateModified != null) {
                applyValues(uri, ContentValues().apply {
                    if (finalDateTaken != null) put(MediaStore.MediaColumns.DATE_TAKEN, finalDateTaken)
                    if (finalDateModified != null) put(MediaStore.MediaColumns.DATE_MODIFIED, finalDateModified)
                })
            } else {
                WriteResult.Ok
            }
        } catch (e: RecoverableSecurityException) {
            WriteResult.NeedsConsent(e.userAction.actionIntent.intentSender)
        } catch (e: SecurityException) {
            android.util.Log.e("GorillaGallery", "SecurityException during edit", e)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                WriteResult.NeedsConsent(createWriteRequest(listOf(uri)))
            } else {
                WriteResult.Error(e.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            android.util.Log.e("GorillaGallery", "Exception during edit", e)
            WriteResult.Error(e.message ?: "Unknown error")
        }

    /** Rename a MediaStore item in place. Non-owned media needs a user consent grant. */
    fun tryRename(uri: Uri, newDisplayName: String): WriteResult =
        applyValues(uri, ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, newDisplayName)
        })

    /** Move a MediaStore item into [targetRelativePath] (e.g. "Pictures/Trips/"). */
    fun tryMove(uri: Uri, targetRelativePath: String): WriteResult =
        applyValues(uri, ContentValues().apply {
            put(MediaStore.MediaColumns.RELATIVE_PATH, normalizePath(targetRelativePath))
        })

    /** Distinct device folders (bucket) as (display name, relative path) for a folder picker. */
    fun foldersFrom(items: List<MediaItem>): List<Pair<String, String>> =
        items.asSequence()
            .filter { !it.isSecured && it.relativePath.isNotBlank() }
            .map { it.bucketName.ifBlank { "Unknown" } to normalizePath(it.relativePath) }
            .distinctBy { it.second }
            .sortedBy { it.first.lowercase() }
            .toList()

    private fun applyValues(uri: Uri, values: ContentValues): WriteResult =
        try {
            resolver.update(uri, values, null, null)
            WriteResult.Ok
        } catch (e: RecoverableSecurityException) {
            WriteResult.NeedsConsent(e.userAction.actionIntent.intentSender)
        } catch (e: SecurityException) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                WriteResult.NeedsConsent(createWriteRequest(listOf(uri)))
            } else {
                WriteResult.Error(e.message ?: "operation failed")
            }
        } catch (e: Exception) {
            WriteResult.Error(e.message ?: "operation failed")
        }

    private fun normalizePath(path: String): String =
        path.trim('/').let { if (it.isEmpty()) it else "$it/" }

    companion object {
        // How many most-recently-modified rows the incremental scan reads. Comfortably covers any
        // realistic single-capture/burst; larger bulk imports trigger a full-scan fallback.
        private const val RECENT_WINDOW = 200

        private val MEDIA_COLLECTION: Uri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        private val MEDIA_PROJECTION = arrayOf(
            FileColumns._ID, FileColumns.DISPLAY_NAME, FileColumns.MIME_TYPE,
            FileColumns.SIZE, FileColumns.DATE_ADDED, FileColumns.DATE_MODIFIED,
            FileColumns.DATE_TAKEN, FileColumns.WIDTH, FileColumns.HEIGHT,
            FileColumns.BUCKET_ID, FileColumns.BUCKET_DISPLAY_NAME,
            FileColumns.RELATIVE_PATH, FileColumns.ORIENTATION,
            FileColumns.DURATION, FileColumns.MEDIA_TYPE,
        )
        private const val MEDIA_SELECTION = "${FileColumns.MEDIA_TYPE} IN (?, ?)"
        private val MEDIA_ARGS = arrayOf(
            FileColumns.MEDIA_TYPE_IMAGE.toString(),
            FileColumns.MEDIA_TYPE_VIDEO.toString(),
        )
    }
}

/** Outcome of a scoped-storage write that may require a one-tap system consent grant. */
sealed interface WriteResult {
    object Ok : WriteResult
    data class NeedsConsent(val sender: IntentSender) : WriteResult
    data class Error(val message: String) : WriteResult
}
