package com.gorilla.gallery.data.repo

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.gorilla.gallery.data.model.MediaType
import java.io.File

/** Shared byte-shuffling between content:// URIs, internal files and fresh MediaStore inserts. */
object MediaIo {

    /** Copy the full contents of [src] (a content:// or file:// uri) into [dest]. */
    fun copyUriToFile(resolver: ContentResolver, src: Uri, dest: File) {
        dest.parentFile?.mkdirs()
        resolver.openInputStream(src)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Cannot open $src")
    }

    /**
     * Insert a brand-new media file the app owns (no consent intent needed) and stream
     * [source] file's bytes into it. Used by Trash-restore and Secure-folder move-out and
     * the editor's "save copy". Returns the new content uri.
     */
    fun insertFromFile(
        context: Context,
        source: File,
        displayName: String,
        mimeType: String,
        type: MediaType,
        relativePath: String,
        dateTakenMs: Long? = null,
    ): Uri {
        val resolver = context.contentResolver
        val collection = if (type == MediaType.VIDEO) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }
        val safePath = relativePath.ifBlank {
            if (type == MediaType.VIDEO) "Movies/" else "Pictures/"
        }.let { if (it.endsWith("/")) it else "$it/" }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, safePath)
            dateTakenMs?.takeIf { it > 0L }?.let {
                put(MediaStore.MediaColumns.DATE_TAKEN, it)
                put(MediaStore.MediaColumns.DATE_ADDED, it / 1000)
                put(MediaStore.MediaColumns.DATE_MODIFIED, it / 1000)
            }
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        var uri: Uri? = null
        try {
            uri = resolver.insert(collection, values)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback if the OS rejects the original relative path (e.g. Download/ for images)
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, if (type == MediaType.VIDEO) "Movies/Restored/" else "Pictures/Restored/")
            try {
                uri = resolver.insert(collection, values)
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
        if (uri == null) error("MediaStore insert failed")
        
        resolver.openOutputStream(uri)?.use { out ->
            source.inputStream().use { it.copyTo(out) }
        } ?: error("Cannot open output for $uri")
        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        dateTakenMs?.takeIf { it > 0L }?.let {
            values.put(MediaStore.MediaColumns.DATE_TAKEN, it)
            values.put(MediaStore.MediaColumns.DATE_ADDED, it / 1000)
            values.put(MediaStore.MediaColumns.DATE_MODIFIED, it / 1000)
        }
        resolver.update(uri, values, null, null)
        return uri
    }

    fun extOf(mimeType: String, fallback: String = "jpg"): String =
        when (mimeType.substringAfterLast('/').lowercase()) {
            "jpeg" -> "jpg"
            "" -> fallback
            else -> mimeType.substringAfterLast('/').lowercase()
        }
}
