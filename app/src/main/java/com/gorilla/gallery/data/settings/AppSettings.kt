package com.gorilla.gallery.data.settings

import androidx.compose.ui.graphics.Color
import com.gorilla.gallery.ui.theme.BlurIntensity
import com.gorilla.gallery.ui.theme.ThemeMode

/** How photos are grouped into date sections on the timeline. */
enum class DateGranularity(val label: String) { ALL("All"), DAY("Day"), MONTH("Month"), YEAR("Year") }

/**
 * Selectable accent presets — identical set to GorillaMusic so the two apps share a
 * palette. ADAPTIVE pulls the dominant colour from the currently viewed photo.
 */
enum class AccentChoice(val color: Color) {
    ADAPTIVE(Color.White),
    RED(Color(0xFFC62828)),
    CRIMSON(Color(0xFFEC5464)),
    ROSE(Color(0xFFD81B60)),
    PURPLE(Color(0xFF8E24AA)),
    DEEP_PURPLE(Color(0xFF5E35B1)),
    INDIGO(Color(0xFF3949AB)),
    BLUE(Color(0xFF1E88E5)),
    SKY_BLUE(Color(0xFF039BE5)),
    CYAN(Color(0xFF00ACC1)),
    ELECTRIC_CYAN(Color(0xFF00E5FF)),
    TEAL(Color(0xFF00897B)),
    GREEN(Color(0xFF43A047)),
    LIGHT_GREEN(Color(0xFF7CB342)),
    LIME(Color(0xFFC0CA33)),
    YELLOW(Color(0xFFFDD835)),
    AMBER(Color(0xFFFFB300)),
    ORANGE(Color(0xFFFB8C00)),
    DEEP_ORANGE(Color(0xFFF4511E)),
    BLUE_GREY(Color(0xFF546E7A));

    fun resolve(isDark: Boolean): Color = when (this) {
        ADAPTIVE -> if (isDark) Color.White else Color.Black
        else -> color
    }
}

/** All persisted settings as a single immutable snapshot. */
data class AppSettings(
    // Appearance
    val themeMode: ThemeMode = ThemeMode.AUTO,
    val accent: AccentChoice = AccentChoice.ADAPTIVE,
    val blurIntensity: BlurIntensity = BlurIntensity.MEDIUM,
    val liquidGlassIntensity: BlurIntensity = BlurIntensity.MEDIUM,
    val surfaceOpacity: Float = 0.4f,
    val gridColumns: Int = 3,
    val dateGranularity: DateGranularity = DateGranularity.ALL,
    val highQualityThumbnails: Boolean = false,

    // Privacy / Secure Folder
    val secureFolderEnabled: Boolean = false,
    val biometricUnlock: Boolean = true,
    val showSecureInAlbums: Boolean = true,
    val pinHash: String? = null,
    val pinSalt: String? = null,
) {
    val hasPin: Boolean get() = !pinHash.isNullOrBlank() && !pinSalt.isNullOrBlank()
}
