package com.gorilla.gallery.ui.theme

import androidx.compose.runtime.getValue


import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

/** App theme mode (Settings → Appearance). */
enum class ThemeMode { AUTO, LIGHT, DARK, AMOLED }




/**
 * Root theme. Wires blur intensity into [LocalGlassTokens], the dynamic album-art
 * palette into [LocalDynamicColors] (animated on track change), and feeds the accent
 * into the Material color scheme. All three update at runtime as Settings change.
 */
@Composable
fun GorillaTheme(
    themeMode: ThemeMode,
    accent: Color,
    blurIntensity: BlurIntensity,
    liquidGlassIntensity: BlurIntensity = BlurIntensity.MEDIUM,
    surfaceOpacity: Float = 0.4f,
    dynamicColors: DynamicColors,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        ThemeMode.AUTO -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }

    // Smoothly animate the palette colors when the track changes.
    val animAccent by animateColorAsState(dynamicColors.accent, SpringSpecs.ColorSpring, label = "accent")
    val animPrimary by animateColorAsState(dynamicColors.artPrimary, SpringSpecs.ColorSpring, label = "artPrimary")
    val animSecondary by animateColorAsState(dynamicColors.artSecondary, SpringSpecs.ColorSpring, label = "artSecondary")
    val animBackground by animateColorAsState(dynamicColors.artBackground, SpringSpecs.ColorSpring, label = "artBg")

    val amoled = themeMode == ThemeMode.AMOLED

    val scheme = rememberDynamicColorScheme(
        seedColor = animAccent,
        isDark = isDark,
        isAmoled = amoled,
        style = PaletteStyle.TonalSpot,
    )


    val appColors = remember(scheme, isDark, amoled) {
        appColorsFrom(scheme, isDark, amoled)
    }

    val tokens = remember(blurIntensity, isDark) {
        GlassTokens(intensity = blurIntensity, isDark = isDark)
    }

    val animatedDynamic = DynamicColors(
        accent = animAccent,
        artPrimary = animPrimary,
        artSecondary = animSecondary,
        artBackground = animBackground,
    )

    CompositionLocalProvider(
        LocalTrueLiquidGlassEnabled provides true,
        LocalTrueLiquidGlassIntensity provides liquidGlassIntensity,
        LocalTrueLiquidGlassSurfaceOpacity provides surfaceOpacity,
        LocalGlassTokens provides tokens,
        LocalDynamicColors provides animatedDynamic,
        LocalAppColors provides appColors,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = GorillaTypography,
            shapes = GorillaShapes,
            content = content,
        )
    }
}

val LocalTrueLiquidGlassEnabled = staticCompositionLocalOf { false }
val LocalTrueLiquidGlassIntensity = staticCompositionLocalOf { BlurIntensity.MEDIUM }
val LocalTrueLiquidGlassSurfaceOpacity = staticCompositionLocalOf { 0.4f }
