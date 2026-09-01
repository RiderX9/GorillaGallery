package com.gorilla.gallery.data.db

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gorilla.gallery.data.model.MediaItem
import com.gorilla.gallery.data.model.MediaType

/** Cached MediaStore row. The grid reads this so scrolling never blocks on a query. */
@Entity(tableName = "media")
data class MediaEntity(
    @PrimaryKey val id: Long,
    val type: String,            // MediaType.name
    val displayName: String,
    val mimeType: String,
    val dateTakenMs: Long,
    val dateAddedSec: Long,
    val dateModifiedSec: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val durationMs: Long,
    val bucketId: Long,
    val bucketName: String,
    val relativePath: String,
    val orientation: Int,
) {
    fun toItem(isFavorite: Boolean): MediaItem {
        val mediaType = runCatching { MediaType.valueOf(type) }.getOrDefault(MediaType.IMAGE)
        val collection = if (mediaType == MediaType.VIDEO) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        return MediaItem(
            id = id,
            uri = ContentUris.withAppendedId(collection, id),
            type = mediaType,
            displayName = displayName,
            mimeType = mimeType,
            dateTakenMs = dateTakenMs,
            dateAddedSec = dateAddedSec,
            dateModifiedSec = dateModifiedSec,
            sizeBytes = sizeBytes,
            width = width,
            height = height,
            durationMs = durationMs,
            bucketId = bucketId,
            bucketName = bucketName,
            relativePath = relativePath,
            orientation = orientation,
            isFavorite = isFavorite,
        )
    }

    companion object {
        fun from(i: MediaItem) = MediaEntity(
            id = i.id,
            type = i.type.name,
            displayName = i.displayName,
            mimeType = i.mimeType,
            dateTakenMs = i.dateTakenMs,
            dateAddedSec = i.dateAddedSec,
            dateModifiedSec = i.dateModifiedSec,
            sizeBytes = i.sizeBytes,
            width = i.width,
            height = i.height,
            durationMs = i.durationMs,
            bucketId = i.bucketId,
            bucketName = i.bucketName,
            relativePath = i.relativePath,
            orientation = i.orientation,
        )
    }
}

/** Source of truth for hearts — survives cache rebuilds independently of [MediaEntity]. */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val mediaId: Long,
)

/** A trashed item: bytes copied to internal storage, public copy deleted. 30-day expiry. */
@Entity(tableName = "trash")
data class TrashEntity(
    @PrimaryKey val id: String,            // uuid
    val internalPath: String,
    val displayName: String,
    val mimeType: String,
    val type: String,                      // MediaType.name
    val originalRelativePath: String,
    val dateTakenMs: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val durationMs: Long,
    val deletedAtMs: Long,
) {
    fun toItem(): MediaItem {
        val mediaType = runCatching { MediaType.valueOf(type) }.getOrDefault(MediaType.IMAGE)
        return MediaItem(
            id = id.hashCode().toLong(),
            uri = Uri.fromFile(java.io.File(internalPath)),
            type = mediaType,
            displayName = displayName,
            mimeType = mimeType,
            dateTakenMs = dateTakenMs,
            dateAddedSec = deletedAtMs / 1000,
            dateModifiedSec = deletedAtMs / 1000,
            sizeBytes = sizeBytes,
            width = width,
            height = height,
            durationMs = durationMs,
            bucketId = -2L,
            bucketName = "Trash",
            relativePath = originalRelativePath,
            orientation = 0,
            trashId = id,
        )
    }
}

/** A secured item: bytes in app-internal storage, never indexed by MediaStore. */
@Entity(tableName = "secure_items")
data class SecureItemEntity(
    @PrimaryKey val id: String,            // uuid
    val internalPath: String,
    val displayName: String,
    val mimeType: String,
    val type: String,                      // MediaType.name
    val originalRelativePath: String,
    val dateTakenMs: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val durationMs: Long,
    val securedAtMs: Long,
) {
    fun toItem(): MediaItem {
        val mediaType = runCatching { MediaType.valueOf(type) }.getOrDefault(MediaType.IMAGE)
        return MediaItem(
            id = id.hashCode().toLong(),
            uri = Uri.fromFile(java.io.File(internalPath)),
            type = mediaType,
            displayName = displayName,
            mimeType = mimeType,
            dateTakenMs = dateTakenMs,
            dateAddedSec = securedAtMs / 1000,
            dateModifiedSec = securedAtMs / 1000,
            sizeBytes = sizeBytes,
            width = width,
            height = height,
            durationMs = durationMs,
            bucketId = -3L,
            bucketName = "Secure Folder",
            relativePath = originalRelativePath,
            orientation = 0,
            secureId = id,
        )
    }
}
