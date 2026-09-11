package com.gorilla.gallery.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.compositeOver

/**
 * Semantic, theme-aware color tokens. [DesignTokens] keeps the non-color design
 * constants (radii, blur tiers, motion); the colors that used to live there as a single
 * hardcoded dark set are now resolved per-theme through [LocalAppColors] so Light mode
 * gets a deliberate palette instead of dark values bleeding through.
 *
 * Most copy already adapts via `MaterialTheme.colorScheme`; these tokens cover the
 * surfaces/borders/tints that are drawn directly (glass fills, chips, dividers, the
 * glass tint + border inside [LiquidGlassSurface]).
 */
@Immutable
data class AppColors(
    val isDark: Boolean,
    val isAmoled: Boolean,
    val bgBase: Color,
    val bgSurface: Color,
    /** Elevated/selected glass fill (e.g. a selected tab chip). */
    val bgGlass: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    /** Hairline glass border — white-ish on dark, black-ish on light. */
    val borderGlass: Color,
    /** Base color the glass frost tint is built from (white on dark, near-black on light). */
    val glassTint: Color,
    /** Multiplier applied to accent glow/bloom strength — toned down in light mode. */
    val glowScale: Float,
)

val DarkAppColors = AppColors(
    isDark = true,
    isAmoled = false,
    bgBase = Color(0xFF000000),
    bgSurface = Color(0xFF1C1C28),
    bgGlass = Color(0x26FFFFFF),     // white 15%
    textPrimary = Color(0xFFE8E8F0),
    textSecondary = Color(0xFF9999B0),
    textDisabled = Color(0xFF555570),
    borderGlass = Color.White.copy(alpha = 0.20f), // white 20%
    glassTint = GlassTintDark,
    glowScale = 1.0f,
)

val LightAppColors = AppColors(
    isDark = false,
    isAmoled = false,
    bgBase = Color(0xFFF2F2F7),
    bgSurface = Color(0xFFFFFFFF),
    bgGlass = Color(0xFFFFFFFF).copy(alpha = 0.65f),
    textPrimary = Color(0xFF1A1A24),
    textSecondary = Color(0xFF5C5C70),
    textDisabled = Color(0xFFAAAABC),
    borderGlass = Color.Black.copy(alpha = 0.08f),
    glassTint = GlassTintLight,
    glowScale = 0.15f,
)

/** AMOLED keeps the canvas black while retaining visible elevation for controls. */
val AmoledAppColors = DarkAppColors.copy(
    isAmoled = true,
    bgBase = Color.Black,
    bgSurface = Color(0xFF191A1C),
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

fun appColorsFrom(
    scheme: ColorScheme,
    isDark: Boolean,
    amoled: Boolean,
): AppColors {
    val base = if (amoled) Color.Black else scheme.background
    val glass = scheme.surfaceContainerHigh.copy(alpha = if (isDark) 0.58f else 0.72f)
    val surface = if (amoled) glass.compositeOver(Color.Black) else scheme.surfaceContainer

    return AppColors(
        isDark = isDark,
        isAmoled = amoled,
        bgBase = base,
        bgSurface = surface,
        bgGlass = glass,
        textPrimary = scheme.onBackground,
        textSecondary = scheme.onSurfaceVariant,
        textDisabled = scheme.onSurfaceVariant.copy(alpha = 0.5f),
        borderGlass = scheme.outlineVariant.copy(alpha = if (isDark) 0.58f else 0.48f),
        glassTint = scheme.surfaceTint,
        glowScale = if (isDark) 1f else 0.15f,
    )
}

/** Content rows stay visibly elevated above the true-black AMOLED background. */
fun AppColors.contentCardColor(): Color =
    if (isAmoled) bgGlass else bgSurface.copy(alpha = 0.94f)
