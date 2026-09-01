package com.gorilla.gallery.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.Folder

import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/** Top-level navigation destinations shown in the GlassNavigationBar. */
enum class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Timeline("timeline", "Photos", Icons.Rounded.Photo),
    Albums("albums", "Albums", Icons.Rounded.Folder),
    Search("search", "Search", Icons.Rounded.Search),
    Settings("settings", "Settings", Icons.Rounded.Settings);

    companion object {
        val bottomBar = listOf(Timeline, Albums, Settings)
        fun fromRoute(route: String?): Destination {
            if (route == null) return Timeline
            val topLevel = entries.firstOrNull { route.startsWith(it.route) }
            if (topLevel != null) return topLevel
            if (route.startsWith("personDetail") || route.startsWith("seeAllTrips")) return Search
            
            // All non-top-level routes (like "album/123", "photos", etc.) belong to the Albums tab
            val isAlbumsSubroute = route.startsWith("album") || route in setOf(
                "photos", "videos", "selfies", "screenshots", "edited", 
                "favorites", "secure", "raw", "panoramas", "trash"
            )
            return if (isAlbumsSubroute) Albums else Timeline
        }
    }
}

/** Non-tab routes hosted in the same NavHost. */
object Routes {
    const val AlbumDetail = "album"          // album/{bucketId}
    const val Photos = "photos"
    const val Videos = "videos"
    const val Selfies = "selfies"
    const val Screenshots = "screenshots"
    const val Edited = "edited"
    const val Favorites = "favorites"
    const val SecureFolder = "secure"
    const val Trash = "trash"
    const val Raw = "raw"
    const val Panoramas = "panoramas"
    const val PersonDetail = "personDetail"
    fun albumDetail(bucketId: Long) = "album/$bucketId"
}
