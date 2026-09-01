package com.gorilla.gallery.data.model

import android.net.Uri

enum class MediaType { IMAGE, VIDEO }

/**
 * One photo or video. Unified model so the timeline, albums, viewer and pager all
 * speak a single type. [uri] is built from the *typed* MediaStore collection
 * (Images/Video) so delete/write consent requests resolve correctly.
 *
 * Secured items live in app-internal storage and have [secureId] set + a file [uri]
 * (Uri.fromFile); they never appear in MediaStore.
 */
data class MediaItem(
    val id: Long,
    val uri: Uri,
    val type: MediaType,
    val displayName: String,
    val mimeType: String,
    val dateTakenMs: Long,        // grouping key for the timeline
    val dateAddedSec: Long,
    val dateModifiedSec: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val durationMs: Long,         // 0 for images
    val bucketId: Long,           // auto-album key
    val bucketName: String,
    val relativePath: String,
    val orientation: Int,
    val isFavorite: Boolean = false,
    val secureId: String? = null, // non-null when loaded from the Secure Folder
    val trashId: String? = null,  // non-null when loaded from the Trash
) {
    val isVideo: Boolean get() = type == MediaType.VIDEO
    val format: String get() = mimeType.substringAfterLast('/').uppercase()
    val isSecured: Boolean get() = secureId != null
    val isScreenshot: Boolean
        get() {
            val path = relativePath.lowercase()
            val bucket = bucketName.lowercase()
            val name = displayName.lowercase()
            return !isVideo && (
                "screenshots" in path ||
                    bucket == "screenshots" ||
                    name.startsWith("screenshot") ||
                    name.startsWith("screen_shot") ||
                    name.startsWith("screen shot")
                )
        }

    val isEdited: Boolean
        get() {
            val path = relativePath.lowercase()
            val name = displayName.lowercase()
            return "_edited_" in name
        }
}
