package com.gorilla.gallery.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow

@Composable
fun Modifier.trueLiquidGlass(
    shape: Shape,
    depth: GlassDepth = GlassDepth.MID,
    backdrop: Backdrop? = LocalLiquidGlassContentBackdrop.current ?: LocalLiquidGlassBackdrop.current,
    tint: Color = LocalDynamicColors.current.accent,
    surfaceColor: Color? = null,
    tintAlphaOverride: Float? = null,
    saturationOverride: Float? = null,
    interactive: Boolean = true,
    shadow: Boolean = true,
    border: Boolean = true,
    enableLens: Boolean = true,
): Modifier {
    val isDark = LocalAppColors.current.isDark
    val intensity = LocalTrueLiquidGlassIntensity.current
    val resolvedSurfaceColor = surfaceColor ?: liquidSurfaceColor(depth, isDark, intensity)
    val highlight = if (interactive) rememberLiquidHighlight() else null
    val tintAlpha = tintAlphaOverride ?: liquidTintAlpha(depth, isDark, intensity)
    val saturation = saturationOverride ?: liquidChroma(depth, intensity)

    if (backdrop == null) {
        return this
            .then(fallbackTrueGlassSurface(shape, resolvedSurfaceColor, tint, tintAlpha, isDark, border))
            .then(highlight?.modifier ?: Modifier)
            .then(highlight?.gestureModifier ?: Modifier)
    }

    return this
        .drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(trueLiquidBlur(depth, intensity).toPx())
                if (enableLens) {
                    lens(
                        refractionHeight = liquidRefractionHeight(depth).toPx(),
                        refractionAmount = liquidRefractionAmount(depth).toPx(),
                        depthEffect = true,
                        chromaticAberration = true,
                    )
                }
            },
            layerBlock = if (interactive && highlight != null) {
                {
                    val p = highlight.progress
                    val scale = 1f + (4.dp.toPx() / size.minDimension.coerceAtLeast(1f)) * p
                    scaleX = scale
                    scaleY = scale
                }
            } else {
                null
            },
            highlight = {
                if (!border) {
                    null
                } else {
                    val reflectScale = when (intensity) {
                        BlurIntensity.LOW -> 0.3f
                        BlurIntensity.MEDIUM -> 1f
                        BlurIntensity.HIGH -> 1.5f
                    }
                    val baseAlpha = if (isDark) 0.48f else 0.24f
                    Highlight.Default.copy(alpha = (baseAlpha * reflectScale).coerceIn(0f, 1f))
                }
            },
            shadow = {
                if (!shadow) {
                    null
                } else {
                    Shadow(
                        radius = when (depth) {
                            GlassDepth.LOW -> 10.dp
                            GlassDepth.MID -> 14.dp
                            GlassDepth.HIGH -> 18.dp
                        },
                        alpha = if (isDark) 0.24f else 0.20f,
                    )
                }
            },
            innerShadow = {
                if (!border) {
                    null
                } else {
                    val reflectScale = when (intensity) {
                        BlurIntensity.LOW -> 0.3f
                        BlurIntensity.MEDIUM -> 1f
                        BlurIntensity.HIGH -> 1.5f
                    }
                    val baseAlpha = if (isDark) 0.28f else 0.24f
                    InnerShadow(
                        radius = when (depth) {
                            GlassDepth.LOW -> 6.dp
                            GlassDepth.MID -> 9.dp
                            GlassDepth.HIGH -> 12.dp
                        },
                        alpha = (baseAlpha * reflectScale).coerceIn(0f, 1f),
                    )
                }
            },
            onDrawSurface = {
                drawRect(tint.copy(alpha = tintAlpha), blendMode = BlendMode.Hue)
                drawRect(resolvedSurfaceColor)
                drawRect(
                    Color.White.copy(alpha = if (isDark) 0.014f else 0.006f),
                    blendMode = BlendMode.Plus,
                )
                if (saturation > 1f) {
                    drawRect(tint.copy(alpha = (saturation - 1f) * 0.018f), blendMode = BlendMode.Hue)
                }
            },
        )
        .then(highlight?.modifier ?: Modifier)
        .then(highlight?.gestureModifier ?: Modifier)
}

@Composable
fun liquidChromeSurfaceColor(depth: GlassDepth = GlassDepth.HIGH): Color {
    val isDark = LocalAppColors.current.isDark
    val intensity = LocalTrueLiquidGlassIntensity.current
    val alphaScale = when (intensity) {
        BlurIntensity.LOW -> if (isDark) 15.0f else 1.05f
        BlurIntensity.MEDIUM -> 1f
        BlurIntensity.HIGH -> 0.2f
    }
    return if (isDark) {
        Color.White.copy(
            alpha = (when (depth) {
                GlassDepth.LOW -> 0.08f
                GlassDepth.MID -> 0.13f
                GlassDepth.HIGH -> 0.17f
            } * alphaScale).coerceIn(0f, 1f),
        )
    } else {
        Color.White.copy(
            alpha = (when (depth) {
                GlassDepth.LOW -> 0.34f
                GlassDepth.MID -> 0.40f
                GlassDepth.HIGH -> 0.46f
            } * alphaScale).coerceIn(0f, 1f),
        )
    }
}

fun liquidSurfaceColor(depth: GlassDepth, isDark: Boolean, intensity: BlurIntensity = BlurIntensity.MEDIUM): Color {
    val alphaScale = when (intensity) {
        BlurIntensity.LOW -> if (isDark) 25.0f else 1.05f
        BlurIntensity.MEDIUM -> 1f
        BlurIntensity.HIGH -> 0.2f
    }
    return if (isDark) {
        Color.White.copy(
            alpha = (when (depth) {
                GlassDepth.LOW -> 0.040f
                GlassDepth.MID -> 0.052f
                GlassDepth.HIGH -> 0.066f
            } * alphaScale).coerceIn(0f, 1f),
        )
    } else {
        Color.White.copy(
            alpha = (when (depth) {
                GlassDepth.LOW -> 0.055f
                GlassDepth.MID -> 0.075f
                GlassDepth.HIGH -> 0.095f
            } * alphaScale).coerceIn(0f, 1f),
        )
    }
}

private fun trueLiquidBlur(depth: GlassDepth, intensity: BlurIntensity): Dp = when (intensity) {
    BlurIntensity.LOW -> 4.dp
    BlurIntensity.MEDIUM -> 5.dp
    BlurIntensity.HIGH -> 6.dp
}

private fun liquidRefractionHeight(depth: GlassDepth): Dp = 24.dp

private fun liquidRefractionAmount(depth: GlassDepth): Dp = 24.dp

private fun liquidTintAlpha(depth: GlassDepth, isDark: Boolean, intensity: BlurIntensity): Float {
    val tintScale = when (intensity) {
        BlurIntensity.LOW -> 0.4f
        BlurIntensity.MEDIUM -> 1f
        BlurIntensity.HIGH -> 1.6f
    }
    return if (isDark) {
        when (depth) {
            GlassDepth.LOW -> 0.034f
            GlassDepth.MID -> 0.052f
            GlassDepth.HIGH -> 0.070f
        }
    } else {
        when (depth) {
            GlassDepth.LOW -> 0.024f
            GlassDepth.MID -> 0.034f
            GlassDepth.HIGH -> 0.046f
        }
    } * tintScale
}

private fun liquidChroma(depth: GlassDepth, intensity: BlurIntensity): Float = 1.5f

internal fun fallbackTrueGlassSurface(
    shape: Shape,
    surfaceColor: Color,
    tint: Color,
    tintAlpha: Float,
    isDark: Boolean,
    border: Boolean,
): Modifier {
    val strokeColor = if (isDark) {
        Color.White.copy(alpha = 0.22f)
    } else {
        Color.Black.copy(alpha = 0.12f)
    }
    return Modifier
        .background(surfaceColor, shape)
        .background(tint.copy(alpha = tintAlpha), shape)
        .then(
            if (border) {
                Modifier.border(1.dp, strokeColor, shape)
            } else {
                Modifier
            },
        )
}

@Composable
fun Modifier.kernelSuGlassBackdrop(
    shape: Shape,
    backdrop: Backdrop? = LocalLiquidGlassContentBackdrop.current ?: LocalLiquidGlassBackdrop.current,
    isBlurEnabled: Boolean = true,
    enableLens: Boolean = true,
    tint: Color = LocalDynamicColors.current.accent,
    surfaceAlphaOverride: Float? = null
): Modifier {
    val isInLightTheme = !LocalAppColors.current.isDark
    val containerColor = if (isBlurEnabled) {
        androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer.copy(surfaceAlphaOverride ?: 0.4f)
    } else {
        androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer
    }
    
    val tintAlpha = if (isInLightTheme) 0.034f else 0.052f

    val intensity = LocalTrueLiquidGlassIntensity.current

    if (backdrop == null) {
        return this.background(containerColor, shape)
            .background(tint.copy(alpha = tintAlpha), shape)
    }

    return this
        .drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                if (isBlurEnabled) {
                    val scale = when (intensity) {
                        BlurIntensity.LOW -> 0.5f // 4dp
                        BlurIntensity.MEDIUM -> 0.625f // 5dp
                        BlurIntensity.HIGH -> 0.75f // 6dp
                    }
                    vibrancy()
                    blur(8f.dp.toPx() * scale)
                    if (enableLens) {
                        lens(24f.dp.toPx(), 24f.dp.toPx(), depthEffect = true, chromaticAberration = true)
                    }
                }
            },
            highlight = {
                Highlight.Default.copy(alpha = if (isBlurEnabled) 1f else 0f)
            },
            shadow = { null },
            onDrawSurface = {
                drawRect(containerColor)
                if (tint != Color.Unspecified) {
                    drawRect(tint.copy(alpha = tintAlpha), blendMode = BlendMode.Hue)
                }
            }
        )
}
