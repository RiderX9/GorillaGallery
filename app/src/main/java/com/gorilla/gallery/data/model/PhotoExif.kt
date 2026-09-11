package com.gorilla.gallery.data.model

/** EXIF/camera metadata for the Photo Info sheet. Null fields are simply hidden. */
data class PhotoExif(
    val cameraMake: String? = null,
    val cameraModel: String? = null,
    val lens: String? = null,
    val aperture: String? = null,        // "f/1.8"
    val shutter: String? = null,         // "1/250 s"
    val iso: String? = null,             // "ISO 100"
    val focalLength: String? = null,     // "26 mm"
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null,    // "Kyoto, Japan"
    val profileBadge: String? = null,
) {
    val hasCamera: Boolean get() = cameraModel != null || aperture != null || iso != null
    val hasLocation: Boolean get() = latitude != null && longitude != null
}
