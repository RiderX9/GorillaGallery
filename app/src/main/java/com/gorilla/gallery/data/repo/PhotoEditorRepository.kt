package com.gorilla.gallery.data.repo

import android.content.Context
import android.content.IntentSender
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.gorilla.gallery.data.model.EditMatrix
import com.gorilla.gallery.data.model.EditState
import com.gorilla.gallery.data.model.MediaItem
import com.gorilla.gallery.data.model.MediaType
import com.gorilla.gallery.data.model.PhotoExif
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

/**
 * Photo editor pipeline. The live preview applies the colour matrix on the GPU in Compose;
 * this repository only does the expensive full-resolution bake on save. Pure Bitmap +
 * ColorMatrixColorFilter — no RenderScript (removed/deprecated on targetSdk 35).
 */
class PhotoEditorRepository(
    private val context: Context,
    private val geocoderRepository: GeocoderRepository,
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("metadata_overrides", Context.MODE_PRIVATE)

    fun saveLocationOverride(id: Long, lat: Double?, lng: Double?) {
        if (lat != null && lng != null) {
            prefs.edit()
                .putFloat("${id}_lat", lat.toFloat())
                .putFloat("${id}_lng", lng.toFloat())
                .apply()
        } else {
            prefs.edit()
                .remove("${id}_lat")
                .remove("${id}_lng")
                .apply()
        }
    }

    private val maxDim = 4096 // guard against OOM on very large photos

    /** Decode (EXIF-corrected, downsampled), then rotate → crop → colour-matrix bake. */
    suspend fun render(uri: Uri, edit: EditState): Bitmap = withContext(Dispatchers.Default) {
        val src = decodeSoftware(uri)
        val rotated = if (edit.rotationDeg % 360f != 0f) {
            Bitmap.createBitmap(src, 0, 0, src.width, src.height,
                Matrix().apply { postRotate(edit.rotationDeg) }, true)
                .also { if (it != src) src.recycle() }
        } else src

        val l = (edit.cropLeft * rotated.width).roundToInt().coerceIn(0, rotated.width - 1)
        val t = (edit.cropTop * rotated.height).roundToInt().coerceIn(0, rotated.height - 1)
        val r = (edit.cropRight * rotated.width).roundToInt().coerceIn(l + 1, rotated.width)
        val b = (edit.cropBottom * rotated.height).roundToInt().coerceIn(t + 1, rotated.height)
        val cropped = if (l == 0 && t == 0 && r == rotated.width && b == rotated.height) {
            rotated
        } else {
            Bitmap.createBitmap(rotated, l, t, r - l, b - t).also { if (it != rotated) rotated.recycle() }
        }

        val out = Bitmap.createBitmap(cropped.width, cropped.height, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(
            cropped, 0f, 0f,
            Paint().apply { colorFilter = ColorMatrixColorFilter(EditMatrix.combined(edit).array) },
        )
        if (cropped != out) cropped.recycle()
        out
    }

    fun decodeSoftware(uri: Uri): Bitmap {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = false
            val w = info.size.width
            val h = info.size.height
            val longest = maxOf(w, h)
            if (longest > maxDim) {
                val scale = maxDim.toFloat() / longest
                decoder.setTargetSize((w * scale).roundToInt(), (h * scale).roundToInt())
            }
        }
    }

    /** Save as a NEW MediaStore image (app-owned, no consent). Returns the new uri. */
    suspend fun saveCopy(bitmap: Bitmap, baseName: String): Uri = withContext(Dispatchers.IO) {
        val tmp = File(context.cacheDir, "edit_${System.currentTimeMillis()}.jpg")
        tmp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        val name = "${baseName.substringBeforeLast('.')}_edited_${System.currentTimeMillis()}.jpg"
        val uri = MediaIo.insertFromFile(
            context = context,
            source = tmp,
            displayName = name,
            mimeType = "image/jpeg",
            type = MediaType.IMAGE,
            relativePath = "Pictures/GorillaGallery/",
        )
        tmp.delete()
        uri
    }

    /** Build a write-consent intent to overwrite an existing (non-owned) photo. */
    fun createOverwriteRequest(uri: Uri): IntentSender =
        MediaStore.createWriteRequest(context.contentResolver, listOf(uri)).intentSender

    /** Write back over the original after consent was granted. */
    suspend fun overwrite(uri: Uri, bitmap: Bitmap) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri, "wt")?.use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)
        } ?: error("Cannot open $uri for overwrite")
    }

    /** Read EXIF for the info sheet (images only; videos return empty). */
    suspend fun readExif(item: MediaItem): PhotoExif = withContext(Dispatchers.IO) {
        if (item.isVideo) {
            return@withContext runCatching {
                val extractor = android.media.MediaExtractor()
                extractor.setDataSource(context, item.uri, null)
                var transfer = -1
                var standard = -1
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(android.media.MediaFormat.KEY_MIME)
                    if (mime?.startsWith("video/") == true) {
                        transfer = if (format.containsKey(android.media.MediaFormat.KEY_COLOR_TRANSFER)) format.getInteger(android.media.MediaFormat.KEY_COLOR_TRANSFER) else -1
                        standard = if (format.containsKey(android.media.MediaFormat.KEY_COLOR_STANDARD)) format.getInteger(android.media.MediaFormat.KEY_COLOR_STANDARD) else -1
                        break
                    }
                }
                
                // Color transfer: 6 = ST2084 (HDR10), 7 = HLG
                // Standard: 6 = BT2020
                val badge = when {
                    transfer == 6 -> "HDR10"
                    transfer == 7 -> "HLG"
                    standard == 6 -> "BT.2020" // HDR Wide color
                    else -> "SDR"
                }
                
                extractor.release()
                PhotoExif(profileBadge = badge)
            }.getOrDefault(PhotoExif(profileBadge = "SDR"))
        }
        runCatching {
            context.contentResolver.openFileDescriptor(item.uri, "r")?.use { pfd ->
                val exif = ExifInterface(pfd.fileDescriptor)
                
                var lat: Double? = null
                var lon: Double? = null
                
                if (prefs.contains("${item.id}_lat")) {
                    lat = prefs.getFloat("${item.id}_lat", 0f).toDouble()
                    lon = prefs.getFloat("${item.id}_lng", 0f).toDouble()
                } else {
                    val latLong = FloatArray(2)
                    if (exif.getLatLong(latLong)) {
                        lat = latLong[0].toDouble()
                        lon = latLong[1].toDouble()
                    }
                }
                
                val colorSpaceInt = exif.getAttributeInt(ExifInterface.TAG_COLOR_SPACE, -1)
                val profile = when (colorSpaceInt) {
                    ExifInterface.COLOR_SPACE_S_RGB -> "sRGB"
                    ExifInterface.COLOR_SPACE_UNCALIBRATED -> "Display P3"
                    else -> "sRGB"
                }
                
                android.util.Log.d("GorillaGallery", "readExif: lat=$lat lon=$lon")
                val locationName = if (lat != null && lon != null) {
                    val name = geocoderRepository.getLocality(lat, lon)
                    android.util.Log.d("GorillaGallery", "readExif: locationName=$name")
                    name ?: "$lat, $lon"
                } else null
                PhotoExif(
                    cameraMake = exif.getAttribute(ExifInterface.TAG_MAKE),
                    cameraModel = exif.getAttribute(ExifInterface.TAG_MODEL),
                    lens = exif.getAttribute(ExifInterface.TAG_LENS_MODEL),
                    aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.let { "f/$it" },
                    shutter = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.let { formatShutter(it) },
                    iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)?.let { "ISO $it" },
                    focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.let { formatFocal(it) },
                    latitude = lat,
                    longitude = lon,
                    locationName = locationName,
                    profileBadge = profile
                )
            } ?: PhotoExif()
        }.getOrDefault(PhotoExif())
    }

    private fun formatShutter(raw: String): String {
        val v = raw.toFloatOrNull() ?: return raw
        return if (v >= 1f) "${v.roundToInt()} s" else "1/${(1f / v).roundToInt()} s"
    }

    private fun formatFocal(raw: String): String {
        // EXIF focal length is often a rational "a/b".
        val v = if ('/' in raw) {
            val (a, b) = raw.split('/').map { it.toFloatOrNull() ?: 0f }
            if (b != 0f) a / b else 0f
        } else raw.toFloatOrNull() ?: return raw
        return "${v.roundToInt()} mm"
    }
}
