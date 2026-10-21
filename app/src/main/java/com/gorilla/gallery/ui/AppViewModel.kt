package com.gorilla.gallery.ui

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import android.app.WallpaperManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.request.ImageRequest
import com.gorilla.gallery.AppContainer
import com.gorilla.gallery.data.model.MediaItem
import com.gorilla.gallery.data.repo.WriteResult
import com.gorilla.gallery.data.settings.AppSettings
import com.gorilla.gallery.data.settings.DateGranularity
import com.gorilla.gallery.data.work.ImageLabelingWorker
import com.gorilla.gallery.ui.theme.DynamicColors
import com.gorilla.gallery.ui.theme.paletteColorsFrom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** A scoped-storage operation that needs the user's confirmation via a system dialog. */
sealed interface ConsentRequest {
    val sender: IntentSender
    data class Trash(override val sender: IntentSender, val mediaIds: List<Long>, val trashIds: List<String>) : ConsentRequest
    data class Delete(override val sender: IntentSender, val mediaIds: List<Long>) : ConsentRequest
    data class SecureIn(
        override val sender: IntentSender,
        val mediaIds: List<Long>,
        val secureIds: List<String>,
        val originalUris: List<Uri>,
    ) : ConsentRequest
    data class Overwrite(override val sender: IntentSender) : ConsentRequest
    /** A generic write grant (rename / move of non-owned media) re-running a held action. */
    data class Write(override val sender: IntentSender) : ConsentRequest
}

/**
 * Global app state + the single consent hub. Screens trigger media operations here; the
 * actual system consent dialogs are launched by [MainActivity], which reports the result
 * back through [onConsentResult]. Also owns the per-photo dynamic palette feeding the
 * reactive background and the ADAPTIVE accent.
 */
class AppViewModel(val container: AppContainer) : ViewModel() {

    private val settingsRepo = container.settingsRepository
    private val mediaRepo = container.mediaRepository
    private val favoritesRepo = container.favoritesRepository
    private val trashRepo = container.trashRepository
    private val secureRepo = container.secureFolderRepository
    private val editorRepo = container.photoEditorRepository

    val videoTextureViews = java.util.concurrent.ConcurrentHashMap<Long, android.view.TextureView>()
    val settings: StateFlow<AppSettings> =
        settingsRepo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun setGridColumns(count: Int) {
        viewModelScope.launch {
            settingsRepo.setGridColumns(count)
        }
    }

    var refreshTrigger by mutableStateOf(0)
        private set

    fun bumpRefresh() {
        refreshTrigger++
    }



    private val _resetToTop = MutableStateFlow(false)
    val resetToTop: StateFlow<Boolean> = _resetToTop.asStateFlow()

    fun setResetToTop(reset: Boolean) {
        _resetToTop.value = reset
    }

    private val _albumsExpanded = MutableStateFlow(false)
    val albumsExpanded: StateFlow<Boolean> = _albumsExpanded.asStateFlow()

    fun setAlbumsExpanded(expanded: Boolean) {
        _albumsExpanded.value = expanded
    }

    fun collapseAlbums() {
        _albumsExpanded.value = false
    }

    fun setGranularity(g: DateGranularity) {
        viewModelScope.launch { settingsRepo.setDateGranularity(g) }
        // "All" is the flat/ungrouped view and is the only grouping that jumps back to the top.
        if (g == DateGranularity.ALL) {
            setResetToTop(true)
        }
    }

    private val _dynamicColors = MutableStateFlow(DynamicColors())
    val dynamicColors: StateFlow<DynamicColors> = _dynamicColors.asStateFlow()

    /** "N new" toast from the silent incremental scan. */
    val mediaAdded: MutableSharedFlow<Int> = MutableSharedFlow(extraBufferCapacity = 4)
    val showSnackbar: MutableSharedFlow<String> = MutableSharedFlow(extraBufferCapacity = 4)
    private val _pendingConsent = MutableSharedFlow<ConsentRequest>(extraBufferCapacity = 4)
    val pendingConsent: MutableSharedFlow<ConsentRequest> = _pendingConsent

    /** Secure Folder unlocked state, mirrored from the repository. */
    val secureUnlocked: StateFlow<Boolean> = secureRepo.unlocked

    private var isDark: Boolean = true
    private var pendingOverwriteBitmap: Bitmap? = null

    init {
        viewModelScope.launch {
            mediaRepo.scanResults.collect { added ->
                mediaAdded.tryEmit(added)
                if (added > 0) {
                    val dm = container.context.resources.displayMetrics
                    val minDimPx = minOf(dm.widthPixels, dm.heightPixels)
                    val sizePx = ((minDimPx / 2) - 6).coerceAtLeast(1)
                    val targetSize = if (settings.value.highQualityThumbnails) sizePx * 2 else sizePx
                    val currentItems = mediaRepo.items.value
                    val toPrewarm = currentItems.take(added.coerceAtMost(48))
                    container.thumbnailRepository.prewarm(toPrewarm, targetSize, settings.value.highQualityThumbnails)
                }
            }
        }
        // Sweep expired trash on launch.
        viewModelScope.launch { trashRepo.emptyExpired() }
    }

    fun updateDarkTheme(dark: Boolean) { isDark = dark }

    // ---- Permission / scanning -----------------------------------------------

    private var started = false
    fun onPermissionResult(granted: Boolean) {
        if (!granted || started) return
        started = true
        viewModelScope.launch {
            com.gorilla.gallery.data.service.NewMediaJobService.scheduleJob(container.context)
            mediaRepo.scan()
            mediaRepo.startObserving()
            ImageLabelingWorker.enqueue(container.context)
        }
    }

    /** Called from MainActivity.onStart with the time spent backgrounded. */
    fun onAppResumed(gapMs: Long) {
        secureRepo.onAppResumed(gapMs)
        // Catch media captured while we were backgrounded (e.g. in the camera app), which the
        // ContentObserver may have missed while our process was frozen. Only after the first scan
        // has run, so cold start doesn't scan twice.
        if (started) mediaRepo.refreshNow()
    }

    // ---- Dynamic palette ------------------------------------------------------

    private val _focusedItem = MutableStateFlow<MediaItem?>(null)
    val focusedItem: StateFlow<MediaItem?> = _focusedItem.asStateFlow()

    /** Extract the current photo's palette → drives the reactive background + ADAPTIVE accent. */
    fun onPhotoFocused(item: MediaItem) {
        _focusedItem.value = item
        viewModelScope.launch(Dispatchers.IO) {
            val accent = settings.value.accent.resolve(isDark)
            val bmp = loadBitmap(item.uri)
            _dynamicColors.value = paletteColorsFrom(bmp, accent)
        }
    }

    /** Reset to the default muted fallback (e.g. when leaving the viewer). */
    fun resetPalette() {
        _dynamicColors.value = DynamicColors(accent = settings.value.accent.resolve(isDark))
    }

    private suspend fun loadBitmap(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val req = ImageRequest.Builder(container.context)
                .data(uri)
                .allowHardware(false)
                .size(240)
                .build()
            val result = container.imageLoader.execute(req)
            (result.drawable as? BitmapDrawable)?.bitmap
        }.getOrNull()
    }

    // ---- Favorites ------------------------------------------------------------

    fun toggleFavorite(item: MediaItem) {
        viewModelScope.launch { favoritesRepo.toggle(item) }
    }

    // ---- Trash / Delete / Secure (consent-driven) -----------------------------

    fun moveToTrash(items: List<MediaItem>) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            val useTrash = settings.value.moveToTrashByDefaultForDelete
            val canDirect = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && android.os.Environment.isExternalStorageManager())
            val skipToasts = settings.value.skipConfirmationsAndToasts
            
            if (useTrash) {
                if (canDirect) {
                    trashRepo.directTrash(items)
                    if (!skipToasts) showSnackbar.tryEmit("Moved to Trash")
                } else {
                    val prep = trashRepo.prepareTrash(items)
                    _pendingConsent.tryEmit(ConsentRequest.Trash(prep.sender, prep.mediaIds, prep.trashIds))
                }
            } else {
                if (canDirect) {
                    trashRepo.directDeleteForeverOriginals(items)
                    if (!skipToasts) showSnackbar.tryEmit("Deleted permanently")
                } else {
                    val prep = trashRepo.prepareDeleteForeverOriginals(items)
                    _pendingConsent.tryEmit(ConsentRequest.Trash(prep.sender, prep.mediaIds, prep.trashIds))
                }
            }
        }
    }

    fun moveToSecure(items: List<MediaItem>) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            val canDirect = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && android.os.Environment.isExternalStorageManager())
            val skipToasts = settings.value.skipConfirmationsAndToasts
            
            if (canDirect) {
                secureRepo.directMoveIn(items)
                if (!skipToasts) showSnackbar.tryEmit("Moved to Secure Folder")
            } else {
                val prep = secureRepo.prepareMoveIn(items)
                _pendingConsent.tryEmit(
                    ConsentRequest.SecureIn(
                        sender = prep.sender,
                        mediaIds = prep.mediaIds,
                        secureIds = prep.secureIds,
                        originalUris = prep.originalUris,
                    )
                )
            }
        }
    }

    fun restoreFromTrash(trashId: String) {
        viewModelScope.launch {
            trashRepo.restore(trashId)
            showSnackbar.tryEmit("Restored")
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            trashRepo.emptyAll()
            showSnackbar.tryEmit("Trash emptied")
        }
    }

    fun moveOutOfSecure(item: MediaItem) {
        val id = item.secureId ?: return
        viewModelScope.launch {
            secureRepo.moveOut(id)
            showSnackbar.tryEmit("Moved out of Secure Folder")
        }
    }
    
    // ---- Import ---------------------------------------------------------------
    
    fun importPhotos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val contentResolver = container.context.contentResolver
            val picturesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)
            val importDir = java.io.File(picturesDir, "GorillaGallery").apply { mkdirs() }
            
            var importedCount = 0
            for (uri in uris) {
                try {
                    val cursor = contentResolver.query(uri, null, null, null, null)
                    var displayName = "imported_${System.currentTimeMillis()}.jpg"
                    var originalDateMs: Long? = null
                    
                    if (cursor != null && cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            val name = cursor.getString(nameIndex)
                            if (!name.isNullOrEmpty()) displayName = name
                        }
                        
                        // Try to extract any date metadata the provider is willing to share
                        val dateTakenIdx = cursor.getColumnIndex("datetaken")
                        val dateModifiedIdx = cursor.getColumnIndex("date_modified")
                        
                        if (dateTakenIdx != -1 && !cursor.isNull(dateTakenIdx)) {
                            val dt = cursor.getLong(dateTakenIdx)
                            if (dt > 0) originalDateMs = dt
                        }
                        if (originalDateMs == null && dateModifiedIdx != -1 && !cursor.isNull(dateModifiedIdx)) {
                            val dm = cursor.getLong(dateModifiedIdx)
                            if (dm > 0) originalDateMs = dm * 1000L // date_modified is usually in seconds
                        }
                        cursor.close()
                    }
                    val importDirObj = java.io.File(importDir, ".temp")
                    importDirObj.mkdirs()
                    val tempFile = java.io.File(importDirObj, displayName)
                    
                    contentResolver.openInputStream(uri)?.use { input ->
                        java.io.FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    // If we didn't get an original date from the cursor, try to extract it from the file itself
                    if (originalDateMs == null || originalDateMs == 0L) {
                        try {
                            val lowerName = displayName.lowercase(java.util.Locale.US)
                            if (lowerName.endsWith(".mp4") || lowerName.endsWith(".mkv") || lowerName.endsWith(".webm") || lowerName.endsWith(".avi")) {
                                val mmr = android.media.MediaMetadataRetriever()
                                mmr.setDataSource(tempFile.absolutePath)
                                val dateStr = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DATE)
                                mmr.release()
                                if (dateStr != null) {
                                    val sdf = java.text.SimpleDateFormat("yyyyMMdd'T'HHmmss.SSS'Z'", java.util.Locale.US)
                                    sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                                    originalDateMs = sdf.parse(dateStr)?.time
                                }
                            } else {
                                val exif = androidx.exifinterface.media.ExifInterface(tempFile.absolutePath)
                                val dateTime = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME_ORIGINAL)
                                    ?: exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME)
                                if (dateTime != null) {
                                    val sdf = java.text.SimpleDateFormat("yyyy:MM:dd HH:mm:ss", java.util.Locale.US)
                                    originalDateMs = sdf.parse(dateTime)?.time
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    val mimeType = contentResolver.getType(uri) ?: if (displayName.lowercase(java.util.Locale.US).let { it.endsWith(".mp4") || it.endsWith(".mkv") || it.endsWith(".webm") || it.endsWith(".avi") }) "video/mp4" else "image/jpeg"
                    val isVideo = mimeType.startsWith("video/")
                    
                    // Atomically insert into MediaStore with correct dates so it never flashes at the top
                    com.gorilla.gallery.data.repo.MediaIo.insertFromFile(
                        context = container.context,
                        source = tempFile,
                        displayName = displayName,
                        mimeType = mimeType,
                        type = if (isVideo) com.gorilla.gallery.data.model.MediaType.VIDEO else com.gorilla.gallery.data.model.MediaType.IMAGE,
                        relativePath = "Pictures/GorillaGallery/",
                        dateTakenMs = originalDateMs
                    )
                    
                    tempFile.delete()
                    
                    importedCount++
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (importedCount > 0) {
                mediaRepo.refreshNow()
                showSnackbar.tryEmit("Imported $importedCount items")
            }
        }
    }

    fun deleteInternalItem(item: MediaItem) {
        viewModelScope.launch {
            when {
                item.trashId != null -> trashRepo.deleteForever(item.trashId)
                item.secureId != null -> secureRepo.deleteForever(item.secureId)
                else -> return@launch
            }
            if (!settings.value.skipConfirmationsAndToasts) showSnackbar.tryEmit("Deleted")
        }
    }

    // ---- Viewer file actions: rename / move / copy / wallpaper ----------------

    /** Held write retried after the user grants the system consent for non-owned media. */
    private var pendingWriteAction: (suspend () -> Unit)? = null

    /** Rename the file (extension preserved). Falls back to a consent grant when required. */
    fun rename(item: MediaItem, newBaseName: String) {
        if (newBaseName.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val newName = mediaRepo.renamedDisplayName(item, newBaseName)
            performWrite(
                okMsg = "Renamed",
                failMsg = "Rename failed",
                op = { mediaRepo.tryRename(item.uri, newName) },
                onSuccess = { mediaRepo.updateCachedItemMetadata(item.id, newName, null) },
            )
        }
    }

    /** Update EXIF metadata and rename the file. Falls back to a consent grant when required. */
    fun editMetadata(item: MediaItem, newBaseName: String, newDateMs: Long?, newLat: Double?, newLng: Double?, onMetadataUpdated: suspend () -> Unit = {}) {
        if (newBaseName.isBlank()) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val newName = if (newBaseName != item.displayName.substringBeforeLast('.')) {
                mediaRepo.renamedDisplayName(item, newBaseName)
            } else item.displayName
            
            performWrite(
                okMsg = "Metadata updated",
                failMsg = "Update failed",
                op = { 
                    // rename if needed
                    var result: com.gorilla.gallery.data.repo.WriteResult = com.gorilla.gallery.data.repo.WriteResult.Ok
                    if (newName != item.displayName) {
                        result = mediaRepo.tryRename(item.uri, newName)
                    }
                    
                    // edit EXIF
                    if (result is com.gorilla.gallery.data.repo.WriteResult.Ok && (newLat != null || newLng != null || newDateMs != null)) {
                        result = mediaRepo.tryEditExif(item.uri, newDateMs, newLat, newLng)
                    }
                    result
                },
                onSuccess = { 
                    if (newLat != null || newLng != null) {
                        container.photoEditorRepository.saveLocationOverride(item.id, newLat, newLng)
                    }
                    if (newName != item.displayName || newDateMs != null) {
                        mediaRepo.updateCachedItemMetadata(item.id, newName, newDateMs) 
                    }
                    onMetadataUpdated()
                },
            )
        }
    }

    /** Move the file into [targetRelativePath]. Falls back to a consent grant when required. */
    fun moveToAlbum(item: MediaItem, targetRelativePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            performWrite(
                okMsg = "Moved", 
                failMsg = "Move failed", 
                op = { mediaRepo.tryMove(item.uri, targetRelativePath) },
                onSuccess = { mediaRepo.refreshItem(item.id) }
            )
        }
    }

    /** Run a scoped write, surfacing a consent dialog and re-running on approval. */
    private suspend fun performWrite(
        okMsg: String,
        failMsg: String,
        op: () -> WriteResult,
        onSuccess: suspend () -> Unit = {},
    ) {
        when (val result = op()) {
            is WriteResult.Ok -> {
                onSuccess()
                showSnackbar.tryEmit(okMsg)
            }
            is WriteResult.NeedsConsent -> {
                pendingWriteAction = {
                    val retry = withContext(Dispatchers.IO) { op() }
                    if (retry is WriteResult.Ok) {
                        onSuccess()
                        showSnackbar.tryEmit(okMsg)
                    } else {
                        showSnackbar.tryEmit(failMsg)
                    }
                }
                _pendingConsent.tryEmit(ConsentRequest.Write(result.sender))
            }
            is WriteResult.Error -> {
                showSnackbar.tryEmit("$failMsg: ${result.message}")
            }
        }
    }

    /** Copy the media [Uri] to the system clipboard. */
    fun copyToClipboard(item: MediaItem) {
        val clipboard = container.context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newUri(container.context.contentResolver, item.displayName, item.uri)
        clipboard?.setPrimaryClip(clip)
        showSnackbar.tryEmit("Copied to clipboard")
    }

    /** Hand the image to Android's native wallpaper picker. */
    fun setAsWallpaper(item: MediaItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = container.context
            val ok = runCatching {
                val wallpaperDir = File(context.cacheDir, "wallpaper").apply { mkdirs() }
                val ext = item.displayName.substringAfterLast('.', "")
                    .ifBlank { if (item.mimeType.substringAfter('/').isNotBlank()) item.mimeType.substringAfter('/') else "jpg" }
                val photoFile = File(wallpaperDir, "wallpaper_${item.id}_${item.dateModifiedSec}.$ext")
                context.contentResolver.openInputStream(item.uri)?.use { input ->
                    photoFile.outputStream().use { output -> input.copyTo(output) }
                } ?: error("Cannot open ${item.uri}")
                val photoUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile,
                )

                try {
                    val intent = WallpaperManager.getInstance(context)
                        .getCropAndSetWallpaperIntent(photoUri)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } catch (e: IllegalArgumentException) {
                    val fallback = Intent(Intent.ACTION_ATTACH_DATA).apply {
                        setDataAndType(photoUri, "image/*")
                        putExtra("mimeType", "image/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(
                        Intent.createChooser(fallback, "Set as wallpaper")
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    )
                }
            }.isSuccess
            if (!ok) showSnackbar.tryEmit("Couldn't open wallpaper picker")
        }
    }

    /** Distinct device folders for the viewer's "Move to…" picker. */
    val deviceFolders: StateFlow<List<Pair<String, String>>> =
        mediaRepo.items
            .map { mediaRepo.foldersFrom(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun unlockSecure() = secureRepo.unlock()
    fun lockSecure() = secureRepo.lock()
    fun verifyPin(pin: String): Boolean = settingsRepo.verifyPin(pin, settings.value)

    /** Editor overwrite path: hold the bitmap and request write consent. */
    fun requestOverwrite(uri: Uri, bitmap: Bitmap, onDone: (() -> Unit)? = null) {
        pendingOverwriteBitmap = bitmap
        pendingOverwriteCallback = onDone
        _pendingConsent.tryEmit(ConsentRequest.Overwrite(editorRepo.createOverwriteRequest(uri)))
        pendingOverwriteUri = uri
    }
    private var pendingOverwriteUri: Uri? = null
    private var pendingOverwriteCallback: (() -> Unit)? = null

    /** Reported back by MainActivity after the system dialog resolves. */
    fun onConsentResult(request: ConsentRequest, approved: Boolean) {
        viewModelScope.launch {
            when (request) {
                is ConsentRequest.Trash ->
                    if (approved) {
                        trashRepo.onTrashConfirmed(request.mediaIds)
                        if (!settings.value.skipConfirmationsAndToasts) showSnackbar.tryEmit(if (request.trashIds.isEmpty()) "Deleted permanently" else "Moved to Trash")
                    } else trashRepo.rollback(request.trashIds)

                is ConsentRequest.Delete ->
                    if (approved) {
                        mediaRepo.dropCached(request.mediaIds)
                        if (!settings.value.skipConfirmationsAndToasts) showSnackbar.tryEmit("Deleted")
                    }

                is ConsentRequest.SecureIn ->
                    if (approved) {
                        secureRepo.onMoveInConfirmed(request.mediaIds, request.originalUris)
                        if (!settings.value.skipConfirmationsAndToasts) showSnackbar.tryEmit("Moved to Secure Folder")
                    } else secureRepo.rollback(request.secureIds)

                is ConsentRequest.Overwrite -> {
                    val uri = pendingOverwriteUri
                    val bmp = pendingOverwriteBitmap
                    val cb = pendingOverwriteCallback
                    if (approved && uri != null && bmp != null) {
                        editorRepo.overwrite(uri, bmp)
                        val id = uri.lastPathSegment?.toLongOrNull()
                        if (id != null) {
                            container.thumbnailRepository.putDirect(id, bmp, 0L)
                        }
                        val currentItem = mediaRepo.items.value.firstOrNull { it.uri == uri }
                        if (currentItem != null) {
                            container.thumbnailRepository.putDirect(currentItem.id, bmp, currentItem.dateModifiedSec)
                        }
                        container.mediaRepository.scan()
                        refreshTrigger++
                        
                        val modified = System.currentTimeMillis() / 1000
                        val req1 = coil.request.ImageRequest.Builder(container.context)
                            .data(uri)
                            .setParameter("date_modified", modified)
                            .memoryCacheKey("${uri}_${modified}_${refreshTrigger}")
                            .diskCacheKey("${uri}_${modified}_${refreshTrigger}")
                            .build()
                        coil.Coil.imageLoader(container.context).execute(req1)

                        if (currentItem != null) {
                            val req2 = coil.request.ImageRequest.Builder(container.context)
                                .data(uri)
                                .setParameter("date_modified", currentItem.dateModifiedSec)
                                .memoryCacheKey("${uri}_${currentItem.dateModifiedSec}_${refreshTrigger}")
                                .diskCacheKey("${uri}_${currentItem.dateModifiedSec}_${refreshTrigger}")
                                .build()
                            coil.Coil.imageLoader(container.context).execute(req2)
                        }
                        
                        kotlinx.coroutines.delay(150)
                        showSnackbar.tryEmit("Saved")
                    }
                    pendingOverwriteBitmap = null
                    pendingOverwriteUri = null
                    pendingOverwriteCallback = null
                    cb?.invoke()
                }

                is ConsentRequest.Write -> {
                    val action = pendingWriteAction
                    pendingWriteAction = null
                    if (approved) action?.invoke()
                }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory { container -> AppViewModel(container) }
    }
}
