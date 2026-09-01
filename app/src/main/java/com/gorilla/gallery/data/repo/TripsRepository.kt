package com.gorilla.gallery.data.repo

import android.content.Context
import androidx.exifinterface.media.ExifInterface
import com.gorilla.gallery.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

data class TripCard(
    val locationName: String,
    val photoCount: Int,
    val dateRange: String,   // "Autumn 2024"
    val representativeUri: android.net.Uri? = null,
    val imagePaths: Set<String> = emptySet(),
)

class TripsRepository(
    private val context: Context,
    private val geocoderRepository: GeocoderRepository,
) {
    /** Build trip cards from a list of media items. Runs in parallel batches. */
    suspend fun buildTrips(items: List<MediaItem>): List<TripCard> = withContext(Dispatchers.IO) {
        val images = items.filter { !it.isVideo }.take(MAX_IMAGES_TO_SCAN)
        if (images.isEmpty()) return@withContext emptyList()

        // Extract GPS coordinates from EXIF in parallel
        val gpsData = coroutineScope {
            images.chunked(PARALLEL_BATCH_SIZE).flatMap { batch ->
                batch.map { item ->
                    async {
                        extractGps(item)
                    }
                }.awaitAll()
            }.filterNotNull()
        }

        if (gpsData.isEmpty()) return@withContext emptyList()

        // Group by approximate location (within ~0.1 degrees ≈ 11km)
        val groups = gpsData.groupBy { (lat, lon, _) ->
            "${(lat * 10).toInt()}_${(lon * 10).toInt()}"
        }

        // Build trip cards with location names
        groups.values.mapNotNull { group ->
            if (group.size < MIN_PHOTOS_FOR_TRIP) return@mapNotNull null
            val first = group.first()
            val locationName = geocoderRepository.getLocality(first.lat, first.lon)
                ?: return@mapNotNull null

            val timestamps = group.map { it.dateTakenMs }.sorted()
            val dateRange = formatTimestampRange(timestamps.first(), timestamps.last())

            TripCard(
                locationName = locationName,
                photoCount = group.size,
                dateRange = dateRange,
                representativeUri = first.uri,
                imagePaths = group.map { it.uri.toString() }.toSet(),
            )
        }.sortedByDescending { it.photoCount }
    }

    private data class GpsInfo(val lat: Double, val lon: Double, val dateTakenMs: Long, val uri: android.net.Uri)

    private fun extractGps(item: MediaItem): GpsInfo? {
        return try {
            context.contentResolver.openInputStream(item.uri)?.use { input ->
                val exif = ExifInterface(input)
                val latLong = FloatArray(2)
                if (exif.getLatLong(latLong)) {
                    GpsInfo(
                        lat = latLong[0].toDouble(),
                        lon = latLong[1].toDouble(),
                        dateTakenMs = item.dateTakenMs,
                        uri = item.uri,
                    )
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun formatTimestampRange(startMs: Long, endMs: Long): String {
        val start = java.time.Instant.ofEpochMilli(startMs).atZone(java.time.ZoneId.systemDefault())
        val end = java.time.Instant.ofEpochMilli(endMs).atZone(java.time.ZoneId.systemDefault())
        val season = when (start.monthValue) {
            in 3..5 -> "Spring"
            in 6..8 -> "Summer"
            in 9..11 -> "Autumn"
            else -> "Winter"
        }
        return "$season ${start.year}"
    }

    companion object {
        private const val MAX_IMAGES_TO_SCAN = 500
        private const val PARALLEL_BATCH_SIZE = 25
        private const val MIN_PHOTOS_FOR_TRIP = 3
    }
}
