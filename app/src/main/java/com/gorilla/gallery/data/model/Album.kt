package com.gorilla.gallery.data.model

import android.net.Uri

/** A device folder surfaced as an album, derived from MediaStore BUCKET grouping. */
data class Album(
    val bucketId: Long,
    val name: String,
    val itemCount: Int,
    val coverUri: Uri?,
    val coverIsVideo: Boolean,
    val previewItems: List<MediaItem>,
    val relativePath: String,
    val kind: AlbumKind,
)

/** Recognised special folders get custom icons/ordering; everything else is GENERIC. */
enum class AlbumKind { CAMERA, SCREENSHOTS, DOWNLOAD, WHATSAPP, GENERIC }

/** Classify by relative path / bucket name — never by bucketId (varies per device). */
fun classifyAlbum(relativePath: String, bucketName: String): AlbumKind {
    val p = relativePath.lowercase()
    val b = bucketName.lowercase()
    return when {
        "dcim/camera" in p || b == "camera" -> AlbumKind.CAMERA
        "screenshots" in p || b == "screenshots" -> AlbumKind.SCREENSHOTS
        p.startsWith("download") || b == "download" || b == "downloads" -> AlbumKind.DOWNLOAD
        "whatsapp" in p || "whatsapp" in b -> AlbumKind.WHATSAPP
        else -> AlbumKind.GENERIC
    }
}
