package com.gorilla.gallery.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.gorilla.gallery.ui.theme.DesignTokens
import com.gorilla.gallery.ui.theme.kernelSuGlassBackdrop
import com.gorilla.gallery.ui.theme.LocalAppColors
import com.gorilla.gallery.ui.theme.pressScale
import com.kyant.backdrop.Backdrop

/**
 * The shared back affordance: a 48.dp liquid-glass circle wrapping the back arrow, matching the
 * viewer/editor chrome so every back arrow in the app reads the same.
 */
@Composable
fun GlassBackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    GlassIconPill(
        icon = Icons.AutoMirrored.Rounded.ArrowBack,
        contentDescription = "Back",
        onClick = onBack,
        modifier = modifier,
    )
}

@Composable
fun GlassIconPill(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 44.dp,
    iconSize: androidx.compose.ui.unit.Dp = 22.dp,
    tint: Color = DesignTokens.TextPrimary,
    isSelected: Boolean = false,
    backdrop: Backdrop? = null,
    surfaceAlphaOverride: Float? = null,
) {
    val shape = RoundedCornerShape(percent = 50)
    val appColors = LocalAppColors.current
    val outlineBrush = Brush.linearGradient(
        colors = if (appColors.isDark) {
            listOf(Color.White.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.20f))
        } else {
            listOf(Color.White.copy(alpha = 0.90f), Color.Black.copy(alpha = 0.16f))
        }
    )
    val indicatorColor = if (appColors.isDark) {
        Color.White.copy(alpha = 0.1f)
    } else {
        Color.Black.copy(alpha = 0.05f)
    }
    Box(
        modifier = modifier
            .pressScale(scale = 0.88f)
            .kernelSuGlassBackdrop(
                backdrop = backdrop,
                shape = shape,
                surfaceAlphaOverride = surfaceAlphaOverride
            )
    ) {
        Box(
            Modifier
                .size(size)
                .clip(shape)
                .then(if (isSelected) Modifier.background(indicatorColor) else Modifier)
                .border(1.dp, outlineBrush, shape)
                .clickable(
                    interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}
