package com.gorilla.gallery.ui.theme

import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import com.kyant.backdrop.Backdrop

/**
 * True Liquid Glass material system replacing flat glassmorphism.
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    depth: GlassDepth = GlassDepth.MID,
    shape: RoundedCornerShape = CardShape,
    tint: Color = LocalDynamicColors.current.accent,
    surfaceColor: Color? = null,
    blurContent: Painter? = null,
    border: Boolean = true,
    tintAlphaOverride: Float? = null,
    saturationOverride: Float? = null,
    shadow: Boolean = true,
    enableLens: Boolean = true,
    useBackdrop: Boolean = true,
    backdrop: Backdrop? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val trueBackdrop = if (useBackdrop) backdrop ?: LocalLiquidGlassBackdrop.current else null
    val trueSurfaceColor = surfaceColor ?: liquidSurfaceColor(depth, LocalAppColors.current.isDark)
    TrueLiquidGlassSurface(
        modifier = modifier,
        depth = depth,
        shape = shape,
        tint = tint,
        surfaceColor = trueSurfaceColor,
        border = border,
        tintAlphaOverride = tintAlphaOverride,
        saturationOverride = saturationOverride,
        shadow = shadow,
        enableLens = enableLens,
        backdrop = trueBackdrop,
        content = content,
    )
}

@Composable
private fun TrueLiquidGlassSurface(
    modifier: Modifier = Modifier,
    depth: GlassDepth = GlassDepth.MID,
    shape: RoundedCornerShape = CardShape,
    tint: Color = LocalDynamicColors.current.accent,
    surfaceColor: Color? = null,
    border: Boolean = true,
    tintAlphaOverride: Float? = null,
    saturationOverride: Float? = null,
    shadow: Boolean = true,
    enableLens: Boolean = true,
    backdrop: Backdrop? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .trueLiquidGlass(
                shape = shape,
                depth = depth,
                tint = tint,
                surfaceColor = surfaceColor,
                tintAlphaOverride = tintAlphaOverride,
                saturationOverride = saturationOverride,
                interactive = false,
                shadow = shadow,
                border = border,
                enableLens = enableLens,
                backdrop = backdrop,
            )
            .clip(shape)
    ) {
        val boxScope = this

        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurface,
        ) {
            boxScope.content()
        }
    }
}

/**
 * Applies a real Gaussian [RenderEffect] blur to this layer (Android 12+). Radius near
 * zero falls back to no effect.
 *
 * IMPORTANT: this does NOT clip. Clipping the blurred layer to the exact visible bounds
 * is what produces the visible "ghost ring" halo — the [Shader.TileMode] kernel samples
 * past the edge and the hard clip slices it into a ring. Callers must instead let the
 * blurred content overflow a padded/oversized layer and clip to the final shape with a
 * separate `Modifier.clip(shape)` applied AFTER this blur (see [LiquidGlassSurface], which
 * oversizes the backdrop and clips on its outer Box). [TileMode.DECAL] also lets the
 * kernel fade to transparent past the source edge instead of smearing a clamped band.
 */
fun Modifier.renderEffectBlur(radius: Dp): Modifier = this.graphicsLayer {
    val px = radius.toPx()
    renderEffect = if (px <= 0.5f) {
        null
    } else {
        RenderEffect
            .createBlurEffect(px, px, Shader.TileMode.DECAL)
            .asComposeRenderEffect()
    }
    clip = false
}
